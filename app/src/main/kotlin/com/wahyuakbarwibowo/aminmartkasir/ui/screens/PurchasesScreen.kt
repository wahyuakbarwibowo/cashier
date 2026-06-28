package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.PurchaseCartItem
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.PurchaseViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.zxing.client.android.Intents
import com.wahyuakbarwibowo.aminmartkasir.ui.scanner.BarcodeCaptureActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: PurchaseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showEditSupplierDialog by remember { mutableStateOf(false) }
    var supplierToDelete by remember { mutableStateOf<SupplierEntity?>(null) }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Pembelian") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                actions = {
                    IconButton(onClick = { showAddItemDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Item")
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && !uiState.isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SupplierSelectorCard(
                            suppliers = uiState.suppliers,
                            selectedSupplier = uiState.selectedSupplier,
                            onSelectSupplier = { viewModel.setSelectedSupplier(it) },
                            onAddSupplier = { showAddSupplierDialog = true },
                            onEditSupplier = { showEditSupplierDialog = true },
                            onDeleteSupplier = { supplierToDelete = it }
                        )
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total Pembelian", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = formatCurrency(uiState.total),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${uiState.cartItems.size} item",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (uiState.cartItems.isEmpty()) {
                        item {
                            EmptyPurchaseCart(onAddItem = { showAddItemDialog = true })
                        }
                    } else {
                        items(uiState.cartItems, key = { it.product.id }) { item ->
                            PurchaseCartItemCard(
                                item = item,
                                onIncreaseQty = { viewModel.updateCartItemQty(item.product.id, item.qty + 1) },
                                onDecreaseQty = { viewModel.updateCartItemQty(item.product.id, item.qty - 1) },
                                onRemove = { viewModel.removeFromCart(item.product.id) }
                            )
                        }
                        item {
                            Button(
                                onClick = { viewModel.processPurchase() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState.cartItems.isNotEmpty() && !uiState.isProcessing
                            ) {
                                if (uiState.isProcessing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Proses Pembelian")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddPurchaseItemDialog(
            products = uiState.products,
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onDismiss = { 
                showAddItemDialog = false 
                viewModel.setSearchQuery("") // Reset search on dismiss
            },
            onSave = { product, qty, price ->
                viewModel.addToCart(product, qty, price)
                showAddItemDialog = false
                viewModel.setSearchQuery("") // Reset search on save
            }
        )
    }

    if (showAddSupplierDialog) {
        AddSupplierDialog(
            onDismiss = { showAddSupplierDialog = false },
            onConfirm = { name, phone, address ->
                viewModel.addSupplier(name, phone, address)
                showAddSupplierDialog = false
            }
        )
    }

    val selectedSupplierForEdit = uiState.selectedSupplier
    if (showEditSupplierDialog && selectedSupplierForEdit != null) {
        EditSupplierDialog(
            supplier = selectedSupplierForEdit,
            onDismiss = { showEditSupplierDialog = false },
            onConfirm = { name, phone, address ->
                viewModel.updateSupplier(
                    selectedSupplierForEdit.copy(
                        name = name,
                        phone = phone.ifBlank { null },
                        address = address.ifBlank { null }
                    )
                )
                showEditSupplierDialog = false
            }
        )
    }

    if (supplierToDelete != null) {
        val supplier = supplierToDelete ?: return
        AlertDialog(
            onDismissRequest = { supplierToDelete = null },
            title = { Text("Hapus Supplier") },
            text = { Text("Yakin ingin menghapus supplier ${supplier.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSupplier(supplier)
                        supplierToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { supplierToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierSelectorCard(
    suppliers: List<SupplierEntity>,
    selectedSupplier: SupplierEntity?,
    onSelectSupplier: (SupplierEntity?) -> Unit,
    onAddSupplier: () -> Unit,
    onEditSupplier: () -> Unit,
    onDeleteSupplier: (SupplierEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Supplier", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onAddSupplier) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tambah Baru")
                }
            }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedSupplier?.name ?: "Tanpa Supplier",
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tanpa Supplier") },
                        onClick = {
                            onSelectSupplier(null)
                            expanded = false
                        }
                    )
                    suppliers.forEach { supplier ->
                        DropdownMenuItem(
                            text = { Text(supplier.name) },
                            onClick = {
                                onSelectSupplier(supplier)
                                expanded = false
                            }
                        )
                    }
                }
            }
            val activeSupplier = selectedSupplier
            if (activeSupplier != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onEditSupplier) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                    TextButton(onClick = { onDeleteSupplier(activeSupplier) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Supplier") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) phone = it },
                    label = { Text("Nomor Telepon") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone, address) },
                enabled = name.isNotBlank()
            ) {
                Text("Simpan")
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
fun EditSupplierDialog(
    supplier: SupplierEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember(supplier.id) { mutableStateOf(supplier.name) }
    var phone by remember(supplier.id) { mutableStateOf(supplier.phone.orEmpty()) }
    var address by remember(supplier.id) { mutableStateOf(supplier.address.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Supplier") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) phone = it },
                    label = { Text("Nomor Telepon") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone, address) },
                enabled = name.isNotBlank()
            ) {
                Text("Simpan")
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
private fun EmptyPurchaseCart(
    onAddItem: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Keranjang pembelian kosong")
            Text(
                text = "Tambah item produk untuk memulai transaksi pembelian.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Tambah Item")
            }
        }
    }
}

@Composable
private fun PurchaseCartItemCard(
    item: PurchaseCartItem,
    onIncreaseQty: () -> Unit,
    onDecreaseQty: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.product.name, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Harga beli: ${formatCurrency(item.price)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatCurrency(item.subtotal),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecreaseQty) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                    }
                    Text(item.qty.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = onIncreaseQty) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                    }
                }
                TextButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPurchaseItemDialog(
    products: List<ProductEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, Int, Double) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var qtyText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val qty = qtyText.toIntOrNull() ?: 0
    val price = priceText.toDoubleOrNull() ?: 0.0

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
                setPrompt("Scan barcode produk untuk pembelian")
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
        title = { Text("Tambah Item Pembelian") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search Field with Scan Icon
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Cari Produk") },
                    placeholder = { Text("Nama atau kode produk...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        readOnly = true,
                        label = { Text("Pilih Produk") },
                        placeholder = { Text("Pilih dari daftar") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (products.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Produk tidak ditemukan") },
                                onClick = { },
                                enabled = false
                            )
                        } else {
                            products.forEach { product ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(product.name, fontWeight = FontWeight.Bold)
                                            if (!product.code.isNullOrBlank()) {
                                                Text(product.code, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedProduct = product
                                        if (priceText.isBlank() || priceText == "0") {
                                            priceText = if (product.purchasePrice > 0) {
                                                product.purchasePrice.toLong().toString()
                                            } else {
                                                ""
                                            }
                                        }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) qtyText = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) priceText = it },
                        label = { Text("Harga Beli") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        prefix = { Text("Rp ") },
                        modifier = Modifier.weight(2f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedProduct!!, qty, price) },
                enabled = selectedProduct != null && qty > 0 && price > 0
            ) {
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
