package com.lunarlog.data

import java.time.LocalDate

/** Shared by interactive writes and backup validation. Missing and explicit zero are distinct entries. */
object LogValidation {
    const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
    const val MAX_BACKUP_RECORDS = 100_000

    fun date(epochDay: Long): LocalDate {
        val date = LocalDate.ofEpochDay(epochDay)
        require(date >= LocalDate.of(1900, 1, 1) && date <= LocalDate.now().plusYears(10)) {
            "Date must be between 1900 and ten years from today"
        }
        return date
    }

    fun entry(entry: LogEntry, legacy: Boolean = false) {
        date(entry.date)
        require(entry.value.isNotBlank()) { "An entry needs a value" }
        fun integer(range: IntRange) {
            require(entry.value.toIntOrNull() in range) { "Invalid ${entry.type.displayName.lowercase()} value" }
        }
        when (entry.type) {
            LogEntryType.FLOW, LogEntryType.MUCUS -> integer(0..4)
            LogEntryType.SLEEP_QUALITY -> integer(0..5)
            LogEntryType.SEX -> integer(0..if (legacy) 5 else 3)
            LogEntryType.WATER -> integer(0..10_000)
            LogEntryType.SLEEP -> require(entry.value.toDoubleOrNull()?.let { it.isFinite() && it in 0.0..24.0 } == true) {
                "Sleep must be between 0 and 24 hours"
            }
            LogEntryType.TEMPERATURE -> require(entry.value.toDoubleOrNull()?.let {
                it.isFinite() && (it in 34.0..43.0 || it in 90.0..110.0)
            } == true) { "Enter a plausible Celsius or Fahrenheit temperature" }
            else -> Unit
        }
    }

    fun sleepTotal(before: List<LogEntry>, after: List<LogEntry>) {
        fun total(entries: List<LogEntry>) = entries.filter { it.type == LogEntryType.SLEEP }
            .sumOf { it.value.toDoubleOrNull() ?: 0.0 }
        val old = total(before)
        val updated = total(after)
        require(updated <= 24.0 || updated <= old) { "This day's sleep would exceed 24 hours. Edit an existing sleep entry instead." }
    }
}
