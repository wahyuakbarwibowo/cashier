package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.wahyuakbarwibowo.aminmartkasir.data.local.AppDatabase
import com.wahyuakbarwibowo.aminmartkasir.data.repository.*

class ViewModelFactory(
    private val database: AppDatabase
) : ViewModelProvider.Factory {

    private val productRepository = ProductRepository(database.productDao())
    private val productVariantRepository = ProductVariantRepository(database.productVariantDao())
    private val customerRepository = CustomerRepository(database.customerDao())
    private val paymentMethodRepository = PaymentMethodRepository(database.paymentMethodDao())
    private val saleRepository = SaleRepository(database)
    private val stockHistoryRepository = StockHistoryRepository(database.stockHistoryDao())
    private val customerPointsHistoryRepository = CustomerPointsHistoryRepository(database.customerPointsHistoryDao())
    private val receivableRepository = ReceivableRepository(database.receivableDao())
    private val payableRepository = PayableRepository(database.payableDao())
    private val supplierRepository = SupplierRepository(database.supplierDao(), database.purchaseDao(), database.purchaseItemDao())
    private val purchaseRepository = PurchaseRepository(database)
    private val expenseRepository = ExpenseRepository(database.expenseDao())
    private val digitalProductRepository = DigitalProductRepository(database.digitalProductDao())
    private val digitalCategoryRepository = DigitalCategoryRepository(database.digitalCategoryDao())
    private val phoneHistoryRepository = PhoneHistoryRepository(database.phoneHistoryDao())
    private val shopProfileRepository = ShopProfileRepository(database.shopProfileDao())
    private val backupRepository = BackupRepository(database)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    productRepository = productRepository,
                    customerRepository = customerRepository,
                    saleRepository = saleRepository
                ) as T
            }
            modelClass.isAssignableFrom(ProductViewModel::class.java) -> {
                ProductViewModel(
                    productRepository = productRepository,
                    productVariantRepository = productVariantRepository,
                    stockHistoryRepository = stockHistoryRepository
                ) as T
            }
            modelClass.isAssignableFrom(SalesViewModel::class.java) -> {
                SalesViewModel(
                    productRepository = productRepository,
                    productVariantRepository = productVariantRepository,
                    customerRepository = customerRepository,
                    paymentMethodRepository = paymentMethodRepository,
                    saleRepository = saleRepository,
                    stockHistoryRepository = stockHistoryRepository,
                    customerPointsHistoryRepository = customerPointsHistoryRepository,
                    receivableRepository = receivableRepository
                ) as T
            }
            modelClass.isAssignableFrom(SalesHistoryViewModel::class.java) -> {
                SalesHistoryViewModel(
                    saleRepository = saleRepository
                ) as T
            }
            modelClass.isAssignableFrom(SaleDetailViewModel::class.java) -> {
                SaleDetailViewModel(
                    saleRepository = saleRepository
                ) as T
            }
            modelClass.isAssignableFrom(PurchaseViewModel::class.java) -> {
                PurchaseViewModel(
                    supplierRepository = supplierRepository,
                    purchaseRepository = purchaseRepository,
                    productRepository = productRepository,
                    stockHistoryRepository = stockHistoryRepository,
                    expenseRepository = expenseRepository
                ) as T
            }
            modelClass.isAssignableFrom(ExpenseViewModel::class.java) -> {
                ExpenseViewModel(
                    expenseRepository = expenseRepository
                ) as T
            }
            modelClass.isAssignableFrom(DigitalTransactionViewModel::class.java) -> {
                DigitalTransactionViewModel(
                    digitalProductRepository = digitalProductRepository,
                    digitalCategoryRepository = digitalCategoryRepository,
                    phoneHistoryRepository = phoneHistoryRepository,
                    paymentMethodRepository = paymentMethodRepository,
                    customerRepository = customerRepository,
                    receivableRepository = receivableRepository
                ) as T
            }
            modelClass.isAssignableFrom(DigitalReportDetailViewModel::class.java) -> {
                DigitalReportDetailViewModel(
                    phoneHistoryRepository = phoneHistoryRepository
                ) as T
            }
            modelClass.isAssignableFrom(StockHistoryViewModel::class.java) -> {
                StockHistoryViewModel(
                    stockHistoryRepository = stockHistoryRepository
                ) as T
            }
            modelClass.isAssignableFrom(CustomerViewModel::class.java) -> {
                CustomerViewModel(
                    customerRepository = customerRepository
                ) as T
            }
            modelClass.isAssignableFrom(DebtViewModel::class.java) -> {
                DebtViewModel(
                    receivableRepository = receivableRepository,
                    payableRepository = payableRepository,
                    customerRepository = customerRepository,
                    supplierRepository = supplierRepository
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    shopProfileRepository = shopProfileRepository,
                    paymentMethodRepository = paymentMethodRepository
                ) as T
            }
            modelClass.isAssignableFrom(PrinterViewModel::class.java) -> {
                PrinterViewModel(
                    shopProfileRepository = shopProfileRepository
                ) as T
            }
            modelClass.isAssignableFrom(BackupViewModel::class.java) -> {
                BackupViewModel(
                    backupRepository = backupRepository
                ) as T
            }
            modelClass.isAssignableFrom(ProfitLossViewModel::class.java) -> {
                ProfitLossViewModel(
                    saleRepository = saleRepository,
                    expenseRepository = expenseRepository,
                    phoneHistoryRepository = phoneHistoryRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
