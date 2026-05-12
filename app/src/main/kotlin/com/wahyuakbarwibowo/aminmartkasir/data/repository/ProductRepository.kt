package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ProductDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
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

    fun getLowStockProducts(threshold: Int = 10): Flow<List<ProductEntity>> {
        return productDao.getLowStockProducts(threshold)
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProducts(query)
    }

    suspend fun insert(product: ProductEntity): Long {
        return productDao.insert(product)
    }

    suspend fun update(product: ProductEntity) {
        productDao.update(product)
    }

    suspend fun delete(product: ProductEntity) {
        productDao.delete(product)
    }

    suspend fun deleteById(id: Long) {
        productDao.deleteById(id)
    }

    suspend fun decreaseStock(productId: Long, qty: Int) {
        productDao.decreaseStock(productId, qty)
    }

    suspend fun increaseStock(productId: Long, qty: Int) {
        productDao.increaseStock(productId, qty)
    }
}
