package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.client.android.Intents
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductVariantEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.scanner.BarcodeCaptureActivity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.ProductViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    productId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: ProductViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var purchasePackagePrice by remember { mutableStateOf("") }
    var purchasePackageQty by remember { mutableStateOf("") }
    var packagePrice by remember { mutableStateOf("") }
    var packageQty by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }
    var variants by remember { mutableStateOf(listOf<ProductVariantEntity>()) }
    var showVariantDialog by remember { mutableStateOf(false) }
    var editingVariantIndex by remember { mutableStateOf<Int?>(null) }
    var formInitialized by remember(productId) { mutableStateOf(false) }

    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val scannedCode = result.contents
        if (!scannedCode.isNullOrBlank()) {
            code = scannedCode
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
    
    val isEdit = productId != null
    
    // Helper to round up to nearest 500
    fun roundUpTo500(value: Double): String {
        val rounded = Math.ceil(value / 500.0) * 500.0
        return rounded.toLong().toString()
    }

    // Helper to format double as integer string (remove .0)
    fun formatDouble(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    // Helper to filter only digits for input
    fun onNumberChange(value: String, onValueChange: (String) -> Unit) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            onValueChange(value)
        }
    }

    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProductById(productId)
        }
    }

    LaunchedEffect(uiState.editingProduct?.id, productId, formInitialized) {
        val editingProduct = uiState.editingProduct
        if (isEdit && !formInitialized && editingProduct != null && editingProduct.id == productId) {
            name = editingProduct.name
            code = editingProduct.code.orEmpty()
            purchasePrice = formatDouble(editingProduct.purchasePrice)
            sellingPrice = formatDouble(editingProduct.sellingPrice)
            stock = editingProduct.stock.toString()
            purchasePackagePrice = formatDouble(editingProduct.purchasePackagePrice)
            purchasePackageQty = editingProduct.purchasePackageQty.toString()
            packagePrice = formatDouble(editingProduct.packagePrice)
            packageQty = editingProduct.packageQty.toString()
            discount = formatDouble(editingProduct.discount)
            variants = viewModel.getVariantsByProductId(editingProduct.id)
            formInitialized = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Produk" else "Tambah Produk") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        enabled = name.isNotBlank(),
                        onClick = {
                            val product = ProductEntity(
                                id = productId ?: 0,
                                name = name,
                                code = code.ifBlank { null },
                                purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                                sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                                stock = stock.toIntOrNull() ?: 0,
                                purchasePackagePrice = purchasePackagePrice.toDoubleOrNull() ?: 0.0,
                                purchasePackageQty = purchasePackageQty.toIntOrNull() ?: 0,
                                packagePrice = packagePrice.toDoubleOrNull() ?: 0.0,
                                packageQty = packageQty.toIntOrNull() ?: 0,
                                discount = discount.toDoubleOrNull() ?: 0.0
                            )
                            
                            if (isEdit) {
                                viewModel.updateProduct(product)
                                scope.launch {
                                    viewModel.replaceVariants(product.id, variants)
                                }
                            } else {
                                viewModel.addProduct(product)
                            }
                            onNavigateBack()
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Informasi Dasar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Produk") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Kode Produk (Barcode)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan Barcode")
                    }
                }
            )
            
            OutlinedTextField(
                value = stock,
                onValueChange = { onNumberChange(it) { stock = it } },
                label = { Text("Stok Awal") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Varian Produk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    editingVariantIndex = null
                    showVariantDialog = true
                }) {
                    Text("Tambah Varian")
                }
            }

            if (variants.isEmpty()) {
                Text(
                    text = "Belum ada varian",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                variants.forEachIndexed { index, variant ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(variant.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "SKU: ${variant.sku.orEmpty()} | Barcode: ${variant.barcode.orEmpty()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Jual ${formatDouble(variant.sellingPrice)} | Stok ${variant.stock}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    editingVariantIndex = index
                                    showVariantDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = {
                                    variants = variants.filterIndexed { i, _ -> i != index }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus")
                                }
                            }
                        }
                    }
                }
            }
            
            Text(
                text = "Harga Beli",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = purchasePrice,
                onValueChange = { onNumberChange(it) { purchasePrice = it } },
                label = { Text("Harga Beli Satuan") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                leadingIcon = { Text("Rp") }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = purchasePackagePrice,
                    onValueChange = { onNumberChange(it) { 
                        purchasePackagePrice = it 
                        // Auto calculate unit price
                        val p = it.toDoubleOrNull() ?: 0.0
                        val q = purchasePackageQty.toIntOrNull() ?: 0
                        if (q > 0) {
                            purchasePrice = roundUpTo500(p / q)
                        }
                    } },
                    label = { Text("Harga Beli Paket") },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    leadingIcon = { Text("Rp") }
                )

                OutlinedTextField(
                    value = purchasePackageQty,
                    onValueChange = { onNumberChange(it) { 
                        purchasePackageQty = it 
                        // Auto calculate unit price
                        val p = purchasePackagePrice.toDoubleOrNull() ?: 0.0
                        val q = it.toIntOrNull() ?: 0
                        if (q > 0) {
                            purchasePrice = roundUpTo500(p / q)
                        }
                    } },
                    label = { Text("Isi Paket") },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
            
            HorizontalDivider()
            
            Text(
                text = "Harga Jual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = sellingPrice,
                onValueChange = { onNumberChange(it) { sellingPrice = it } },
                label = { Text("Harga Jual Satuan") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                leadingIcon = { Text("Rp") }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = packagePrice,
                    onValueChange = { onNumberChange(it) { 
                        packagePrice = it 
                        // Auto calculate unit price
                        val p = it.toDoubleOrNull() ?: 0.0
                        val q = packageQty.toIntOrNull() ?: 0
                        if (q > 0) {
                            sellingPrice = roundUpTo500(p / q)
                        }
                    } },
                    label = { Text("Harga Jual Paket") },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    leadingIcon = { Text("Rp") }
                )

                OutlinedTextField(
                    value = packageQty,
                    onValueChange = { onNumberChange(it) { 
                        packageQty = it 
                        // Auto calculate unit price
                        val p = packagePrice.toDoubleOrNull() ?: 0.0
                        val q = it.toIntOrNull() ?: 0
                        if (q > 0) {
                            sellingPrice = roundUpTo500(p / q)
                        }
                    } },
                    label = { Text("Isi Paket") },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
            
            OutlinedTextField(
                value = discount,
                onValueChange = { onNumberChange(it) { discount = it } },
                label = { Text("Diskon (%)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                suffix = { Text("%") }
            )
        }
    }

    if (showVariantDialog) {
        VariantFormDialog(
            initial = editingVariantIndex?.let { variants[it] },
            onDismiss = { showVariantDialog = false },
            onSave = { variant ->
                variants = if (editingVariantIndex == null) {
                    variants + variant
                } else {
                    variants.mapIndexed { index, old ->
                        if (index == editingVariantIndex) variant else old
                    }
                }
                showVariantDialog = false
            }
        )
    }
}

@Composable
private fun VariantFormDialog(
    initial: ProductVariantEntity?,
    onDismiss: () -> Unit,
    onSave: (ProductVariantEntity) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var sku by remember(initial?.id) { mutableStateOf(initial?.sku.orEmpty()) }
    var barcode by remember(initial?.id) { mutableStateOf(initial?.barcode.orEmpty()) }
    var purchasePrice by remember(initial?.id) { mutableStateOf(initial?.purchasePrice?.toLong()?.toString().orEmpty()) }
    var sellingPrice by remember(initial?.id) { mutableStateOf(initial?.sellingPrice?.toLong()?.toString().orEmpty()) }
    var stock by remember(initial?.id) { mutableStateOf(initial?.stock?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Varian" else "Edit Varian") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Varian") })
                OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") })
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") })
                OutlinedTextField(value = purchasePrice, onValueChange = { purchasePrice = it.filter { ch -> ch.isDigit() } }, label = { Text("Harga Modal") })
                OutlinedTextField(value = sellingPrice, onValueChange = { sellingPrice = it.filter { ch -> ch.isDigit() } }, label = { Text("Harga Jual") })
                OutlinedTextField(value = stock, onValueChange = { stock = it.filter { ch -> ch.isDigit() } }, label = { Text("Stok") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        ProductVariantEntity(
                            id = initial?.id ?: 0,
                            productId = initial?.productId ?: 0,
                            name = name.trim(),
                            sku = sku.trim().ifBlank { null },
                            barcode = barcode.trim().ifBlank { null },
                            purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                            sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                            stock = stock.toIntOrNull() ?: 0
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
