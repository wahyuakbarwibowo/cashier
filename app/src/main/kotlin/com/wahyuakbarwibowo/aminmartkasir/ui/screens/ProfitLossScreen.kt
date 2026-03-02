package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SsidChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.DigitalTransactionViewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.ExpenseViewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.SalesHistoryViewModel
import com.wahyuakbarwibowo.aminmartkasir.utils.CurrencyUtils.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitLossScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory? = null,
    salesHistoryViewModel: SalesHistoryViewModel = viewModel(factory = viewModelFactory),
    expenseViewModel: ExpenseViewModel = viewModel(factory = viewModelFactory),
    digitalTransactionViewModel: DigitalTransactionViewModel = viewModel(factory = viewModelFactory)
) {
    val salesState by salesHistoryViewModel.uiState.collectAsState()
    val expenseState by expenseViewModel.uiState.collectAsState()
    val digitalState by digitalTransactionViewModel.uiState.collectAsState()
    var selectedPeriod by remember { mutableStateOf(ProfitLossPeriod.THIS_MONTH) }

    val todayPattern = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val monthPattern = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }

    fun isMatchPeriod(createdAt: String?): Boolean {
        val value = createdAt.orEmpty()
        return when (selectedPeriod) {
            ProfitLossPeriod.TODAY -> value.startsWith(todayPattern)
            ProfitLossPeriod.THIS_MONTH -> value.startsWith(monthPattern)
            ProfitLossPeriod.ALL_TIME -> true
        }
    }

    val cashierRevenue = remember(salesState.sales, selectedPeriod) {
        salesState.sales.filter { isMatchPeriod(it.createdAt) }.sumOf { it.total }
    }
    val digitalRevenue = remember(digitalState.phoneHistory, selectedPeriod) {
        digitalState.phoneHistory.filter { isMatchPeriod(it.createdAt) }.sumOf { it.sellingPrice }
    }
    val digitalProfit = remember(digitalState.phoneHistory, selectedPeriod) {
        digitalState.phoneHistory.filter { isMatchPeriod(it.createdAt) }.sumOf { it.profit }
    }
    val totalExpense = remember(expenseState.expenses, selectedPeriod) {
        expenseState.expenses.filter { isMatchPeriod(it.createdAt) }.sumOf { it.amount }
    }

    val estimatedCashFlow = cashierRevenue + digitalRevenue - totalExpense
    val estimatedProfit = digitalProfit - totalExpense

    val expenseByCategory = remember(expenseState.expenses, selectedPeriod) {
        expenseState.expenses
            .filter { isMatchPeriod(it.createdAt) }
            .groupBy { it.category }
            .mapValues { (_, value) -> value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laba Rugi") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfitLossPeriod.entries.forEachIndexed { index, period ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ProfitLossPeriod.entries.size),
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.label) }
                        )
                    }
                }
            }

            item {
                ProfitLossMetricCard(
                    title = "Arus Kas Estimasi",
                    value = formatCurrency(estimatedCashFlow),
                    description = "Omzet kasir + omzet digital - pengeluaran",
                    icon = Icons.Default.SsidChart
                )
            }
            item {
                ProfitLossMetricCard(
                    title = "Laba Bersih Estimasi",
                    value = formatCurrency(estimatedProfit),
                    description = "Profit digital - pengeluaran",
                    icon = Icons.Default.Calculate
                )
            }

            item { HorizontalDivider() }

            item {
                SummaryLine(label = "Omzet Kasir", value = formatCurrency(cashierRevenue))
            }
            item {
                SummaryLine(label = "Omzet Digital", value = formatCurrency(digitalRevenue))
            }
            item {
                SummaryLine(label = "Profit Digital", value = formatCurrency(digitalProfit))
            }
            item {
                SummaryLine(
                    label = "Pengeluaran",
                    value = formatCurrency(totalExpense),
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
            if (expenseByCategory.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada pengeluaran pada periode ini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(expenseByCategory) { (category, amount) ->
                    SummaryLine(label = category, value = formatCurrency(amount))
                }
            }
        }
    }
}

private enum class ProfitLossPeriod(val label: String) {
    TODAY("Hari Ini"),
    THIS_MONTH("Bulan Ini"),
    ALL_TIME("Semua")
}

@Composable
private fun ProfitLossMetricCard(
    title: String,
    value: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
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
