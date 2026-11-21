package com.wahyuakbarwibowo.aminmartkasir.utils

import java.text.NumberFormat
import java.util.*

object CurrencyUtils {
    // Dipanggil per baris list saat recomposition; NumberFormat mahal dibuat dan tidak
    // thread-safe, jadi satu instance per thread.
    private val rupiah = ThreadLocal.withInitial {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }

    fun formatCurrency(amount: Double): String = formatCurrency(amount.toLong())

    fun formatCurrency(amount: Long): String = rupiah.get()!!.format(amount)
}
