package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ShopProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopProfileDao {
    @Query("SELECT * FROM shop_profile LIMIT 1")
    fun getShopProfile(): Flow<ShopProfileEntity?>

    @Query("SELECT * FROM shop_profile LIMIT 1")
    suspend fun getShopProfileOnce(): ShopProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ShopProfileEntity): Long

    @Update
    suspend fun update(profile: ShopProfileEntity)

    @Delete
    suspend fun delete(profile: ShopProfileEntity)
}
