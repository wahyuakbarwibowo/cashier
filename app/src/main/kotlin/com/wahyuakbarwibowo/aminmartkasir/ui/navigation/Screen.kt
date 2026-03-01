package com.wahyuakbarwibowo.aminmartkasir.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Products : Screen("products")
    object ProductForm : Screen("product_form?productId={productId}") {
        fun createRoute(productId: Long? = null) = if (productId != null) {
            "product_form?productId=$productId"
        } else {
            "product_form"
        }
    }
    object Customers : Screen("customers")
    object SalesTransaction : Screen("sales_transaction")
    object SalesHistory : Screen("sales_history")
    object SaleDetail : Screen("sale_detail/{saleId}") {
        fun createRoute(saleId: Long) = "sale_detail/$saleId"
    }
    object Purchases : Screen("purchases")
    object Suppliers : Screen("suppliers")
    object Expenses : Screen("expenses")
    object Receivables : Screen("receivables")
    object Payables : Screen("payables")
    object DigitalTransaction : Screen("digital_transaction")
    object DigitalReports : Screen("digital_reports")
    object Settings : Screen("settings")
    object LowStock : Screen("low_stock")
    object Reports : Screen("reports")
    object ProfitLoss : Screen("profit_loss")
}
