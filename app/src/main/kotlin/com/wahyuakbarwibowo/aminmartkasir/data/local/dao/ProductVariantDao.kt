package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM product_variants ORDER BY id ASC")
    fun getAllVariantsForBackup(): Flow<List<ProductVariantEntity>>

    @Query("UPDATE product_variants SET stock = stock - :qty WHERE id = :variantId")
    suspend fun decreaseStock(variantId: Long, qty: Int)

    @Query("UPDATE product_variants SET stock = stock + :qty WHERE id = :variantId")
    suspend fun increaseStock(variantId: Long, qty: Int)

    @Query("SELECT * FROM product_variants WHERE id = :id")
    suspend fun getVariantById(id: Long): ProductVariantEntity?

    /**
     * Samakan varian produk dengan daftar dari form: update yang id-nya ada, insert yang
     * baru (id = 0), hapus sisanya. ID varian lama dipertahankan karena sales_items.variantId
     * merujuk ke sana (dipakai untuk mengembalikan stok saat transaksi diedit).
     */
    @Transaction
    suspend fun syncForProduct(productId: Long, variants: List<ProductVariantEntity>) {
        val existing = getByProductIdOnce(productId)
        val keepIds = variants.filter { it.id != 0L }.map { it.id }.toSet()
        existing.filter { it.id !in keepIds }.forEach { delete(it) }
        val existingIds = existing.map { it.id }.toSet()
        variants.forEach { variant ->
            if (variant.id != 0L && variant.id in existingIds) {
                update(variant.copy(productId = productId))
            } else {
                insert(variant.copy(id = 0, productId = productId))
            }
        }
    }
}
