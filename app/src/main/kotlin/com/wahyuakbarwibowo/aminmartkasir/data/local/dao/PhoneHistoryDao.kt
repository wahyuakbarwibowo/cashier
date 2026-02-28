package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
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

    @Query("SELECT * FROM phone_history ORDER BY id DESC LIMIT 100")
    fun getPhoneHistoryByDateRange(): Flow<List<PhoneHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phoneHistory: PhoneHistoryEntity): Long

    @Update
    suspend fun update(phoneHistory: PhoneHistoryEntity)

    @Delete
    suspend fun delete(phoneHistory: PhoneHistoryEntity)

    @Query("DELETE FROM phone_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
