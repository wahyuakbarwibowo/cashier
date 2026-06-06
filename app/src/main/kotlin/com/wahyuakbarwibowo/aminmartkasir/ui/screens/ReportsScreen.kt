package com.wahyuakbarwibowo.aminmartkasir.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.DigitalTransactionViewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.ExpenseViewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.SalesHistoryViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import com.wahyuakbarwibowo.aminmartkasir.utils.ExcelExportUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory? = null,
    salesHistoryViewModel: SalesHistoryViewModel = viewModel(factory = viewModelFactory),
    expenseViewModel: ExpenseViewModel = viewModel(factory = viewModelFactory),
    digitalTransactionViewModel: DigitalTransactionViewModel = viewModel(factory = viewModelFactory)
) {
    val salesState by salesHistoryViewModel.uiState.collectAsStateWithLifecycle()
    val expenseState by expenseViewModel.uiState.collectAsStateWithLifecycle()
    val digitalState by digitalTransactionViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cashierRevenue = remember(salesState.sales) { salesState.sales.sumOf { it.total } }
    val cashierCount = salesState.sales.size
    val digitalRevenue = remember(digitalState.phoneHistory) { digitalState.phoneHistory.sumOf { it.sellingPrice } }
    val digitalCount = digitalState.phoneHistory.size
    val digitalProfit = remember(digitalState.phoneHistory) { digitalState.phoneHistory.sumOf { it.profit } }
    val expenseTotal = remember(expenseState.expenses) { expenseState.expenses.sumOf { it.amount } }
    val grossRevenue = cashierRevenue + digitalRevenue
    val estimatedNet = grossRevenue - expenseTotal

    val recentDigital = remember(digitalState.phoneHistory) {
        digitalState.phoneHistory.sortedByDescending { it.createdAt.orEmpty() }.take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val file = ExcelExportUtils.exportFullReport(
                                context,
                                salesState.sales,
                                digitalState.phoneHistory,
                                expenseState.expenses
                            )
                            if (file != null) {
                                ExcelExportUtils.shareFile(context, file)
                            }
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Ekspor Excel")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (salesState.isLoading || expenseState.isLoading || digitalState.isLoading) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryStatCard(
                    title = "Omzet Kasir",
                    value = formatCurrency(cashierRevenue),
                    subtitle = "$cashierCount transaksi",
                    icon = Icons.Default.PointOfSale
                )
            }
            item {
                SummaryStatCard(
                    title = "Omzet Digital",
                    value = formatCurrency(digitalRevenue),
                    subtitle = "$digitalCount transaksi digital",
                    icon = Icons.Default.PhoneIphone
                )
            }
            item {
                SummaryStatCard(
                    title = "Laba Digital",
                    value = formatCurrency(digitalProfit),
                    subtitle = "Dari margin produk digital",
                    icon = Icons.Default.Assessment
                )
            }
            item {
                SummaryStatCard(
                    title = "Total Pengeluaran",
                    value = formatCurrency(expenseTotal),
                    subtitle = "${expenseState.expenses.size} catatan",
                    icon = Icons.Default.AttachMoney
                )
            }
            item { HorizontalDivider() }
            item {
                SummaryStatCard(
                    title = if (estimatedNet >= 0) "Estimasi Laba" else "Estimasi Rugi",
                    value = formatCurrency(estimatedNet),
                    subtitle = "Omzet kasir + omzet digital - pengeluaran",
                    icon = Icons.Default.Assessment,
                    highlighted = true,
                    isPositive = estimatedNet >= 0
                )
            }
            item {
                Text(
                    text = "Transaksi Digital Terbaru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (recentDigital.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada transaksi digital",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentDigital, key = { it.id }) { trx ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${trx.provider.orEmpty()} - ${trx.category}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = trx.phoneNumber.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatCurrency(trx.sellingPrice),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlighted: Boolean = false,
    isPositive: Boolean? = null
) {
    val containerColor = when {
        isPositive == true -> Color(0xFFE8F5E9)
        isPositive == false -> Color(0xFFFFEBEE)
        highlighted -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when {
        isPositive == true -> Color(0xFF2E7D32)
        isPositive == false -> Color(0xFFC62828)
        highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val valueColor = when {
        isPositive == true -> Color(0xFF1B5E20)
        isPositive == false -> Color(0xFFB71C1C)
        highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
        }
    }
}
