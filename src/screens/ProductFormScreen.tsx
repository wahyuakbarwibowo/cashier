import React, { useCallback, useEffect, useState } from "react";
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  View,
} from "react-native";
import { DrawerScreenProps } from "@react-navigation/drawer";
import { DrawerParamList } from "../navigation/types";
import { Product } from "../types/database";
import { addProduct, updateProduct } from "../database/products";
import BarcodeScannerModal from "../components/BarcodeScannerModal";
import { useCameraPermissions } from "expo-camera";
import {
  Button,
  Divider,
  Surface,
  Subheading,
  TextInput as PaperTextInput,
  Title,
  useTheme,
} from "react-native-paper";

// Helper to format number with thousand separators (Indonesian locale - dots)
const formatCurrency = (value: string): string => {
  // Remove all non-numeric characters
  const numericValue = value.replace(/[^0-9]/g, "");
  if (!numericValue) return "";
  // Format with thousand separators (dots for Indonesian locale)
  return parseInt(numericValue, 10).toLocaleString("id-ID");
};

// Helper to parse formatted currency string to number
const parseCurrency = (value: string): number => {
  // Remove all non-numeric characters
  const numericValue = value.replace(/[^0-9]/g, "");
  if (!numericValue) return 0;
  return parseInt(numericValue, 10);
};

type Props = DrawerScreenProps<DrawerParamList, "ProductForm">;

interface ProductFormData {
  code: string;
  name: string;
  purchasePrice: string;
  purchasePackagePrice: string;
  purchasePackageQty: string;
  sellingPrice: string;
  packagePrice: string;
  packageQty: string;
  discount: string;
  stock: string;
}

const initialFormData: ProductFormData = {
  code: "",
  name: "",
  purchasePrice: "",
  purchasePackagePrice: "",
  purchasePackageQty: "",
  sellingPrice: "",
  packagePrice: "",
  packageQty: "",
  discount: "",
  stock: "",
};

type SaveResult = {
  isEdit: boolean;
};

function useProductForm(onSuccess?: (result: SaveResult) => void) {
  const [formData, setFormData] = useState<ProductFormData>(initialFormData);
  const [editId, setEditId] = useState<number | null>(null);

  const updateField = useCallback(
    <K extends keyof ProductFormData>(field: K, value: ProductFormData[K]) => {
      setFormData((prev) => {
        let newData = { ...prev, [field]: value };

        if (field === "purchasePackagePrice" || field === "purchasePackageQty") {
          const packagePrice =
            parseCurrency(
              field === "purchasePackagePrice"
                ? (value as string)
                : prev.purchasePackagePrice
            );
          const packageQty =
            parseInt(
              field === "purchasePackageQty"
                ? (value as string)
                : prev.purchasePackageQty
            ) || 0;

          if (packagePrice > 0 && packageQty > 0) {
            newData.purchasePrice = String(Math.round(packagePrice / packageQty));
          }
        }

        if (field === "packagePrice" || field === "packageQty") {
          const packagePrice =
            parseCurrency(
              field === "packagePrice" ? (value as string) : prev.packagePrice
            );
          const packageQty =
            parseInt(
              field === "packageQty" ? (value as string) : prev.packageQty
            ) || 0;

          if (packagePrice > 0 && packageQty > 0) {
            newData.sellingPrice = String(Math.round(packagePrice / packageQty));
          }
        }

        return newData;
      });
    },
    []
  );

  const resetForm = useCallback(() => {
    setFormData(initialFormData);
    setEditId(null);
  }, []);

  const handleSave = useCallback(async () => {
    if (!formData.name.trim()) {
      Alert.alert("Validasi", "Nama produk wajib diisi");
      return;
    }

    const formattedProduct: Product = {
      code: formData.code.trim() || undefined,
      name: formData.name.trim(),
      purchase_price: parseCurrency(formData.purchasePrice),
      purchase_package_price: parseCurrency(formData.purchasePackagePrice),
      purchase_package_qty: parseInt(formData.purchasePackageQty) || 0,
      selling_price: parseCurrency(formData.sellingPrice),
      package_price: parseCurrency(formData.packagePrice),
      package_qty: parseInt(formData.packageQty) || 0,
      discount: parseCurrency(formData.discount),
      stock: parseInt(formData.stock) || 0,
    };

    const isEdit = Boolean(editId);

    try {
      if (isEdit && editId) {
        await updateProduct(editId, formattedProduct);
      } else {
        await addProduct(formattedProduct);
      }

      resetForm();
      onSuccess?.({ isEdit });
    } catch (error) {
      console.error(error);
      Alert.alert("Error", "Gagal menyimpan produk");
    }
  }, [editId, formData, onSuccess, resetForm]);

  const populateForm = useCallback((item: Product) => {
    setEditId(item.id ?? null);
    setFormData({
      code: item.code || "",
      name: item.name,
      purchasePrice: String(item.purchase_price ?? ""),
      purchasePackagePrice: String(item.purchase_package_price ?? ""),
      purchasePackageQty: String(item.purchase_package_qty ?? ""),
      sellingPrice: String(item.selling_price ?? ""),
      packagePrice: String(item.package_price ?? ""),
      packageQty: String(item.package_qty ?? ""),
      discount: String(item.discount ?? ""),
      stock: String(item.stock ?? ""),
    });
  }, []);

  const generateRandomCode = useCallback(() => {
    const random = Math.floor(Math.random() * 90000000) + 10000000;
    updateField("code", String(random));
  }, [updateField]);

  return {
    formData,
    editId,
    updateField,
    resetForm,
    handleSave,
    populateForm,
    generateRandomCode,
  };
}

function useBarcodeScanner(onScanned: (code: string) => void) {
  const [permission, requestPermission] = useCameraPermissions();
  const [isScanning, setIsScanning] = useState(false);

  const startScan = useCallback(async () => {
    if (!permission?.granted) {
      const res = await requestPermission();
      if (!res.granted) {
        Alert.alert("Permission", "Akses kamera dibutuhkan untuk scan barcode");
        return;
      }
    }
    setIsScanning(true);
  }, [permission, requestPermission]);

  const handleBarcodeScanned = useCallback(
    ({ data }: { data: string }) => {
      onScanned(data);
      setIsScanning(false);
    },
    [onScanned]
  );

  const closeScan = useCallback(() => setIsScanning(false), []);

  return { isScanning, startScan, handleBarcodeScanned, closeScan };
}

export default function ProductFormScreen({ navigation, route }: Props) {
  const theme = useTheme();
  const params = route.params;
  const productToEdit = params?.product;
  const initialCode = params?.initialCode;

  const onSaveSuccess = useCallback(
    ({ isEdit }: SaveResult) => {
      Alert.alert(
        "Sukses",
        isEdit ? "Produk berhasil diperbarui" : "Produk berhasil ditambahkan",
        [
          {
            text: "OK",
            onPress: () => navigation.navigate("Product"),
          },
        ]
      );
    },
    [navigation]
  );

  const {
    formData,
    editId,
    updateField,
    resetForm,
    handleSave,
    populateForm,
    generateRandomCode,
  } = useProductForm(onSaveSuccess);

  const {
    isScanning,
    startScan,
    handleBarcodeScanned,
    closeScan,
  } = useBarcodeScanner((code) => updateField("code", code));

  useEffect(() => {
    if (productToEdit) {
      populateForm(productToEdit);
    }
  }, [populateForm, productToEdit]);

  useEffect(() => {
    if (initialCode) {
      updateField("code", initialCode);
      navigation.setParams({ initialCode: undefined });
    }
  }, [initialCode, navigation, updateField]);

  const isEditing = Boolean(editId);

  return (
    <KeyboardAvoidingView
      style={styles.screen}
      behavior={Platform.OS === "ios" ? "padding" : "height"}
    >
      <View style={styles.container}>
        <View style={styles.toolbar}>
          <View>
            <Title style={styles.title}>
              {isEditing ? "Edit Produk" : "Tambah Produk"}
            </Title>
            <Subheading style={styles.subtitle}>
              {isEditing
                ? "Perbarui detail produk yang sudah ada"
                : "Isi detail produk baru"}
            </Subheading>
          </View>
          {isEditing && (
            <Button mode="text" compact textColor="#111827" onPress={resetForm}>
              Reset
            </Button>
          )}
        </View>

        <Surface
          style={[styles.surface, { backgroundColor: theme.colors.surface }]}
        >
          <ScrollView
            showsVerticalScrollIndicator={false}
            contentContainerStyle={styles.formContent}
          >
            <PaperTextInput
              label="Kode (Scan)"
              value={formData.code}
              onChangeText={(value) => updateField("code", value)}
              mode="outlined"
              style={styles.input}
            />

            <View style={styles.scanRow}>
              <Button
                mode="contained-tonal"
                buttonColor="#10B981"
                textColor="#FFF"
                compact
                onPress={generateRandomCode}
                style={[styles.scanButton, styles.scanButtonPrimary]}
              >
                Buat
              </Button>
              <Button
                mode="contained"
                buttonColor="#2563EB"
                textColor="#FFF"
                compact
                onPress={startScan}
                style={styles.scanButton}
              >
                Scan
              </Button>
            </View>

            <PaperTextInput
              label="Nama produk"
              value={formData.name}
              onChangeText={(value) => updateField("name", value)}
              mode="outlined"
              style={styles.input}
            />

            <PaperTextInput
              label="Stok"
              value={formData.stock}
              onChangeText={(value) => updateField("stock", value)}
              keyboardType="numeric"
              mode="outlined"
              style={styles.input}
            />

            <Subheading style={styles.sectionLabel}>Harga Beli</Subheading>
            <View style={styles.row}>
              <PaperTextInput
                label="Satuan"
                value={formData.purchasePrice ? formatCurrency(formData.purchasePrice) : ""}
                onChangeText={(value) => {
                  const rawValue = value.replace(/[^0-9]/g, "");
                  updateField("purchasePrice", rawValue);
                }}
                keyboardType="numeric"
                mode="outlined"
                style={[styles.input, styles.flexInput]}
              />
              <PaperTextInput
                label="Paket"
                value={formData.purchasePackagePrice ? formatCurrency(formData.purchasePackagePrice) : ""}
                onChangeText={(value) => {
                  const rawValue = value.replace(/[^0-9]/g, "");
                  updateField("purchasePackagePrice", rawValue);
                }}
                keyboardType="numeric"
                mode="outlined"
                style={[styles.input, styles.flexInput]}
              />
              <PaperTextInput
                label="Isi"
                value={formData.purchasePackageQty}
                onChangeText={(value) =>
                  updateField("purchasePackageQty", value)
                }
                keyboardType="numeric"
                mode="outlined"
                style={[styles.input, styles.qtyInput]}
              />
            </View>

            <Subheading style={styles.sectionLabel}>Harga Jual</Subheading>
            <View style={styles.row}>
              <PaperTextInput
                label="Satuan"
                value={formData.sellingPrice ? formatCurrency(formData.sellingPrice) : ""}
                onChangeText={(value) => {
                  const rawValue = value.replace(/[^0-9]/g, "");
                  updateField("sellingPrice", rawValue);
                }}
                keyboardType="numeric"
                mode="outlined"
                style={[styles.input, styles.flexInput]}
              />
              <PaperTextInput
                label="Paket"
                value={formData.packagePrice ? formatCurrency(formData.packagePrice) : ""}
                onChangeText={(value) => {
                  const rawValue = value.replace(/[^0-9]/g, "");
                  updateField("packagePrice", rawValue);
                }}
                keyboardType="numeric"
                mode="outlined"
                style={[styles.input, styles.flexInput]}
              />
              <PaperTextInput
                label="Isi"
                value={formData.packageQty}
                onChangeText={(value) => updateField("packageQty", value)}
                keyboardType="numeric"
                mode="outlined"
                style={[styles.input, styles.qtyInput]}
              />
            </View>

            <PaperTextInput
              label="Diskon (%)"
              value={formData.discount ? formatCurrency(formData.discount) : ""}
              onChangeText={(value) => {
                const rawValue = value.replace(/[^0-9]/g, "");
                updateField("discount", rawValue);
              }}
              keyboardType="numeric"
              mode="outlined"
              style={styles.input}
            />

            <Divider style={styles.divider} />

            <Button
              mode="contained"
              buttonColor="#111827"
              textColor="#FFF"
              contentStyle={styles.primaryButtonContent}
              onPress={handleSave}
            >
              {isEditing ? "Update" : "Simpan"}
            </Button>

            {isEditing && (
              <Button
                mode="outlined"
                style={styles.cancelButton}
                onPress={resetForm}
              >
                Batal
              </Button>
            )}

            <View style={styles.bottomSpacer} />
          </ScrollView>
        </Surface>
      </View>

      <BarcodeScannerModal
        visible={isScanning}
        onScanned={handleBarcodeScanned}
        onClose={closeScan}
      />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#F3F4F6",
  },
  container: {
    flex: 1,
    padding: 16,
  },
  toolbar: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "flex-start",
    marginBottom: 12,
  },
  title: {
    fontSize: 24,
    fontWeight: "700",
    color: "#111827",
  },
  subtitle: {
    fontSize: 12,
    color: "#6B7280",
    marginTop: 2,
  },
  surface: {
    borderRadius: 20,
    padding: 16,
    flex: 1,
    elevation: 4,
  },
  formContent: {
    paddingBottom: 16,
  },
  input: {
    marginBottom: 12,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
  },
  flexInput: {
    flex: 1,
    marginRight: 8,
  },
  qtyInput: {
    width: 80,
  },
  scanRow: {
    flexDirection: "row",
    marginBottom: 12,
  },
  scanButton: {
    flex: 1,
  },
  scanButtonPrimary: {
    marginRight: 8,
  },
  sectionLabel: {
    fontSize: 14,
    fontWeight: "bold",
    color: "#374151",
    marginBottom: 8,
  },
  primaryButtonContent: {
    height: 48,
  },
  cancelButton: {
    marginTop: 8,
    borderColor: "#E5E7EB",
  },
  divider: {
    marginVertical: 12,
  },
  bottomSpacer: {
    height: 24,
  },
});
