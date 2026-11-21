package com.wahyuakbarwibowo.aminmartkasir.ui.viewmodel

import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PaymentMethodEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftCashTest {

    private val tunai = PaymentMethodEntity(id = 1, name = "Tunai")
    private val qris = PaymentMethodEntity(id = 2, name = "QRIS")
    private val hutang = PaymentMethodEntity(id = 3, name = "Hutang")

    @Test
    fun `hanya uang tunai yang benar-benar diterima`() {
        val sales = listOf(
            SaleEntity(paymentMethodId = 1, total = 10000.0, paid = 20000.0, change = 10000.0), // kembalian tidak dihitung
            SaleEntity(paymentMethodId = 2, total = 50000.0, paid = 50000.0),                   // non-tunai
            SaleEntity(paymentMethodId = 3, total = 100000.0, paid = 20000.0, change = -80000.0) // hutang: DP saja
        )
        val digital = listOf(PhoneHistoryEntity(sellingPrice = 12000.0, paid = 15000.0))

        assertEquals(42000.0, cashReceived(sales, digital, listOf(tunai, qris, hutang)), 0.0)
    }
}
