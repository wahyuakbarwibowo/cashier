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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wahyuakbarwibowo.aminmartkasir.data.remote.AuthManager
import com.wahyuakbarwibowo.aminmartkasir.data.remote.model.UserRole
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

data class AppMenuGroup(
    val title: String,
    val items: List<AppMenuItem>
)

val secondaryMenuGroups = listOf(
    AppMenuGroup(
        "Transaksi",
        listOf(
            AppMenuItem(Screen.SalesHistory.route, "Riwayat Penjualan", Icons.AutoMirrored.Filled.ReceiptLong),
            AppMenuItem(Screen.DigitalReports.route, "Riwayat Digital", Icons.Default.Receipt),
            AppMenuItem(Screen.Purchases.route, "Pembelian", Icons.Default.LocalShipping),
            AppMenuItem(Screen.Expenses.route, "Pengeluaran", Icons.Default.MoneyOff),
            AppMenuItem(Screen.Receivable.route, "Buku Hutang", Icons.AutoMirrored.Filled.MenuBook)
        )
    ),
    AppMenuGroup(
        "Laporan",
        listOf(
            AppMenuItem(Screen.Reports.route, "Laporan", Icons.Default.Assessment),
            AppMenuItem(Screen.ProfitLoss.route, "Laba Rugi", Icons.AutoMirrored.Filled.ShowChart),
            AppMenuItem(Screen.StockHistory.route, "Riwayat Stok", Icons.Default.History),
            AppMenuItem(Screen.Shift.route, "Shift Kasir", Icons.Default.PointOfSale)
        )
    ),
    AppMenuGroup(
        "Data Master",
        listOf(
            AppMenuItem(Screen.Customers.route, "Daftar Pelanggan", Icons.Default.People),
            AppMenuItem(Screen.DigitalManagement.route, "Kelola Produk Digital", Icons.Default.AppRegistration)
        )
    ),
    AppMenuGroup(
        "Sistem",
        listOf(
            AppMenuItem(Screen.Tenants.route, "Kelola Tenant", Icons.Default.Store),
            AppMenuItem(Screen.Staff.route, "Kelola Pegawai", Icons.Default.ManageAccounts),
            AppMenuItem(Screen.Backup.route, "Backup & Restore", Icons.Default.Backup),
            AppMenuItem(Screen.Settings.route, "Pengaturan", Icons.Default.Settings)
        )
    )
)

val secondaryMenuItems = secondaryMenuGroups.flatMap { it.items }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreMenuSheet(
    currentRoute: String?,
    settingsBadgeCount: Int = 0,
    pendingApprovalCount: Int = 0,
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
        val authState by AuthManager.state.collectAsStateWithLifecycle()
        val role = (authState as? AuthManager.UiState.Authenticated)?.profile?.role
        fun allowed(route: String) = when (route) {
            Screen.Tenants.route -> role == UserRole.SUPER_ADMIN
            Screen.Staff.route -> role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN
            else -> true
        }
        secondaryMenuGroups.forEach { group ->
            Text(
                text = group.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            group.items.filter { allowed(it.route) }.forEach { item ->
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
                                val badge = when (item.route) {
                                    Screen.Settings.route -> settingsBadgeCount
                                    Screen.Staff.route -> pendingApprovalCount
                                    else -> 0
                                }
                                if (badge > 0) {
                                    Badge(modifier = Modifier.padding(end = if (currentRoute == item.route) 8.dp else 0.dp)) {
                                        Text(badge.toString())
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
            }
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
