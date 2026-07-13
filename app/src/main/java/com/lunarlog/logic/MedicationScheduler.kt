package com.lunarlog.logic

import com.lunarlog.data.Medication
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

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

    fun getNextReminderTime(
        medication: Medication,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        val reminderMinutes = medication.reminderTime ?: return null
        if (reminderMinutes !in 0L..1439L || medication.frequency == "as_needed") return null

        val nowLocal = ZonedDateTime.ofInstant(now, zoneId)
        val medicationStart = LocalDate.ofEpochDay(medication.startDate)
        val medicationEnd = medication.endDate?.let(LocalDate::ofEpochDay)
        var candidateDate = maxOf(nowLocal.toLocalDate(), medicationStart)

        repeat(370) {
            if (medicationEnd != null && candidateDate.isAfter(medicationEnd)) return null
            if (isMedicationDueToday(medication, candidateDate)) {
                val candidate = ZonedDateTime.of(
                    candidateDate,
                    LocalTime.MIDNIGHT.plusMinutes(reminderMinutes),
                    zoneId
                ).toInstant()
                if (candidate.isAfter(now)) return candidate.toEpochMilli()
            }
            candidateDate = candidateDate.plusDays(1)
        }
        return null
    }
}
