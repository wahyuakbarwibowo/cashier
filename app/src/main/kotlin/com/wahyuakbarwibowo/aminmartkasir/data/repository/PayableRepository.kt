package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.PayableDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PayableEntity
import kotlinx.coroutines.flow.Flow

class PayableRepository(private val payableDao: PayableDao) {
    val allPayables: Flow<List<PayableEntity>> = payableDao.getAllPayables()

    suspend fun getPayables(limit: Int, offset: Int): List<PayableEntity> {
        return payableDao.getPayables(limit, offset)
    }

    suspend fun getPayablesByStatusPaginated(status: String, limit: Int, offset: Int): List<PayableEntity> {
        return payableDao.getPayablesByStatus(status, limit, offset)
    }

    fun getPayablesBySupplierId(supplierId: Long): Flow<List<PayableEntity>> {
        return payableDao.getPayablesBySupplierId(supplierId)
    }

    fun getPayablesByStatus(status: String): Flow<List<PayableEntity>> {
        return payableDao.getPayablesByStatus(status)
    }

    suspend fun insert(payable: PayableEntity): Long {
        return payableDao.insert(payable)
    }

    suspend fun update(payable: PayableEntity) {
        payableDao.update(payable)
    }

    suspend fun delete(payable: PayableEntity) {
        payableDao.delete(payable)
    }

    suspend fun updateStatus(id: Long, status: String) {
        payableDao.updateStatus(id, status)
    }
}
