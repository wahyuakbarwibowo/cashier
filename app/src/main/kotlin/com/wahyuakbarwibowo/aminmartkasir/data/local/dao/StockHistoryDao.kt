package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockHistoryDao {
    @Query("SELECT * FROM stock_history ORDER BY createdAt DESC, id DESC")
    fun getAllStockHistory(): Flow<List<StockHistoryEntity>>

    @Query("SELECT * FROM stock_history WHERE productId = :productId ORDER BY createdAt DESC, id DESC")
    fun getStockHistoryByProduct(productId: Long): Flow<List<StockHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: StockHistoryEntity): Long
}

