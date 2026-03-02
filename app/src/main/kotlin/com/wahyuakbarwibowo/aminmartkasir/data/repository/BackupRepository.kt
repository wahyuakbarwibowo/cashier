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
                // Re-insert everything
                for (product in backupData.products) {
                    database.productDao().insert(product)
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
                for (saleItem in backupData.saleItems) {
                    database.saleItemDao().insert(saleItem)
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
                    database.receivableDao().insert(receivable)
                }
                for (payable in backupData.payables) {
                    database.payableDao().insert(payable)
                }
                for (history in backupData.phoneHistory) {
                    database.phoneHistoryDao().insert(history)
                }
                for (digitalProduct in backupData.digitalProducts) {
                    database.digitalProductDao().insert(digitalProduct)
                }
                for (digitalCategory in backupData.digitalCategories) {
                    database.digitalCategoryDao().insert(digitalCategory)
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
