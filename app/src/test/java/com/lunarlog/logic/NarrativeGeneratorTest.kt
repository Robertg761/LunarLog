package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NarrativeGeneratorTest {

    @Test
    fun `generateCycleSummary returns valid summary for typical cycle`() {
        val startDate = LocalDate.of(2023, 1, 1)
        val cycle = Cycle(id = 1, startDate = startDate, endDate = startDate.plusDays(4))
        val interval = CompletedCycleInterval(cycle, startDate.plusDays(28))
        
        val logs = listOf(
            DailyLog(date = startDate, flowLevel = 3, symptoms = listOf("Cramps")),
            DailyLog(date = startDate.plusDays(1), flowLevel = 3, symptoms = listOf("Cramps")),
            DailyLog(date = startDate.plusDays(2), flowLevel = 2, mood = listOf("Happy")),
            DailyLog(date = startDate.plusDays(14), mood = listOf("Happy"))
        )

        val summary = NarrativeGenerator.generateCycleSummary(interval, logs)

        assertEquals(28, summary.length)
        assertTrue(summary.narrative.contains("Cycle beginning 2023-01-01 lasted 28 days"))
        assertTrue(summary.narrative.contains("This is within the typical range"))
    }

    @Test
    fun `generateCycleSummary identifies short cycle`() {
        val startDate = LocalDate.of(2023, 1, 1)
        val cycle = Cycle(id = 2, startDate = startDate, endDate = startDate.plusDays(4))
        val interval = CompletedCycleInterval(cycle, startDate.plusDays(18))
        
        val summary = NarrativeGenerator.generateCycleSummary(interval, emptyList())
        
        assertEquals(18, summary.length)
        assertTrue(summary.keyInsights.contains("Short cycle length (18 days)."))
    }

    @Test
    fun `generateWeeklyDigest ignores days without a sleep value`() {
        val today = LocalDate.now()
        val logs = listOf(
            DailyLog(date = today, sleepHours = 8f),
            DailyLog(date = today.minusDays(1), sleepHours = 0f)
        )

        val digest = NarrativeGenerator.generateWeeklyDigest(logs, today)

        assertTrue(digest.narrative.contains("Average sleep: 8.0 hours"))
    }

    @Test
    fun `generateWeeklyDigest handles empty logs`() {
        val today = LocalDate.now()
        val digest = NarrativeGenerator.generateWeeklyDigest(emptyList(), today)
        
        assertEquals("No logs recorded this week.", digest.narrative)
    }

    @Test
    fun `generateWeeklyDigest calculates dominant mood and symptom`() {
        val today = LocalDate.now()
        
        val logs = listOf(
            DailyLog(date = today, mood = listOf("Happy"), symptoms = listOf("Headache")),
            DailyLog(date = today.minusDays(1), mood = listOf("Happy"), symptoms = listOf("Headache")),
            DailyLog(date = today.minusDays(2), mood = listOf("Sad"), symptoms = listOf("Cramps"))
        )
        
        val digest = NarrativeGenerator.generateWeeklyDigest(logs, today)
        
        assertEquals("Happy", digest.dominantMood)
        assertEquals("Headache", digest.dominantSymptom)
        assertTrue(digest.narrative.contains("You mostly felt Happy"))
        assertTrue(digest.narrative.contains("Top symptom was Headache"))
    }
}
