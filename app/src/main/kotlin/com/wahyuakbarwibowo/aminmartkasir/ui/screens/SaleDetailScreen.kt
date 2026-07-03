package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.SaleDetailViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import com.wahyuakbarwibowo.aminmartkasir.utils.BluetoothPrinterHelper
import com.wahyuakbarwibowo.aminmartkasir.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import com.wahyuakbarwibowo.aminmartkasir.ui.screens.LastTransactionData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    saleId: Long,
    onNavigateBack: () -> Unit,
    onEditSale: (Long) -> Unit,
    viewModelFactory: Factory? = null,
    viewModel: SaleDetailViewModel = viewModel(factory = viewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sale = uiState.sale
    val items = uiState.items
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
    var showPrinterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saleId) {
        viewModel.loadSaleDetail(saleId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Transaksi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                actions = {
                    if (sale != null) {
                        IconButton(onClick = { onEditSale(sale.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showPrinterDialog = true }) {
                            Icon(Icons.Default.Print, contentDescription = "Cetak")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (sale == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.error ?: "Transaksi tidak ditemukan")
            }
            return@Scaffold
        }

        val calculatedSubtotal = items.sumOf { it.subtotal }
        val calculatedDiscount = (calculatedSubtotal - sale.total).coerceAtLeast(0.0)
        
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
                    
                    SaleDetailRow(label = "Nomor Transaksi", value = "#TRX-${sale.id}")
                    SaleDetailRow(
                        label = "Tanggal", 
                        value = sale.createdAt?.let { 
                            try {
                                dateFormat.format(DateUtils.parseDateTime(it))
                            } catch (e: Exception) {
                                it
                            }
                        } ?: ""
                    )
                    SaleDetailRow(label = "Status", value = "Selesai")
                }
            }
            
            // Items Section
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Daftar Item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    HorizontalDivider()
                    
                    items.forEach { item ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.qty} x ${formatCurrency(item.price)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(item.subtotal),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider()
                    
                    SaleDetailRow(label = "Subtotal", value = formatCurrency(calculatedSubtotal))
                    if (calculatedDiscount > 0) {
                        SaleDetailRow(label = "Diskon", value = "- ${formatCurrency(calculatedDiscount)}")
                    }
                    SaleDetailRow(label = "Total", value = formatCurrency(sale.total), isBold = true)
                    SaleDetailRow(label = "Tunai", value = formatCurrency(sale.paid))
                    SaleDetailRow(label = "Kembalian", value = formatCurrency(sale.change))
                }
            }
        }
    }
    
    if (showPrinterDialog && sale != null) {
        val calculatedSubtotal = items.sumOf { it.subtotal }
        val calculatedDiscount = (calculatedSubtotal - sale.total).coerceAtLeast(0.0)

        val transactionData = LastTransactionData(
            transactionId = "TRX-${sale.id}",
            items = items.map { item ->
                BluetoothPrinterHelper.ReceiptItem(
                    name = item.productName,
                    qty = item.qty,
                    price = item.price,
                    subtotal = item.subtotal
                )
            },
            subtotal = calculatedSubtotal,
            discount = calculatedDiscount,
            total = sale.total,
            paid = sale.paid,
            change = sale.change
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
private fun SaleDetailRow(
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
