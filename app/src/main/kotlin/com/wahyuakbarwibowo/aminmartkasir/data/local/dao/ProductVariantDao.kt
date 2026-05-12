package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductVariantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductVariantDao {
    @Query("SELECT * FROM product_variants WHERE productId = :productId ORDER BY id ASC")
    fun getByProductId(productId: Long): Flow<List<ProductVariantEntity>>

    @Query("SELECT * FROM product_variants WHERE productId = :productId ORDER BY id ASC")
    suspend fun getByProductIdOnce(productId: Long): List<ProductVariantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(variant: ProductVariantEntity): Long

    @Update
    suspend fun update(variant: ProductVariantEntity)

    @Delete
    suspend fun delete(variant: ProductVariantEntity)

    @Query("DELETE FROM product_variants WHERE productId = :productId")
    suspend fun deleteByProductId(productId: Long)
}
