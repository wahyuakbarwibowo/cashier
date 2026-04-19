package com.wahyuakbarwibowo.aminmartkasir.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.ViewModelProvider.Factory
import com.wahyuakbarwibowo.aminmartkasir.ui.screens.*

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route,
    viewModelFactory: Factory,
    onOpenDrawer: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToProducts = { navController.navigate(Screen.Products.route) },
                onNavigateToSales = { navController.navigate(Screen.SalesTransaction.route) },
                onNavigateToLowStock = { navController.navigate(Screen.LowStock.route) },
                onNavigateToDigital = { navController.navigate(Screen.DigitalTransaction.route) },
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Products.route) {
            ProductsScreen(
                onNavigateToProductForm = { productId ->
                    navController.navigate(Screen.ProductForm.createRoute(productId))
                },
                onNavigateToStockHistory = { navController.navigate(Screen.StockHistory.route) },
                onNavigateBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.StockHistory.route) {
            StockHistoryScreen(
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }
        
        composable(
            route = Screen.ProductForm.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.LongType
                    defaultValue = -1L
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: -1L
            ProductFormScreen(
                productId = if (productId == -1L) null else productId,
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }
        
        composable(Screen.SalesTransaction.route) {
            SalesTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateProduct = { navController.navigate(Screen.ProductForm.createRoute()) },
                onOpenDrawer = onOpenDrawer,
                viewModelFactory = viewModelFactory,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.SalesHistory.route) {
            SalesHistoryScreen(
                onNavigateToSaleDetail = { saleId ->
                    navController.navigate(Screen.SaleDetail.createRoute(saleId))
                },
                onNavigateBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(
            route = Screen.SaleDetail.route,
            arguments = listOf(
                navArgument("saleId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val saleId = backStackEntry.arguments?.getLong("saleId") ?: 0L
            SaleDetailScreen(
                saleId = saleId,
                onNavigateBack = { navController.popBackStack() },
                viewModelFactory = viewModelFactory,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Purchases.route) {
            PurchasesScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Expenses.route) {
            ExpensesScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.DigitalTransaction.route) {
            DigitalTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Screen.DigitalReports.route) },
                onOpenDrawer = onOpenDrawer,
                viewModelFactory = viewModelFactory,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.DigitalReports.route) {
            DigitalReportsScreen(
                onNavigateToDetail = { reportId ->
                    navController.navigate(Screen.DigitalReportDetail.createRoute(reportId))
                },
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(
            route = Screen.DigitalReportDetail.route,
            arguments = listOf(
                navArgument("reportId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getLong("reportId") ?: 0L
            DigitalReportDetailScreen(
                reportId = reportId,
                onNavigateBack = { navController.popBackStack() },
                viewModelFactory = viewModelFactory,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.DigitalManagement.route) {
            DigitalManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.LowStock.route) {
            LowStockScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }
        
        composable(Screen.Reports.route) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                viewModelFactory = viewModelFactory,
                salesHistoryViewModel = viewModel(factory = viewModelFactory),
                expenseViewModel = viewModel(factory = viewModelFactory),
                digitalTransactionViewModel = viewModel(factory = viewModelFactory)
            )
        }
        
        composable(Screen.ProfitLoss.route) {
            ProfitLossScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                viewModelFactory = viewModelFactory,
                salesHistoryViewModel = viewModel(factory = viewModelFactory),
                expenseViewModel = viewModel(factory = viewModelFactory),
                digitalTransactionViewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Backup.route) {
            BackupScreen(
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Receivable.route) {
            ReceivableScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Customers.route) {
            CustomersScreen(
                onOpenDrawer = onOpenDrawer,
                viewModel = viewModel(factory = viewModelFactory)
            )
        }
    }
}
