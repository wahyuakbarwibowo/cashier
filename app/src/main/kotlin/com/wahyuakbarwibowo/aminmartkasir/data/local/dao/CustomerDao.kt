package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getCustomers(limit: Int, offset: Int): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE instr(LOWER(name), LOWER(:query)) > 0 OR instr(LOWER(phone), LOWER(:query)) > 0")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    /** Tolak bila masih ada piutang belum lunas; histori lunas tetap ada (FK SET NULL). Return 0 = ditolak. */
    @Query("DELETE FROM customers WHERE id = :id AND NOT EXISTS (SELECT 1 FROM receivables WHERE customerId = :id AND status != 'paid')")
    suspend fun deleteIfNoUnpaidReceivable(id: Long): Int

    @Query("UPDATE customers SET points = points + :points WHERE id = :id")
    suspend fun addPoints(id: Long, points: Int)

    @Query("UPDATE customers SET points = MAX(points - :points, 0) WHERE id = :id")
    suspend fun deductPoints(id: Long, points: Int)

    @Query("SELECT COUNT(*) FROM customers")
    fun getCustomerCount(): Flow<Int>
}
