package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.SaleDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.SaleItemDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

class SaleRepository(
    private val saleDao: SaleDao,
    private val saleItemDao: SaleItemDao
) {
    val allSales: Flow<List<SaleEntity>> = saleDao.getAllSales()
    val saleCount: Flow<Int> = saleDao.getSaleCount()

    suspend fun getSales(limit: Int, offset: Int): List<SaleEntity> {
        return saleDao.getSales(limit, offset)
    }

    suspend fun getSaleById(id: Long): SaleEntity? {
        return saleDao.getSaleById(id)
    }

    fun getSaleItems(saleId: Long): Flow<List<SaleItemEntity>> {
        return saleItemDao.getSaleItemsBySaleId(saleId)
    }

    suspend fun getSaleItemsOnce(saleId: Long): List<SaleItemEntity> {
        return saleItemDao.getSaleItemsBySaleIdOnce(saleId)
    }

    suspend fun getTotalSalesByDateRange(): Double {
        return saleDao.getTotalSalesByDateRange() ?: 0.0
    }

    suspend fun insertSaleWithItems(sale: SaleEntity, items: List<SaleItemEntity>): Long {
        val saleId = saleDao.insert(sale)
        items.forEach { item ->
            saleItemDao.insert(item.copy(saleId = saleId))
        }
        return saleId
    }

    suspend fun updateSaleWithItems(sale: SaleEntity, items: List<SaleItemEntity>) {
        saleDao.update(sale)
        saleItemDao.deleteBySaleId(sale.id)
        items.forEach { item ->
            saleItemDao.insert(item)
        }
    }

    suspend fun deleteSale(sale: SaleEntity) {
        saleDao.delete(sale)
    }
}
