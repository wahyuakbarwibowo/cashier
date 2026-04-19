package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ReceivableEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.DebtViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivableScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: DebtViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showPaymentDialog by remember { mutableStateOf<ReceivableEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Infinite scroll
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
                title = { Text("Buku Hutang (Piutang)") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Catat Hutang Baru")
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
                // Filter Tabs
                ScrollableTabRow(
                    selectedTabIndex = when(uiState.filterStatus) {
                        "ALL" -> 0
                        "PENDING" -> 1
                        "PAID" -> 2
                        else -> 0
                    },
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    Tab(selected = uiState.filterStatus == "ALL", onClick = { viewModel.setFilterStatus("ALL") }, text = { Text("Semua") })
                    Tab(selected = uiState.filterStatus == "PENDING", onClick = { viewModel.setFilterStatus("PENDING") }, text = { Text("Belum Lunas") })
                    Tab(selected = uiState.filterStatus == "PAID", onClick = { viewModel.setFilterStatus("PAID") }, text = { Text("Lunas") })
                }

                if (uiState.isLoading && !uiState.isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.receivables.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada data hutang")
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.receivables, key = { it.id }) { receivable ->
                            val customer = uiState.customers.find { it.id == receivable.customerId }
                            ReceivableItemCard(
                                receivable = receivable,
                                customerName = customer?.name ?: "Pelanggan Terhapus",
                                onPay = { showPaymentDialog = receivable },
                                onDelete = { viewModel.deleteReceivable(receivable) }
                            )
                        }

                        if (uiState.isLoadMoreLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog != null) {
        DebtPaymentDialog(
            receivable = showPaymentDialog!!,
            onDismiss = { showPaymentDialog = null },
            onConfirm = { amount ->
                viewModel.recordPayment(showPaymentDialog!!, amount)
                showPaymentDialog = null
            }
        )
    }

    if (showAddDialog) {
        AddDebtDialog(
            customers = uiState.customers,
            onDismiss = { showAddDialog = false },
            onConfirm = { custId, amount, due, notes ->
                viewModel.addReceivable(custId, amount, due, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ReceivableItemCard(
    receivable: ReceivableEntity,
    customerName: String,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tgl: ${receivable.createdAt?.take(10) ?: "-"}", style = MaterialTheme.typography.bodySmall)
                }
                
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (receivable.status == "paid") Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (receivable.status == "paid") "LUNAS" else "BELUM LUNAS",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (receivable.status == "paid") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Hutang", style = MaterialTheme.typography.labelSmall)
                    Text(formatCurrency(receivable.amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sisa", style = MaterialTheme.typography.labelSmall)
                    val sisa = receivable.amount - receivable.paidAmount
                    Text(formatCurrency(sisa), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (!receivable.notes.isNullOrBlank()) {
                Text(
                    text = "Ket: ${receivable.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
                if (receivable.status != "paid") {
                    Button(onClick = onPay) {
                        Icon(Icons.Default.Payments, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Bayar")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Catatan Hutang") },
            text = { Text("Apakah Anda yakin ingin menghapus catatan hutang ini?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun DebtPaymentDialog(
    receivable: ReceivableEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val sisa = receivable.amount - receivable.paidAmount
    var amountText by remember { mutableStateOf(BigDecimal.valueOf(sisa).stripTrailingZeros().toPlainString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bayar Hutang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sisa hutang: ${formatCurrency(sisa)}")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) amountText = it },
                    label = { Text("Jumlah Bayar") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("Rp ") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amountText.toDoubleOrNull() ?: 0.0) },
                enabled = amountText.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Simpan Pembayaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtDialog(
    customers: List<com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, String?, String?) -> Unit
) {
    var selectedCust by remember { mutableStateOf<com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerEntity?>(null) }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Hutang Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCust?.name ?: "Pilih Pelanggan",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pelanggan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        customers.forEach { cust ->
                            DropdownMenuItem(text = { Text(cust.name) }, onClick = { selectedCust = cust; expanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.all { ch -> ch.isDigit() }) amountText = it },
                    label = { Text("Jumlah Hutang") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("Rp ") }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Keterangan (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedCust!!.id, amountText.toDoubleOrNull() ?: 0.0, null, notes) },
                enabled = selectedCust != null && amountText.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
