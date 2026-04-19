package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.*
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.DigitalTransactionViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import java.math.BigDecimal

// Category colors mapping
private val CategoryColors = mapOf(
    "PULSA" to Color(0xFF2196F3),     // Blue
    "PLN" to Color(0xFFFFC107),       // Amber
    "E-WALLET" to Color(0xFF9C27B0),  // Purple
    "PAKET DATA" to Color(0xFF4CAF50),// Green
    "INTERNET" to Color(0xFF00BCD4),  // Cyan
    "GAME" to Color(0xFFFF5722)       // Deep Orange
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                title = { 
                    Column {
                        Text("Transaksi Digital", style = MaterialTheme.typography.titleMedium)
                        Text("PPOB & Top Up", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Categories Selection (Scrollable)
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                ) {
                    CategorySelectionSection(
                        categories = uiState.categories,
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = { viewModel.setSelectedCategory(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Input Section & Recent (Main Content)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Input Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ContactPhone, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Nomor Tujuan / ID Pelanggan",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = uiState.targetNumber,
                                onValueChange = { viewModel.setTargetNumber(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("08xx / ID Pelanggan") },
                                trailingIcon = {
                                    if (uiState.targetNumber.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setTargetNumber("") }) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Clear")
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = uiState.transactionNote,
                                onValueChange = { viewModel.setTransactionNote(it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Keterangan Tambahan (Opsional)") },
                                placeholder = { Text("Contoh: Token PLN") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Recent Targets
                            val recentTargets = remember(uiState.phoneHistory) {
                                uiState.phoneHistory
                                    .mapNotNull { it.phoneNumber?.trim() }
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                    .take(5)
                            }
                            
                            if (recentTargets.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Input Terakhir:", style = MaterialTheme.typography.labelMedium)
                                FlowRow(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    recentTargets.forEach { target ->
                                        SuggestionChip(
                                            onClick = { viewModel.setTargetNumber(target) },
                                            label = { Text(target) },
                                            shape = CircleShape
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Provider Tabs (if any)
                    val providers = uiState.products.map { it.provider }.distinct()
                    if (providers.size > 1) {
                        ProviderTabsSection(
                            providers = providers,
                            selectedProvider = uiState.selectedProvider,
                            onProviderSelected = { viewModel.setSelectedProvider(it) }
                        )
                    }

                    // 4. Products Grid
                    val filteredProducts = if (uiState.selectedProvider != null) {
                        uiState.products.filter { it.provider == uiState.selectedProvider }
                    } else {
                        uiState.products
                    }

                    if (uiState.isLoading && !uiState.isRefreshing) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (filteredProducts.isEmpty()) {
                        EmptyProductsSection(uiState.selectedCategory ?: "Kategori")
                    } else {
                        // We use a non-scrolling grid inside verticalScroll
                        // Or we can manually calculate rows. Let's use a Column with Rows for simplicity in a scrollable view
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Pilih Produk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            filteredProducts.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { product ->
                                        ProductCard(
                                            modifier = Modifier.weight(1f),
                                            product = product,
                                            onClick = {
                                                if (uiState.targetNumber.isBlank()) {
                                                    // Trigger validation error
                                                    viewModel.processTransaction(product, product.sellingPrice, null, null)
                                                } else {
                                                    selectedProductForPayment = product
                                                    viewModel.setPaidAmount(product.sellingPrice.toInt().toString())
                                                }
                                            },
                                            onEdit = {
                                                productToEdit = product
                                                showEditDialog = true
                                            },
                                            onDelete = {
                                                productToDelete = product
                                                showDeleteConfirmation = true
                                            }
                                        )
                                    }
                                    if (rowItems.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Payment Confirmation Dialog (Enhanced)
    if (selectedProductForPayment != null) {
        DigitalPaymentDialog(
            product = selectedProductForPayment!!,
            paidAmount = uiState.paidAmount,
            paymentMethods = uiState.paymentMethods,
            customers = uiState.customers,
            selectedCustomer = uiState.selectedCustomer,
            selectedPaymentMethod = uiState.selectedPaymentMethod,
            onPaidAmountChange = { viewModel.setPaidAmount(it) },
            onSelectCustomer = { viewModel.setSelectedCustomer(it) },
            onSelectPaymentMethod = { viewModel.setSelectedPaymentMethod(it) },
            onDismiss = { selectedProductForPayment = null },
            onConfirm = { product, paid, method, customer ->
                viewModel.processTransaction(product, paid, method, customer)
                selectedProductForPayment = null
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

    // Delete Confirmation
    if (showDeleteConfirmation && productToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Hapus Produk") },
            text = { Text("Apakah Anda yakin ingin menghapus produk \"${productToDelete!!.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProduct(productToDelete!!)
                    showDeleteConfirmation = false
                    productToDelete = null
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Batal") }
            }
        )
    }

    var showPrinterDialog by remember { mutableStateOf(false) }

    // Success Dialog
    if (uiState.successMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearMessages() },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                    Spacer(Modifier.width(8.dp))
                    Text("Transaksi Berhasil")
                }
            },
            text = { Text(uiState.successMessage!!) },
            confirmButton = {
                Button(onClick = { showPrinterDialog = true }) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Cetak Struk")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text("Tutup")
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
            title = { Text("Gagal") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                Button(onClick = { viewModel.clearMessages() }) {
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
            title = { Text("Memproses Transaksi...") },
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
            .padding(top = 8.dp, bottom = 20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { category ->
            val color = CategoryColors[category.name.uppercase()] ?: MaterialTheme.colorScheme.secondary
            val isSelected = selectedCategory == category.name
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(80.dp)
                    .clickable { onCategorySelected(category.name) }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color.White else color.copy(alpha = 0.2f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = when (category.name.uppercase()) {
                            "PULSA" -> Icons.Default.PhoneIphone
                            "PLN" -> Icons.Default.Bolt
                            "E-WALLET" -> Icons.Default.AccountBalanceWallet
                            "PAKET DATA" -> Icons.Default.SignalCellularAlt
                            "INTERNET" -> Icons.Default.Wifi
                            "GAME" -> Icons.Default.SportsEsports
                            else -> Icons.Default.Category
                        }
                        Icon(
                            icon, 
                            contentDescription = null, 
                            tint = if (isSelected) color else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
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
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[if (selectedProvider == null) 0 else providers.indexOf(selectedProvider) + 1]),
                color = MaterialTheme.colorScheme.primary
            )
        }
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
fun ProductCard(
    modifier: Modifier = Modifier,
    product: DigitalProductEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            // Edit/Delete Menu
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { onEdit(); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                        onClick = { onDelete(); expanded = false }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = product.provider,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = formatCurrency(product.sellingPrice),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun EmptyProductsSection(category: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DigitalPaymentDialog(
    product: DigitalProductEntity,
    paidAmount: String,
    paymentMethods: List<PaymentMethodEntity>,
    customers: List<CustomerEntity>,
    selectedCustomer: CustomerEntity?,
    selectedPaymentMethod: PaymentMethodEntity?,
    onPaidAmountChange: (String) -> Unit,
    onSelectCustomer: (CustomerEntity?) -> Unit,
    onSelectPaymentMethod: (PaymentMethodEntity?) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (DigitalProductEntity, Double, PaymentMethodEntity?, CustomerEntity?) -> Unit
) {
    val paidVal = paidAmount.toDoubleOrNull() ?: 0.0
    val change = paidVal - product.sellingPrice
    var customerExpanded by remember { mutableStateOf(false) }
    
    // Auto-select first method if none
    LaunchedEffect(paymentMethods) {
        if (selectedPaymentMethod == null && paymentMethods.isNotEmpty()) {
            onSelectPaymentMethod(paymentMethods.first())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi Pembayaran") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Box
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(product.provider, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = formatCurrency(product.sellingPrice),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Customer Selection
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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Sisa Poin: ${selectedCustomer.points}", 
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = paidAmount,
                    onValueChange = { onPaidAmountChange(it) },
                    label = { Text("Jumlah Bayar") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    prefix = { Text("Rp ") },
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (paidVal > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kembalian:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatCurrency(change),
                            color = if (change >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                // Payment Method Selector
                Text("Metode Pembayaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    paymentMethods.forEach { method ->
                        FilterChip(
                            selected = selectedPaymentMethod == method,
                            onClick = { onSelectPaymentMethod(method) },
                            label = { Text(method.name) }
                        )
                    }
                    
                    val hasHutangInDb = paymentMethods.any { it.name.contains("Hutang", ignoreCase = true) }
                    if (!hasHutangInDb) {
                        FilterChip(
                            selected = selectedPaymentMethod?.name == "Hutang",
                            onClick = { 
                                onSelectPaymentMethod(PaymentMethodEntity(id = -1, name = "Hutang"))
                            },
                            label = { Text("Hutang") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val isDebt = selectedPaymentMethod?.name?.contains("Hutang", ignoreCase = true) == true
            val isCustomerSelected = selectedCustomer != null
            val isPaidValid = if (isDebt) true else paidVal >= product.sellingPrice

            Button(
                onClick = {
                    onConfirm(product, paidVal, selectedPaymentMethod, selectedCustomer)
                },
                enabled = selectedPaymentMethod != null && (!isDebt || isCustomerSelected) && isPaidValid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Konfirmasi Bayar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
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
