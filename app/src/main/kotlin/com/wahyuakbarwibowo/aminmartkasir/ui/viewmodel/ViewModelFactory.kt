package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.wahyuakbarwibowo.aminmartkasir.data.local.AppDatabase
import com.wahyuakbarwibowo.aminmartkasir.data.repository.*

class ViewModelFactory(
    private val database: AppDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    productRepository = ProductRepository(database.productDao()),
                    customerRepository = CustomerRepository(database.customerDao()),
                    saleRepository = SaleRepository(database.saleDao(), database.saleItemDao())
                ) as T
            }
            modelClass.isAssignableFrom(ProductViewModel::class.java) -> {
                ProductViewModel(
                    productRepository = ProductRepository(database.productDao()),
                    stockHistoryRepository = StockHistoryRepository(database.stockHistoryDao())
                ) as T
            }
            modelClass.isAssignableFrom(SalesViewModel::class.java) -> {
                SalesViewModel(
                    productRepository = ProductRepository(database.productDao()),
                    customerRepository = CustomerRepository(database.customerDao()),
                    paymentMethodRepository = PaymentMethodRepository(database.paymentMethodDao()),
                    saleRepository = SaleRepository(database.saleDao(), database.saleItemDao()),
                    stockHistoryRepository = StockHistoryRepository(database.stockHistoryDao()),
                    customerPointsHistoryRepository = CustomerPointsHistoryRepository(database.customerPointsHistoryDao()),
                    receivableRepository = ReceivableRepository(database.receivableDao())
                ) as T
            }
            modelClass.isAssignableFrom(SalesHistoryViewModel::class.java) -> {
                SalesHistoryViewModel(
                    saleRepository = SaleRepository(database.saleDao(), database.saleItemDao())
                ) as T
            }
            modelClass.isAssignableFrom(SaleDetailViewModel::class.java) -> {
                SaleDetailViewModel(
                    saleRepository = SaleRepository(database.saleDao(), database.saleItemDao())
                ) as T
            }
            modelClass.isAssignableFrom(PurchaseViewModel::class.java) -> {
                PurchaseViewModel(
                    supplierRepository = SupplierRepository(
                        database.supplierDao(),
                        database.purchaseDao(),
                        database.purchaseItemDao()
                    ),
                    purchaseRepository = PurchaseRepository(
                        database.purchaseDao(),
                        database.purchaseItemDao(),
                        database.productDao()
                    ),
                    productRepository = ProductRepository(database.productDao()),
                    stockHistoryRepository = StockHistoryRepository(database.stockHistoryDao()),
                    expenseRepository = ExpenseRepository(database.expenseDao())
                ) as T
            }
            modelClass.isAssignableFrom(ExpenseViewModel::class.java) -> {
                ExpenseViewModel(
                    expenseRepository = ExpenseRepository(database.expenseDao())
                ) as T
            }
            modelClass.isAssignableFrom(DigitalTransactionViewModel::class.java) -> {
                DigitalTransactionViewModel(
                    digitalProductRepository = DigitalProductRepository(database.digitalProductDao()),
                    digitalCategoryRepository = DigitalCategoryRepository(database.digitalCategoryDao()),
                    phoneHistoryRepository = PhoneHistoryRepository(database.phoneHistoryDao()),
                    paymentMethodRepository = PaymentMethodRepository(database.paymentMethodDao()),
                    customerRepository = CustomerRepository(database.customerDao()),
                    receivableRepository = ReceivableRepository(database.receivableDao())
                ) as T
            }
            modelClass.isAssignableFrom(DigitalReportDetailViewModel::class.java) -> {
                DigitalReportDetailViewModel(
                    phoneHistoryRepository = PhoneHistoryRepository(database.phoneHistoryDao())
                ) as T
            }
            modelClass.isAssignableFrom(StockHistoryViewModel::class.java) -> {
                StockHistoryViewModel(
                    stockHistoryRepository = StockHistoryRepository(database.stockHistoryDao())
                ) as T
            }
            modelClass.isAssignableFrom(CustomerViewModel::class.java) -> {
                CustomerViewModel(
                    customerRepository = CustomerRepository(database.customerDao())
                ) as T
            }
            modelClass.isAssignableFrom(DebtViewModel::class.java) -> {
                DebtViewModel(
                    receivableRepository = ReceivableRepository(database.receivableDao()),
                    payableRepository = PayableRepository(database.payableDao()),
                    customerRepository = CustomerRepository(database.customerDao()),
                    supplierRepository = SupplierRepository(database.supplierDao(), database.purchaseDao(), database.purchaseItemDao())
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    shopProfileRepository = ShopProfileRepository(database.shopProfileDao()),
                    paymentMethodRepository = PaymentMethodRepository(database.paymentMethodDao())
                ) as T
            }
            modelClass.isAssignableFrom(PrinterViewModel::class.java) -> {
                PrinterViewModel(
                    shopProfileRepository = ShopProfileRepository(database.shopProfileDao())
                ) as T
            }
            modelClass.isAssignableFrom(BackupViewModel::class.java) -> {
                BackupViewModel(
                    backupRepository = BackupRepository(database)
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
