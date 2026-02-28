package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.CustomerPointsHistoryDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.CustomerPointsHistoryEntity
import kotlinx.coroutines.flow.Flow

class CustomerPointsHistoryRepository(private val customerPointsHistoryDao: CustomerPointsHistoryDao) {
    val allCustomerPointsHistory: Flow<List<CustomerPointsHistoryEntity>> = customerPointsHistoryDao.getAllCustomerPointsHistory()

    fun getCustomerPointsHistoryByCustomerId(customerId: Long): Flow<List<CustomerPointsHistoryEntity>> {
        return customerPointsHistoryDao.getCustomerPointsHistoryByCustomerId(customerId)
    }

    suspend fun insert(customerPointsHistory: CustomerPointsHistoryEntity): Long {
        return customerPointsHistoryDao.insert(customerPointsHistory)
    }

    suspend fun update(customerPointsHistory: CustomerPointsHistoryEntity) {
        customerPointsHistoryDao.update(customerPointsHistory)
    }

    suspend fun delete(customerPointsHistory: CustomerPointsHistoryEntity) {
        customerPointsHistoryDao.delete(customerPointsHistory)
    }

    suspend fun deleteBySaleId(saleId: Long) {
        customerPointsHistoryDao.deleteBySaleId(saleId)
    }
}
