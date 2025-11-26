package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class NarrativeGeneratorTest {

    @Test
    fun `generateCycleSummary returns valid summary for typical cycle`() {
        val startDate = LocalDate.of(2023, 1, 1).toEpochDay()
        val endDate = LocalDate.of(2023, 1, 28).toEpochDay()
        val cycle = Cycle(id = 1, startDate = startDate, endDate = endDate)
        
        val logs = listOf(
            DailyLog(date = startDate, flowLevel = 3, symptoms = listOf("Cramps")),
            DailyLog(date = startDate + 1, flowLevel = 3, symptoms = listOf("Cramps")),
            DailyLog(date = startDate + 2, flowLevel = 2, mood = listOf("Happy")),
            DailyLog(date = startDate + 14, mood = listOf("Happy"))
        )

        val summary = NarrativeGenerator.generateCycleSummary(cycle, logs)

        assertNotNull(summary)
        assertEquals(28, summary!!.length)
        assert(summary.narrative.contains("Cycle 1 lasted 28 days"))
        assert(summary.narrative.contains("This is within the typical range"))
    }

    @Test
    fun `generateCycleSummary identifies short cycle`() {
        val startDate = LocalDate.of(2023, 1, 1).toEpochDay()
        val endDate = LocalDate.of(2023, 1, 18).toEpochDay() // 18 days
        val cycle = Cycle(id = 2, startDate = startDate, endDate = endDate)
        
        val summary = NarrativeGenerator.generateCycleSummary(cycle, emptyList())
        
        assertNotNull(summary)
        assertEquals(18, summary!!.length)
        assert(summary.keyInsights.contains("Short cycle length (18 days)."))
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
        val todayEpoch = today.toEpochDay()
        
        val logs = listOf(
            DailyLog(date = todayEpoch, mood = listOf("Happy"), symptoms = listOf("Headache")),
            DailyLog(date = todayEpoch - 1, mood = listOf("Happy"), symptoms = listOf("Headache")),
            DailyLog(date = todayEpoch - 2, mood = listOf("Sad"), symptoms = listOf("Cramps"))
        )
        
        val digest = NarrativeGenerator.generateWeeklyDigest(logs, today)
        
        assertEquals("Happy", digest.dominantMood)
        assertEquals("Headache", digest.dominantSymptom)
        assert(digest.narrative.contains("You mostly felt Happy"))
        assert(digest.narrative.contains("Top symptom was Headache"))
    }
}
