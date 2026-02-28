package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sales_items WHERE saleId = :saleId ORDER BY id ASC")
    fun getSaleItemsBySaleId(saleId: Long): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sales_items WHERE saleId = :saleId ORDER BY id ASC")
    suspend fun getSaleItemsBySaleIdOnce(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sales_items WHERE id = :id")
    suspend fun getSaleItemById(id: Long): SaleItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(saleItem: SaleItemEntity): Long

    @Update
    suspend fun update(saleItem: SaleItemEntity)

    @Delete
    suspend fun delete(saleItem: SaleItemEntity)

    @Query("DELETE FROM sales_items WHERE saleId = :saleId")
    suspend fun deleteBySaleId(saleId: Long)
}
