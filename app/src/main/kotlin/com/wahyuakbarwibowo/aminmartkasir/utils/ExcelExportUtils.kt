package com.wahyuakbarwibowo.aminmartkasir.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.ExpenseEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.PhoneHistoryEntity
import com.wahyuakbarwibowo.aminmartkasir.data.local.entity.SaleEntity
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelExportUtils {

    fun exportFullReport(
        context: Context,
        sales: List<SaleEntity>,
        digitalHistory: List<PhoneHistoryEntity>,
        expenses: List<ExpenseEntity>
    ): File? {
        try {
            val workbook = XSSFWorkbook()
            
            // 1. Sales Sheet
            val salesSheet = workbook.createSheet("Penjualan Retail")
            val salesHeader = salesSheet.createRow(0)
            listOf("ID", "Tanggal", "Total", "Dibayar", "Kembalian", "Poin").forEachIndexed { i, s -> 
                salesHeader.createCell(i).setCellValue(s) 
            }
            sales.forEachIndexed { index, sale ->
                val row = salesSheet.createRow(index + 1)
                row.createCell(0).setCellValue(sale.id.toDouble())
                row.createCell(1).setCellValue(sale.createdAt ?: "-")
                row.createCell(2).setCellValue(sale.total)
                row.createCell(3).setCellValue(sale.paid)
                row.createCell(4).setCellValue(sale.change)
                row.createCell(5).setCellValue(sale.pointsEarned.toDouble())
            }

            // 2. Digital Sheet
            val digitalSheet = workbook.createSheet("Transaksi Digital")
            val digitalHeader = digitalSheet.createRow(0)
            listOf("ID", "Tanggal", "Kategori", "Provider", "Nomor", "Harga Jual", "Profit", "Catatan").forEachIndexed { i, s -> 
                digitalHeader.createCell(i).setCellValue(s) 
            }
            digitalHistory.forEachIndexed { index, trx ->
                val row = digitalSheet.createRow(index + 1)
                row.createCell(0).setCellValue(trx.id.toDouble())
                row.createCell(1).setCellValue(trx.createdAt ?: "-")
                row.createCell(2).setCellValue(trx.category)
                row.createCell(3).setCellValue(trx.provider ?: "-")
                row.createCell(4).setCellValue(trx.phoneNumber ?: "-")
                row.createCell(5).setCellValue(trx.sellingPrice)
                row.createCell(6).setCellValue(trx.profit)
                row.createCell(7).setCellValue(trx.notes ?: "-")
            }

            // 3. Expenses Sheet
            val expenseSheet = workbook.createSheet("Pengeluaran")
            val expHeader = expenseSheet.createRow(0)
            listOf("ID", "Tanggal", "Kategori", "Nominal", "Catatan").forEachIndexed { i, s -> 
                expHeader.createCell(i).setCellValue(s) 
            }
            expenses.forEachIndexed { index, exp ->
                val row = expenseSheet.createRow(index + 1)
                row.createCell(0).setCellValue(exp.id.toDouble())
                row.createCell(1).setCellValue(exp.createdAt ?: "-")
                row.createCell(2).setCellValue(exp.category)
                row.createCell(3).setCellValue(exp.amount)
                row.createCell(4).setCellValue(exp.notes ?: "-")
            }

            val fileName = "Laporan_Aminmart_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.xlsx"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            outputStream.close()
            workbook.close()
            
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
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Laporan"))
    }
}
