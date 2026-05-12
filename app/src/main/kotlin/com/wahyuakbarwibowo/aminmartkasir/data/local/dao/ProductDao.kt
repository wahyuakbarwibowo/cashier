package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getProducts(limit: Int, offset: Int): List<ProductEntity>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE ASC LIMIT :limit OFFSET :offset")
    suspend fun getProductsOrderByNameAsc(limit: Int, offset: Int): List<ProductEntity>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE DESC LIMIT :limit OFFSET :offset")
    suspend fun getProductsOrderByNameDesc(limit: Int, offset: Int): List<ProductEntity>

    @Query("SELECT * FROM products ORDER BY stock ASC, name COLLATE NOCASE ASC LIMIT :limit OFFSET :offset")
    suspend fun getProductsOrderByStockAsc(limit: Int, offset: Int): List<ProductEntity>

    @Query("SELECT * FROM products ORDER BY stock DESC, name COLLATE NOCASE ASC LIMIT :limit OFFSET :offset")
    suspend fun getProductsOrderByStockDesc(limit: Int, offset: Int): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE code = :code")
    suspend fun getProductByCode(code: String): ProductEntity?

    @Query("SELECT * FROM products WHERE stock < :threshold ORDER BY stock ASC")
    fun getLowStockProducts(threshold: Int = 10): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE products SET stock = stock - :qty WHERE id = :productId")
    suspend fun decreaseStock(productId: Long, qty: Int)

    @Query("UPDATE products SET stock = stock + :qty WHERE id = :productId")
    suspend fun increaseStock(productId: Long, qty: Int)

    @Query("SELECT COUNT(*) FROM products")
    fun getProductCount(): Flow<Int>
}
