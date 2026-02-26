package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerPointsHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerPointsHistoryDao {
    @Query("SELECT * FROM customer_points_history ORDER BY id DESC")
    fun getAllCustomerPointsHistory(): Flow<List<CustomerPointsHistoryEntity>>

    @Query("SELECT * FROM customer_points_history WHERE customerId = :customerId ORDER BY id DESC")
    fun getCustomerPointsHistoryByCustomerId(customerId: Long): Flow<List<CustomerPointsHistoryEntity>>

    @Query("SELECT * FROM customer_points_history WHERE saleId = :saleId ORDER BY id DESC")
    fun getCustomerPointsHistoryBySaleId(saleId: Long): Flow<List<CustomerPointsHistoryEntity>>

    @Query("SELECT * FROM customer_points_history WHERE customerId = :customerId ORDER BY id DESC LIMIT 100")
    fun getCustomerPointsHistoryByDateRange(customerId: Long): Flow<List<CustomerPointsHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customerPointsHistory: CustomerPointsHistoryEntity): Long

    @Update
    suspend fun update(customerPointsHistory: CustomerPointsHistoryEntity)

    @Delete
    suspend fun delete(customerPointsHistory: CustomerPointsHistoryEntity)

    @Query("DELETE FROM customer_points_history WHERE saleId = :saleId")
    suspend fun deleteBySaleId(saleId: Long)
}
