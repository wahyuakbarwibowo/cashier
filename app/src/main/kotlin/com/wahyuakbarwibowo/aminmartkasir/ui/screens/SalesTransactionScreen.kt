package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
                        .padding(bottom = 12.dp) // Small padding above bottom nav
                ) {
                    // Summary Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = formatCurrency(uiState.total),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (uiState.pointsEarned > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Potensi Poin", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = "+${uiState.pointsEarned} poin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                    
                    // Action Buttons (Produk & Pembayaran)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showProductSelector = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.size(4.dp))
                            Text("Produk")
                        }
                        
                        OutlinedButton(
                            onClick = { showPaymentMethodSelector = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null)
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = uiState.selectedPaymentMethod?.name ?: "Pembayaran",
                                maxLines = 1
                            )
                        }
                    }
                    
                    // Main Process Button
                    Button(
                        onClick = {
                            val transactionId = "TRX-${System.currentTimeMillis()}"
                            val transactionData = LastTransactionData(
                                transactionId = transactionId,
                                items = uiState.cartItems.map { cartItem ->
                                    BluetoothPrinterHelper.ReceiptItem(
                                        name = cartItem.product.name,
                                        qty = cartItem.qty,
                                        price = cartItem.price,
                                        subtotal = cartItem.subtotal
                                    )
                                },
                                subtotal = uiState.subtotal,
                                discount = uiState.discount,
                                total = uiState.total,
                                paid = uiState.paid,
                                change = uiState.change,
                                pointsEarned = uiState.pointsEarned
                            )

                            successTransactionId = transactionId
                            successTransactionData = transactionData
                            lastTransactionData = transactionData
                            viewModel.processTransaction()
                            showSuccessDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = uiState.cartItems.isNotEmpty() && uiState.paid >= uiState.total
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Proses Transaksi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    if (showProductSelector) {
        ProductSelectorDialog(
            products = uiState.allProducts,
            onProductSelected = { product ->
                viewModel.addToCart(product)
                showProductSelector = false
            },
            onCreateProduct = {
                showProductSelector = false
                onNavigateToCreateProduct()
            },
            onDismiss = { showProductSelector = false }
        )
    }
    
    if (showPaymentMethodSelector) {
        PaymentMethodSelectorDialog(
            paymentMethods = uiState.paymentMethods,
            selectedPaymentMethod = uiState.selectedPaymentMethod,
            onPaymentMethodSelected = { viewModel.setSelectedPaymentMethod(it) },
            onPaidAmountChanged = { viewModel.setPaid(it) },
            totalAmount = uiState.total,
            onDismiss = { showPaymentMethodSelector = false }
        )
    }

    if (showPrinterDialog) {
        BluetoothPrinterDialog(
            onDismiss = { showPrinterDialog = false },
            onDeviceConnected = {
                // Print last transaction if available
                lastTransactionData?.let { data ->
                    // Print will be triggered from dialog
                }
            },
            transactionData = lastTransactionData,
            viewModelFactory = viewModelFactory
        )
    }

    if (showEditProductDialog && productToEdit != null) {
        EditProductDialog(
            product = productToEdit!!,
            onDismiss = {
                showEditProductDialog = false
                productToEdit = null
            },
            onProductUpdated = { updatedProduct ->
                // Remove old item and add updated product
                viewModel.removeFromCart(productToEdit!!.id)
                viewModel.addToCart(updatedProduct)
                showEditProductDialog = false
                productToEdit = null
            }
        )
    }

    if (showSuccessDialog && successTransactionId != null && successTransactionData != null) {
        val transactionId = successTransactionId ?: return
        val transactionData = successTransactionData ?: return

        // Show success dialog with print option
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                successTransactionId = null
                successTransactionData = null
            },
            title = { Text("Transaksi Berhasil") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Transaction ID: $transactionId")
                    Text("Total: ${formatCurrency(transactionData.total)}")
                    Text("Poin Earned: ${transactionData.pointsEarned}")
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showPrinterDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("Cetak")
                    }
                    TextButton(
                        onClick = {
                            showSuccessDialog = false
                            successTransactionId = null
                            successTransactionData = null
                        }
                    ) {
                        Text("Selesai")
                    }
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDecreaseQty) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                    }
                    Text(
                        text = item.qty.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(onClick = onIncreaseQty) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                    }
                }
                
                Text(
                    text = formatCurrency(item.subtotal),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
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
    onProductSelected: (ProductEntity) -> Unit,
    onCreateProduct: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val scannedCode = result.contents
        if (!scannedCode.isNullOrBlank()) {
            searchQuery = scannedCode
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
                    onValueChange = { searchQuery = it },
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
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val filteredProducts = if (searchQuery.isBlank()) {
                        products
                    } else {
                        products.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            (it.code?.contains(searchQuery, ignoreCase = true) == true)
                        }
                    }
                    
                    items(filteredProducts, key = { it.id }) { product ->
                        Card(
                            onClick = { onProductSelected(product) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Stok: ${product.stock}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = formatCurrency(product.sellingPrice),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onCreateProduct) {
                    Text("Buat Produk")
                }
                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSelectorDialog(
    paymentMethods: List<PaymentMethodEntity>,
    selectedPaymentMethod: PaymentMethodEntity?,
    onPaymentMethodSelected: (PaymentMethodEntity?) -> Unit,
    onPaidAmountChanged: (Double) -> Unit,
    totalAmount: Double,
    onDismiss: () -> Unit
) {
    var paidAmount by remember(totalAmount) { mutableStateOf(formatNumberForInput(totalAmount)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Payment Method Selection
                Text(
                    text = "Metode Pembayaran",
                    style = MaterialTheme.typography.titleSmall
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Card(
                            onClick = { onPaymentMethodSelected(null) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPaymentMethod == null) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Text(
                                text = "Tunai",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    items(paymentMethods, key = { it.id }) { method ->
                        Card(
                            onClick = { onPaymentMethodSelected(method) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPaymentMethod?.id == method.id) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Text(
                                text = method.name,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Paid Amount Input
                Text(
                    text = "Jumlah Bayar",
                    style = MaterialTheme.typography.titleSmall
                )
                
                OutlinedTextField(
                    value = paidAmount,
                    onValueChange = {
                        paidAmount = it
                        onPaidAmountChanged(it.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Dibayar") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    leadingIcon = { Text("Rp") }
                )
                
                val change = (paidAmount.toDoubleOrNull() ?: 0.0) - totalAmount
                if (change >= 0) {
                    Text(
                        text = "Kembalian: ${formatCurrency(change)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Kurang: ${formatCurrency(-change)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

private fun formatNumberForInput(value: Double): String {
    // Avoid showing "0.0" in numeric inputs; keep the raw number without trailing zeros.
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onProductUpdated: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var code by remember { mutableStateOf(product.code ?: "") }
    var sellingPrice by remember { mutableStateOf(formatNumberForInput(product.sellingPrice)) }
    var stock by remember { mutableStateOf(product.stock.toString()) }
    var purchasePrice by remember { mutableStateOf(formatNumberForInput(product.purchasePrice)) }
    var packagePrice by remember { mutableStateOf(formatNumberForInput(product.packagePrice)) }
    var packageQty by remember { mutableStateOf(product.packageQty.toString()) }
    var discount by remember { mutableStateOf(formatNumberForInput(product.discount)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Produk") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Produk") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Kode Produk") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Harga Jual") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        leadingIcon = { Text("Rp") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("Harga Beli") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        leadingIcon = { Text("Rp") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Stok") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
                item {
                    OutlinedTextField(
                        value = packagePrice,
                        onValueChange = { packagePrice = it },
                        label = { Text("Harga Paket") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        leadingIcon = { Text("Rp") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = packageQty,
                        onValueChange = { packageQty = it },
                        label = { Text("Jumlah Paket") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
                item {
                    OutlinedTextField(
                        value = discount,
                        onValueChange = { discount = it },
                        label = { Text("Diskon") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        leadingIcon = { Text("Rp") }
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        val updatedProduct = product.copy(
                            name = name,
                            code = code.ifBlank { null },
                            sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                            stock = stock.toIntOrNull() ?: 0,
                            purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                            packagePrice = packagePrice.toDoubleOrNull() ?: 0.0,
                            packageQty = packageQty.toIntOrNull() ?: 0,
                            discount = discount.toDoubleOrNull() ?: 0.0
                        )
                        onProductUpdated(updatedProduct)
                    }
                ) {
                    Text("Simpan")
                }
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        }
    )
}
