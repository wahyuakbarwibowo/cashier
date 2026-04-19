package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.StockHistoryDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import kotlinx.coroutines.flow.Flow

class StockHistoryRepository(private val stockHistoryDao: StockHistoryDao) {
    val allStockHistory: Flow<List<StockHistoryEntity>> = stockHistoryDao.getAllStockHistory()

    suspend fun getStockHistory(limit: Int, offset: Int): List<StockHistoryEntity> {
        return stockHistoryDao.getStockHistory(limit, offset)
    }

    fun getStockHistoryByProduct(productId: Long): Flow<List<StockHistoryEntity>> {
        return stockHistoryDao.getStockHistoryByProduct(productId)
    }

    suspend fun getStockHistoryById(id: Long): StockHistoryEntity? {
        return stockHistoryDao.getStockHistoryById(id)
    }

    suspend fun insert(history: StockHistoryEntity): Long {
        return stockHistoryDao.insert(history)
    }

    suspend fun update(history: StockHistoryEntity) {
        stockHistoryDao.update(history)
    }

    suspend fun delete(history: StockHistoryEntity) {
        stockHistoryDao.delete(history)
    }

    suspend fun deleteByProductId(productId: Long) {
        stockHistoryDao.deleteByProductId(productId)
    }
}

