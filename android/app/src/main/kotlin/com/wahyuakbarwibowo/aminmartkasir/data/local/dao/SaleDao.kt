package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY id DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getSales(limit: Int, offset: Int): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): SaleEntity?

    @Query("SELECT * FROM sales ORDER BY id DESC")
    fun getSalesByDateRange(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getSalesByDateRange(limit: Int, offset: Int): List<SaleEntity>

    @Query("SELECT SUM(total) FROM sales")
    suspend fun getTotalSalesByDateRange(): Double?

    // getTotalPointsEarnedByDateRange removed - use getAllSales and calculate manually

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: SaleEntity): Long

    @Update
    suspend fun update(sale: SaleEntity)

    @Delete
    suspend fun delete(sale: SaleEntity)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM sales")
    fun getSaleCount(): Flow<Int>
}
