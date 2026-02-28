package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.DigitalCategoryDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.DigitalProductDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.PhoneHistoryDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalCategoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.DigitalProductEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
import kotlinx.coroutines.flow.Flow

class DigitalCategoryRepository(private val digitalCategoryDao: DigitalCategoryDao) {
    val allDigitalCategories: Flow<List<DigitalCategoryEntity>> = digitalCategoryDao.getAllDigitalCategories()

    suspend fun insert(digitalCategory: DigitalCategoryEntity): Long {
        return digitalCategoryDao.insert(digitalCategory)
    }

    suspend fun update(digitalCategory: DigitalCategoryEntity) {
        digitalCategoryDao.update(digitalCategory)
    }

    suspend fun delete(digitalCategory: DigitalCategoryEntity) {
        digitalCategoryDao.delete(digitalCategory)
    }

    suspend fun deleteById(id: Long) {
        digitalCategoryDao.deleteById(id)
    }
}

class DigitalProductRepository(private val digitalProductDao: DigitalProductDao) {
    val allDigitalProducts: Flow<List<DigitalProductEntity>> = digitalProductDao.getAllDigitalProducts()

    fun getDigitalProductsByCategory(category: String): Flow<List<DigitalProductEntity>> {
        return digitalProductDao.getDigitalProductsByCategory(category)
    }

    fun searchDigitalProducts(query: String): Flow<List<DigitalProductEntity>> {
        return digitalProductDao.searchDigitalProducts(query)
    }

    suspend fun insert(digitalProduct: DigitalProductEntity): Long {
        return digitalProductDao.insert(digitalProduct)
    }

    suspend fun update(digitalProduct: DigitalProductEntity) {
        digitalProductDao.update(digitalProduct)
    }

    suspend fun delete(digitalProduct: DigitalProductEntity) {
        digitalProductDao.delete(digitalProduct)
    }

    suspend fun deleteById(id: Long) {
        digitalProductDao.deleteById(id)
    }
}

class PhoneHistoryRepository(private val phoneHistoryDao: PhoneHistoryDao) {
    val allPhoneHistory: Flow<List<PhoneHistoryEntity>> = phoneHistoryDao.getAllPhoneHistory()

    fun getPhoneHistoryByCategory(category: String): Flow<List<PhoneHistoryEntity>> {
        return phoneHistoryDao.getPhoneHistoryByCategory(category)
    }

    suspend fun insert(phoneHistory: PhoneHistoryEntity): Long {
        return phoneHistoryDao.insert(phoneHistory)
    }

    suspend fun update(phoneHistory: PhoneHistoryEntity) {
        phoneHistoryDao.update(phoneHistory)
    }

    suspend fun delete(phoneHistory: PhoneHistoryEntity) {
        phoneHistoryDao.delete(phoneHistory)
    }

    suspend fun deleteById(id: Long) {
        phoneHistoryDao.deleteById(id)
    }
}
