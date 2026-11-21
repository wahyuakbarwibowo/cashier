package com.wahyuakbarwibowo.aminmartkasir.data.local.dao

import androidx.room.*
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.StockHistoryEntity
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

    @Query("SELECT * FROM products WHERE stock < lowStockThreshold AND isDeleted = 0 ORDER BY stock ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products WHERE stock < lowStockThreshold AND isDeleted = 0")
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%')")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    /** Data produk belum terkirim, atau ada delta stok yang belum terkirim. */
    @Query("SELECT * FROM products WHERE isSynced = 0 OR stock != syncedStock")
    suspend fun getPendingSync(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(product: ProductEntity): Long

    @Query("""UPDATE products SET code = :code, name = :name,
        purchasePrice = :purchasePrice, purchasePackagePrice = :purchasePackagePrice,
        purchasePackageQty = :purchasePackageQty, sellingPrice = :sellingPrice,
        packagePrice = :packagePrice, packageQty = :packageQty,
        discount = :discount,
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
        lowStockThreshold: Int,
        createdAt: String?,
        updatedAt: String?,
        remoteId: String,
        syncUpdatedAt: Long,
        isDeleted: Boolean,
        expectedSyncUpdatedAt: Long,
        expectedIsSynced: Boolean
    ): Int

    /**
     * Samakan stok dengan cloud tanpa membuang delta lokal yang belum terkirim.
     * Basis null hanya diambil alih bila tidak ada perubahan lokal yang tertunda.
     */
    @Query("""UPDATE products SET stock = :remoteStock + COALESCE(stock - syncedStock, 0),
        syncedStock = :remoteStock
        WHERE id = :id AND (syncedStock IS NOT NULL OR isSynced = 1)
        AND (syncedStock IS NULL OR syncedStock != :remoteStock)""")
    suspend fun reconcileStock(id: Long, remoteStock: Int)

    /** Delta dari [pushedStock] sudah dijumlahkan server menjadi [serverStock]. */
    @Query("UPDATE products SET stock = stock + (:serverStock - :pushedStock), syncedStock = :serverStock WHERE id = :id")
    suspend fun markStockPushed(id: Long, pushedStock: Int, serverStock: Int)

    /** Pasang remoteId tanpa menulis ulang baris (stok bisa berubah karena penjualan di sela sync). */
    @Query("UPDATE products SET remoteId = :remoteId WHERE id = :id AND remoteId IS NULL")
    suspend fun assignRemoteId(id: Long, remoteId: String)

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

    @Insert
    suspend fun insertStockHistory(history: StockHistoryEntity)

    /**
     * Update dari form produk. Stok diubah sebagai delta (`stock = stock + :stockDelta`)
     * agar penjualan yang terjadi selama form terbuka tidak ikut tertimpa. remoteId,
     * createdAt, dan lowStockThreshold (tidak ada di form) tidak disentuh.
     */
    @Query("""UPDATE products SET code = :code, name = :name,
        purchasePrice = :purchasePrice, purchasePackagePrice = :purchasePackagePrice,
        purchasePackageQty = :purchasePackageQty, sellingPrice = :sellingPrice,
        packagePrice = :packagePrice, packageQty = :packageQty, discount = :discount,
        stock = stock + :stockDelta, updatedAt = :updatedAt,
        isSynced = 0, isDeleted = 0, syncUpdatedAt = :now
        WHERE id = :id""")
    suspend fun updateDetails(
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
        stockDelta: Int,
        updatedAt: String,
        now: Long
    )

    @Transaction
    suspend fun insertWithInitialStock(product: ProductEntity, createdAt: String): Long {
        val id = insert(product)
        if (product.stock != 0) {
            insertStockHistory(
                StockHistoryEntity(
                    productId = id,
                    productName = product.name,
                    changeQty = product.stock,
                    stockBefore = 0,
                    stockAfter = product.stock,
                    reason = "Stok awal produk baru",
                    createdAt = createdAt
                )
            )
        }
        return id
    }

    @Transaction
    suspend fun updateWithStockDelta(product: ProductEntity, stockDelta: Int, updatedAt: String, now: Long) {
        val before = getProductById(product.id)?.stock ?: return
        updateDetails(
            id = product.id, code = product.code, name = product.name,
            purchasePrice = product.purchasePrice, purchasePackagePrice = product.purchasePackagePrice,
            purchasePackageQty = product.purchasePackageQty, sellingPrice = product.sellingPrice,
            packagePrice = product.packagePrice, packageQty = product.packageQty,
            discount = product.discount, stockDelta = stockDelta, updatedAt = updatedAt, now = now
        )
        if (stockDelta != 0) {
            insertStockHistory(
                StockHistoryEntity(
                    productId = product.id,
                    productName = product.name,
                    changeQty = stockDelta,
                    stockBefore = before,
                    stockAfter = before + stockDelta,
                    reason = "Update stok manual",
                    createdAt = updatedAt
                )
            )
        }
    }

    /** Return false bila produk tidak ditemukan. */
    @Transaction
    suspend fun adjustStockWithHistory(productId: Long, changeQty: Int, reason: String, createdAt: String): Boolean {
        val product = getProductById(productId) ?: return false
        if (changeQty > 0) increaseStock(productId, changeQty) else decreaseStock(productId, -changeQty)
        insertStockHistory(
            StockHistoryEntity(
                productId = productId,
                productName = product.name,
                changeQty = changeQty,
                stockBefore = product.stock,
                stockAfter = product.stock + changeQty,
                reason = reason,
                createdAt = createdAt
            )
        )
        return true
    }
}
