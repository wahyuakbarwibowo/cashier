package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ProductVariantDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ProductVariantEntity
import kotlinx.coroutines.flow.Flow

class ProductVariantRepository(private val dao: ProductVariantDao) {
    fun getByProductId(productId: Long): Flow<List<ProductVariantEntity>> = dao.getByProductId(productId)
    suspend fun getByProductIdOnce(productId: Long): List<ProductVariantEntity> = dao.getByProductIdOnce(productId)
    suspend fun insert(variant: ProductVariantEntity): Long = dao.insert(variant)
    suspend fun update(variant: ProductVariantEntity) = dao.update(variant)
    suspend fun delete(variant: ProductVariantEntity) = dao.delete(variant)
    suspend fun deleteByProductId(productId: Long) = dao.deleteByProductId(productId)
}
