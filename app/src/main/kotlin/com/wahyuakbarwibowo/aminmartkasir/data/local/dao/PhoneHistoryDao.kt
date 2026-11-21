package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ReceivableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneHistoryDao {
    @Query("SELECT * FROM phone_history ORDER BY id DESC")
    fun getAllPhoneHistory(): Flow<List<PhoneHistoryEntity>>

    @Query("SELECT * FROM phone_history ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getPhoneHistory(limit: Int, offset: Int): List<PhoneHistoryEntity>

    @Query("SELECT * FROM phone_history WHERE id = :id")
    suspend fun getPhoneHistoryById(id: Long): PhoneHistoryEntity?

    @Query("SELECT * FROM phone_history WHERE category = :category ORDER BY id DESC")
    fun getPhoneHistoryByCategory(category: String): Flow<List<PhoneHistoryEntity>>


    @Query("SELECT * FROM phone_history WHERE createdAt >= :startDate AND createdAt <= :endDate ORDER BY id DESC")
    suspend fun getPhoneHistoryByRange(startDate: String, endDate: String): List<PhoneHistoryEntity>

    @Query("SELECT SUM(sellingPrice) FROM phone_history WHERE createdAt >= :startDate AND createdAt <= :endDate")
    suspend fun getTotalDigitalRevenueByDateRange(startDate: String, endDate: String): Double?

    @Query("SELECT SUM(profit) FROM phone_history WHERE createdAt >= :startDate AND createdAt <= :endDate")
    suspend fun getTotalDigitalProfitByDateRange(startDate: String, endDate: String): Double?

    @Query("SELECT SUM(sellingPrice) FROM phone_history")
    suspend fun getTotalDigitalRevenueAllTime(): Double?

    @Query("SELECT SUM(profit) FROM phone_history")
    suspend fun getTotalDigitalProfitAllTime(): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phoneHistory: PhoneHistoryEntity): Long

    @Update
    suspend fun update(phoneHistory: PhoneHistoryEntity)

    @Delete
    suspend fun delete(phoneHistory: PhoneHistoryEntity)

    @Query("DELETE FROM phone_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insertReceivable(receivable: ReceivableEntity): Long

    /** Transaksi digital dan piutangnya (bila Hutang) disimpan bersama atau tidak sama sekali. */
    @Transaction
    suspend fun insertWithReceivable(phoneHistory: PhoneHistoryEntity, receivable: ReceivableEntity?): Long {
        val id = insert(phoneHistory)
        receivable?.let { insertReceivable(it) }
        return id
    }

    @Query("""SELECT * FROM phone_history
        WHERE instr(LOWER(phoneNumber), LOWER(:query)) > 0 OR instr(LOWER(provider), LOWER(:query)) > 0
            OR instr(LOWER(notes), LOWER(:query)) > 0 OR instr(LOWER(customerName), LOWER(:query)) > 0
        ORDER BY createdAt DESC, id DESC LIMIT :limit""")
    suspend fun searchPhoneHistory(query: String, limit: Int): List<PhoneHistoryEntity>
}
