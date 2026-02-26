package com.wahyuakbarwibowo.aminmartkasir.data.repository

import com.wahyuakbarwibowo.aminmartkasir.data.local.dao.PaymentMethodDao
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

class PaymentMethodRepository(private val paymentMethodDao: PaymentMethodDao) {
    val allPaymentMethods: Flow<List<PaymentMethodEntity>> = paymentMethodDao.getAllPaymentMethods()

    suspend fun getPaymentMethodById(id: Long): PaymentMethodEntity? {
        return paymentMethodDao.getPaymentMethodById(id)
    }

    suspend fun insert(paymentMethod: PaymentMethodEntity): Long {
        return paymentMethodDao.insert(paymentMethod)
    }

    suspend fun update(paymentMethod: PaymentMethodEntity) {
        paymentMethodDao.update(paymentMethod)
    }

    suspend fun delete(paymentMethod: PaymentMethodEntity) {
        paymentMethodDao.delete(paymentMethod)
    }

    suspend fun deleteById(id: Long) {
        paymentMethodDao.deleteById(id)
    }
}
