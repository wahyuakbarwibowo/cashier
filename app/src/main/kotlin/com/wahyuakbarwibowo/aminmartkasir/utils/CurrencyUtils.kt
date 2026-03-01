package com.wahyuakbarwibowo.aminmartkasir.utils

import java.text.NumberFormat
import java.util.*

object CurrencyUtils {
    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount)
    }
}
