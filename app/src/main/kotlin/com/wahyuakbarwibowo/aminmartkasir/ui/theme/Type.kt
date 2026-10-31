package com.wahyuakbarwibowo.aminmartkasir.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Banyak pengguna berusia lanjut: semua teks diperbesar 15% dari ukuran Material bawaan
private const val TEXT_SCALE = 1.15f

private fun TextStyle.scaled() = copy(fontSize = fontSize * TEXT_SCALE, lineHeight = lineHeight * TEXT_SCALE)

private val base = Typography(
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp)
)

val Typography = Typography(
    displayLarge = base.displayLarge.scaled(),
    displayMedium = base.displayMedium.scaled(),
    displaySmall = base.displaySmall.scaled(),
    headlineLarge = base.headlineLarge.scaled(),
    headlineMedium = base.headlineMedium.scaled(),
    headlineSmall = base.headlineSmall.scaled(),
    titleLarge = base.titleLarge.scaled(),
    titleMedium = base.titleMedium.scaled(),
    titleSmall = base.titleSmall.scaled(),
    bodyLarge = base.bodyLarge.scaled(),
    bodyMedium = base.bodyMedium.scaled(),
    bodySmall = base.bodySmall.scaled(),
    labelLarge = base.labelLarge.scaled(),
    labelMedium = base.labelMedium.scaled(),
    labelSmall = base.labelSmall.scaled()
)
