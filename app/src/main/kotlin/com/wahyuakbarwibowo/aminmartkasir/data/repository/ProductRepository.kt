package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ProductDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val productCount: Flow<Int> = productDao.getProductCount()

    suspend fun getProducts(limit: Int, offset: Int): List<ProductEntity> {
        return productDao.getProducts(limit, offset)
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
