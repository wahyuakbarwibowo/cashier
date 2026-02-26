package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DigitalProductDao {
    @Query("SELECT * FROM digital_products ORDER BY name ASC")
    fun getAllDigitalProducts(): Flow<List<DigitalProductEntity>>

    @Query("SELECT * FROM digital_products WHERE category = :category ORDER BY name ASC")
    fun getDigitalProductsByCategory(category: String): Flow<List<DigitalProductEntity>>

    @Query("SELECT * FROM digital_products WHERE id = :id")
    suspend fun getDigitalProductById(id: Long): DigitalProductEntity?

    @Query("SELECT * FROM digital_products WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchDigitalProducts(query: String): Flow<List<DigitalProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(digitalProduct: DigitalProductEntity): Long

    @Update
    suspend fun update(digitalProduct: DigitalProductEntity)

    @Delete
    suspend fun delete(digitalProduct: DigitalProductEntity)

    @Query("DELETE FROM digital_products WHERE id = :id")
    suspend fun deleteById(id: Long)
}
