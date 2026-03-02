package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.DigitalTransactionViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalReportDetailScreen(
    reportId: Long,
    onNavigateBack: () -> Unit,
    viewModelFactory: Factory? = null,
    viewModel: DigitalTransactionViewModel = viewModel(factory = viewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val history = uiState.phoneHistory.find { it.id == reportId }
    val productName = history?.let { parseDigitalProductName(it.notes) } ?: "Produk Digital"
    val tokenNote = history?.let { parseDigitalNote(it.notes) }
    var showPrinterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Transaksi Digital") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (history != null) {
                        IconButton(onClick = { showPrinterDialog = true }) {
                            Icon(Icons.Default.Print, contentDescription = "Cetak")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (history == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Detail transaksi tidak ditemukan")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Status Transaksi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider()
                        
                        DetailRow("No. Transaksi", "#TRX-DIG-${history.id}")
                        DetailRow("Waktu", history.createdAt ?: "-")
                        DetailRow("Kategori", history.category)
                        DetailRow("Provider", history.provider ?: "-")
                        DetailRow("Nomor Tujuan", history.phoneNumber ?: "-")
                        
                        HorizontalDivider()
                        
                        DetailRow("Produk", productName)
                        DetailRow("Harga Jual", formatCurrency(history.sellingPrice), isBold = true)
                        DetailRow("Dibayar", formatCurrency(history.paid))
                        DetailRow("Kembalian", formatCurrency(history.paid - history.sellingPrice))
                        
                        if (!tokenNote.isNullOrBlank()) {
                            HorizontalDivider()
                            Text(
                                text = "Keterangan / SN:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = tokenNote,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPrinterDialog && history != null) {
        BluetoothPrinterDialog(
            onDismiss = { showPrinterDialog = false },
            onDeviceConnected = {},
            digitalTransaction = history,
            viewModelFactory = viewModelFactory
        )
    }
}

private fun parseDigitalProductName(rawNotes: String?): String {
    if (rawNotes.isNullOrBlank()) return "Produk Digital"
    return rawNotes
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("TRX Digital: ") }
        ?.removePrefix("TRX Digital: ")
        ?.ifBlank { "Produk Digital" }
        ?: "Produk Digital"
}

private fun parseDigitalNote(rawNotes: String?): String? {
    if (rawNotes.isNullOrBlank()) return null
    return rawNotes
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("NOTE: ") }
        ?.removePrefix("NOTE: ")
        ?.trim()
        ?.ifBlank { null }
}

@Composable
fun DetailRow(
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
                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium
            }
        )
    }
}
