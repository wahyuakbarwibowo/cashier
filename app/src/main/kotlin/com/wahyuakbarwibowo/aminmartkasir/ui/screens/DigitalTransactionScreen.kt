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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaksi Digital") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredProducts.isEmpty()) {
                EmptyProductsSection(uiState.selectedCategory ?: "Kategori")
            } else {
                ProductsGrid(
                    products = filteredProducts,
                    onProductClick = { viewModel.processTransaction(it) }
                )
            }
        }
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
    products: List<DigitalProductEntity>,
    onProductClick: (DigitalProductEntity) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(products) { product ->
            ProductCard(product, onProductClick)
        }
    }
}

@Composable
fun ProductCard(
    product: DigitalProductEntity,
    onClick: (DigitalProductEntity) -> Unit
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
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                minLines = 2
            )
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
                color = MaterialTheme.colorScheme.primary
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
