package com.lunarlog.logic

import com.lunarlog.data.Medication
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationSchedulerTest {
    private val monday = LocalDate.of(2026, 7, 13)

    @Test
    fun `weekly medication is due only on start weekday and within date range`() {
        val medication = medication(frequency = "weekly", endDate = monday.plusDays(14).toEpochDay())

        assertTrue(MedicationScheduler.isMedicationDueToday(medication, monday.plusDays(7)))
        assertFalse(MedicationScheduler.isMedicationDueToday(medication, monday.plusDays(8)))
        assertFalse(MedicationScheduler.isMedicationDueToday(medication, monday.plusDays(21)))
    }

    @Test
    fun `as needed medication never creates an automatic reminder`() {
        val medication = medication(frequency = "as_needed", reminderTime = 9L * 60L)

        assertFalse(MedicationScheduler.isMedicationDueToday(medication, monday))
        assertNull(
            MedicationScheduler.getNextReminderTime(
                medication,
                Instant.parse("2026-07-13T10:00:00Z"),
                ZoneId.of("UTC")
            )
        )
    }

    @Test
    fun `next reminder advances to the next valid schedule occurrence`() {
        val medication = medication(frequency = "weekly", reminderTime = 9L * 60L)

        assertEquals(
            Instant.parse("2026-07-20T09:00:00Z").toEpochMilli(),
            MedicationScheduler.getNextReminderTime(
                medication,
                Instant.parse("2026-07-13T09:01:00Z"),
                ZoneId.of("UTC")
            )
        )
    }

    private fun medication(
        frequency: String,
        reminderTime: Long? = null,
        endDate: Long? = null
    ) = Medication(
        id = 1,
        name = "Test",
        frequency = frequency,
        startDate = monday.toEpochDay(),
        endDate = endDate,
        reminderTime = reminderTime
    )
}
