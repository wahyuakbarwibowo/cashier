package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

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
    var successTransactionData by remember { mutableStateOf<LastTransactionData?>(null) }
    val cartListState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(uiState.cartItems) {
        viewModel.calculatePoints()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Kasir Retail", style = MaterialTheme.typography.titleMedium)
                        if (uiState.cartItems.isNotEmpty()) {
                            Text(
                                "${uiState.cartItems.size} Item terpilih", 
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                actions = {
                    if (uiState.cartItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Kosongkan", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Cart Items Section
                if (uiState.cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(100.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.padding(24.dp).size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                "Keranjang masih kosong", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mulai tambahkan produk untuk berjualan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { showProductSelector = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Search, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Cari & Tambah Produk")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        state = cartListState,
                        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 120.dp), // Extra bottom padding for dock
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            
            // 2. Modern Floating Bottom Dock
            if (uiState.cartItems.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Summary Row (Simplified for Dock)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    "Total Bayar", 
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(uiState.total),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Button(
                                onClick = { showPaymentMethodSelector = true },
                                modifier = Modifier.height(56.dp).widthIn(min = 140.dp),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp)
                            ) {
                                Text("BAYAR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }
                    }
                }
            }

            // Quick Add Floating Button when cart is not empty
            if (uiState.cartItems.isNotEmpty()) {
                SmallFloatingActionButton(
                    onClick = { showProductSelector = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 100.dp, end = 24.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
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
            isRefreshing = uiState.isRefreshingProducts,
            onSearchQueryChange = { viewModel.searchProducts(it) },
            onLoadNextPage = { viewModel.loadNextProductPage() },
            onRefresh = { viewModel.refreshProducts() },
            onProductSelected = { product ->
                viewModel.addToCart(product)
                // showProductSelector = false // Allow multiple add
                Toast.makeText(context, "${product.name} ditambah", Toast.LENGTH_SHORT).show()
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
                
                val receiptItems = uiState.cartItems.map { item ->
                    BluetoothPrinterHelper.ReceiptItem(
                        name = item.product.name,
                        qty = item.qty,
                        price = item.price,
                        subtotal = item.subtotal
                    )
                }
                
                successTransactionData = LastTransactionData(
                    transactionId = "", 
                    items = receiptItems,
                    subtotal = uiState.subtotal,
                    discount = uiState.discount,
                    total = uiState.total,
                    paid = paid,
                    change = paid - uiState.total
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
            confirmButton = {
                Button(
                    onClick = { 
                        showSuccessDialog = false 
                        showPrinterDialog = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Print, null)
                    Spacer(Modifier.width(8.dp))
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
            },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(8.dp))
                    Text("Berhasil")
                }
            },
            text = { Text("Transaksi telah berhasil disimpan.") }
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
            confirmButton = {
                Button(onClick = {
                    showEditProductDialog = false
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProductDialog = false }) { Text("Batal") }
            },
            title = { Text("Ubah Harga Sementara") },
            text = {
                Column {
                    Text(productToEdit!!.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Hanya berlaku untuk transaksi ini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPrice,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) newPrice = it },
                        label = { Text("Harga Baru") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        )
    }
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
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
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatCurrency(item.price),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large tactile qty controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onDecreaseQty,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(20.dp))
                    }
                    
                    Text(
                        text = item.qty.toString(),
                        modifier = Modifier.widthIn(min = 32.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    IconButton(
                        onClick = onIncreaseQty,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(item.subtotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSelectorDialog(
    products: List<ProductEntity>,
    searchQuery: String,
    canLoadMore: Boolean,
    isLoadMoreLoading: Boolean,
    isRefreshing: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onLoadNextPage: () -> Unit,
    onRefresh: () -> Unit,
    onProductSelected: (ProductEntity) -> Unit,
    onCreateProduct: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

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
    
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pilih Produk", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Cari nama atau barcode...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            IconButton(onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) }) {
                                Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(Modifier.size(12.dp))
                    
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (products.isEmpty() && !isRefreshing) {
                            Box(
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Produk tidak ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(products, key = { it.id }) { product ->
                                    Card(
                                        onClick = { onProductSelected(product) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(product.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    "Stok: ${product.stock}", 
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (product.stock <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                formatCurrency(product.sellingPrice),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                if (isLoadMoreLoading) {
                                    item {
                                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = onCreateProduct,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Produk Baru")
                    }
                }
            }
        }
    )
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

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Selesaikan Pembayaran", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    // Bill Display
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                            Text("Total Tagihan", style = MaterialTheme.typography.labelLarge)
                            Text(
                                formatCurrency(currentTotal), 
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Customer Selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pelanggan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        ExposedDropdownMenuBox(
                            expanded = customerExpanded,
                            onExpandedChange = { customerExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.name ?: "Pilih Pelanggan",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = customerExpanded,
                                onDismissRequest = { customerExpanded = false }
                            ) {
                                DropdownMenuItem(text = { Text("Tanpa Pelanggan") }, onClick = { onSelectCustomer(null); customerExpanded = false })
                                customers.forEach { customer ->
                                    DropdownMenuItem(text = { Text(customer.name) }, onClick = { onSelectCustomer(customer); customerExpanded = false })
                                }
                            }
                        }
                    }

                    // Input paid and discount
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) {
                                discountText = it
                                onDiscountChange(it.toDoubleOrNull() ?: 0.0)
                            }},
                            label = { Text("Diskon") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = paidText,
                            onValueChange = { if (it.all { ch -> ch.isDigit() }) paidText = it },
                            label = { Text("Dibayar") },
                            prefix = { Text("Rp ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Change Info
                    if (changeValue != 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (changeValue > 0) "Kembalian" else "Kurang", style = MaterialTheme.typography.titleMedium)
                            Text(
                                formatCurrency(changeValue), 
                                style = MaterialTheme.typography.titleLarge, 
                                fontWeight = FontWeight.ExtraBold,
                                color = if (changeValue < 0) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                            )
                        }
                    }

                    // Payment Method
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Metode Pembayaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            paymentMethods.forEach { method ->
                                FilterChip(
                                    selected = selectedMethod == method,
                                    onClick = { selectedMethod = method },
                                    label = { Text(method.name) },
                                    shape = CircleShape
                                )
                            }
                            
                            val hasHutangInDb = paymentMethods.any { it.name.contains("Hutang", ignoreCase = true) }
                            if (!hasHutangInDb) {
                                FilterChip(
                                    selected = selectedMethod?.name == "Hutang",
                                    onClick = { selectedMethod = PaymentMethodEntity(id = -1, name = "Hutang") },
                                    label = { Text("Hutang") },
                                    shape = CircleShape
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Final Action
                    val isDebt = selectedMethod?.name?.contains("Hutang", ignoreCase = true) == true
                    val isCustomerSelected = selectedCustomer != null
                    val isPaidValid = if (isDebt) true else paidValue >= currentTotal

                    Button(
                        onClick = { if (selectedMethod != null) onProcess(paidValue, selectedMethod!!) },
                        enabled = selectedMethod != null && (!isDebt || isCustomerSelected) && isPaidValid,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("KONFIRMASI PEMBAYARAN", fontWeight = FontWeight.Bold)
                    }
                    
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Batal")
                    }
                }
            }
        }
    )
}
