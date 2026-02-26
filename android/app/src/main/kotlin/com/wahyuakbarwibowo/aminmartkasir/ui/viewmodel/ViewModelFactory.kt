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
                    saleRepository = SaleRepository(database.saleDao(), database.saleItemDao()),
                    expenseRepository = ExpenseRepository(database.expenseDao()),
                    supplierRepository = SupplierRepository(
                        database.supplierDao(),
                        database.purchaseDao(),
                        database.purchaseItemDao()
                    )
                ) as T
            }
            modelClass.isAssignableFrom(ProductViewModel::class.java) -> {
                ProductViewModel(
                    productRepository = ProductRepository(database.productDao())
                ) as T
            }
            modelClass.isAssignableFrom(CustomerViewModel::class.java) -> {
                CustomerViewModel(
                    customerRepository = CustomerRepository(database.customerDao())
                ) as T
            }
            modelClass.isAssignableFrom(SalesViewModel::class.java) -> {
                SalesViewModel(
                    productRepository = ProductRepository(database.productDao()),
                    customerRepository = CustomerRepository(database.customerDao()),
                    paymentMethodRepository = PaymentMethodRepository(database.paymentMethodDao()),
                    saleRepository = SaleRepository(database.saleDao(), database.saleItemDao()),
                    receivableRepository = ReceivableRepository(database.receivableDao()),
                    customerPointsHistoryRepository = CustomerPointsHistoryRepository(database.customerPointsHistoryDao())
                ) as T
            }
            modelClass.isAssignableFrom(SalesHistoryViewModel::class.java) -> {
                SalesHistoryViewModel(
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
                    productRepository = ProductRepository(database.productDao())
                ) as T
            }
            modelClass.isAssignableFrom(SupplierViewModel::class.java) -> {
                SupplierViewModel(
                    supplierRepository = SupplierRepository(
                        database.supplierDao(),
                        database.purchaseDao(),
                        database.purchaseItemDao()
                    )
                ) as T
            }
            modelClass.isAssignableFrom(ExpenseViewModel::class.java) -> {
                ExpenseViewModel(
                    expenseRepository = ExpenseRepository(database.expenseDao())
                ) as T
            }
            modelClass.isAssignableFrom(ReceivableViewModel::class.java) -> {
                ReceivableViewModel(
                    receivableRepository = ReceivableRepository(database.receivableDao()),
                    customerRepository = CustomerRepository(database.customerDao())
                ) as T
            }
            modelClass.isAssignableFrom(PayableViewModel::class.java) -> {
                PayableViewModel(
                    payableRepository = PayableRepository(database.payableDao()),
                    supplierRepository = SupplierRepository(
                        database.supplierDao(),
                        database.purchaseDao(),
                        database.purchaseItemDao()
                    )
                ) as T
            }
            modelClass.isAssignableFrom(DigitalTransactionViewModel::class.java) -> {
                DigitalTransactionViewModel(
                    digitalProductRepository = DigitalProductRepository(database.digitalProductDao()),
                    digitalCategoryRepository = DigitalCategoryRepository(database.digitalCategoryDao()),
                    phoneHistoryRepository = PhoneHistoryRepository(database.phoneHistoryDao())
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
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
