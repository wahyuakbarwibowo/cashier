package com.wahyuakbarwibowo.aminmartkasir.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ExpenseEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelExportUtils {

    private fun sanitizeCsvField(field: Any?): String {
        if (field == null) return ""
        val str = field.toString()
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\""
        }
        return str
    }

    /**
     * Menggunakan format CSV agar 100% kompatibel dengan Android tanpa library tambahan
     * File CSV dapat dibuka langsung di Microsoft Excel
     */
    suspend fun exportFullReport(
        context: Context,
        sales: List<SaleEntity>,
        digitalHistory: List<PhoneHistoryEntity>,
        expenses: List<ExpenseEntity>
    ): File? = withContext(Dispatchers.IO) {
        try {
            // Bersihkan file laporan lama di cache untuk menghindari kebocoran penyimpanan (storage leak)
            try {
                context.cacheDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("Laporan_Aminmart_") && file.name.endsWith(".csv")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val sb = StringBuilder()

            // --- 1. SEKSI PENJUALAN RETAIL ---
            sb.append("LAPORAN PENJUALAN RETAIL\n")
            sb.append("ID,Tanggal,Total,Dibayar,Kembalian,Poin\n")
            sales.forEach { sale ->
                sb.append("${sanitizeCsvField(sale.id)},")
                sb.append("${sanitizeCsvField(sale.createdAt)},")
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
                sb.append("${sanitizeCsvField(trx.id)},")
                sb.append("${sanitizeCsvField(trx.createdAt)},")
                sb.append("${sanitizeCsvField(trx.category)},")
                sb.append("${sanitizeCsvField(trx.provider)},")
                sb.append("${sanitizeCsvField("'" + trx.phoneNumber)},") // Tanda petik agar nomor tidak terpotong nol-nya di Excel
                sb.append("${trx.sellingPrice},")
                sb.append("${trx.profit},")
                sb.append("${sanitizeCsvField(trx.notes ?: "-")}\n")
            }

            sb.append("\n\n")

            // --- 3. SEKSI PENGELUARAN ---
            sb.append("LAPORAN PENGELUARAN\n")
            sb.append("ID,Tanggal,Kategori,Nominal,Catatan\n")
            expenses.forEach { exp ->
                sb.append("${sanitizeCsvField(exp.id)},")
                sb.append("${sanitizeCsvField(exp.createdAt)},")
                sb.append("${sanitizeCsvField(exp.category)},")
                sb.append("${exp.amount},")
                sb.append("${sanitizeCsvField(exp.notes ?: "-")}\n")
            }

            val fileName = "Laporan_Aminmart_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            outputStream.write(sb.toString().toByteArray())
            outputStream.close()
            
            return@withContext file
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
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
