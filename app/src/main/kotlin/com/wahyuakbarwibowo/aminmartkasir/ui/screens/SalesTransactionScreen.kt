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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.*
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.*
import com.wahyuakbarwibowo.aminmartkasir.utils.BluetoothPrinterHelper
import com.wahyuakbarwibowo.aminmartkasir.ui.screens.LastTransactionData
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesTransactionScreen(
    onNavigateBack: () -> Unit,
    onTransactionSuccess: () -> Unit,
    viewModel: SalesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showProductSelector by remember { mutableStateOf(false) }
    var showCustomerSelector by remember { mutableStateOf(false) }
    var showPaymentMethodSelector by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var lastTransactionData by remember { mutableStateOf<LastTransactionData?>(null) }

    LaunchedEffect(uiState.cartItems) {
        viewModel.calculatePoints()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaksi Penjualan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
            // Cart Items
            if (uiState.cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.cartItems, key = { it.product.id }) { item ->
                        CartItemCard(
                            item = item,
                            onIncreaseQty = { viewModel.updateCartItemQty(item.product.id, item.qty + 1) },
                            onDecreaseQty = { viewModel.updateCartItemQty(item.product.id, item.qty - 1) },
                            onRemove = { viewModel.removeFromCart(item.product.id) }
                        )
                    }
                }
            }
            
            // Summary Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryRow("Subtotal", formatCurrency(uiState.subtotal))
                    if (uiState.discount > 0) {
                        SummaryRow("Diskon", "- ${formatCurrency(uiState.discount)}")
                    }
                    if (uiState.pointsRedeemed > 0) {
                        SummaryRow("Poin Digunakan", "- ${uiState.pointsRedeemed} poin")
                    }
                    HorizontalDivider()
                    SummaryRow(
                        "Total",
                        formatCurrency(uiState.total),
                        isBold = true,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SummaryRow("Dibayar", formatCurrency(uiState.paid))
                    SummaryRow(
                        "Kembalian",
                        formatCurrency(uiState.change),
                        isBold = true
                    )
                    if (uiState.pointsEarned > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Poin Earned:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${uiState.pointsEarned} poin",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showProductSelector = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text("Produk")
                }
                
                OutlinedButton(
                    onClick = { showCustomerSelector = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.People, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = uiState.selectedCustomer?.name ?: "Pelanggan",
                        maxLines = 1
                    )
                }
                
                OutlinedButton(
                    onClick = { showPaymentMethodSelector = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = uiState.selectedPaymentMethod?.name ?: "Pembayaran",
                        maxLines = 1
                    )
                }
            }
            
            Button(
                onClick = {
                    viewModel.processTransaction()
                    showSuccessDialog = true
                    onTransactionSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = uiState.cartItems.isNotEmpty() && uiState.paid >= uiState.total
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Proses Transaksi")
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
            onDismiss = { showProductSelector = false }
        )
    }
    
    if (showCustomerSelector) {
        CustomerSelectorDialog(
            customers = uiState.customers,
            selectedCustomer = uiState.selectedCustomer,
            onCustomerSelected = { viewModel.setSelectedCustomer(it) },
            onDismiss = { showCustomerSelector = false }
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
            transactionData = lastTransactionData
        )
    }

    if (showSuccessDialog) {
        val transactionId = "TRX-${System.currentTimeMillis()}"
        val transactionDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        
        showSuccessDialog = false
        
        // Store transaction data for printing
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
        lastTransactionData = transactionData
        
        // Show success dialog with print option
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                viewModel.clearCart()
                onTransactionSuccess()
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
                            viewModel.clearCart()
                            onTransactionSuccess()
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
    onRemove: () -> Unit
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
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
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
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
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
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSelectorDialog(
    customers: List<CustomerEntity>,
    selectedCustomer: CustomerEntity?,
    onCustomerSelected: (CustomerEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Pelanggan") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Card(
                        onClick = { onCustomerSelected(null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedCustomer == null) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Text(
                            text = "Tanpa Pelanggan",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                items(customers, key = { it.id }) { customer ->
                    Card(
                        onClick = { onCustomerSelected(customer) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedCustomer?.id == customer.id) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!customer.phone.isNullOrBlank()) {
                                Text(
                                    text = customer.phone!!,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = "${customer.points} Poin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
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
    var paidAmount by remember { mutableStateOf(totalAmount.toString()) }
    
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
