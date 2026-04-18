package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DigitalCategoryDao {
    @Query("SELECT * FROM digital_categories ORDER BY sortOrder ASC, name ASC")
    fun getAllDigitalCategories(): Flow<List<DigitalCategoryEntity>>

    @Query("SELECT * FROM digital_categories WHERE id = :id")
    suspend fun getDigitalCategoryById(id: Long): DigitalCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(digitalCategory: DigitalCategoryEntity): Long

    @Update
    suspend fun update(digitalCategory: DigitalCategoryEntity)

    @Delete
    suspend fun delete(digitalCategory: DigitalCategoryEntity)

    @Query("DELETE FROM digital_categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
