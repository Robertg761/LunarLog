package com.lunarlog.logic

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(";") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return if (value.isNullOrEmpty()) {
            emptyList()
        } else {
            value.split(";")
        }
    }

    @TypeConverter
    fun fromSymptomCategory(value: com.lunarlog.data.SymptomCategory): String {
        return value.name
    }

    @TypeConverter
    fun toSymptomCategory(value: String): com.lunarlog.data.SymptomCategory {
        return try {
            com.lunarlog.data.SymptomCategory.valueOf(value)
        } catch (e: IllegalArgumentException) {
            com.lunarlog.data.SymptomCategory.OTHER
        }
    }

    @TypeConverter
    fun fromLogEntryType(value: com.lunarlog.data.LogEntryType): String {
        return value.name
    }

    @TypeConverter
    fun toLogEntryType(value: String): com.lunarlog.data.LogEntryType {
        return try {
            com.lunarlog.data.LogEntryType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            // Default or fallback? Maybe NOTE is safest fallback?
            com.lunarlog.data.LogEntryType.NOTE
        }
    }
}
