package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.util.*

import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToProducts: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToLowStock: () -> Unit,
    onNavigateToDigital: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Model producers for Vico charts
    val salesModelProducer = remember { CartesianChartModelProducer.build() }
    val topProductsModelProducer = remember { CartesianChartModelProducer.build() }
    
    LaunchedEffect(uiState.weeklySales) {
        if (uiState.weeklySales.isNotEmpty()) {
            salesModelProducer.tryRunTransaction {
                lineSeries {
                    series(uiState.weeklySales.map { it.second })
                }
            }
        }
    }
    
    LaunchedEffect(uiState.topProducts) {
        if (uiState.topProducts.isNotEmpty()) {
            topProductsModelProducer.tryRunTransaction {
                columnSeries {
                    series(uiState.topProducts.map { it.second })
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aminmart Kasir") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Lainnya")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            Text(
                text = "Ringkasan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Penjualan Hari Ini",
                    value = formatCurrency(uiState.todaySales),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer
                )
                
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Transaksi",
                    value = uiState.totalSales.toString(),
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }
            
            // Weekly Sales Chart
            if (uiState.weeklySales.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tren Penjualan (7 Hari)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                startAxis = rememberStartAxis(),
                                bottomAxis = rememberBottomAxis(),
                            ),
                            modelProducer = salesModelProducer,
                            modifier = Modifier.height(200.dp)
                        )
                    }
                }
            }

            // Quick Actions
            Text(
                text = "Menu Cepat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            QuickActionButton(
                text = "Transaksi Penjualan",
                icon = Icons.Default.ShoppingCart,
                onClick = onNavigateToSales
            )
            
            QuickActionButton(
                text = "Transaksi Digital",
                icon = Icons.Default.PhoneIphone,
                onClick = onNavigateToDigital
            )
            
            QuickActionButton(
                text = "Manajemen Produk",
                icon = Icons.Default.Inventory,
                onClick = onNavigateToProducts
            )
            
            // Low Stock Alert
            if (uiState.lowStockCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    onClick = onNavigateToLowStock
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Column {
                                Text(
                                    text = "Stok Rendah",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "${uiState.lowStockCount} produk perlu restock",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Top Products
            if (uiState.topProducts.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Produk Terlaris (Qty)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberColumnCartesianLayer(),
                                startAxis = rememberStartAxis(),
                                bottomAxis = rememberBottomAxis(),
                            ),
                            modelProducer = topProductsModelProducer,
                            modifier = Modifier.height(200.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        // Legend
                        uiState.topProducts.forEachIndexed { index, pair ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${index + 1}. ${pair.first}: ${pair.second}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
            
            // Stats Footer
            StatRow(
                label = "Total Produk",
                value = uiState.totalProducts.toString()
            )
            StatRow(
                label = "Total Pelanggan",
                value = uiState.totalCustomers.toString()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    backgroundColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.minimumFractionDigits = 0
    format.maximumFractionDigits = 0
    return format.format(amount)
}
