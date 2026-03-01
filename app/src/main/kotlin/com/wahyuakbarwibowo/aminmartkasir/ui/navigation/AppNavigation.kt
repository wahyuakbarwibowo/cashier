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
    viewModelFactory: Factory
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
                onNavigateToLowStock = { navController.navigate(Screen.LowStock.route) },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Products.route) {
            ProductsScreen(
                onNavigateToProductForm = { productId ->
                    navController.navigate(Screen.ProductForm.createRoute(productId))
                },
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }
        
        composable(
            route = Screen.ProductForm.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.LongType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val productId = if (args?.containsKey("productId") == true) {
                args.getLong("productId")
            } else {
                null
            }
            ProductFormScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Customers.route) {
            CustomersScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.SalesTransaction.route) {
            SalesTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateProduct = { navController.navigate(Screen.ProductForm.createRoute()) },
                onTransactionSuccess = { navController.navigate(Screen.SalesHistory.route) },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.SalesHistory.route) {
            SalesHistoryScreen(
                onNavigateToSaleDetail = { saleId ->
                    navController.navigate(Screen.SaleDetail.createRoute(saleId))
                },
                onNavigateBack = { navController.popBackStack() },
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
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Purchases.route) {
            PurchasesScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Suppliers.route) {
            SuppliersScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Expenses.route) {
            ExpensesScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Receivables.route) {
            ReceivablesScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Payables.route) {
            PayablesScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.DigitalTransaction.route) {
            DigitalTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel(factory = viewModelFactory)
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
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
