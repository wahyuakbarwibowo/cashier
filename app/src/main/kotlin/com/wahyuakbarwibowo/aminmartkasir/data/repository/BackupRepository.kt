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
        val backupData = gson.fromJson(json, BackupData::class.java) ?: throw Exception("Format data tidak valid")
        
        // Clear all existing data first
        database.clearAllTables()

        database.withTransaction {
            kotlin.runCatching {
                // Re-insert everything with sanitization for older versions
                backupData.products?.forEach { product ->
                    val sanitized = if (product.lowStockThreshold == 0) {
                        product.copy(
                            name = product.name ?: "Produk Tanpa Nama",
                            lowStockThreshold = 5
                        )
                    } else {
                        if (product.name == null) product.copy(name = "Produk Tanpa Nama") else product
                    }
                    database.productDao().insert(sanitized)
                }

                backupData.customers?.forEach { customer ->
                    val sanitized = if (customer.name == null) customer.copy(name = "Pelanggan Tanpa Nama") else customer
                    database.customerDao().insert(sanitized)
                }
                
                // Sanitize PaymentMethods (v6 added sortOrder)
                backupData.paymentMethods?.forEachIndexed { index, method ->
                    var sanitized = if (method.sortOrder == 0) method.copy(sortOrder = index) else method
                    if (sanitized.name == null) sanitized = sanitized.copy(name = "Metode Pembayaran ${index + 1}")
                    database.paymentMethodDao().insert(sanitized)
                }
                
                backupData.shopProfile?.let { profile ->
                    database.shopProfileDao().insert(profile)
                }

                backupData.sales?.forEach { sale ->
                    database.saleDao().insert(sale)
                }
                
                // Sanitize SaleItems (v4 added productName)
                backupData.saleItems?.forEach { saleItem ->
                    val sanitized = if (saleItem.productName.isNullOrBlank()) {
                        saleItem.copy(productName = "Produk #${saleItem.productId}")
                    } else {
                        saleItem
                    }
                    database.saleItemDao().insert(sanitized)
                }
                
                backupData.suppliers?.forEach { supplier ->
                    val sanitized = if (supplier.name == null) supplier.copy(name = "Supplier Tanpa Nama") else supplier
                    database.supplierDao().insert(sanitized)
                }

                backupData.purchases?.forEach { purchase ->
                    database.purchaseDao().insert(purchase)
                }

                backupData.purchaseItems?.forEach { purchaseItem ->
                    val sanitized = if (purchaseItem.productName.isNullOrBlank()) {
                        purchaseItem.copy(productName = "Produk #${purchaseItem.productId}")
                    } else {
                        purchaseItem
                    }
                    database.purchaseItemDao().insert(sanitized)
                }

                backupData.receivables?.forEach { receivable ->
                    val sanitized = if (receivable.status == null || receivable.status.isBlank()) {
                        receivable.copy(status = "pending")
                    } else {
                        receivable
                    }
                    database.receivableDao().insert(sanitized)
                }

                backupData.payables?.forEach { payable ->
                    val sanitized = if (payable.status == null || payable.status.isBlank()) {
                        payable.copy(status = "pending")
                    } else {
                        payable
                    }
                    database.payableDao().insert(sanitized)
                }

                backupData.phoneHistory?.forEach { history ->
                    val sanitized = if (history.category == null) history.copy(category = "PULSA") else history
                    database.phoneHistoryDao().insert(sanitized)
                }
                
                // Sanitize DigitalProducts (v3 added sortOrder)
                backupData.digitalProducts?.forEachIndexed { index, prod ->
                    var sanitized = if (prod.sortOrder == 0) prod.copy(sortOrder = index) else prod
                    if (sanitized.name == null) sanitized = sanitized.copy(name = "Produk Digital ${index + 1}")
                    database.digitalProductDao().insert(sanitized)
                }

                // Sanitize DigitalCategories (v3 added sortOrder)
                backupData.digitalCategories?.forEachIndexed { index, cat ->
                    var sanitized = if (cat.sortOrder == 0) cat.copy(sortOrder = index) else cat
                    if (sanitized.name == null) sanitized = sanitized.copy(name = "Kategori Digital ${index + 1}")
                    database.digitalCategoryDao().insert(sanitized)
                }

                backupData.expenses?.forEach { expense ->
                    val sanitized = if (expense.category == null) expense.copy(category = "UMUM") else expense
                    database.expenseDao().insert(sanitized)
                }

                backupData.customerPointsHistory?.forEach { history ->
                    val sanitized = if (history.type == null) history.copy(type = "ADJUSTMENT") else history
                    database.customerPointsHistoryDao().insert(sanitized)
                }

                backupData.stockHistory?.forEach { history ->
                    val sanitized = if (history.productName == null || history.reason == null || history.createdAt == null) {
                        history.copy(
                            productName = history.productName ?: "Produk",
                            reason = history.reason ?: "Migrasi Data",
                            createdAt = history.createdAt ?: ""
                        )
                    } else {
                        history
                    }
                    database.stockHistoryDao().insert(sanitized)
                }
            }.onFailure {
                throw it
            }
        }
    }
}
