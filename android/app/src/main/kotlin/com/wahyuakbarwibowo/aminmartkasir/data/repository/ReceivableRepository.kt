package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.ReceivableDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ReceivableEntity
import kotlinx.coroutines.flow.Flow

class ReceivableRepository(private val receivableDao: ReceivableDao) {
    val allReceivables: Flow<List<ReceivableEntity>> = receivableDao.getAllReceivables()

    fun getReceivablesByCustomerId(customerId: Long): Flow<List<ReceivableEntity>> {
        return receivableDao.getReceivablesByCustomerId(customerId)
    }

    fun getReceivablesByStatus(status: String): Flow<List<ReceivableEntity>> {
        return receivableDao.getReceivablesByStatus(status)
    }

    suspend fun getReceivableBySaleId(saleId: Long): ReceivableEntity? {
        return receivableDao.getReceivableBySaleId(saleId)
    }

    suspend fun insert(receivable: ReceivableEntity): Long {
        return receivableDao.insert(receivable)
    }

    suspend fun update(receivable: ReceivableEntity) {
        receivableDao.update(receivable)
    }

    suspend fun delete(receivable: ReceivableEntity) {
        receivableDao.delete(receivable)
    }

    suspend fun updateStatus(id: Long, status: String) {
        receivableDao.updateStatus(id, status)
    }
}
