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
