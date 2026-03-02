package com.wahyuakbarwibowo.aminmartkasir.data.local.converter

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromDouble(value: Double?): Double {
        return value ?: 0.0
    }

    @TypeConverter
    fun toDouble(value: String?): Double {
        return value?.toDoubleOrNull() ?: 0.0
    }
}
