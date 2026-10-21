package com.wahyuakbarwibowo.aminmartkasir.data.repository

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.google.gson.JsonParser
import java.io.OutputStream
import java.text.DateFormat
import java.util.Date

/** Read-only, paginated copy of every group in a local backup. */
object PdfBackupExporter {
    fun write(output: OutputStream, backupJson: String) {
        val data = JsonParser.parseString(backupJson).asJsonObject
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
        }
        val left = 40f
        val right = 555f
        val bottom = 802f
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun nextPage() {
            page?.let(document::finishPage)
            pageNumber++
            val newPage = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            page = newPage
            y = 48f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 16f
            newPage.canvas.drawText("AminMart - Ekspor Data Toko", left, y, paint)
            y += 22f
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f
            newPage.canvas.drawText(DateFormat.getDateTimeInstance().format(Date()) + "  |  Halaman $pageNumber", left, y, paint)
            y += 24f
        }

        fun line(value: String, heading: Boolean = false) {
            paint.typeface = if (heading) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            paint.textSize = if (heading) 13f else 10f
            val lineHeight = if (heading) 19f else 15f
            value.split('\n').forEach { part ->
                var remaining = part
                do {
                    if (page == null || y + lineHeight > bottom) nextPage()
                    paint.typeface = if (heading) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    paint.textSize = if (heading) 13f else 10f
                    val length = if (remaining.isEmpty()) 0 else
                        paint.breakText(remaining, true, right - left, null).coerceAtLeast(1)
                    page!!.canvas.drawText(remaining.take(length), left, y, paint)
                    y += lineHeight
                    remaining = remaining.drop(length)
                } while (remaining.isNotEmpty())
            }
        }

        try {
            nextPage()
            ExcelBackupCodec.sheetNames().forEach { name ->
                val value = data.get(name)
                val rows = when {
                    value == null || value.isJsonNull -> emptyList()
                    value.isJsonArray -> value.asJsonArray.map { it.asJsonObject }
                    else -> listOf(value.asJsonObject)
                }
                y += 8f
                line("$name (${rows.size})", heading = true)
                if (rows.isEmpty()) line("Tidak ada data.")
                rows.forEachIndexed { index, row ->
                    y += 5f
                    line("Data ${index + 1}", heading = true)
                    row.entrySet().forEach { (field, cell) ->
                        line("$field: ${if (cell.isJsonNull) "-" else cell.asString}")
                    }
                }
            }
            page?.let(document::finishPage)
            document.writeTo(output)
        } finally {
            document.close()
        }
    }
}
