package com.wahyuakbarwibowo.aminmartkasir.ui.screens

import com.wahyuakbarwibowo.aminmartkasir.utils.BluetoothPrinterHelper

data class LastTransactionData(
    val transactionId: String,
    val items: List<BluetoothPrinterHelper.ReceiptItem>,
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val paid: Double,
    val change: Double,
    val pointsEarned: Int
)
