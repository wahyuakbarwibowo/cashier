import React, { useState, useEffect, useRef } from "react"; // Added useRef
import DateTimePicker from '@react-native-community/datetimepicker';
import {
  View,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  Alert, // Keep for critical errors, but will replace some with Snackbar
  ScrollView,
  KeyboardAvoidingView,
  Platform,
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
  SegmentedButtons,
  HelperText, // Import HelperText
  Snackbar // Import Snackbar
} from "react-native-paper";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useNavigation, useRoute } from "@react-navigation/native";
import { useFocusEffect } from "@react-navigation/native";
import { CameraView, useCameraPermissions } from "expo-camera";
import BarcodeScannerModal from "../components/BarcodeScannerModal";
import { getProducts } from "../database/products";
import { addSale, getSaleDetail, updateSale, SaleItemPayload } from "../database/sales";
import { getCustomers, addCustomer } from "../database/customers";
import { getPaymentMethods } from "../database/payment_methods";
import { Product, Customer, PaymentMethod, Sale } from "../types/database";

type CartItem = {
  product: Product;
  qty: number; // Total units
};

// Helper to optimize cart items by consolidating quantities for the same product
const optimizeCartItems = (items: CartItem[]): CartItem[] => {
  const map = new Map<number, CartItem>();
  for (const item of items) {
    const id = item.product.id!;
    if (map.has(id)) {
      // If item exists, increase quantity
      map.get(id)!.qty += item.qty;
    } else {
      // Otherwise, add new item
      map.set(id, { ...item });
    }
  }
  // Return only items with quantity greater than 0
  return Array.from(map.values()).filter(i => i.qty > 0);
};

// Helper to calculate effective price breakdown for display, considering packages
const getPriceBreakdown = (item: CartItem) => {
  const p = item.product;
  const qty = item.qty;

  // Check if package pricing is available and applicable
  if (p.package_price && p.package_qty && p.package_qty > 0) {
    const numPacks = Math.floor(qty / p.package_qty);
    const remainder = qty % p.package_qty;
    const totalPrice = (numPacks * p.package_price) + (remainder * (p.selling_price || 0));
    const unitPrice = totalPrice / qty; // Effective unit price

    let label = "";
    if (numPacks > 0 && remainder > 0) {
      label = `${numPacks} Pkt + ${remainder} Sat`;
    } else if (numPacks > 0) {
      label = `${numPacks} Paket`;
    } else {
      label = `${remainder} Satuan`;
    }

    return { totalPrice, label, unitPrice };
  }

  // Default to single unit pricing
  const totalPrice = qty * (p.selling_price || 0);
  return { totalPrice, label: `${qty} Satuan`, unitPrice: p.selling_price || 0 };
};

// Helper to format number with thousand separators (Indonesian locale - dots)
const formatCurrency = (value: string): string => {
  // Remove all non-numeric characters except minus sign
  const numericValue = value.replace(/[^0-9]/g, "");
  if (!numericValue) return "0";
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

export default function SalesTransactionScreen() {
  const navigation = useNavigation<any>();
  const route = useRoute<any>();
  const insets = useSafeAreaInsets();
  const [products, setProducts] = useState<Product[]>([]);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [paidAmount, setPaidAmount] = useState<string>("");
  const [transactionDate, setTransactionDate] = useState(new Date().toISOString().split('T')[0]);
  const [searchQuery, setSearchQuery] = useState("");

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([]);
  const [selectedCustomerId, setSelectedCustomerId] = useState<number | null>(null);
  const [selectedPaymentMethodId, setSelectedPaymentMethodId] = useState<number | null>(null);
  const [customerName, setCustomerName] = useState("");
  const [redeemPoints, setRedeemPoints] = useState(false);
  const [editSaleId, setEditSaleId] = useState<number | null>(null);
  const [loadingEditSale, setLoadingEditSale] = useState(false);
  const lastLoadedEditSaleId = useRef<number | null>(null);

  // Barcode Scanner & Date Picker State
  const [permission, requestPermission] = useCameraPermissions();
  const [isScanning, setIsScanning] = useState(false);
  const [showDatePicker, setShowDatePicker] = useState(false);

  // Snackbar State
  const [snackbarVisible, setSnackbarVisible] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState("");
  const [snackbarType, setSnackbarType] = useState<'success' | 'error'>('success'); // 'success' or 'error'

  // Validation States
  const [paidAmountError, setPaidAmountError] = useState<string | null>(null);
  const [customerNameError, setCustomerNameError] = useState<string | null>(null);
  const [transactionDateError, setTransactionDateError] = useState<string | null>(null);
  const [selectedPaymentMethodError, setSelectedPaymentMethodError] = useState<string | null>(null);

  // Submitting State
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Ref for accessing navigation params safely
  const navigationRef = useRef(navigation);
  useEffect(() => {
    navigationRef.current = navigation;
  }, [navigation]);
  const exitEditMode = () => {
    setEditSaleId(null);
    lastLoadedEditSaleId.current = null;
    navigationRef.current.setParams({ editSaleId: undefined });
    setLoadingEditSale(false);
  };

  const cancelEdit = () => {
    resetForm();
    exitEditMode();
    showSnackbar("Edit transaksi dibatalkan", "success");
  };

  // Calculate isDebt here so it's available in the component's render scope
  const selectedMethod = paymentMethods.find(m => m.id === selectedPaymentMethodId);
  const isDebt = selectedMethod?.name?.toLowerCase().includes("hutang") ?? false;

  useFocusEffect(
    React.useCallback(() => {
      loadInitialData();

      const params = route.params as { addProductId?: number; editSaleId?: number };
      if (params?.addProductId) {
        const productId = params.addProductId;
        // Search in fresh products
        getProducts().then(allProducts => {
          const p = allProducts.find(x => x.id === productId);
          if (p) handleAddToCart(p);
          // Clear the param after using it
          navigationRef.current.setParams({ addProductId: undefined });
        });
      }
      if (params?.editSaleId) {
        setEditSaleId(params.editSaleId);
      }
      // Clear customer/payment selections if they were made on a previous screen that's not the current one.
      // This might be too aggressive, consider if this reset is always desired.
      // For now, we'll rely on explicit selections within this screen.
    }, [route.params])
  );

  const loadInitialData = async () => {
    try {
      const [p, c, m] = await Promise.all([
        getProducts(),
        getCustomers(),
        getPaymentMethods()
      ]);
      setProducts(p);
      setCustomers(c);
      setPaymentMethods(m);
      if (m.length > 0) {
        setSelectedPaymentMethodId(m[0].id!);
      } else {
        setSelectedPaymentMethodId(null); // No payment methods available
      }
    } catch (error) {
      console.error("Error loading initial data:", error);
      showSnackbar("Gagal memuat data. Coba lagi.", "error");
    }
  };

  const loadSaleForEdit = async (saleId: number) => {
    setLoadingEditSale(true);
    try {
      const detail = await getSaleDetail(saleId);
      if (!detail) {
        showSnackbar("Transaksi tidak ditemukan.", "error");
        exitEditMode();
        return;
      }
      const sale = detail.sale;
      setSelectedCustomerId(sale.customer_id || null);
      setSelectedPaymentMethodId(sale.payment_method_id || null);
      setPaidAmount(typeof sale.paid === "number" ? sale.paid.toString() : "0");
      setTransactionDate(sale.created_at ? sale.created_at.split(' ')[0] : new Date().toISOString().split('T')[0]);
      setRedeemPoints((sale.points_redeemed || 0) > 0);
      setCustomerName("");
      setPaidAmountError(null);
      setCustomerNameError(null);
      setSelectedPaymentMethodError(null);
      setTransactionDateError(null);
      setSearchQuery("");

      const aggregatedCart = new Map<number, CartItem>();
      const productMap = new Map<number, Product>(
        products.filter(p => p.id).map(p => [p.id!, p])
      );

      for (const item of detail.items) {
        const storedProduct = productMap.get(item.product_id);
        const fallbackProduct: Product = {
          id: item.product_id,
          name: storedProduct?.name || "Produk Terhapus",
          selling_price: storedProduct?.selling_price ?? item.price,
          stock: storedProduct?.stock ?? 0,
        };

        const productForCart = storedProduct ? { ...storedProduct } : fallbackProduct;
        if (!productForCart.selling_price) {
          productForCart.selling_price = item.price;
        }

        if (aggregatedCart.has(item.product_id)) {
          aggregatedCart.get(item.product_id)!.qty += item.qty;
        } else {
          aggregatedCart.set(item.product_id, { product: productForCart, qty: item.qty });
        }
      }

      setCart(Array.from(aggregatedCart.values()));
      lastLoadedEditSaleId.current = saleId;
    } catch (error) {
      console.error("Load sale for edit error:", error);
      showSnackbar("Gagal memuat transaksi untuk diedit.", "error");
      exitEditMode();
    } finally {
      setLoadingEditSale(false);
    }
  };


  const filteredProducts = products.filter((p) =>
    p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (p.code && p.code.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  useEffect(() => {
    if (editSaleId && lastLoadedEditSaleId.current !== editSaleId) {
      loadSaleForEdit(editSaleId);
    }
  }, [editSaleId]);

  const handleAddToCart = (product: Product, asPackage: boolean = false) => {
    const unitsToAdd = asPackage ? (product.package_qty || 1) : 1;
    setCart((prevCart) => {
      const existingIndex = prevCart.findIndex((i) => i.product.id === product.id);

      if (existingIndex > -1) {
        // If item exists, increase quantity
        const updatedCart = prevCart.map((item, idx) =>
          idx === existingIndex ? { ...item, qty: item.qty + unitsToAdd } : item
        );
        return updatedCart.filter(i => i.qty > 0); // Ensure no negative qty, though logic prevents it
      } else {
        // Add new item to cart
        return [...prevCart, { product, qty: unitsToAdd }];
      }
    });
    setSearchQuery(""); // Clear search query after adding
    showSnackbar(`${product.name} berhasil ditambahkan.`, "success");
  };

  const handleBarcodeScanned = ({ data }: { data: string }) => {
    setIsScanning(false);

    // Find product with this code from the loaded products
    const product = products.find(p => p.code === data);
    if (product) {
      handleAddToCart(product);
      // No need to clear search query here as we are coming from scanner
    } else {
      // Use Alert for "not found" scenario, or navigate to add product
      Alert.alert(
        "Produk Tidak Ditemukan",
        `Barang dengan kode ${data} belum terdaftar.`,
        [
          { text: "Tutup", style: "cancel" },
          {
            text: "Tambah Produk",
            onPress: () => navigation.navigate("ProductForm", { initialCode: data }) // Navigate to add product screen with scanned code
          }
        ]
      );
    }
  };

  const startScan = async () => {
    if (!permission?.granted) {
      const res = await requestPermission();
      if (!res.granted) {
        Alert.alert("Izin Kamera", "Akses kamera dibutuhkan untuk scan barcode. Silakan aktifkan di pengaturan.");
        return;
      }
    }
    setIsScanning(true);
  };

  const updateCartItemQty = (id: number, newQty: number) => {
    if (newQty <= 0) {
      handleRemoveFromCart(id);
      return;
    }
    setCart((prev) => prev.map((i) => (i.product.id === id ? { ...i, qty: newQty } : i)));
  };

  const decreaseCartItemQty = (id: number) => {
    setCart((prev) => prev.map((i) => (i.product.id === id ? { ...i, qty: i.qty - 1 } : i)).filter(i => i.qty > 0));
  };

  const handleRemoveFromCart = (id: number) => {
    setCart((prev) => prev.filter((i) => i.product.id !== id));
    // Optionally show snackbar for removal
    // showSnackbar("Item dihapus dari keranjang.", "success");
  };

  // Calculate total amount based on cart items and their prices
  const total = cart.reduce((sum, item) => sum + getPriceBreakdown(item).totalPrice, 0);

  // Loyalty Points Logic
  const selectedCustomer = customers.find(c => c.id === selectedCustomerId);
  const availablePoints = selectedCustomer?.points || 0;
  // Redeem points only if enabled and customer has enough points
  const pointsToRedeem = redeemPoints ? Math.min(availablePoints, total) : 0;
  const finalTotal = total - pointsToRedeem; // Total after points redemption

  // Calculate change based on paid amount and final total
  const paidValue = parseCurrency(paidAmount);
  const change = paidValue - finalTotal;

  // Calculate points earned based on final total (after redemption)
  const earnedPoints = Math.floor(finalTotal / 1000); // Example: 1 point per 1000 IDR

  const resetForm = () => {
    setCart([]);
    setPaidAmount("");
    setSearchQuery("");
    setSelectedCustomerId(null);
    setCustomerName("");
    setRedeemPoints(false);
    setTransactionDate(new Date().toISOString().split('T')[0]);
    // Reset validation errors
    setPaidAmountError(null);
    setCustomerNameError(null);
    setTransactionDateError(null);
    setSelectedPaymentMethodError(null);
    // Reset payment method to default if available
    if (paymentMethods.length > 0) {
      setSelectedPaymentMethodId(paymentMethods[0].id!);
    } else {
      setSelectedPaymentMethodId(null);
    }
  };

  const handleReload = async () => {
    try {
      // Reload all data (products, customers, payment methods)
      const [p, c, m] = await Promise.all([
        getProducts(),
        getCustomers(),
        getPaymentMethods()
      ]);

      setProducts(p);
      setCustomers(c);
      setPaymentMethods(m);
      if (m.length > 0) {
        setSelectedPaymentMethodId(m[0].id!);
      } else {
        setSelectedPaymentMethodId(null);
      }

      // Update cart items with fresh product data to reflect latest stock
      const productMap = new Map<number, Product>(
        p.map(prod => [prod.id!, prod])
      );
      setCart(prevCart => {
        return prevCart.map(item => {
          const freshProduct = productMap.get(item.product.id!);
          return freshProduct ? { ...item, product: freshProduct } : item;
        });
      });

      showSnackbar("Data berhasil dimuat ulang", "success");
    } catch (error) {
      console.error("Error reloading data:", error);
      showSnackbar("Gagal memuat ulang data. Coba lagi.", "error");
    }
  };

  const showSnackbar = (message: string, type: 'success' | 'error' = 'success') => {
    setSnackbarMessage(message);
    setSnackbarType(type);
    setSnackbarVisible(true);
  };

  const handleFinishTransaction = async () => {
    // Prevent double submit
    if (isSubmitting) return;

    // --- Basic Validation ---
    let isValid = true;

    // Cart validation
    if (cart.length === 0) {
      showSnackbar("Keranjang kosong. Tambahkan produk terlebih dahulu.", "error");
      return;
    }

    // Stock Validation - handled visually and with Alert for blocking issue
    // Skip stock validation when editing existing transaction
    if (!editSaleId) {
      for (const item of cart) {
        if (item.qty > (item.product.stock || 0)) {
          // Use Alert for critical stock issue as it's blocking
          Alert.alert(
            "Stok Tidak Cukup",
            `Produk "${item.product.name}" hanya memiliki stok ${item.product.stock || 0}, tapi di keranjang ada ${item.qty}. Harap sesuaikan jumlahnya.`
          );
          return;
        }
      }
    }

    // Date Validation
    if (!transactionDate) {
      setTransactionDateError("Tanggal transaksi wajib diisi.");
      isValid = false;
    }

    // Payment Method Validation
    if (!selectedPaymentMethodId) {
      setSelectedPaymentMethodError("Metode pembayaran wajib dipilih.");
      isValid = false;
    }

    const selectedMethod = paymentMethods.find(m => m.id === selectedPaymentMethodId);
    const isDebt = selectedMethod?.name.toLowerCase().includes("hutang");

    // Paid Amount Validation
    if (!isDebt) {
      if (!paidAmount.trim()) {
        setPaidAmountError("Jumlah pembayaran wajib diisi.");
        isValid = false;
      } else {
        const paidValue = parseCurrency(paidAmount);
        if (isNaN(paidValue) || paidValue < 0) {
          setPaidAmountError("Jumlah pembayaran harus berupa angka positif.");
          isValid = false;
        } else if (paidValue < finalTotal) {
          setPaidAmountError("Pembayaran kurang.");
          isValid = false;
        }
      }
    }

    // Customer Name Validation for Debt
    if (isDebt && !selectedCustomerId && !customerName.trim()) {
      setCustomerNameError("Nama pelanggan wajib diisi untuk transaksi hutang.");
      isValid = false;
    }

    if (!isValid) {
      return; // Stop if validation fails
    }
    // --- End Validation ---

    setIsSubmitting(true);
    try {
      let finalCustomerId = selectedCustomerId;

      // If manual name is entered, create a new customer (for any transaction type)
      if (!finalCustomerId && customerName.trim()) {
        const newCustomerId = await addCustomer({
          name: customerName.trim(),
          phone: "", // Phone is optional for now
          address: ""
        });
        finalCustomerId = newCustomerId;
        showSnackbar("Pelanggan baru ditambahkan.", "success");
      }

      // Prepare items for sale, handling package vs. single units
      const finalSalesItems: SaleItemPayload[] = [];
      for (const item of cart) {
        const p = item.product;
        if (p.package_qty && p.package_price && p.package_qty > 0) {
          const numPacks = Math.floor(item.qty / p.package_qty);
          const remainder = item.qty % p.package_qty;

          if (numPacks > 0) {
            finalSalesItems.push({
              product_id: p.id!,
              qty: numPacks * p.package_qty, // Store total units in this entry
              price: p.package_price / p.package_qty, // Unit price from package
              subtotal: numPacks * p.package_price,
            });
          }
          if (remainder > 0) {
            finalSalesItems.push({
              product_id: p.id!,
              qty: remainder,
              price: p.selling_price || 0, // Unit price for single item
              subtotal: remainder * (p.selling_price || 0),
            });
          }
        } else {
          // Default to single unit if no package price defined
          finalSalesItems.push({
            product_id: p.id!,
            qty: item.qty,
            price: p.selling_price || 0,
            subtotal: item.qty * (p.selling_price || 0),
          });
        }
      }

      // Construct the timestamp for created_at. Use the selected date and current time.
      const currentTime = new Date().toLocaleTimeString('en-GB'); // e.g., "14:30:00"
      const createdAtTimestamp = `${transactionDate} ${currentTime}`;
      const salePayload: Sale = {
        customer_id: finalCustomerId,
        payment_method_id: selectedPaymentMethodId,
        total: finalTotal,
        paid: isDebt ? finalTotal : paidValue,
        change: isDebt ? 0 : change,
        points_earned: earnedPoints,
        points_redeemed: pointsToRedeem,
        created_at: createdAtTimestamp,
      };

      if (editSaleId) {
        await updateSale(editSaleId, salePayload, finalSalesItems);
        showSnackbar("Transaksi berhasil diperbarui", "success");
        resetForm();
        exitEditMode();
        navigation.navigate("SaleDetail", { saleId: editSaleId });
        return;
      }

      const saleId = await addSale(salePayload, finalSalesItems);

      showSnackbar("Transaksi berhasil disimpan", "success");
      resetForm();
      navigation.navigate("SaleDetail", { saleId });

    } catch (error) {
      console.error("Transaction saving error:", error);
      showSnackbar("Gagal menyimpan transaksi. Coba lagi.", "error");
    } finally {
      setIsSubmitting(false);
    }
  };

  // Helper function to clear specific error states
  const clearErrorOnInput = (setErrorFn: (err: null) => void) => {
    setErrorFn(null);
  };

  // Determine if the final checkout button should be disabled
  // Skip stock validation when editing existing transaction
  const stockIssue = editSaleId ? false : cart.some(i => i.qty > (i.product.stock || 0));
  const isCheckoutButtonDisabled =
    isSubmitting || // Prevent double submit
    cart.length === 0 || // Empty cart
    stockIssue || // Stock issue
    !transactionDate || // Missing date
    !selectedPaymentMethodId || // Missing payment method
    (change < 0 && !isDebt) || // Payment less than total, not debt
    (isDebt && !selectedCustomerId && !customerName.trim()) || // Debt requires customer
    (paidAmountError !== null && !isDebt) || // Paid amount validation failed, not debt
    (customerNameError !== null && isDebt) || // Customer name validation failed for debt
    loadingEditSale;

  const baseCheckoutLabel = editSaleId
    ? (isDebt ? "Perbarui Hutang" : "Perbarui Transaksi")
    : (isDebt ? "Simpan Hutang" : "Selesaikan & Bayar");
  const checkoutLabel = stockIssue ? "Stok Kurang" : baseCheckoutLabel;

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === "ios" ? "padding" : "height"}
      keyboardVerticalOffset={Platform.OS === "ios" ? 0 : 20} // Adjust if needed
    >
      <View style={{ flex: 1, backgroundColor: "#F3F4F6", paddingTop: 0 }}>
        {/* Top: Search Bar & Quick Actions */}
        <Surface elevation={2} style={styles.topSearchSurface}>
          <View style={styles.searchRow}>
            <TextInput
              label="Cari Produk" // Added label
              placeholder="Cari produk atau barcode..."
              value={searchQuery}
              onChangeText={setSearchQuery}
              mode="outlined"
              style={styles.searchInputPaper}
              outlineStyle={{ borderRadius: 12 }}
              left={<TextInput.Icon icon="magnify" />}
              right={searchQuery.length > 0 ? (
                <TextInput.Icon icon="close" onPress={() => setSearchQuery("")} />
              ) : (
                <TextInput.Icon icon="camera" onPress={startScan} />
              )}
            />
            <IconButton icon="history" size={24} onPress={() => navigation.navigate("SalesHistory")} style={styles.headerIconBtn} />
            <IconButton icon="refresh" size={24} onPress={handleReload} style={styles.headerIconBtn} />
          </View>

          {/* Search Results Overlay */}
          {searchQuery.length > 0 && (
            <Surface elevation={4} style={styles.searchResultOverlayPaper}>
              <FlatList
                data={filteredProducts}
                keyExtractor={(item) => item.id?.toString() ?? ""}
                showsVerticalScrollIndicator={false}
                keyboardShouldPersistTaps="handled"
                renderItem={({ item }) => (
                  <>
                    <List.Item
                      title={item.name}
                      description={`${item.code}\nRp ${item.selling_price?.toLocaleString("id-ID")}${item.package_price ? ` • Paket: Rp ${item.package_price?.toLocaleString("id-ID")}` : ''}`}
                      descriptionStyle={{ fontSize: 13 }}
                      left={props => <List.Icon {...props} icon="package-variant" />}
                      right={() => (
                        <View style={styles.productActionsPaper}>
                          <IconButton
                            icon="plus-circle-outline"
                            onPress={() => handleAddToCart(item, false)}
                            size={24}
                          />
                          {item.package_price && item.package_qty ? ( // Only show package add if package details exist
                            <IconButton
                              icon="package-variant-plus"
                              onPress={() => handleAddToCart(item, true)}
                              iconColor="#8B5CF6" // Indigo color
                              size={24}
                            />
                          ) : null}
                        </View>
                      )}
                    />
                    <Divider />
                  </>
                )}
                ListEmptyComponent={
                  <Text style={styles.emptyTextPaper}>Produk tidak ditemukan</Text>
                }
              />
            </Surface>
          )}
        </Surface>

        {/* Middle: Cart List - Expandable */}
        <View style={{ flex: 1, paddingVertical: 8 }}>
          <FlatList
            data={cart}
            keyExtractor={(item) => `${item.product.id}`}
            keyboardShouldPersistTaps="handled"
            contentContainerStyle={{ paddingHorizontal: 16, paddingBottom: 16 }}
            renderItem={({ item }) => {
              const info = getPriceBreakdown(item);
              // Check for stock validity directly in render (skip when editing)
              const isStockLow = editSaleId ? false : item.qty > (item.product.stock || 0);
              return (
                <Surface elevation={1} style={styles.cartItemSurface}>
                  <List.Item
                    title={item.product.name}
                    titleStyle={isStockLow ? { color: '#E11D48', fontWeight: 'bold' } : { fontWeight: '600' }}
                    description={`${info.label} • Rp ${Math.round(info.unitPrice).toLocaleString("id-ID")}${isStockLow ? `\n⚠️ Stok kurang (Sisa: ${item.product.stock})` : ''}`}
                    descriptionStyle={isStockLow ? { color: '#E11D48' } : undefined}
                    right={() => (
                      <View style={styles.cartQtyActions}>
                        <IconButton icon="minus-circle-outline" size={22} onPress={() => decreaseCartItemQty(item.product.id!)} disabled={item.qty <= 1 && !item.product.package_qty} />
                        <Text style={styles.cartQtyText}>{item.qty}</Text>
                        <IconButton icon="plus-circle-outline" size={22} onPress={() => handleAddToCart(item.product, false)} />
                        <IconButton icon="delete-outline" size={22} iconColor="#E11D48" onPress={() => handleRemoveFromCart(item.product.id!)} />
                      </View>
                    )}
                  />
                </Surface>
              );
            }}
            ListEmptyComponent={
              <View style={styles.emptyCartContainer}>
                <IconButton icon="cart-outline" size={64} style={{ opacity: 0.2 }} />
                <Text style={styles.emptyTextPaper}>Keranjang masih kosong</Text>
                <Button mode="outlined" onPress={() => setSearchQuery("")} style={{ marginTop: 16 }}>Mulai Belanja</Button>
              </View>
            }
          />
        </View>

        {/* Bottom: Transaction Details & Checkout */}
        <Surface elevation={5} style={[styles.checkoutSurface, { paddingBottom: Math.max(insets.bottom, 16) }]}>
          <ScrollView showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
            {/* Header for bottom section */}
            <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <Text variant="titleMedium" style={{ fontWeight: 'bold' }}>Detail Transaksi</Text>
              <TextInput
                label="Tanggal" // Added label for clarity
                value={transactionDate}
                onChangeText={(text) => { setTransactionDate(text); clearErrorOnInput(setTransactionDateError); }} // Clear error on input
                mode="flat"
                placeholder="YYYY-MM-DD"
                style={styles.dateInputFlat} // Custom style for flat input
                contentStyle={{ paddingHorizontal: 0 }}
                underlineColor="transparent"
                activeUnderlineColor="transparent"
                error={!!transactionDateError}
                right={<TextInput.Icon
                  icon="calendar"
                  size={24}
                  forceTextInputFocus={false}
                  onPress={() => setShowDatePicker(true)}
                />}
              />
            </View>
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
                    clearErrorOnInput(setTransactionDateError); // Clear error on selection
                  }
                }}
              />
            )}

            {/* Customer & Payment Options Row */}
            <View style={styles.bottomSelectorsRow}>
              {/* Customer Selection */}
              <View style={{ flex: 1, marginRight: 8 }}>
                <Text variant="labelSmall" style={styles.bottomLabel}>Pelanggan:</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipScrollView}>
                  {/* "Umum" chip */}
                  <Chip
                    selected={!selectedCustomerId && !customerName.trim()}
                    onPress={() => {
                      setSelectedCustomerId(null);
                      setCustomerName("");
                      clearErrorOnInput(setCustomerNameError);
                    }}
                    style={styles.bottomChip}
                    selectedColor="#6366F1"
                    compact
                  >
                    Umum
                  </Chip>
                  {/* Map up to 5 customers */}
                  {customers.slice(0, 5).map(c => (
                    <Chip
                      key={c.id}
                      selected={selectedCustomerId === c.id}
                      onPress={() => {
                        setSelectedCustomerId(c.id!);
                        setCustomerName(""); // Clear manual name when selecting existing customer
                        clearErrorOnInput(setCustomerNameError);
                      }}
                      style={styles.bottomChip}
                      selectedColor="#6366F1"
                      compact
                    >
                      {c.name}
                    </Chip>
                  ))}
                  {/* Add button to navigate to customer list if more than 5 */}
                  {customers.length > 5 && (
                    <Chip
                      icon="account-plus"
                      onPress={() => navigation.navigate("Customers")} // Assuming a Customers screen exists
                      style={styles.bottomChip}
                      selectedColor="#6366F1"
                      compact
                    >
                      Lainnya
                    </Chip>
                  )}
                </ScrollView>
                {/* Manual Customer Name Input */}
                <TextInput
                  label="Nama Pelanggan"
                  placeholder="Masukkan nama pelanggan baru..."
                  value={customerName}
                  onChangeText={(t) => {
                    setCustomerName(t);
                    if (t.trim()) { // If name is entered, deselect existing customer
                      setSelectedCustomerId(null);
                    }
                    clearErrorOnInput(setCustomerNameError);
                  }}
                  mode="outlined"
                  dense
                  style={styles.bottomInput}
                  outlineStyle={{ borderRadius: 8 }}
                  selectTextOnFocus
                  error={!!customerNameError}
                />
                <HelperText type="error" visible={!!customerNameError}>
                  {customerNameError}
                </HelperText>
              </View>

              {/* Payment Method Selection */}
              <View style={{ flex: 1 }}>
                <Text variant="labelSmall" style={styles.bottomLabel}>Metode Bayar:</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipScrollView}>
                  {paymentMethods.map(m => (
                    <Chip
                      key={m.id}
                      selected={selectedPaymentMethodId === m.id}
                      onPress={() => {
                        setSelectedPaymentMethodId(m.id!);
                        clearErrorOnInput(setSelectedPaymentMethodError);
                      }}
                      style={styles.bottomChip}
                      selectedColor="#6366F1"
                      compact
                    >
                      {m.name}
                    </Chip>
                  ))}
                </ScrollView>
                <HelperText type="error" visible={!!selectedPaymentMethodError}>
                  {selectedPaymentMethodError}
                </HelperText>
                <TextInput
                  label="Jumlah Bayar" // Added label
                  value={paidAmount ? formatCurrency(paidAmount) : ""}
                  onChangeText={(text) => {
                    // Store raw numeric value (without formatting) in state
                    const rawValue = text.replace(/[^0-9]/g, "");
                    setPaidAmount(rawValue);
                    clearErrorOnInput(setPaidAmountError);
                  }}
                  keyboardType="numeric"
                  mode="outlined"
                  dense
                  style={styles.bottomInput}
                  outlineStyle={{ borderRadius: 8 }}
                  selectTextOnFocus
                  left={<TextInput.Affix text="Rp " />}
                  error={!!paidAmountError}
                  disabled={isDebt} // Disable paid amount input for debt
                />
                <HelperText type="error" visible={!!paidAmountError}>
                  {paidAmountError}
                </HelperText>
              </View>
            </View>

            {/* Loyalty Section (Inline) */}
            {selectedCustomerId && availablePoints > 0 && (
              <View style={styles.loyaltyInline}>
                <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                  <List.Icon icon="star-circle" color="#FB7185" style={{ margin: 0 }} />
                  <Text variant="labelSmall" style={{ marginLeft: 4 }}>Poin: {availablePoints} Pts</Text>
                </View>
                <Button
                  mode={redeemPoints ? "contained" : "text"}
                  onPress={() => {
                    setRedeemPoints(!redeemPoints);
                    // Clear paid amount error if redeeming/unredeeming points affects it
                    if (!isDebt) setPaidAmountError(null);
                  }}
                  compact
                  labelStyle={{ fontSize: 10 }}
                  buttonColor={redeemPoints ? "#FB7185" : undefined}
                  textColor={redeemPoints ? "#FFF" : "#FB7185"}
                >
                  {redeemPoints ? "Batal Pakai Poin" : "Tukar Poin?"}
                </Button>
              </View>
            )}

            <Divider style={{ marginVertical: 12, opacity: 0.5 }} />

            {/* Summary Row */}
            <View style={styles.summaryRow}>
              <View style={{ flex: 1 }}>
                <Text variant="labelSmall" style={{ color: '#64748B' }}>Total Yang Harus Dibayar</Text>
                <Text variant="headlineSmall" style={styles.finalTotalText}>
                  Rp {finalTotal.toLocaleString("id-ID")}
                </Text>
                {selectedCustomerId && (
                  <Text variant="labelSmall" style={{ color: '#10B981', marginTop: 2 }}>
                    <IconButton icon="star-plus" size={12} style={{ margin: 0, padding: 0 }} />
                    Dapat {earnedPoints} Poin
                  </Text>
                )}
              </View>
              <View style={{ alignItems: 'flex-end' }}>
                <Text variant="labelSmall" style={{ color: '#64748B' }}>
                  {isDebt
                    ? "Sisa Hutang"
                    : "Kembalian"}
                </Text>
                <Text variant="titleLarge" style={[styles.changeText, { color: change >= 0 ? '#10B981' : '#EF4444' }]}>
                  Rp {Math.abs(change).toLocaleString("id-ID")}
                </Text>
              </View>
            </View>

            {/* Final Checkout Button */}
            <Button
              mode="contained"
              onPress={handleFinishTransaction}
              disabled={isCheckoutButtonDisabled}
              loading={isSubmitting}
              style={styles.finalCheckoutBtn}
              contentStyle={{ height: 50 }}
              labelStyle={{ fontSize: 16, fontWeight: 'bold' }}
              icon={isSubmitting ? undefined : "cart-check"}
            >
              {checkoutLabel}
            </Button>
            {editSaleId && (
              <Button
                mode="outlined"
                onPress={cancelEdit}
                disabled={loadingEditSale}
                style={[styles.finalCheckoutBtn, { marginTop: 8 }]}
                contentStyle={{ height: 50 }}
                labelStyle={{ fontSize: 16, fontWeight: 'bold' }}
                icon="close-circle"
              >
                Batal Edit
              </Button>
            )}
          </ScrollView>
        </Surface>

        {/* Snackbar for feedback */}
        <Snackbar
          visible={snackbarVisible}
          onDismiss={() => setSnackbarVisible(false)}
          duration={3000} // Standard duration
          style={{ backgroundColor: snackbarType === 'success' ? '#10B981' : '#EF4444' }}
        >
          {snackbarMessage}
        </Snackbar>

        {/* Barcode Scanner Modal */}
        <BarcodeScannerModal
          visible={isScanning}
          onScanned={handleBarcodeScanned}
          onClose={() => setIsScanning(false)}
        />
      </View>
    </KeyboardAvoidingView >
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F3F4F6",
  },
  topSearchSurface: {
    padding: 12,
    backgroundColor: '#FFF',
    borderBottomLeftRadius: 16,
    borderBottomRightRadius: 16,
    zIndex: 100,
  },
  searchRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  searchInputPaper: {
    flex: 1,
    backgroundColor: '#FFF',
  },
  headerIconBtn: {
    margin: 0,
    marginLeft: 4,
  },
  scanBtnPaper: {
    marginLeft: 8,
    borderRadius: 8,
  },
  searchResultOverlayPaper: {
    position: "absolute",
    top: 70, // Position below the search bar
    left: 12,
    right: 12,
    backgroundColor: "white",
    borderRadius: 12,
    maxHeight: 400,
    zIndex: 1000,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: '#E5E7EB',
    elevation: 8, // Shadow for overlay
  },
  productActionsPaper: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  cartItemSurface: {
    marginBottom: 8,
    borderRadius: 12,
    overflow: 'hidden',
    backgroundColor: '#FFF',
  },
  cartQtyActions: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  cartQtyText: {
    fontWeight: 'bold',
    minWidth: 28,
    textAlign: 'center',
    fontSize: 16,
  },
  emptyCartContainer: {
    flex: 1,
    padding: 60,
    alignItems: 'center',
    justifyContent: 'center',
    opacity: 0.5,
  },
  emptyTextPaper: {
    marginTop: 8,
    textAlign: 'center',
    color: '#9CA3AF',
    fontSize: 16,
  },
  checkoutSurface: {
    padding: 16,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    backgroundColor: '#FFF',
    elevation: 10, // Shadow for the checkout area
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.1,
    shadowRadius: 12,
    maxHeight: '55%',
  },
  dateInputFlat: {
    backgroundColor: 'transparent',
    height: 40,
    fontSize: 13,
    width: 150,
    marginLeft: 'auto', // Push to the right
  },
  bottomSelectorsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  bottomLabel: {
    color: '#64748B',
    marginBottom: 4,
    fontWeight: '700',
  },
  chipScrollView: {
    marginBottom: 4, // Space between scroll view and text input
  },
  bottomChip: {
    marginRight: 6,
    height: 32,
    justifyContent: 'center', // Center content vertically
  },
  bottomInput: {
    backgroundColor: '#FFF',
    marginTop: 4,
  },
  loyaltyInline: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFF5F7', // Light red background for loyalty points
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    justifyContent: 'space-between',
    marginTop: 4,
    marginBottom: 12, // Space before divider
  },
  summaryRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "flex-start",
    marginBottom: 16,
  },
  finalTotalText: {
    fontWeight: "900",
    color: "#6366F1", // Indigo color for emphasis
    lineHeight: 32,
  },
  changeText: {
    fontWeight: "bold",
    lineHeight: 32,
  },
  finalCheckoutBtn: {
    borderRadius: 12,
    marginTop: 8,
  },
});
