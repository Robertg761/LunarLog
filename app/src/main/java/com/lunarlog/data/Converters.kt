package com.lunarlog.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class Converters {
    private val gson = Gson()

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
        return if (value == null) "" else gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return if (value.isNullOrEmpty()) {
            emptyList()
        } else {
            // Try to parse as JSON first
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(value, type)
            } catch (e: Exception) {
                // Fallback to old delimiter for backward compatibility during migration
                value.split(";").filter { it.isNotEmpty() }
            }
        }
    }

    @TypeConverter
    fun fromSymptomCategory(value: SymptomCategory): String {
        return value.name
    }

    @TypeConverter
    fun toSymptomCategory(value: String): SymptomCategory {
        return try {
            SymptomCategory.valueOf(value)
        } catch (e: IllegalArgumentException) {
            SymptomCategory.OTHER
        }
    }

    @TypeConverter
    fun fromLogEntryType(value: LogEntryType): String {
        return value.name
    }

    @TypeConverter
    fun toLogEntryType(value: String): LogEntryType {
        return try {
            LogEntryType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            LogEntryType.NOTE
        }
    }
}
