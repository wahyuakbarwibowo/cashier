package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.SalesHistoryViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import com.wahyuakbarwibowo.aminmartkasir.utils.BluetoothPrinterHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    saleId: Long,
    onNavigateBack: () -> Unit,
    viewModelFactory: Factory? = null,
    viewModel: SalesHistoryViewModel = viewModel(factory = viewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val sale = uiState.sales.find { it.id == saleId }
    val items = uiState.saleItems[saleId] ?: emptyList()
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
    var showPrinterDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Transaksi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (sale != null) {
                        IconButton(onClick = { showPrinterDialog = true }) {
                            Icon(Icons.Default.Print, contentDescription = "Cetak")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (sale == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Transaksi tidak ditemukan")
            }
            return@Scaffold
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Info
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Informasi Transaksi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    HorizontalDivider()
                    
                    InfoRow("No. Transaksi", "#TRX-${sale.id}")
                    InfoRow(
                        "Tanggal",
                        sale.createdAt?.let {
                            try {
                                dateFormat.format(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it)!!)
                            } catch (e: Exception) {
                                it
                            }
                        } ?: "-"
                    )
                    InfoRow("Total", formatCurrency(sale.total), isBold = true)
                    InfoRow("Dibayar", formatCurrency(sale.paid))
                    InfoRow("Kembalian", formatCurrency(sale.change))
                    
                    if (sale.pointsEarned > 0) {
                        InfoRow("Poin Earned", "+${sale.pointsEarned} poin")
                    }
                    if (sale.pointsRedeemed > 0) {
                        InfoRow("Poin Digunakan", "-${sale.pointsRedeemed} poin")
                    }
                }
            }
            
            // Items
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Item Pembelian",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    HorizontalDivider()
                    
                    items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Product ID: ${item.productId}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Qty: ${item.qty} x ${formatCurrency(item.price)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = formatCurrency(item.subtotal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPrinterDialog && sale != null) {
        val transactionData = LastTransactionData(
            transactionId = "TRX-${sale.id}",
            items = items.map { item ->
                BluetoothPrinterHelper.ReceiptItem(
                    name = "Product ${item.productId}", // Ideally we should have product name here
                    qty = item.qty,
                    price = item.price,
                    subtotal = item.subtotal
                )
            },
            subtotal = items.sumOf { it.subtotal },
            discount = 0.0, // We should probably store discount in SaleEntity if needed
            total = sale.total,
            paid = sale.paid,
            change = sale.change,
            pointsEarned = sale.pointsEarned
        )

        BluetoothPrinterDialog(
            onDismiss = { showPrinterDialog = false },
            onDeviceConnected = {},
            transactionData = transactionData,
            viewModelFactory = viewModelFactory
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isBold) {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium
            }
        )
    }
}
