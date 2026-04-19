package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PayableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayableDao {
    @Query("SELECT * FROM payables ORDER BY createdAt DESC, id DESC")
    fun getAllPayables(): Flow<List<PayableEntity>>

    @Query("SELECT * FROM payables ORDER BY createdAt DESC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun getPayables(limit: Int, offset: Int): List<PayableEntity>

    @Query("SELECT * FROM payables WHERE status = :status ORDER BY createdAt DESC, id DESC LIMIT :limit OFFSET :offset")
    suspend fun getPayablesByStatus(status: String, limit: Int, offset: Int): List<PayableEntity>

    @Query("SELECT * FROM payables WHERE supplierId = :supplierId ORDER BY createdAt DESC, id DESC")
    fun getPayablesBySupplierId(supplierId: Long): Flow<List<PayableEntity>>

    @Query("SELECT * FROM payables WHERE id = :id")
    suspend fun getPayableById(id: Long): PayableEntity?

    @Query("SELECT * FROM payables WHERE purchaseId = :purchaseId")
    suspend fun getPayableByPurchaseId(purchaseId: Long): PayableEntity?

    @Query("SELECT * FROM payables WHERE status = :status ORDER BY dueDate ASC")
    fun getPayablesByStatus(status: String): Flow<List<PayableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payable: PayableEntity): Long

    @Update
    suspend fun update(payable: PayableEntity)

    @Delete
    suspend fun delete(payable: PayableEntity)

    @Query("UPDATE payables SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
