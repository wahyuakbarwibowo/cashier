package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.zxing.client.android.Intents
import com.wahyuakbarwibowo.aminmartkasir.ui.scanner.BarcodeCaptureActivity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.ProductViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    onNavigateToProductForm: (Long?) -> Unit,
    onNavigateToStockHistory: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: ProductViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val scannedCode = result.contents
        if (!scannedCode.isNullOrBlank()) {
            viewModel.searchProducts(scannedCode)
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

    // Infinite scroll detection
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            uiState.canLoadMore && lastVisibleItemIndex >= totalItemsCount - 5 && totalItemsCount > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !uiState.isLoadMoreLoading) {
            viewModel.loadNextPage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Produk") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToProductForm(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.searchProducts(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text("Cari nama atau kode produk...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                            }
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchProducts("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                }
                            }
                        }
                    },
                    singleLine = true
                )

                if (uiState.isLoading && !uiState.isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.products.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada produk")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.products, key = { it.id }) { product ->
                            ProductItemCard(
                                product = product,
                                onClick = { onNavigateToProductForm(product.id) },
                                onDelete = { viewModel.deleteProduct(product) },
                                onAdjustStock = { qty, reason -> 
                                    viewModel.adjustStock(product.id, qty, reason)
                                }
                            )
                        }

                        if (uiState.isLoadMoreLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItemCard(
    product: ProductEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: (Int, String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStockAdjust by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!product.code.isNullOrBlank()) {
                        Text(
                            text = product.code,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opsi")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sesuaikan Stok") },
                            onClick = {
                                showStockAdjust = true
                                menuExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Tune, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus") },
                            onClick = {
                                showDeleteConfirm = true
                                menuExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Harga Jual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(product.sellingPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (product.stock <= 5) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Stok: ${product.stock}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (product.stock <= 5) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Produk") },
            text = { Text("Apakah Anda yakin ingin menghapus ${product.name}? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showStockAdjust) {
        StockAdjustmentDialog(
            product = product,
            onDismiss = { showStockAdjust = false },
            onConfirm = { qty, reason ->
                onAdjustStock(qty, reason)
                showStockAdjust = false
            }
        )
    }
}

@Composable
fun StockAdjustmentDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var adjustmentQty by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sesuaikan Stok - ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Stok saat ini: ${product.stock}")
                OutlinedTextField(
                    value = adjustmentQty,
                    onValueChange = { value ->
                        if (value.isEmpty() || value == "-" || value.toIntOrNull() != null) {
                            adjustmentQty = value
                        }
                    },
                    label = { Text("Jumlah Penyesuaian") },
                    placeholder = { Text("Contoh: 10 atau -5") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan Penyesuaian") },
                    placeholder = { Text("Contoh: Stok masuk, rusak, dll.") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = adjustmentQty.toIntOrNull()
                    if (qty != null && qty != 0 && reason.isNotBlank()) {
                        onConfirm(qty, reason)
                    }
                },
                enabled = adjustmentQty.toIntOrNull() != null && adjustmentQty.toIntOrNull() != 0 && reason.isNotBlank()
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
