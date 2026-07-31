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
    object StockHistory : Screen("stock_history")
    object SalesTransaction : Screen("sales_transaction?saleId={saleId}") {
        fun createRoute(saleId: Long? = null) = if (saleId != null) {
            "sales_transaction?saleId=$saleId"
        } else {
            "sales_transaction"
        }
    }
    object SalesHistory : Screen("sales_history")
    object SaleDetail : Screen("sale_detail/{saleId}") {
        fun createRoute(saleId: Long) = "sale_detail/$saleId"
    }
    object Purchases : Screen("purchases")
    object Expenses : Screen("expenses")
    object DigitalTransaction : Screen("digital_transaction")
    object DigitalReports : Screen("digital_reports")
    object DigitalReportDetail : Screen("digital_report_detail/{reportId}") {
        fun createRoute(reportId: Long) = "digital_report_detail/$reportId"
    }
    object DigitalManagement : Screen("digital_management")
    object Settings : Screen("settings")
    object LowStock : Screen("low_stock")
    object Reports : Screen("reports")
    object ProfitLoss : Screen("profit_loss")
    object Backup : Screen("backup")
    object Receivable : Screen("receivable")
    object Customers : Screen("customers")
    object GlobalSearch : Screen("global_search")
    object Shift : Screen("shift")
}
