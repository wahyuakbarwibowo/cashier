import React, { useState, useEffect } from "react";
import DateTimePicker from '@react-native-community/datetimepicker';
import {
  View,
  StyleSheet,
  Alert,
  FlatList,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  Pressable,
} from "react-native";
import {
  Card,
  Text,
  TextInput,
  Button,
  IconButton,
  List,
  Chip,
  TouchableRipple,
  Divider,
  Portal,
  Modal as PaperModal,
  Surface,
  Avatar,
  HelperText, // Imported HelperText
} from "react-native-paper";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { getDB } from "../database/initDB";
import { addDigitalTransaction, updateDigitalTransaction, getRecentNumbers, DigitalTransaction } from "../database/pulsa";
import {
  getDigitalProducts,
  DigitalProductMaster,
  getDigitalCategories,
  DigitalCategory,
  getDistinctProvidersByCategory
} from "../database/digital_products";
import { useNavigation, useRoute } from "@react-navigation/native";
import { parseISODate, getCurrentDateISO, createTimestamp } from "../utils/dateUtils";
import { validateDigitalTransaction } from "../utils/validation";
import { ROUTES } from "../constants/routes";

// Helper to format number with thousand separators (Indonesian locale - dots)
const formatCurrency = (value: string): string => {
  // Remove all non-numeric characters except minus sign
  const numericValue = value.replace(/[^0-9]/g, "");
  if (!numericValue) return "";
  // Format with thousand separators (dots for Indonesian locale)
  return parseInt(numericValue, 10).toLocaleString("id-ID");
};

// Helper to parse formatted currency string to number
const parseCurrency = (value: string): number => {
  // Remove all non-numeric characters except minus sign
  const numericValue = value.replace(/[^0-9]/g, "");
  if (!numericValue) return 0;
  return parseInt(numericValue, 10);
};

export default function DigitalTransactionScreen() {
  const navigation = useNavigation<any>();
  const route = useRoute<any>();
  const insets = useSafeAreaInsets();
  const [editTrxId, setEditTrxId] = useState<number | null>(null);
  const [categories, setCategories] = useState<DigitalCategory[]>([]);
  const [category, setCategory] = useState<string>("PULSA");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [customerName, setCustomerName] = useState("");
  const [provider, setProvider] = useState("");
  const [providers, setProviders] = useState<string[]>([]);
  const [amount, setAmount] = useState("");
  const [costPrice, setCostPrice] = useState("");
  const [sellingPrice, setSellingPrice] = useState("");
  const [paid, setPaid] = useState("");
  const [notes, setNotes] = useState("");
  const [transactionDate, setTransactionDate] = useState(getCurrentDateISO());
  const [history, setHistory] = useState<{ phone_number: string, customer_name: string }[]>([]);
  const [showHistoryModal, setShowHistoryModal] = useState(false);

  const [templates, setTemplates] = useState<DigitalProductMaster[]>([]);
  const [showProviderModal, setShowProviderModal] = useState(false);
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Validation States
  const [phoneNumberError, setPhoneNumberError] = useState<string | null>(null);
  const [transactionDateError, setTransactionDateError] = useState<string | null>(null);
  const [providerError, setProviderError] = useState<string | null>(null);
  const [amountError, setAmountError] = useState<string | null>(null);
  const [sellingPriceError, setSellingPriceError] = useState<string | null>(null);
  const [costPriceError, setCostPriceError] = useState<string | null>(null);

  // Profit state
  const [profit, setProfit] = useState<number>(0);

  const resetForm = () => {
    setEditTrxId(null);
    setCategory("PULSA");
    setPhoneNumber("");
    setCustomerName("");
    setProvider("");
    setAmount("");
    setCostPrice("");
    setSellingPrice("");
    setPaid("");
    setNotes("");
    setTransactionDate(getCurrentDateISO());
    setPhoneNumberError(null);
    setTransactionDateError(null);
    setProviderError(null);
    setAmountError(null);
    setSellingPriceError(null);
    setCostPriceError(null);
    setTemplates([]);
    setProviders([]);
    setProfit(0);
  };

  useEffect(() => {
    loadCategories();
    loadHistory();

    // Reset form when screen is opened without editTrxId param
    if (!route.params?.editTrxId) {
      resetForm();
    }
  }, []);

  // Reset form when screen gains focus (when navigating back)
  useEffect(() => {
    const unsubscribe = navigation.addListener('focus', () => {
      if (!route.params?.editTrxId) {
        resetForm();
      }
    });
    return unsubscribe;
  }, [navigation, route.params?.editTrxId]);

  useEffect(() => {
    const loadEditTransaction = async () => {
      const editTrxId = route.params?.editTrxId;
      if (!editTrxId) return;

      try {
        const db = await getDB();
        const etrx = await db.getFirstAsync<DigitalTransaction>(
          "SELECT * FROM phone_history WHERE id = ?",
          [editTrxId]
        );

        if (etrx) {
          setEditTrxId(etrx.id ?? null);
          setCategory(etrx.category);
          setPhoneNumber(etrx.phone_number);
          setCustomerName(etrx.customer_name || "");
          setProvider(etrx.provider);
          setAmount((etrx.amount ?? 0).toString());
          setCostPrice((etrx.cost_price ?? 0).toString());
          setSellingPrice((etrx.selling_price ?? 0).toString());
          setPaid((etrx.paid ?? etrx.selling_price ?? 0).toString());
          setNotes(etrx.notes || "");
          setTransactionDate(parseISODate(etrx.created_at));
        }
      } catch (error) {
        console.error("Error loading transaction for edit:", error);
        Alert.alert("Error", "Gagal memuat transaksi untuk diedit");
      }
    };

    loadEditTransaction();
  }, [route.params?.editTrxId]);

  useEffect(() => {
    if (category) {
      loadProviders();
    }
  }, [category]);

  const loadCategories = async () => {
    const data = await getDigitalCategories();
    // Taruh kategori default (PULSA) di paling kiri
    const defaultCat = "PULSA";
    const sorted = [...data].sort((a, b) => {
      if (a.name === defaultCat) return -1;
      if (b.name === defaultCat) return 1;
      return 0;
    });
    setCategories(sorted);
    if (sorted.length > 0 && !category) {
      setCategory(sorted[0].name);
    }
  };

  const loadProviders = async () => {
    const data = await getDistinctProvidersByCategory(category);
    const providerNames = data.map((d: any) => d.provider);
    setProviders(providerNames);
    // Auto-select "Lainnya" if no providers are found for the category
    if (providerNames.length === 0) {
      setProvider("Lainnya");
    } else if (provider && !providerNames.includes(provider) && provider !== "Lainnya") {
      // If current provider is not in the new list and not "Lainnya", reset it to force selection
      setProvider("");
      setProviderError("Provider wajib dipilih."); // Set error if reset
    } else if (provider === "Lainnya" && providerNames.length > 0) {
        // If user previously selected "Lainnya" but now providers exist, reset
        setProvider("");
        setProviderError("Provider wajib dipilih.");
    }
  };

  useEffect(() => {
    if (category && provider) {
      loadTemplates();
    } else {
      setTemplates([]);
    }
  }, [category, provider]);

  const loadHistory = async () => {
    const data = await getRecentNumbers();
    setHistory(data);
  };

  const loadTemplates = async () => {
    const data = await getDigitalProducts(category, provider);
    setTemplates(data);
    // If there are templates, and current amount/prices are empty, pre-fill from first template
    if (data.length > 0 && !amount && !costPrice && !sellingPrice) {
      selectTemplate(data[0]);
    }
  };

  const selectTemplate = (item: DigitalProductMaster) => {
    setAmount((item.nominal ?? 0).toString());
    setCostPrice((item.cost_price ?? 0).toString());
    setSellingPrice((item.selling_price ?? 0).toString());
    // Clear potential errors if user selects a template after making errors
    setAmountError(null);
    setCostPriceError(null);
    setSellingPriceError(null);
  };

  // Recalculate profit when prices change
  useEffect(() => {
    const costValue = parseCurrency(costPrice);
    const sellValue = parseCurrency(sellingPrice);
    setProfit(sellValue - costValue);
  }, [costPrice, sellingPrice]);

  // Handler to clear specific error when user types into a field
  const clearErrorOnInput = (setErrorFn: (err: null) => void) => {
    setErrorFn(null);
  };

  const handleTransaction = async () => {
    // Prevent double submit
    if (isSubmitting) return;

    // Reset previous errors
    setPhoneNumberError(null);
    setTransactionDateError(null);
    setProviderError(null);
    setAmountError(null);
    setSellingPriceError(null);
    setCostPriceError(null);

    // Validate using utility
    const errors = validateDigitalTransaction({
      phoneNumber,
      transactionDate,
      provider,
      providers,
      amount,
      sellingPrice,
      costPrice,
    });

    // Set errors from validation
    if (errors.phoneNumber) setPhoneNumberError(errors.phoneNumber);
    if (errors.transactionDate) setTransactionDateError(errors.transactionDate);
    if (errors.provider) setProviderError(errors.provider);
    if (errors.amount) setAmountError(errors.amount);
    if (errors.sellingPrice) setSellingPriceError(errors.sellingPrice);
    if (errors.costPrice) setCostPriceError(errors.costPrice);

    if (Object.keys(errors).length > 0) {
      return; // Stop if validation fails
    }

    const finalCostValue = parseCurrency(costPrice);
    const finalSellValue = parseCurrency(sellingPrice);
    const profitValue = finalSellValue - finalCostValue;

    setIsSubmitting(true);
    try {
      // Construct the timestamp for created_at. Use the selected date and current time.
      const createdAtTimestamp = createTimestamp(transactionDate);

      const paidValue = parseCurrency(paid) || finalSellValue;

      let trxId: number;
      if (editTrxId) {
        console.log("Updating transaction:", editTrxId, "notes:", notes);
        await updateDigitalTransaction(editTrxId, {
          category,
          phone_number: phoneNumber.trim(),
          customer_name: customerName.trim(),
          provider: provider.trim(),
          amount: parseCurrency(amount),
          cost_price: finalCostValue,
          selling_price: finalSellValue,
          paid: paidValue,
          profit: profitValue,
          notes: notes.trim(),
          created_at: createdAtTimestamp
        });
        console.log("Update completed");
        Alert.alert("Sukses", "Transaksi berhasil diperbarui");
        trxId = editTrxId;
      } else {
        trxId = await addDigitalTransaction({
          category,
          phone_number: phoneNumber.trim(),
          customer_name: customerName.trim(),
          provider: provider.trim(),
          amount: parseCurrency(amount),
          cost_price: finalCostValue,
          selling_price: finalSellValue,
          paid: paidValue,
          profit: profitValue,
          notes: notes.trim(),
          created_at: createdAtTimestamp
        });
        Alert.alert("Sukses", "Transaksi berhasil disimpan");
      }

      loadHistory(); // Always load history after transaction
      navigation.setParams({ editTrxId: undefined }); // Clear edit param
      resetForm(); // Reset form after transaction
      navigation.navigate(ROUTES.DIGITAL_DETAIL, { trxId }); // Navigate to detail screen

    } catch (e) {
      console.error("Transaction error:", e);
      Alert.alert("Error", "Gagal menyimpan transaksi");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === "ios" ? "padding" : "height"}
      keyboardVerticalOffset={Platform.OS === "ios" ? 0 : 20}
    >
      <ScrollView
        style={styles.container}
        contentContainerStyle={{ paddingBottom: 100, paddingTop: 12 }}
        keyboardShouldPersistTaps="handled"
      >
        {/* Categories Tab */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.categoryScroll}>
          {categories.map((cat) => (
            <Surface
              key={cat.id}
              elevation={category === cat.name ? 2 : 0}
              style={[styles.categoryCardPaper, category === cat.name && styles.activeCategoryCardPaper]}
            >
              <TouchableRipple
                onPress={() => {
                  setCategory(cat.name);
                  setProvider(""); // Reset provider when category changes
                  setTemplates([]); // Clear templates
                  clearErrorOnInput(setProviderError); // Clear provider error
                }}
                style={styles.categoryRipple}
              >
                <View style={styles.categoryContent}>
                  <Text style={styles.categoryIcon}>{cat.icon}</Text>
                  <Text style={[styles.categoryLabel, category === cat.name && styles.activeCategoryLabel]}>
                    {cat.name}
                  </Text>
                </View>
              </TouchableRipple>
            </Surface>
          ))}
        </ScrollView>

        <Card style={styles.mainCardPaper}>
          <Card.Content>
            {/* Phone Number Input */}
            <View style={styles.inputRowPaper}>
              <TextInput
                label="No. Pelanggan / HP / ID"
                placeholder="0812xxx atau ID Pelanggan"
                value={phoneNumber}
                onChangeText={(text) => { setPhoneNumber(text); clearErrorOnInput(setPhoneNumberError); }}
                keyboardType="numeric"
                mode="outlined"
                style={{ flex: 1 }}
                error={!!phoneNumberError}
                right={
                  <TextInput.Icon
                    icon="history"
                    onPress={() => navigation.navigate(ROUTES.DIGITAL_HISTORY)} // Navigating to history screen
                  />
                }
                left={
                  <TextInput.Icon
                    icon="text-box-search-outline"
                    onPress={() => setShowHistoryModal(true)} // Opening modal for history selection
                  />
                }
              />
            </View>
            <HelperText type="error" visible={!!phoneNumberError}>
              {phoneNumberError}
            </HelperText>

            {/* Date Input */}
            <TextInput
              label="Tanggal Transaksi"
              placeholder="YYYY-MM-DD"
              value={transactionDate}
              onChangeText={(text) => { setTransactionDate(text); clearErrorOnInput(setTransactionDateError); }}
              mode="outlined"
              style={styles.fieldMargin}
              error={!!transactionDateError}
              right={<TextInput.Icon
                icon="calendar"
                onPress={() => setShowDatePicker(true)}
              />}
            />
            <HelperText type="error" visible={!!transactionDateError}>
              {transactionDateError}
            </HelperText>

            {showDatePicker && (
              <DateTimePicker
                value={new Date(transactionDate)}
                mode="date"
                display="default"
                onChange={(event, selectedDate) => {
                  setShowDatePicker(false);
                  if (selectedDate) {
                    setTransactionDate(selectedDate.toISOString().split('T')[0]);
                    clearErrorOnInput(setTransactionDateError); // Clear date error on selection
                  }
                }}
              />
            )}

            {/* Customer Name Input */}
            <TextInput
              label="Nama Pelanggan (Opsional)"
              placeholder="Contoh: Budi Santoso"
              value={customerName}
              onChangeText={(text) => { setCustomerName(text); }} // No error for optional field
              mode="outlined"
              style={styles.fieldMargin}
            />

            {/* Provider List */}
            <Text style={styles.labelPaper}>Provider / Bank / Game</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.providerScroll}>
              {[...providers, "Lainnya"].map((prov) => (
                <Surface
                  key={prov}
                  elevation={provider === prov ? 2 : 0}
                  style={[styles.providerCardPaper, provider === prov && styles.activeProviderCardPaper]}
                >
                  <TouchableRipple
                    onPress={() => {
                      setProvider(prov);
                      clearErrorOnInput(setProviderError);
                    }}
                    style={styles.providerRipple}
                  >
                    <Text style={[styles.providerLabel, provider === prov && styles.activeProviderLabel]}>
                      {prov}
                    </Text>
                  </TouchableRipple>
                </Surface>
              ))}
            </ScrollView>
            <HelperText type="error" visible={!!providerError}>
              {providerError}
            </HelperText>

            {/* Manual Provider Input (if "Lainnya" is selected) */}
            {provider === "Lainnya" && (
              <TextInput
                label="Nama Provider Manual"
                placeholder="Masukkan Nama Provider/Bank Manual"
                value={provider}
                onChangeText={(text) => { setProvider(text); clearErrorOnInput(setProviderError); }}
                mode="outlined"
                style={styles.fieldMargin}
              />
            )}

            {/* Product Templates from Master */}
            {templates.length > 0 && (
              <View style={styles.templateSectionPaper}>
                <Text variant="labelLarge" style={styles.labelPaper}>Pilih Produk (Master)</Text>
                <View style={styles.templateGridPaper}>
                  {templates.map((t) => (
                    <Chip
                      key={t.id}
                      selected={amount === (t.nominal ?? 0).toString()}
                      onPress={() => selectTemplate(t)}
                      style={styles.templateChip}
                      showSelectedOverlay
                      mode="outlined"
                    >
                      {t.name} (Rp {(t.selling_price ?? 0).toLocaleString("id-ID")})
                    </Chip>
                  ))}
                </View>
              </View>
            )}

            {/* Amount Input */}
            <TextInput
              label="Nominal / Item"
              placeholder="Contoh: 10000"
              value={amount ? formatCurrency(amount) : ""}
              onChangeText={(text) => {
                // Store raw numeric value (without formatting) in state
                const rawValue = text.replace(/[^0-9]/g, "");
                setAmount(rawValue);
                clearErrorOnInput(setAmountError);
              }}
              keyboardType="numeric"
              mode="outlined"
              style={styles.fieldMargin}
              error={!!amountError}
            />
            <HelperText type="error" visible={!!amountError}>
              {amountError}
            </HelperText>

            {/* Price Inputs Row */}
            <View style={styles.row}>
              <View style={{ flex: 1, marginRight: 8 }}>
                <TextInput
                  label="Harga Modal"
                  placeholder="0"
                  value={costPrice ? formatCurrency(costPrice) : ""}
                  onChangeText={(text) => {
                    // Store raw numeric value (without formatting) in state
                    const rawValue = text.replace(/[^0-9]/g, "");
                    setCostPrice(rawValue);
                    clearErrorOnInput(setCostPriceError);
                  }}
                  keyboardType="numeric"
                  mode="outlined"
                  error={!!costPriceError}
                />
                <HelperText type="error" visible={!!costPriceError}>
                  {costPriceError}
                </HelperText>
              </View>
              <View style={{ flex: 1 }}>
                <TextInput
                  label="Harga Jual"
                  placeholder="0"
                  value={sellingPrice ? formatCurrency(sellingPrice) : ""}
                  onChangeText={(text) => {
                    // Store raw numeric value (without formatting) in state
                    const rawValue = text.replace(/[^0-9]/g, "");
                    setSellingPrice(rawValue);
                    clearErrorOnInput(setSellingPriceError);
                  }}
                  keyboardType="numeric"
                  mode="outlined"
                  error={!!sellingPriceError}
                />
                <HelperText type="error" visible={!!sellingPriceError}>
                  {sellingPriceError}
                </HelperText>
              </View>
            </View>

            {/* Display Profit */}
            <View style={styles.profitDisplay}>
              <Text variant="titleMedium">Keuntungan:</Text>
              <Text variant="titleMedium" style={styles.profitValue}>
                Rp {profit.toLocaleString("id-ID")}
              </Text>
            </View>

            {/* Paid Amount Input */}
            <TextInput
              label="Jumlah Dibayar"
              placeholder="0"
              value={paid ? formatCurrency(paid) : ""}
              onChangeText={(text) => {
                // Store raw numeric value (without formatting) in state
                const rawValue = text.replace(/[^0-9]/g, "");
                setPaid(rawValue);
              }}
              keyboardType="numeric"
              mode="outlined"
              style={styles.fieldMargin}
            />

            {/* Notes Input */}
            <TextInput
              label="Catatan (Opsional)"
              placeholder="Contoh: Token Listrik"
              value={notes}
              onChangeText={(text) => { setNotes(text); }}
              mode="outlined"
              style={styles.fieldMargin}
            />

            {/* Action Buttons */}
            <Button
              mode="contained"
              onPress={handleTransaction}
              style={styles.mainActionBtn}
              contentStyle={{ paddingVertical: 8 }}
              loading={isSubmitting}
              disabled={Boolean(
                isSubmitting ||
                phoneNumber.length === 0 ||
                !transactionDate ||
                (providers.length > 0 && provider.length === 0) ||
                amount.length === 0 ||
                sellingPrice.length === 0 ||
                (costPrice && parseCurrency(costPrice) < 0) || // Check cost price negativity
                (amount && parseCurrency(amount) <= 0) ||
                (sellingPrice && parseCurrency(sellingPrice) <= 0)
              )}
            >
              {editTrxId ? "Update Transaksi" : "Proses Transaksi"}
            </Button>

            {editTrxId && (
              <Button
                mode="outlined"
                onPress={() => navigation.goBack()}
                style={[styles.mainActionBtn, { marginTop: 12 }]}
                contentStyle={{ paddingVertical: 8 }}
              >
                Batal
              </Button>
            )}
          </Card.Content>
        </Card>
      </ScrollView>

      {/* Provider List Modal */}
      <Portal>
        <PaperModal
          visible={showProviderModal}
          onDismiss={() => { setShowProviderModal(false); clearErrorOnInput(setProviderError); }} // Clear error when modal is dismissed
          contentContainerStyle={styles.modalContentPaper}
        >
          <Text variant="titleLarge" style={styles.modalTitlePaper}>Pilih Provider / Operator</Text>
          <Divider style={{ marginVertical: 12 }} />
          <FlatList
            data={[...providers, "Lainnya"]}
            keyExtractor={(item) => item}
            renderItem={({ item }) => (
              <List.Item
                title={item}
                onPress={() => {
                  setProvider(item);
                  setShowProviderModal(false);
                  clearErrorOnInput(setProviderError); // Clear error on selection
                }}
                right={props => item === provider ? <List.Icon {...props} icon="check" color="#6366F1" /> : null}
              />
            )}
            style={{ maxHeight: 400 }}
          />
          <Button mode="text" onPress={() => { setShowProviderModal(false); clearErrorOnInput(setProviderError); }} style={{ marginTop: 8 }}>
            Tutup
          </Button>
        </PaperModal>
      </Portal>

      {/* History Modal */}
      <Portal>
        <PaperModal
          visible={showHistoryModal}
          onDismiss={() => setShowHistoryModal(false)}
          contentContainerStyle={styles.modalContentPaper}
        >
          <Text variant="titleLarge" style={styles.modalTitlePaper}>Nomor Terakhir</Text>
          <Divider style={{ marginVertical: 12 }} />
          <FlatList
            data={history}
            keyExtractor={(item, index) => index.toString()}
            renderItem={({ item }) => (
              <List.Item
                title={item.phone_number}
                description={item.customer_name || "Tanpa Nama"}
                left={props => <List.Icon {...props} icon="phone-outline" />}
                onPress={() => {
                  setPhoneNumber(item.phone_number);
                  if (item.customer_name) setCustomerName(item.customer_name);
                  setShowHistoryModal(false);
                  clearErrorOnInput(setPhoneNumberError); // Clear phone number error on selection
                }}
              />
            )}
            ListEmptyComponent={
              <Text style={styles.emptyTextModal}>Belum ada riwayat nomor</Text>
            }
            style={{ maxHeight: 400 }}
          />
          <Button mode="text" onPress={() => setShowHistoryModal(false)} style={{ marginTop: 8 }}>
            Tutup
          </Button>
        </PaperModal>
      </Portal>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F9FAFB",
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 20,
    marginBottom: 8,
  },
  headerTitle: {
    fontWeight: "800",
    color: "#1E293B",
  },
  categoryScroll: {
    paddingHorizontal: 20,
    marginBottom: 16,
  },
  categoryCardPaper: {
    borderRadius: 16,
    marginRight: 12,
    marginVertical: 4,
    backgroundColor: "#FFF",
    overflow: 'hidden',
    minWidth: 80,
  },
  activeCategoryCardPaper: {
    backgroundColor: "#6366F1",
  },
  categoryRipple: {
    padding: 12,
  },
  categoryContent: {
    alignItems: "center",
  },
  categoryIcon: {
    fontSize: 24,
    marginBottom: 4,
  },
  categoryLabel: {
    fontSize: 12,
    fontWeight: "bold",
    color: "#334155",
  },
  activeCategoryLabel: {
    color: "#FFF",
  },
  mainCardPaper: {
    margin: 20,
    marginTop: 0,
    borderRadius: 20,
  },
  inputRowPaper: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 0, // Removed, HelperText will provide spacing below
  },
  fieldMargin: {
    marginBottom: 0, // Removed, HelperText will provide spacing below
  },
  templateSectionPaper: {
    marginBottom: 16,
  },
  labelPaper: {
    marginBottom: 8,
    color: "#334155",
    fontWeight: "600",
  },
  templateGridPaper: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
  },
  templateChip: {
    marginBottom: 4,
  },
  row: {
    flexDirection: "row",
    alignItems: "flex-start", // Align items to top to account for HelperText
  },
  mainActionBtn: {
    marginTop: 8,
    borderRadius: 12,
  },
  modalContentPaper: {
    backgroundColor: "white",
    padding: 24,
    margin: 20,
    borderRadius: 20,
  },
  modalTitlePaper: {
    fontWeight: "bold",
  },
  emptyTextModal: {
    textAlign: "center",
    color: "#94A3B8",
    paddingVertical: 20,
  },
  // New styles for profit display and adjusted spacing
  profitDisplay: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginTop: 6,
    marginBottom: 8,
    paddingVertical: 6,
    paddingHorizontal: 4,
  },
  profitValue: {
    fontWeight: "bold",
    color: "#6366F1", // Example color, matching active category
  },
  providerScroll: {
    paddingHorizontal: 0,
    marginBottom: 0,
    marginVertical: 4,
  },
  providerCardPaper: {
    borderRadius: 12,
    marginRight: 8,
    backgroundColor: "#FFF",
    overflow: "hidden",
    minWidth: 80,
  },
  activeProviderCardPaper: {
    backgroundColor: "#6366F1",
  },
  providerRipple: {
    padding: 10,
    justifyContent: "center",
    alignItems: "center",
  },
  providerLabel: {
    fontSize: 12,
    fontWeight: "bold",
    color: "#334155",
    textAlign: "center",
  },
  activeProviderLabel: {
    color: "#FFF",
  },
});
