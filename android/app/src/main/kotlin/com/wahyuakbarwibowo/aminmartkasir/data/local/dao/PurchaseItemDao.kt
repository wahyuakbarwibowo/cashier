package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PurchaseItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseItemDao {
    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY id ASC")
    fun getPurchaseItemsByPurchaseId(purchaseId: Long): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY id ASC")
    suspend fun getPurchaseItemsByPurchaseIdOnce(purchaseId: Long): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchaseItem: PurchaseItemEntity): Long

    @Update
    suspend fun update(purchaseItem: PurchaseItemEntity)

    @Delete
    suspend fun delete(purchaseItem: PurchaseItemEntity)

    @Query("DELETE FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun deleteByPurchaseId(purchaseId: Long)
}
