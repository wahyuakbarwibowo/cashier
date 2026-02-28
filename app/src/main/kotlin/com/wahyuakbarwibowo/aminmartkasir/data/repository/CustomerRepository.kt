package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.CustomerDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val customerDao: CustomerDao) {
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val customerCount: Flow<Int> = customerDao.getCustomerCount()

    suspend fun getCustomers(limit: Int, offset: Int): List<CustomerEntity> {
        return customerDao.getCustomers(limit, offset)
    }

    suspend fun getCustomerById(id: Long): CustomerEntity? {
        return customerDao.getCustomerById(id)
    }

    fun searchCustomers(query: String): Flow<List<CustomerEntity>> {
        return customerDao.searchCustomers(query)
    }

    suspend fun insert(customer: CustomerEntity): Long {
        return customerDao.insert(customer)
    }

    suspend fun update(customer: CustomerEntity) {
        customerDao.update(customer)
    }

    suspend fun delete(customer: CustomerEntity) {
        customerDao.delete(customer)
    }

    suspend fun deleteById(id: Long) {
        customerDao.deleteById(id)
    }

    suspend fun addPoints(id: Long, points: Int) {
        customerDao.addPoints(id, points)
    }

    suspend fun deductPoints(id: Long, points: Int) {
        customerDao.deductPoints(id, points)
    }
}
