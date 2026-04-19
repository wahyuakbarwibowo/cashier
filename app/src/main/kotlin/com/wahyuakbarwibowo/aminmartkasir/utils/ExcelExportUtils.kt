package com.wahyuakbarwibowo.aminmartkasir.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ExpenseEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelExportUtils {

    /**
     * Menggunakan format CSV agar 100% kompatibel dengan Android tanpa library tambahan
     * File CSV dapat dibuka langsung di Microsoft Excel
     */
    fun exportFullReport(
        context: Context,
        sales: List<SaleEntity>,
        digitalHistory: List<PhoneHistoryEntity>,
        expenses: List<ExpenseEntity>
    ): File? {
        try {
            val sb = StringBuilder()
            val delimiter = "," // Gunakan koma sebagai pemisah standar CSV

            // --- 1. SEKSI PENJUALAN RETAIL ---
            sb.append("LAPORAN PENJUALAN RETAIL\n")
            sb.append("ID,Tanggal,Total,Dibayar,Kembalian,Poin\n")
            sales.forEach { sale ->
                sb.append("${sale.id},")
                sb.append("${sale.createdAt},")
                sb.append("${sale.total},")
                sb.append("${sale.paid},")
                sb.append("${sale.change},")
                sb.append("${sale.pointsEarned}\n")
            }
            
            sb.append("\n\n") // Jarak antar tabel

            // --- 2. SEKSI TRANSAKSI DIGITAL ---
            sb.append("LAPORAN TRANSAKSI DIGITAL\n")
            sb.append("ID,Tanggal,Kategori,Provider,Nomor,Harga Jual,Profit,Catatan\n")
            digitalHistory.forEach { trx ->
                sb.append("${trx.id},")
                sb.append("${trx.createdAt},")
                sb.append("${trx.category},")
                sb.append("${trx.provider},")
                sb.append("'${trx.phoneNumber},") // Tanda petik agar nomor tidak terpotong nol-nya di Excel
                sb.append("${trx.sellingPrice},")
                sb.append("${trx.profit},")
                val sanitizedNotes = trx.notes?.replace("\n", " ")?.replace(",", ";") ?: "-"
                sb.append("$sanitizedNotes\n")
            }

            sb.append("\n\n")

            // --- 3. SEKSI PENGELUARAN ---
            sb.append("LAPORAN PENGELUARAN\n")
            sb.append("ID,Tanggal,Kategori,Nominal,Catatan\n")
            expenses.forEach { exp ->
                sb.append("${exp.id},")
                sb.append("${exp.createdAt},")
                sb.append("${exp.category},")
                sb.append("${exp.amount},")
                val sanitizedNotes = exp.notes?.replace("\n", " ")?.replace(",", ";") ?: "-"
                sb.append("$sanitizedNotes\n")
            }

            val fileName = "Laporan_Aminmart_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            outputStream.write(sb.toString().toByteArray())
            outputStream.close()
            
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            // Mimetype untuk CSV
            type = "text/comma-separated-values"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Laporan (CSV)"))
    }
}
