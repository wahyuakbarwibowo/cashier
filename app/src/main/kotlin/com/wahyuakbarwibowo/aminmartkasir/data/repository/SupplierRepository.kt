package com.wahyuakbarwibowo.aminmartkasir.data.repository

import androidx.room.withTransaction
import com.wahyuakbarwibowo.aminmartkasir.data.local.AppDatabase
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.SupplierDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.PurchaseDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.PurchaseItemDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PurchaseEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PurchaseItemEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class SupplierRepository(
    private val supplierDao: SupplierDao,
    private val purchaseDao: PurchaseDao,
    private val purchaseItemDao: PurchaseItemDao
) {
    val allSuppliers: Flow<List<SupplierEntity>> = supplierDao.getAllSuppliers()
    val supplierCount: Flow<Int> = supplierDao.getSupplierCount()

    suspend fun getSuppliers(limit: Int, offset: Int): List<SupplierEntity> {
        return supplierDao.getSuppliers(limit, offset)
    }

    suspend fun getSupplierById(id: Long): SupplierEntity? {
        return supplierDao.getSupplierById(id)
    }

    suspend fun insert(supplier: SupplierEntity): Long {
        return supplierDao.insert(supplier)
    }

    suspend fun update(supplier: SupplierEntity) {
        supplierDao.update(supplier)
    }

    suspend fun delete(supplier: SupplierEntity) {
        supplierDao.delete(supplier)
    }

    suspend fun deleteById(id: Long) {
        supplierDao.deleteById(id)
    }
}

class PurchaseRepository(
    private val database: AppDatabase
) {
    private val purchaseDao = database.purchaseDao()
    private val purchaseItemDao = database.purchaseItemDao()
    private val productDao = database.productDao()

    val allPurchases: Flow<List<PurchaseEntity>> = purchaseDao.getAllPurchases()

    suspend fun getPurchases(limit: Int, offset: Int): List<PurchaseEntity> {
        return purchaseDao.getPurchases(limit, offset)
    }

    suspend fun getPurchaseById(id: Long): PurchaseEntity? {
        return purchaseDao.getPurchaseById(id)
    }

    fun getPurchaseItems(purchaseId: Long): Flow<List<PurchaseItemEntity>> {
        return purchaseItemDao.getPurchaseItemsByPurchaseId(purchaseId)
    }

    suspend fun deletePurchase(purchase: PurchaseEntity) {
        purchaseDao.delete(purchase)
    }

    /**
     * Memproses pembelian stok dari supplier secara atomik di dalam sebuah Room Database Transaction.
     */
    suspend fun processPurchaseTransaction(
        purchase: PurchaseEntity,
        items: List<PurchaseItemEntity>,
        expense: ExpenseEntity
    ): Long = database.withTransaction {
        val purchaseId = purchaseDao.insert(purchase)
        
        items.forEach { item ->
            purchaseItemDao.insert(item.copy(purchaseId = purchaseId))
            
            item.productId?.let { productId ->
                val product = productDao.getProductById(productId)
                if (product != null) {
                    productDao.increaseStock(productId, item.qty)
                    database.stockHistoryDao().insert(
                        StockHistoryEntity(
                            productId = productId,
                            productName = product.name,
                            changeQty = item.qty,
                            stockBefore = product.stock,
                            stockAfter = product.stock + item.qty,
                            reason = "Pembelian dari supplier",
                            createdAt = purchase.createdAt ?: ""
                        )
                    )
                }
            }
        }

        database.expenseDao().insert(expense)
        
        purchaseId
    }
}
