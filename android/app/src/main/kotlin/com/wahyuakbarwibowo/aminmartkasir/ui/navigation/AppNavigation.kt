package com.wahyuakbarwibowo.aminmartkasir.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wahyuakbarwibowo.aminmartkasir.ui.screens.*

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToProducts = { navController.navigate(Screen.Products.route) },
                onNavigateToSales = { navController.navigate(Screen.SalesTransaction.route) },
                onNavigateToCustomers = { navController.navigate(Screen.Customers.route) },
                onNavigateToSuppliers = { navController.navigate(Screen.Suppliers.route) },
                onNavigateToLowStock = { navController.navigate(Screen.LowStock.route) }
            )
        }
        
        composable(Screen.Products.route) {
            ProductsScreen(
                onNavigateToProductForm = { productId ->
                    navController.navigate(Screen.ProductForm.createRoute(productId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.ProductForm.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.LongType
                    defaultValue = -1L
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId")
            ProductFormScreen(
                productId = if (productId == -1L) null else productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Customers.route) {
            CustomersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.SalesTransaction.route) {
            SalesTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onTransactionSuccess = { navController.navigate(Screen.SalesHistory.route) }
            )
        }
        
        composable(Screen.SalesHistory.route) {
            SalesHistoryScreen(
                onNavigateToSaleDetail = { saleId ->
                    navController.navigate(Screen.SaleDetail.createRoute(saleId))
                },
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Purchases.route) {
            PurchasesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Suppliers.route) {
            SuppliersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Expenses.route) {
            ExpensesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Receivables.route) {
            ReceivablesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Payables.route) {
            PayablesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.DigitalTransaction.route) {
            DigitalTransactionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.LowStock.route) {
            LowStockScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Reports.route) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ProfitLoss.route) {
            ProfitLossScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
