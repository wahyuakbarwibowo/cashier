package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.StockHistoryDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import kotlinx.coroutines.flow.Flow

class StockHistoryRepository(private val stockHistoryDao: StockHistoryDao) {
    val allStockHistory: Flow<List<StockHistoryEntity>> = stockHistoryDao.getAllStockHistory()

    fun getStockHistoryByProduct(productId: Long): Flow<List<StockHistoryEntity>> {
        return stockHistoryDao.getStockHistoryByProduct(productId)
    }

    suspend fun insert(history: StockHistoryEntity): Long {
        return stockHistoryDao.insert(history)
    }
}

