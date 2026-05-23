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

    @Query("SELECT SUM(total) FROM sales WHERE createdAt >= :startDate AND createdAt <= :endDate")
    suspend fun getTotalSalesByDateRange(startDate: String, endDate: String): Double?

    @Query("SELECT SUM(profit) FROM sales WHERE createdAt >= :startDate AND createdAt <= :endDate")
    suspend fun getTotalProfitByDateRange(startDate: String, endDate: String): Double?

    @Query("SELECT SUM(total) FROM sales")
    suspend fun getTotalSalesAllTime(): Double?

    @Query("SELECT SUM(profit) FROM sales")
    suspend fun getTotalProfitAllTime(): Double?

    @Query("SELECT * FROM sales WHERE createdAt >= :startDate ORDER BY id DESC")
    suspend fun getSalesSince(startDate: String): List<SaleEntity>

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
