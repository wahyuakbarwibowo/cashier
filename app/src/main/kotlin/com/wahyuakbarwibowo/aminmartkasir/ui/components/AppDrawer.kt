package com.wahyuakbarwibowo.aminmartkasir.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wahyuakbarwibowo.aminmartkasir.ui.navigation.Screen

data class DrawerMenuItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val drawerMenuItems = listOf(
    DrawerMenuItem(Screen.Dashboard.route, "Beranda", Icons.Default.Dashboard),
    DrawerMenuItem(Screen.DigitalTransaction.route, "Transaksi Digital", Icons.Default.PhoneIphone),
    DrawerMenuItem(Screen.SalesTransaction.route, "Kasir", Icons.Default.ShoppingCart),
    DrawerMenuItem(Screen.SalesHistory.route, "Riwayat Penjualan", Icons.Default.History),
    DrawerMenuItem(Screen.DigitalManagement.route, "Kelola Produk Digital", Icons.Default.AppRegistration),
    DrawerMenuItem(Screen.Products.route, "Produk", Icons.Default.Inventory),
    DrawerMenuItem(Screen.Customers.route, "Pelanggan", Icons.Default.People),
    DrawerMenuItem(Screen.Suppliers.route, "Supplier", Icons.Default.LocalShipping),
    DrawerMenuItem(Screen.Expenses.route, "Pengeluaran", Icons.Default.MoneyOff),
    DrawerMenuItem(Screen.Receivables.route, "Piutang", Icons.Default.AccountBalance),
    DrawerMenuItem(Screen.Payables.route, "Hutang", Icons.Default.Payment),
    DrawerMenuItem(Screen.Reports.route, "Laporan", Icons.Default.Assessment),
    DrawerMenuItem(Screen.ProfitLoss.route, "Laba Rugi", Icons.Default.ShowChart),
    DrawerMenuItem(Screen.Backup.route, "Backup & Restore", Icons.Default.Backup),
    DrawerMenuItem(Screen.Settings.route, "Pengaturan", Icons.Default.Settings)
)

@Composable
fun AppDrawer(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Aminmart Kasir",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            drawerMenuItems.forEach { item ->
                NavigationDrawerItem(
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.title) },
                    selected = currentRoute == item.route,
                    onClick = {
                        onNavigate(item.route)
                        onCloseDrawer()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    }
}
