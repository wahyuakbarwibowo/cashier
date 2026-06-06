package com.wahyuakbarwibowo.aminmartkasir.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Menampilkan angka dengan pemisah ribuan (titik) di OutlinedTextField,
 * tanpa mengubah nilai mentah (tetap digit murni "100000").
 * Contoh: input "100000" tampil "100.000".
 *
 * Pakai bersama input yang hanya menerima digit (filter isDigit di onValueChange).
 */
class RupiahVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        if (digits.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formatted = StringBuilder()
        val len = digits.length
        for (i in 0 until len) {
            formatted.append(digits[i])
            val posFromEnd = len - i - 1
            if (posFromEnd > 0 && posFromEnd % 3 == 0) {
                formatted.append('.')
            }
        }
        val out = formatted.toString()

        // Jumlah pemisah yang disisipkan sebelum offset original tertentu.
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val digitsBefore = offset.coerceAtMost(len)
                // separator dihitung dari posisi end: setiap kelipatan 3 digit dari kanan.
                val separatorsBefore = (len - digitsBefore).let { remaining ->
                    val totalSeparators = (len - 1) / 3
                    val separatorsAfter = if (remaining > 0) (remaining - 1) / 3 + 1 else 0
                    (totalSeparators - separatorsAfter).coerceAtLeast(0)
                }
                return (offset + separatorsBefore).coerceAtMost(out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val sub = out.take(offset.coerceAtMost(out.length))
                return sub.count { it.isDigit() }
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
