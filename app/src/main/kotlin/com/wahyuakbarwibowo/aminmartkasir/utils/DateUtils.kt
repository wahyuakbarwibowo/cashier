package com.wahyuakbarwibowo.aminmartkasir.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateTimeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }
    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    private val monthFormat = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM", Locale.getDefault())
    }
    private val dayFormat = ThreadLocal.withInitial {
        SimpleDateFormat("dd/MM", Locale.getDefault())
    }

    /** Parse a stored "yyyy-MM-dd HH:mm:ss" DB timestamp. */
    fun parseDateTime(value: String): Date = dateTimeFormat.get()!!.parse(value)!!

    fun nowDateTime(): String = dateTimeFormat.get()!!.format(Date())
    fun nowDate(): String = dateFormat.get()!!.format(Date())
    fun nowMonth(): String = monthFormat.get()!!.format(Date())
    fun formatDay(date: Date): String = dayFormat.get()!!.format(date)
    fun formatDate(date: Date): String = dateFormat.get()!!.format(date)
    fun formatDateTime(date: Date): String = dateTimeFormat.get()!!.format(date)
}
