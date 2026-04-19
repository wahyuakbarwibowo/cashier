package com.wahyuakbarwibowo.aminmartkasir.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wahyuakbarwibowo.aminmartkasir.ui.navigation.Screen

data class AppMenuItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val primaryMenuItems = listOf(
    AppMenuItem(Screen.Dashboard.route, "Beranda", Icons.Default.Dashboard),
    AppMenuItem(Screen.SalesTransaction.route, "Kasir", Icons.Default.PointOfSale),
    AppMenuItem(Screen.DigitalTransaction.route, "Digital", Icons.Default.PhoneIphone),
    AppMenuItem(Screen.Products.route, "Produk", Icons.Default.Inventory2)
)

val secondaryMenuItems = listOf(
    AppMenuItem(Screen.SalesHistory.route, "Riwayat Penjualan", Icons.AutoMirrored.Filled.ReceiptLong),
    AppMenuItem(Screen.Reports.route, "Laporan", Icons.Default.Assessment),
    AppMenuItem(Screen.ProfitLoss.route, "Laba Rugi", Icons.AutoMirrored.Filled.ShowChart),
    AppMenuItem(Screen.Customers.route, "Daftar Pelanggan", Icons.Default.People),
    AppMenuItem(Screen.Expenses.route, "Pengeluaran", Icons.Default.MoneyOff),
    AppMenuItem(Screen.StockHistory.route, "Riwayat Stok", Icons.Default.History),
    AppMenuItem(Screen.DigitalReports.route, "Riwayat Digital", Icons.Default.Receipt),
    AppMenuItem(Screen.Receivable.route, "Buku Hutang", Icons.AutoMirrored.Filled.MenuBook),
    AppMenuItem(Screen.DigitalManagement.route, "Kelola Produk Digital", Icons.Default.AppRegistration),
    AppMenuItem(Screen.Purchases.route, "Pembelian", Icons.Default.LocalShipping),
    AppMenuItem(Screen.Backup.route, "Backup & Restore", Icons.Default.Backup),
    AppMenuItem(Screen.Settings.route, "Pengaturan", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuSheet(
    currentRoute: String?,
    settingsBadgeCount: Int = 0,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Lainnya",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Akses menu sekunder aplikasi",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        secondaryMenuItems.forEach { item ->
            Surface(
                onClick = { onNavigate(item.route) },
                tonalElevation = if (currentRoute == item.route) 2.dp else 0.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text(item.title) },
                    leadingContent = { Icon(item.icon, contentDescription = null) },
                    trailingContent = {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            if (item.route == Screen.Settings.route && settingsBadgeCount > 0) {
                                Badge(modifier = Modifier.padding(end = if (currentRoute == item.route) 8.dp else 0.dp)) {
                                    Text(settingsBadgeCount.toString())
                                }
                            }
                            if (currentRoute == item.route) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
            HorizontalDivider()
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(androidx.compose.ui.Alignment.End)
        ) {
            Text("Tutup")
        }
    }
}
