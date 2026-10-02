package com.lunarlog.ui.analysis

import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class ReportGeneratorTest {
    @Test
    fun `CSV preserves dose events estimated dates and temperature units`() {
        val output = ByteArrayOutputStream()
        val date = LocalDate.of(2026, 1, 1)
        ReportGenerator.generateCsv(output,
            listOf(Cycle(startDate = date, endDate = date.plusDays(4), endEstimated = true)),
            listOf(DailyLog(date = date, temperature = 36.5f)), emptyList(),
            listOf(com.lunarlog.data.Medication(id = 1, name = "Example", startDate = date.toEpochDay(), dosesPerDay = 2)),
            listOf(com.lunarlog.data.MedicationLog(date = date.toEpochDay(), medicationId = 1, timestamp = 1000),
                com.lunarlog.data.MedicationLog(date = date.toEpochDay(), medicationId = 1, timestamp = 2000)))
        val csv = output.toString(Charsets.UTF_8.name())
        assertTrue(csv.contains("End date estimated"))
        assertTrue(csv.contains("°C"))
        org.junit.Assert.assertEquals(2, csv.lineSequence().count { it.startsWith("\"MEDICATION_DOSE\"") })
    }

    @Test
    fun `CSV contains periods summaries raw entries and escaped values`() {
        val output = ByteArrayOutputStream()
        val date = LocalDate.of(2026, 1, 1)

        ReportGenerator.generateCsv(
            output,
            periods = listOf(Cycle(startDate = date, endDate = date.plusDays(4))),
            dailyLogs = listOf(DailyLog(date = date, symptoms = listOf("Pain, severe"))),
            logEntries = listOf(
                LogEntry(
                    date = date.toEpochDay(),
                    time = 1_767_225_600_000,
                    type = LogEntryType.NOTE,
                    value = "Said \"hello\"",
                    details = "=HYPERLINK(\"https://example.invalid\")"
                )
            )
        )

        val csv = output.toString(Charsets.UTF_8.name())
        assertTrue(csv.contains("\"PERIOD\""))
        assertTrue(csv.contains("\"DAILY_SUMMARY\""))
        assertTrue(csv.contains("\"LOG_ENTRY\""))
        assertTrue(csv.contains("\"Pain, severe\""))
        assertTrue(csv.contains("\"Said \"\"hello\"\"\""))
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"https://example.invalid\"\")\""))
    }
}
