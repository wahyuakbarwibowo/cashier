package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.ProfitLossViewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.ProfitLossPeriod
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitLossScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory? = null,
    profitLossViewModel: ProfitLossViewModel = viewModel(factory = viewModelFactory)
) {
    val state by profitLossViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laba Rugi") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
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
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfitLossPeriod.entries.forEachIndexed { index, period ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ProfitLossPeriod.entries.size),
                            selected = state.selectedPeriod == period,
                            onClick = { profitLossViewModel.setPeriod(period) },
                            label = { Text(period.label) }
                        )
                    }
                }
            }

            item {
                ProfitLossMetricCard(
                    title = "Arus Kas Estimasi",
                    value = formatCurrency(state.estimatedCashFlow),
                    description = "Omzet kasir + omzet digital - pengeluaran",
                    icon = Icons.Default.SsidChart,
                    isPositive = state.estimatedCashFlow >= 0
                )
            }
            item {
                ProfitLossMetricCard(
                    title = "Laba Bersih Estimasi",
                    value = formatCurrency(state.estimatedProfit),
                    description = "Profit (Kasir + Digital) - Pengeluaran",
                    icon = Icons.Default.Calculate,
                    isPositive = state.estimatedProfit >= 0
                )
            }

            item { HorizontalDivider() }

            item {
                SummaryLine(label = "Omzet Kasir", value = formatCurrency(state.cashierRevenue))
            }
            item {
                SummaryLine(label = "Profit Kasir (Est)", value = formatCurrency(state.cashierProfit))
            }
            item {
                SummaryLine(label = "Omzet Digital", value = formatCurrency(state.digitalRevenue))
            }
            item {
                SummaryLine(label = "Profit Digital", value = formatCurrency(state.digitalProfit))
            }
            item {
                SummaryLine(
                    label = "Pengeluaran",
                    value = formatCurrency(state.totalExpense),
                    emphasized = true
                )
            }

            item {
                Text(
                    text = "Pengeluaran per Kategori",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (state.expenseByCategory.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada pengeluaran pada periode ini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.expenseByCategory) { item ->
                    SummaryLine(label = item.category, value = formatCurrency(item.totalAmount))
                }
            }
        }
    }
}

@Composable
private fun ProfitLossMetricCard(
    title: String,
    value: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPositive: Boolean? = null
) {
    val containerColor = when (isPositive) {
        true -> Color(0xFFE8F5E9)
        false -> Color(0xFFFFEBEE)
        null -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (isPositive) {
        true -> Color(0xFF2E7D32)
        false -> Color(0xFFC62828)
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val valueColor = when (isPositive) {
        true -> Color(0xFF1B5E20)
        false -> Color(0xFFB71C1C)
        null -> MaterialTheme.colorScheme.onSurface
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                )
                Text(
                    text = description,
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

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    emphasized: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
