package com.lunarlog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LogEntryType {
    SYMPTOM, MOOD, FLOW, WATER, SLEEP, SLEEP_QUALITY, NOTE, SEX, TEMPERATURE, MUCUS
}

/**
 * Human-readable label for a [LogEntryType].
 *
 * The enum name leaks into the UI otherwise, which surfaces as `SLEEP_QUALITY` on log cards and
 * `Sleep_quality` in the add-entry sheet.
 */
val LogEntryType.displayName: String
    get() = when (this) {
        LogEntryType.SYMPTOM -> "Symptom"
        LogEntryType.MOOD -> "Mood"
        LogEntryType.FLOW -> "Flow"
        LogEntryType.WATER -> "Water"
        LogEntryType.SLEEP -> "Sleep"
        LogEntryType.SLEEP_QUALITY -> "Sleep quality"
        LogEntryType.NOTE -> "Note"
        LogEntryType.SEX -> "Sex drive"
        LogEntryType.TEMPERATURE -> "Basal temperature"
        LogEntryType.MUCUS -> "Cervical mucus"
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
