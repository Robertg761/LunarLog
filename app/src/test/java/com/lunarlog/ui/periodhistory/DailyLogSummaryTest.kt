package com.lunarlog.ui.periodhistory

import com.lunarlog.core.model.DailyLog
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLogSummaryTest {

    private val date = LocalDate.of(2026, 2, 15)

    @Test
    fun `flow mapping`() {
        val lines = buildDailyLogSummaryLines(DailyLog(date = date, flowLevel = 3))
        assertEquals("Flow: Medium", lines.primary)
        assertEquals("", lines.secondary)
    }

    @Test
    fun `mood truncation`() {
        val lines = buildDailyLogSummaryLines(
            DailyLog(date = date, mood = listOf("Happy", "Calm", "Sad"))
        )
        assertEquals("Mood: Happy, Calm +1", lines.primary)
    }

    @Test
    fun `symptoms truncation`() {
        val lines = buildDailyLogSummaryLines(
            DailyLog(date = date, symptoms = listOf("Cramps", "Headache", "Bloating", "Nausea"))
        )
        assertEquals("Symptoms: Cramps, Headache +2", lines.primary)
    }

    @Test
    fun `sleep formatting`() {
        val lines = buildDailyLogSummaryLines(
            DailyLog(date = date, sleepHours = 7.5f, sleepQuality = 4)
        )
        assertEquals("Sleep: 7.5h (4/5)", lines.secondary)
    }

    @Test
    fun `fertility formatting`() {
        val lines = buildDailyLogSummaryLines(
            DailyLog(date = date, temperature = 97.6f, cervicalMucus = 4)
        )
        assertTrue(lines.secondary.contains("Temp: 97.6"))
        assertTrue(lines.secondary.contains("Mucus: Egg White"))
    }

    @Test
    fun `notes flag`() {
        val lines = buildDailyLogSummaryLines(
            DailyLog(date = date, notes = "x")
        )
        assertEquals("Notes", lines.secondary)
    }

    @Test
    fun `empty log yields blank lines`() {
        val lines = buildDailyLogSummaryLines(DailyLog(date = date))
        assertEquals("", lines.primary)
        assertEquals("", lines.secondary)
    }
}

