package com.lunarlog.logic

import com.lunarlog.data.Medication
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object MedicationScheduler {

    fun isMedicationDueToday(medication: Medication, date: LocalDate): Boolean {
        val startDate = LocalDate.ofEpochDay(medication.startDate)
        val endDate = medication.endDate?.let { LocalDate.ofEpochDay(it) }

        if (date.isBefore(startDate)) return false
        if (endDate != null && date.isAfter(endDate)) return false

        return when (medication.frequency) {
            "daily" -> true
            "weekly" -> date.dayOfWeek == startDate.dayOfWeek
            "as_needed" -> false // Only manual logging
            else -> true
        }
    }

    fun getNextReminderTime(medication: Medication): Long? {
        // Return timestamp for next alarm
        if (medication.reminderTime == null) return null
        
        // Logic to calculate next alarm based on frequency
        // ... (Simplified for now)
        return null
    }
}
