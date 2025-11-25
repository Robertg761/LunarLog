package com.lunarlog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LogEntryType {
    SYMPTOM, MOOD, FLOW, WATER, SLEEP, SLEEP_QUALITY, NOTE, SEX, TEMPERATURE, MUCUS
}

@Entity(
    tableName = "log_entries",
    indices = [Index(value = ["date"], name = "index_log_entries_date")]
)
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // Epoch Day
    val time: Long, // Epoch Millis
    val type: LogEntryType,
    val value: String, // Stored as String, converted based on type
    val details: String? = null // Optional notes for this specific entry
)
