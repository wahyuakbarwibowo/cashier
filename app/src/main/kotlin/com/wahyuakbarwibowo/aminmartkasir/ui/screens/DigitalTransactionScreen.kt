package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalCategoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalProductEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.DigitalTransactionViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalTransactionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModelFactory: Factory? = null,
    viewModel: DigitalTransactionViewModel = viewModel(factory = viewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedProductForPayment by remember { mutableStateOf<DigitalProductEntity?>(null) }
    var productToEdit by remember { mutableStateOf<DigitalProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<DigitalProductEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaksi Digital") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Lainnya")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
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
            // 1. Categories Selection
            CategorySelectionSection(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.setSelectedCategory(it) }
            )

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            // 2. Input Section (Target Number)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                val recentTargets = remember(uiState.phoneHistory) {
                    uiState.phoneHistory
                        .mapNotNull { it.phoneNumber?.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(8)
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Input Nomor / ID Pelanggan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = uiState.targetNumber,
                        onValueChange = { viewModel.setTargetNumber(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Masukkan nomor tujuan") },
                        trailingIcon = {
                            if (uiState.targetNumber.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setTargetNumber("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.transactionNote,
                        onValueChange = { viewModel.setTransactionNote(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note / Token PLN") },
                        placeholder = { Text("Contoh: 1234 5678 9012 3456") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (recentTargets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Riwayat Input Terakhir",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recentTargets) { target ->
                                FilterChip(
                                    selected = uiState.targetNumber == target,
                                    onClick = { viewModel.setTargetNumber(target) },
                                    label = { Text(target) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Provider Selection (if any)
            val providers = uiState.products.map { it.provider }.distinct()
            if (providers.size > 1) {
                ProviderTabsSection(
                    providers = providers,
                    selectedProvider = uiState.selectedProvider,
                    onProviderSelected = { viewModel.setSelectedProvider(it) }
                )
            }

            // 4. Products List
            val filteredProducts = if (uiState.selectedProvider != null) {
                uiState.products.filter { it.provider == uiState.selectedProvider }
            } else {
                uiState.products
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    EmptyProductsSection(uiState.selectedCategory ?: "Kategori")
                }
            } else {
                ProductsGrid(
                    modifier = Modifier.weight(1f),
                    products = filteredProducts,
                    onProductClick = {
                        if (uiState.targetNumber.isBlank()) {
                            viewModel.setTargetNumber("") // To trigger error if blank
                            viewModel.processTransaction(it, it.sellingPrice) // Will trigger error in VM
                        } else {
                            selectedProductForPayment = it
                            viewModel.setPaidAmount(it.sellingPrice.toInt().toString())
                        }
                    },
                    onEditProduct = {
                        productToEdit = it
                        showEditDialog = true
                    },
                    onDeleteProduct = {
                        productToDelete = it
                        showDeleteConfirmation = true
                    }
                )
            }
        }
    }

    // Payment Confirmation Dialog
    if (selectedProductForPayment != null) {
        val product = selectedProductForPayment!!
        val paidVal = uiState.paidAmount.toDoubleOrNull() ?: 0.0
        val change = paidVal - product.sellingPrice

        AlertDialog(
            onDismissRequest = { selectedProductForPayment = null },
            title = { Text("Konfirmasi Pembayaran") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(product.name, fontWeight = FontWeight.Bold)
                        Text(product.provider, style = MaterialTheme.typography.bodySmall)
                        Text(formatCurrency(product.sellingPrice),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = uiState.paidAmount,
                        onValueChange = { viewModel.setPaidAmount(it) },
                        label = { Text("Jumlah Bayar") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        prefix = { Text("Rp ") }
                    )

                    if (paidVal > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kembalian:")
                            Text(
                                formatCurrency(change),
                                color = if (change >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.processTransaction(product, paidVal)
                        selectedProductForPayment = null
                    },
                    enabled = paidVal >= product.sellingPrice
                ) {
                    Text("Proses")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProductForPayment = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit Product Dialog
    if (showEditDialog && productToEdit != null) {
        EditDigitalProductDialog(
            product = productToEdit!!,
            onDismiss = {
                showEditDialog = false
                productToEdit = null
            },
            onProductUpdated = { updatedProduct ->
                viewModel.updateProduct(updatedProduct)
                showEditDialog = false
                productToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation && productToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                productToDelete = null
            },
            title = { Text("Hapus Produk") },
            text = {
                Text("Apakah Anda yakin ingin menghapus produk \"${productToDelete!!.name}\"?")
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            viewModel.deleteProduct(productToDelete!!)
                            showDeleteConfirmation = false
                            productToDelete = null
                        }
                    ) {
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = {
                        showDeleteConfirmation = false
                        productToDelete = null
                    }) {
                        Text("Batal")
                    }
                }
            }
        )
    }

    var showPrinterDialog by remember { mutableStateOf(false) }

    // Success Dialog
    if (uiState.successMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearMessages() },
            title = { Text("Berhasil") },
            text = { Text(uiState.successMessage!!) },
            confirmButton = {
                Row {
                    TextButton(onClick = { 
                        showPrinterDialog = true
                    }) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Cetak Struk")
                    }
                    TextButton(onClick = { viewModel.clearMessages() }) {
                        Text("OK")
                    }
                }
            }
        )
    }

    if (showPrinterDialog && uiState.lastProcessedTransaction != null) {
        BluetoothPrinterDialog(
            onDismiss = { 
                showPrinterDialog = false
                viewModel.clearMessages()
            },
            onDeviceConnected = {},
            digitalTransaction = uiState.lastProcessedTransaction,
            viewModelFactory = viewModelFactory
        )
    }

    // Error Dialog
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearMessages() },
            title = { Text("Galat") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Processing Overlay
    if (uiState.isProcessing) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Memproses...") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        )
    }
}

@Composable
fun CategorySelectionSection(
    categories: List<DigitalCategoryEntity>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category.name,
                onClick = { onCategorySelected(category.name) },
                label = { Text(category.name) },
                leadingIcon = {
                    val icon = when (category.name.uppercase()) {
                        "PULSA" -> Icons.Default.PhoneIphone
                        "PLN" -> Icons.Default.Bolt
                        "E-WALLET" -> Icons.Default.AccountBalanceWallet
                        "PAKET DATA" -> Icons.Default.SignalCellularAlt
                        else -> Icons.Default.Category
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
    }
}

@Composable
fun ProviderTabsSection(
    providers: List<String>,
    selectedProvider: String?,
    onProviderSelected: (String?) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = if (selectedProvider == null) 0 else providers.indexOf(selectedProvider) + 1,
        edgePadding = 16.dp,
        containerColor = Color.Transparent,
        divider = {}
    ) {
        Tab(
            selected = selectedProvider == null,
            onClick = { onProviderSelected(null) },
            text = { Text("Semua") }
        )
        providers.forEach { provider ->
            Tab(
                selected = selectedProvider == provider,
                onClick = { onProviderSelected(provider) },
                text = { Text(provider) }
            )
        }
    }
}

@Composable
fun ProductsGrid(
    modifier: Modifier = Modifier,
    products: List<DigitalProductEntity>,
    onProductClick: (DigitalProductEntity) -> Unit,
    onEditProduct: (DigitalProductEntity) -> Unit,
    onDeleteProduct: (DigitalProductEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(products) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product) },
                onEdit = { onEditProduct(product) },
                onDelete = { onDeleteProduct(product) }
            )
        }
    }
}

@Composable
fun ProductCard(
    product: DigitalProductEntity,
    onClick: (DigitalProductEntity) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(product) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = product.provider,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(product.sellingPrice),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun EmptyProductsSection(category: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tidak ada produk di $category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDigitalProductDialog(
    product: DigitalProductEntity,
    onDismiss: () -> Unit,
    onProductUpdated: (DigitalProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var category by remember { mutableStateOf(product.category) }
    var provider by remember { mutableStateOf(product.provider) }
    var nominal by remember { mutableStateOf(formatNumberForInput(product.nominal)) }
    var costPrice by remember { mutableStateOf(formatNumberForInput(product.costPrice)) }
    var sellingPrice by remember { mutableStateOf(formatNumberForInput(product.sellingPrice)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Produk Digital") },
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
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text("Provider") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = nominal,
                        onValueChange = { nominal = it },
                        label = { Text("Nominal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Text("Rp") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Harga Modal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Text("Rp") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Harga Jual") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            category = category,
                            provider = provider,
                            nominal = nominal.toDoubleOrNull() ?: 0.0,
                            costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                            sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0
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

private fun formatNumberForInput(value: Double): String {
    return value.toLong().toString()
}
