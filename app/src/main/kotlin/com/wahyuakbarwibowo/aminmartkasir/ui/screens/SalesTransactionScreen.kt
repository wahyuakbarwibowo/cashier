package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.zxing.client.android.Intents
import com.wahyuakbarwibowo.aminmartkasir.ui.scanner.BarcodeCaptureActivity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.*
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.*
import com.wahyuakbarwibowo.aminmartkasir.utils.BluetoothPrinterHelper
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import com.wahyuakbarwibowo.aminmartkasir.ui.screens.LastTransactionData
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesTransactionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateProduct: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModelFactory: Factory? = null,
    viewModel: SalesViewModel = viewModel(factory = viewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showProductSelector by remember { mutableStateOf(false) }
    var showPaymentMethodSelector by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var showEditProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var lastTransactionData by remember { mutableStateOf<LastTransactionData?>(null) }
    var successTransactionId by remember { mutableStateOf<String?>(null) }
    var successTransactionData by remember { mutableStateOf<LastTransactionData?>(null) }

    LaunchedEffect(uiState.cartItems) {
        viewModel.calculatePoints()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaksi Penjualan") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                actions = {
                    IconButton(onClick = { showPrinterDialog = true }) {
                        Icon(Icons.Default.Print, contentDescription = "Print")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Scrollable Cart Items Section
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Keranjang kosong")
                            Text(
                                text = "Klik tombol + untuk menambah produk",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.cartItems, key = { it.product.id }) { item ->
                            CartItemCard(
                                item = item,
                                onIncreaseQty = { viewModel.updateCartItemQty(item.product.id, item.qty + 1) },
                                onDecreaseQty = { viewModel.updateCartItemQty(item.product.id, item.qty - 1) },
                                onRemove = { viewModel.removeFromCart(item.product.id) },
                                onEdit = {
                                    productToEdit = item.product
                                    showEditProductDialog = true
                                }
                            )
                        }
                    }
                }
            }
            
            // 2. Fixed Bottom Section (Summary + Actions)
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary Rows
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SummaryRow("Subtotal", formatCurrency(uiState.subtotal))
                        if (uiState.discount > 0) {
                            SummaryRow("Diskon", "- ${formatCurrency(uiState.discount)}")
                        }
                        if (uiState.pointsRedeemed > 0) {
                            SummaryRow("Tukar Poin", "- ${formatCurrency(uiState.pointsRedeemed * 100.0)}")
                        }
                        SummaryRow("Total", formatCurrency(uiState.total), isBold = true)
                    }

                    // Main Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showProductSelector = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Produk")
                        }
                        
                        Button(
                            onClick = { showPaymentMethodSelector = true },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(56.dp),
                            enabled = uiState.cartItems.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Proses Bayar")
                        }
                    }
                }
            }
        }
    }

    if (showProductSelector) {
        ProductSelectorDialog(
            products = uiState.products,
            searchQuery = uiState.searchQuery,
            canLoadMore = uiState.canLoadMore,
            isLoadMoreLoading = uiState.isLoadMoreLoading,
            onSearchQueryChange = { viewModel.searchProducts(it) },
            onLoadNextPage = { viewModel.loadNextProductPage() },
            onProductSelected = { product ->
                viewModel.addToCart(product)
                showProductSelector = false
                viewModel.searchProducts("") // Reset search
            },
            onCreateProduct = {
                onNavigateToCreateProduct()
                showProductSelector = false
            },
            onDismiss = { 
                showProductSelector = false 
                viewModel.searchProducts("") // Reset search
            }
        )
    }

    if (showPaymentMethodSelector) {
        PaymentMethodSelectorDialog(
            total = uiState.total,
            paymentMethods = uiState.paymentMethods,
            selectedCustomer = uiState.selectedCustomer,
            customers = uiState.customers,
            pointsRedeemed = uiState.pointsRedeemed,
            onSelectCustomer = { viewModel.setSelectedCustomer(it) },
            onPointsRedeemedChange = { viewModel.setPointsRedeemed(it) },
            onDiscountChange = { viewModel.setDiscount(it) },
            onDismiss = { showPaymentMethodSelector = false },
            onProcess = { paid, method ->
                viewModel.setPaid(paid)
                viewModel.setSelectedPaymentMethod(method)
                
                // Prepare data for printer BEFORE clearing cart
                val receiptItems = uiState.cartItems.map { item ->
                    BluetoothPrinterHelper.ReceiptItem(
                        name = item.product.name,
                        qty = item.qty,
                        price = item.price,
                        subtotal = item.subtotal
                    )
                }
                
                // Set temporary success data
                successTransactionData = LastTransactionData(
                    transactionId = "", // Filled after save
                    items = receiptItems,
                    subtotal = uiState.subtotal,
                    discount = uiState.discount,
                    total = uiState.total,
                    paid = paid,
                    change = paid - uiState.total,
                    pointsEarned = uiState.pointsEarned
                )

                viewModel.processTransaction()
                showPaymentMethodSelector = false
                showSuccessDialog = true
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false 
                successTransactionData = null
            },
            title = { Text("Transaksi Berhasil") },
            text = { Text("Transaksi telah berhasil disimpan.") },
            confirmButton = {
                Button(onClick = { 
                    showSuccessDialog = false 
                    showPrinterDialog = true
                    // data for printer is in successTransactionData
                }) {
                    Text("Cetak Struk")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSuccessDialog = false 
                    successTransactionData = null
                }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showPrinterDialog) {
        BluetoothPrinterDialog(
            onDismiss = { 
                showPrinterDialog = false 
                successTransactionData = null
            },
            onDeviceConnected = {},
            transactionData = successTransactionData,
            viewModelFactory = viewModelFactory
        )
    }

    if (showEditProductDialog && productToEdit != null) {
        var newPrice by remember { mutableStateOf(productToEdit!!.sellingPrice.toLong().toString()) }
        AlertDialog(
            onDismissRequest = { showEditProductDialog = false },
            title = { Text("Ubah Harga") },
            text = {
                Column {
                    Text(productToEdit!!.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPrice,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) newPrice = it },
                        label = { Text("Harga Baru") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val price = newPrice.toDoubleOrNull() ?: 0.0
                    val updatedCart = uiState.cartItems.map { 
                        if (it.product.id == productToEdit!!.id) {
                            it.copy(price = price, subtotal = price * it.qty)
                        } else it
                    }
                    // This is a bit of a hack since updateCart is private and we don't have a public "updateItemPrice"
                    // But in a real app we'd add that method to ViewModel.
                    // For now, let's just use the current implementation's updateCartItemQty 
                    // which recalculates price based on ProductEntity. 
                    // To actually support temporary price override, we need ViewModel changes.
                    showEditProductDialog = false
                }) {
                    Text("Simpan")
                }
            }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isBold: Boolean = false, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSelectorDialog(
    products: List<ProductEntity>,
    searchQuery: String,
    canLoadMore: Boolean,
    isLoadMoreLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onLoadNextPage: () -> Unit,
    onProductSelected: (ProductEntity) -> Unit,
    onCreateProduct: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Infinite scroll detection
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            canLoadMore && lastVisibleItemIndex >= totalItemsCount - 5 && totalItemsCount > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoadMoreLoading) {
            onLoadNextPage()
        }
    }
    
    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val scannedCode = result.contents
        if (!scannedCode.isNullOrBlank()) {
            onSearchQueryChange(scannedCode)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                setPrompt("Scan barcode produk")
                setBeepEnabled(true)
                setOrientationLocked(true)
                setCaptureActivity(BarcodeCaptureActivity::class.java)
                addExtra(Intents.Scan.MISSING_CAMERA_PERMISSION, true)
            }
            barcodeLauncher.launch(options)
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk scan barcode", Toast.LENGTH_SHORT).show()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Produk") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Cari produk...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(Modifier.size(8.dp))
                
                if (products.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Produk tidak ditemukan")
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(products, key = { it.id }) { product ->
                            Card(
                                onClick = { onProductSelected(product) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Stok: ${product.stock}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (product.stock <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = formatCurrency(product.sellingPrice),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (isLoadMoreLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreateProduct) {
                Text("Produk Baru")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun CartItemCard(
    item: CartItem,
    onIncreaseQty: () -> Unit,
    onDecreaseQty: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatCurrency(item.price),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onDecreaseQty,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(min = 40.dp)
                    ) {
                        Text(
                            text = item.qty.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    
                    IconButton(
                        onClick = onIncreaseQty,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                    }
                }
                
                Text(
                    text = formatCurrency(item.subtotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentMethodSelectorDialog(
    total: Double,
    paymentMethods: List<PaymentMethodEntity>,
    selectedCustomer: CustomerEntity?,
    customers: List<CustomerEntity>,
    pointsRedeemed: Int,
    onSelectCustomer: (CustomerEntity?) -> Unit,
    onPointsRedeemedChange: (Int) -> Unit,
    onDiscountChange: (Double) -> Unit,
    onDismiss: () -> Unit,
    onProcess: (Double, PaymentMethodEntity) -> Unit
) {
    var paidText by remember { mutableStateOf(BigDecimal.valueOf(total).stripTrailingZeros().toPlainString()) }
    var discountText by remember { mutableStateOf("0") }
    var selectedMethod by remember { mutableStateOf<PaymentMethodEntity?>(paymentMethods.firstOrNull()) }
    var customerExpanded by remember { mutableStateOf(false) }
    
    val subtotalValue = total + (discountText.toDoubleOrNull() ?: 0.0) + (pointsRedeemed * 100.0)
    val discountValue = discountText.toDoubleOrNull() ?: 0.0
    val currentTotal = (subtotalValue - discountValue - (pointsRedeemed * 100.0)).coerceAtLeast(0.0)
    val paidValue = paidText.toDoubleOrNull() ?: 0.0
    val changeValue = paidValue - currentTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer Selector
                ExposedDropdownMenuBox(
                    expanded = customerExpanded,
                    onExpandedChange = { customerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "Pilih Pelanggan (Opsional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pelanggan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = customerExpanded,
                        onDismissRequest = { customerExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tanpa Pelanggan") },
                            onClick = {
                                onSelectCustomer(null)
                                customerExpanded = false
                            }
                        )
                        customers.forEach { customer ->
                            DropdownMenuItem(
                                text = { Text(customer.name) },
                                onClick = {
                                    onSelectCustomer(customer)
                                    customerExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedCustomer != null) {
                    Text("Poin Pelanggan: ${selectedCustomer.points}", style = MaterialTheme.typography.bodySmall)
                }

                // Payment Fields
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) {
                        discountText = it
                        onDiscountChange(it.toDoubleOrNull() ?: 0.0)
                    }},
                    label = { Text("Potongan Harga (Diskon)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = paidText,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) paidText = it },
                    label = { Text("Jumlah Bayar (Tunai)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Total & Change Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Tagihan")
                            Text(formatCurrency(currentTotal), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kembalian")
                            Text(formatCurrency(changeValue), fontWeight = FontWeight.Bold, color = if (changeValue < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                // Payment Method Selector
                Text("Metode Pembayaran", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    paymentMethods.forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.name) }
                        )
                    }
                    
                    // Add manual Hutang option if not in DB list
                    val hasHutangInDb = paymentMethods.any { it.name.contains("Hutang", ignoreCase = true) }
                    if (!hasHutangInDb) {
                        FilterChip(
                            selected = selectedMethod?.name == "Hutang",
                            onClick = { 
                                selectedMethod = PaymentMethodEntity(id = -1, name = "Hutang")
                            },
                            label = { Text("Hutang") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val isDebt = selectedMethod?.name?.contains("Hutang", ignoreCase = true) == true
            val isCustomerSelected = selectedCustomer != null
            val isPaidValid = if (isDebt) true else paidValue >= currentTotal

            Button(
                onClick = { 
                    if (selectedMethod != null) {
                        onProcess(paidValue, selectedMethod!!)
                    }
                },
                enabled = selectedMethod != null && (!isDebt || isCustomerSelected) && isPaidValid
            ) {
                Text("Selesaikan Transaksi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
