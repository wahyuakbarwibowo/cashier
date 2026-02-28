package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.SupplierDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.PurchaseDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.PurchaseItemDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SupplierEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PurchaseEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PurchaseItemEntity
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
    private val purchaseDao: PurchaseDao,
    private val purchaseItemDao: PurchaseItemDao,
    private val productDao: com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ProductDao
) {
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

    suspend fun insertPurchaseWithItems(purchase: PurchaseEntity, items: List<PurchaseItemEntity>): Long {
        val purchaseId = purchaseDao.insert(purchase)
        items.forEach { item ->
            purchaseItemDao.insert(item.copy(purchaseId = purchaseId))
            // Update product stock
            productDao.increaseStock(item.productId, item.qty)
        }
        return purchaseId
    }

    suspend fun updatePurchaseWithItems(purchase: PurchaseEntity, items: List<PurchaseItemEntity>) {
        purchaseDao.update(purchase)
        purchaseItemDao.deleteByPurchaseId(purchase.id)
        items.forEach { item ->
            purchaseItemDao.insert(item)
        }
    }

    suspend fun deletePurchase(purchase: PurchaseEntity) {
        purchaseDao.delete(purchase)
    }
}
