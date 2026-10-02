package com.lunarlog.logic

import com.lunarlog.core.model.*
import com.lunarlog.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class AuditRegressionTest {
    private val start = LocalDate.of(2026, 1, 1)
    @Test fun postBleedingSymptomsBelongToTheCycleUntilNextStart() {
        val cycles = listOf(Cycle(startDate = start, endDate = start.plusDays(4)), Cycle(startDate = start.plusDays(28), endDate = start.plusDays(32)))
        val logs = listOf(DailyLog(date = start.plusDays(20), symptoms = listOf("Headache")))
        assertEquals(listOf("Headache"), SymptomStatsCalculator.getTopSymptomsForPhase(21, cycles, logs))
    }
    @Test fun notesOnlyDaysDoNotConfirmMucusPeakButExplicitDryDoes() {
        val logs = listOf(DailyLog(date = start, cervicalMucus = 4)) + (1L..3L).map { DailyLog(date = start.plusDays(it), notes = "Note") }
        assertNull(AdvancedCycleIntelligence.detectPeakMucusDay(start, logs))
        assertEquals(start, AdvancedCycleIntelligence.detectPeakMucusDay(start, logs, logs.mapTo(mutableSetOf()) { it.date }))
    }
    @Test fun secondReminderSurvivesFirstDoseAndSkipsAfterSecondDose() {
        val medication = Medication(id = 1, name = "Example", startDate = start.toEpochDay(), dosesPerDay = 2, reminderTimes = listOf(480, 1200))
        assertFalse(MedicationScheduler.needsReminder(medication, 1, 480))
        assertTrue(MedicationScheduler.needsReminder(medication, 1, 1200))
        assertFalse(MedicationScheduler.needsReminder(medication, 2, 1200))
        assertEquals(start.atTime(20, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), MedicationScheduler.getNextReminderTime(medication, start.atTime(10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC))
        assertNull(MedicationScheduler.getNextReminderTime(medication.copy(isArchived = true)))
        val state = MedicationWidgetStateCalculator.calculate(listOf(medication), listOf(MedicationLog(date = start.toEpochDay(), medicationId = 1, timestamp = 1)), start)
        assertEquals(1, state.items.single().doseCount)
        assertFalse(state.allTaken)
    }
    @Test fun rejectsInvalidValuesAndExcessSleepWithoutRejectingLegacyCorrections() {
        fun entry(type: LogEntryType, value: String) = LogEntry(date = start.toEpochDay(), time = 1, type = type, value = value)
        for ((type, value) in listOf(LogEntryType.FLOW to "999", LogEntryType.SLEEP to "NaN", LogEntryType.TEMPERATURE to "Infinity", LogEntryType.MUCUS to "-1")) {
            assertThrows(IllegalArgumentException::class.java) { LogValidation.entry(entry(type, value)) }
        }
        assertThrows(IllegalArgumentException::class.java) { LogValidation.date(LocalDate.MIN.toEpochDay()) }
        val sleeps = List(3) { entry(LogEntryType.SLEEP, "12") }
        assertThrows(IllegalArgumentException::class.java) { LogValidation.sleepTotal(emptyList(), sleeps) }
        LogValidation.sleepTotal(sleeps, sleeps.take(2))
        LogValidation.entry(entry(LogEntryType.MUCUS, "0"))
    }
}
