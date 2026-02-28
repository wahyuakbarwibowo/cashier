package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ReceivableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceivableDao {
    @Query("SELECT * FROM receivables ORDER BY dueDate ASC")
    fun getAllReceivables(): Flow<List<ReceivableEntity>>

    @Query("SELECT * FROM receivables WHERE customerId = :customerId ORDER BY dueDate ASC")
    fun getReceivablesByCustomerId(customerId: Long): Flow<List<ReceivableEntity>>

    @Query("SELECT * FROM receivables WHERE id = :id")
    suspend fun getReceivableById(id: Long): ReceivableEntity?

    @Query("SELECT * FROM receivables WHERE saleId = :saleId")
    suspend fun getReceivableBySaleId(saleId: Long): ReceivableEntity?

    @Query("SELECT * FROM receivables WHERE status = :status ORDER BY dueDate ASC")
    fun getReceivablesByStatus(status: String): Flow<List<ReceivableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receivable: ReceivableEntity): Long

    @Update
    suspend fun update(receivable: ReceivableEntity)

    @Delete
    suspend fun delete(receivable: ReceivableEntity)

    @Query("UPDATE receivables SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
