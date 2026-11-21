package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ProductDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import com.wahyuakbarwibowo.aminmartkasir.utils.DateUtils
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val onChanged: () -> Unit = {}
) {
    private fun scheduleSync() { runCatching(onChanged) }

    enum class ProductSort {
        NEWEST,
        NAME_ASC,
        NAME_DESC,
        STOCK_ASC,
        STOCK_DESC
    }

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val productCount: Flow<Int> = productDao.getProductCount()

    suspend fun getProducts(limit: Int, offset: Int): List<ProductEntity> {
        return productDao.getProducts(limit, offset)
    }

    suspend fun getProductsSorted(limit: Int, offset: Int, sort: ProductSort): List<ProductEntity> {
        return when (sort) {
            ProductSort.NEWEST -> productDao.getProducts(limit, offset)
            ProductSort.NAME_ASC -> productDao.getProductsOrderByNameAsc(limit, offset)
            ProductSort.NAME_DESC -> productDao.getProductsOrderByNameDesc(limit, offset)
            ProductSort.STOCK_ASC -> productDao.getProductsOrderByStockAsc(limit, offset)
            ProductSort.STOCK_DESC -> productDao.getProductsOrderByStockDesc(limit, offset)
        }
    }

    suspend fun getProductById(id: Long): ProductEntity? {
        return productDao.getProductById(id)
    }

    suspend fun getProductByCode(code: String): ProductEntity? {
        return productDao.getProductByCode(code)
    }

    fun getLowStockProducts(): Flow<List<ProductEntity>> = productDao.getLowStockProducts()

    val lowStockCount: Flow<Int> = productDao.getLowStockCount()

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProducts(query)
    }

    /** Simpan produk baru beserta riwayat stok awalnya dalam satu transaksi. */
    suspend fun insert(product: ProductEntity): Long {
        val id = productDao.insertWithInitialStock(
            product.copy(isSynced = false, isDeleted = false, syncUpdatedAt = System.currentTimeMillis()),
            DateUtils.nowDateTime()
        )
        scheduleSync()
        return id
    }

    /**
     * Simpan perubahan dari form. [stockDelta] = stok di form - stok saat form dibuka,
     * diterapkan relatif ke stok terkini agar penjualan di sela-selanya tidak hilang.
     */
    suspend fun update(product: ProductEntity, stockDelta: Int) {
        productDao.updateWithStockDelta(product, stockDelta, DateUtils.nowDateTime(), System.currentTimeMillis())
        scheduleSync()
    }

    /** Penyesuaian stok + riwayatnya dalam satu transaksi. False bila produk tidak ada. */
    suspend fun adjustStock(productId: Long, changeQty: Int, reason: String): Boolean {
        val adjusted = productDao.adjustStockWithHistory(productId, changeQty, reason, DateUtils.nowDateTime())
        if (adjusted) scheduleSync()
        return adjusted
    }

    suspend fun delete(product: ProductEntity) {
        productDao.delete(product.id)
        scheduleSync()
    }

    suspend fun deleteById(id: Long) {
        productDao.deleteById(id)
        scheduleSync()
    }

    suspend fun decreaseStock(productId: Long, qty: Int) {
        productDao.decreaseStock(productId, qty)
        scheduleSync()
    }

    suspend fun increaseStock(productId: Long, qty: Int) {
        productDao.increaseStock(productId, qty)
        scheduleSync()
    }
}
