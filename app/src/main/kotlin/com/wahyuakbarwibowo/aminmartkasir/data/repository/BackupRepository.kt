package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.google.gson.Gson
import com.wahyuakbarwibowo.aminmartkasir.data.local.AppDatabase
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.*

import androidx.room.withTransaction

class BackupRepository(private val database: AppDatabase) {
    private val gson = Gson()

    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        val backupData = BackupData(
            products = database.productDao().getAllProducts().first(),
            customers = database.customerDao().getAllCustomers().first(),
            paymentMethods = database.paymentMethodDao().getAllPaymentMethods().first(),
            shopProfile = database.shopProfileDao().getShopProfile().first(),
            sales = database.saleDao().getAllSales().first(),
            saleItems = database.saleItemDao().getAllSaleItemsForBackup().first(),
            suppliers = database.supplierDao().getAllSuppliers().first(),
            purchases = database.purchaseDao().getAllPurchases().first(),
            purchaseItems = database.purchaseItemDao().getAllPurchaseItemsForBackup().first(),
            receivables = database.receivableDao().getAllReceivables().first(),
            payables = database.payableDao().getAllPayables().first(),
            phoneHistory = database.phoneHistoryDao().getAllPhoneHistory().first(),
            digitalProducts = database.digitalProductDao().getAllDigitalProducts().first(),
            digitalCategories = database.digitalCategoryDao().getAllDigitalCategories().first(),
            expenses = database.expenseDao().getAllExpenses().first(),
            customerPointsHistory = database.customerPointsHistoryDao().getAllCustomerPointsHistory().first(),
            stockHistory = database.stockHistoryDao().getAllStockHistory().first()
        )
        gson.toJson(backupData)
    }

    suspend fun importData(json: String) = withContext(Dispatchers.IO) {
        val backupData = gson.fromJson(json, BackupData::class.java)
        
        // Clear all existing data first
        database.clearAllTables()

        database.withTransaction {
            kotlin.runCatching {
                // Re-insert everything with sanitization for older versions
                for (product in backupData.products) {
                    val sanitized = if (product.lowStockThreshold == 0) product.copy(lowStockThreshold = 5) else product
                    database.productDao().insert(sanitized)
                }
                for (customer in backupData.customers) {
                    database.customerDao().insert(customer)
                }
                for (paymentMethod in backupData.paymentMethods) {
                    database.paymentMethodDao().insert(paymentMethod)
                }
                backupData.shopProfile?.let { database.shopProfileDao().insert(it) }
                for (sale in backupData.sales) {
                    database.saleDao().insert(sale)
                }
                
                // Sanitize SaleItems (v4 added productName)
                for (saleItem in backupData.saleItems) {
                    val sanitized = if (saleItem.productName.isNullOrBlank()) {
                        saleItem.copy(productName = "Produk #${saleItem.productId}")
                    } else {
                        saleItem
                    }
                    database.saleItemDao().insert(sanitized)
                }
                
                for (supplier in backupData.suppliers) {
                    database.supplierDao().insert(supplier)
                }
                for (purchase in backupData.purchases) {
                    database.purchaseDao().insert(purchase)
                }
                for (purchaseItem in backupData.purchaseItems) {
                    database.purchaseItemDao().insert(purchaseItem)
                }
                for (receivable in backupData.receivables) {
                    val sanitized = if (receivable.status.isBlank()) receivable.copy(status = "pending") else receivable
                    database.receivableDao().insert(sanitized)
                }
                for (payable in backupData.payables) {
                    val sanitized = if (payable.status.isBlank()) payable.copy(status = "pending") else payable
                    database.payableDao().insert(sanitized)
                }
                for (history in backupData.phoneHistory) {
                    database.phoneHistoryDao().insert(history)
                }
                
                // Sanitize DigitalProducts (v3 added sortOrder)
                backupData.digitalProducts.forEachIndexed { index, prod ->
                    // If sortOrder is 0, it might be from old version, assign an index
                    val sanitized = if (prod.sortOrder == 0) prod.copy(sortOrder = index) else prod
                    database.digitalProductDao().insert(sanitized)
                }

                // Sanitize DigitalCategories (v3 added sortOrder)
                backupData.digitalCategories.forEachIndexed { index, cat ->
                    val sanitized = if (cat.sortOrder == 0) cat.copy(sortOrder = index) else cat
                    database.digitalCategoryDao().insert(sanitized)
                }

                for (expense in backupData.expenses) {
                    database.expenseDao().insert(expense)
                }
                for (history in backupData.customerPointsHistory) {
                    database.customerPointsHistoryDao().insert(history)
                }
                for (history in backupData.stockHistory) {
                    database.stockHistoryDao().insert(history)
                }
            }.onFailure {
                throw it
            }
        }
    }
}
