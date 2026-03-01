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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.client.android.Intents
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.scanner.BarcodeCaptureActivity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.ProductViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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
            purchasePrice = editingProduct.purchasePrice.toString()
            sellingPrice = editingProduct.sellingPrice.toString()
            stock = editingProduct.stock.toString()
            purchasePackagePrice = editingProduct.purchasePackagePrice.toString()
            purchasePackageQty = editingProduct.purchasePackageQty.toString()
            packagePrice = editingProduct.packagePrice.toString()
            packageQty = editingProduct.packageQty.toString()
            discount = editingProduct.discount.toString()
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
                onValueChange = { stock = it },
                label = { Text("Stok Awal") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            
            HorizontalDivider()
            
            Text(
                text = "Harga Beli",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = purchasePrice,
                onValueChange = { purchasePrice = it },
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
                    onValueChange = { purchasePackagePrice = it },
                    label = { Text("Harga Beli Paket") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    leadingIcon = { Text("Rp") }
                )
                
                OutlinedTextField(
                    value = purchasePackageQty,
                    onValueChange = { purchasePackageQty = it },
                    label = { Text("Isi Paket") },
                    modifier = Modifier.weight(1f),
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
                onValueChange = { sellingPrice = it },
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
                    onValueChange = { packagePrice = it },
                    label = { Text("Harga Jual Paket") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    leadingIcon = { Text("Rp") }
                )
                
                OutlinedTextField(
                    value = packageQty,
                    onValueChange = { packageQty = it },
                    label = { Text("Isi Paket") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
            
            OutlinedTextField(
                value = discount,
                onValueChange = { discount = it },
                label = { Text("Diskon (%)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                suffix = { Text("%") }
            )
        }
    }
}
