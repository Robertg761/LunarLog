package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SymptomCorrelationEngineTest {
    @Test
    fun `correlation uses observed cycle days rather than period duration`() {
        val starts = listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 29),
            LocalDate.of(2026, 2, 26),
            LocalDate.of(2026, 3, 26)
        )
        val cycles = starts.mapIndexed { index, start ->
            Cycle(id = index + 1, startDate = start, endDate = start.plusDays(4))
        }
        val logs = starts.take(3).mapIndexed { index, start ->
            DailyLog(
                date = start.plusDays(9),
                symptoms = if (index < 2) listOf("Headache") else emptyList()
            )
        }

        val result = SymptomCorrelationEngine.analyzeCorrelations(cycles, logs)

        assertEquals(1, result.size)
        assertEquals(10, result.single().cycleDay)
        assertEquals(2f / 3f, result.single().frequency, 0.001f)
    }

    @Test
    fun `correlation requires three actually observed cycle days`() {
        val starts = listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 29),
            LocalDate.of(2026, 2, 26),
            LocalDate.of(2026, 3, 26)
        )
        val cycles = starts.map { Cycle(startDate = it, endDate = it.plusDays(4)) }
        val logs = starts.take(2).map { DailyLog(date = it.plusDays(9), symptoms = listOf("Headache")) }

        assertTrue(SymptomCorrelationEngine.analyzeCorrelations(cycles, logs).isEmpty())
    }
}
