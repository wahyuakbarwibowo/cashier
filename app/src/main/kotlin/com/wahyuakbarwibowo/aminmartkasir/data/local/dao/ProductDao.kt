package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProductsForBackup(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getProducts(limit: Int, offset: Int): List<ProductEntity>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE ASC LIMIT :limit OFFSET :offset")
    suspend fun getProductsOrderByNameAsc(limit: Int, offset: Int): List<ProductEntity>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE DESC LIMIT :limit OFFSET :offset")
    suspend fun getProductsOrderByNameDesc(limit: Int, offset: Int): List<ProductEntity>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY stock ASC, name COLLATE NOCASE ASC LIMIT :limit OFFSET :offset")
    suspend fun getProductsOrderByStockAsc(limit: Int, offset: Int): List<ProductEntity>

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY stock DESC, name COLLATE NOCASE ASC LIMIT :limit OFFSET :offset")
    suspend fun getProductsOrderByStockDesc(limit: Int, offset: Int): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id AND isDeleted = 0")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE code = :code AND isDeleted = 0")
    suspend fun getProductByCode(code: String): ProductEntity?

    @Query("SELECT * FROM products WHERE stock < :threshold AND isDeleted = 0 ORDER BY stock ASC")
    fun getLowStockProducts(threshold: Int = 10): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%')")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("SELECT * FROM products WHERE isSynced = 0")
    suspend fun getPendingSync(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(product: ProductEntity): Long

    @Query("""UPDATE products SET code = :code, name = :name,
        purchasePrice = :purchasePrice, purchasePackagePrice = :purchasePackagePrice,
        purchasePackageQty = :purchasePackageQty, sellingPrice = :sellingPrice,
        packagePrice = :packagePrice, packageQty = :packageQty,
        discount = :discount, stock = :stock,
        lowStockThreshold = :lowStockThreshold, createdAt = :createdAt,
        updatedAt = :updatedAt, remoteId = :remoteId,
        syncUpdatedAt = :syncUpdatedAt, isSynced = 1, isDeleted = :isDeleted
        WHERE id = :id AND syncUpdatedAt = :expectedSyncUpdatedAt
        AND isSynced = :expectedIsSynced""")
    suspend fun applyRemoteIfUnchanged(
        id: Long,
        code: String?,
        name: String,
        purchasePrice: Double,
        purchasePackagePrice: Double,
        purchasePackageQty: Int,
        sellingPrice: Double,
        packagePrice: Double,
        packageQty: Int,
        discount: Double,
        stock: Int,
        lowStockThreshold: Int,
        createdAt: String?,
        updatedAt: String?,
        remoteId: String,
        syncUpdatedAt: Long,
        isDeleted: Boolean,
        expectedSyncUpdatedAt: Long,
        expectedIsSynced: Boolean
    ): Int

    @Query("UPDATE products SET isSynced = 1 WHERE id = :id AND syncUpdatedAt = :syncUpdatedAt")
    suspend fun markSynced(id: Long, syncUpdatedAt: Long)

    @Query("UPDATE products SET isDeleted = 1, isSynced = 0, syncUpdatedAt = :now WHERE id = :id")
    suspend fun delete(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE products SET isDeleted = 1, isSynced = 0, syncUpdatedAt = :now WHERE id = :id")
    suspend fun deleteById(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stock = stock - :qty, isSynced = 0, syncUpdatedAt = :now WHERE id = :productId AND isDeleted = 0")
    suspend fun decreaseStock(productId: Long, qty: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stock = stock + :qty, isSynced = 0, syncUpdatedAt = :now WHERE id = :productId AND isDeleted = 0")
    suspend fun increaseStock(productId: Long, qty: Int, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM products WHERE isDeleted = 0")
    fun getProductCount(): Flow<Int>
}
