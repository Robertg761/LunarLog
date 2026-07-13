package com.lunarlog.logic

import com.lunarlog.core.model.DailyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AdvancedCycleIntelligenceTest {
    private val start = LocalDate.of(2026, 1, 1)

    @Test
    fun `BBT shift requires six lows and three consecutive higher readings`() {
        val temperatures = listOf(36.3f, 36.4f, 36.35f, 36.3f, 36.4f, 36.35f, 36.7f, 36.75f, 36.8f)
        val logs = temperatures.mapIndexed { index, value ->
            DailyLog(date = start.plusDays(index.toLong()), temperature = value)
        }

        assertEquals(start.plusDays(5), AdvancedCycleIntelligence.detectOvulationFromBBT(start, logs))
    }

    @Test
    fun `BBT shift rejects missing calendar days`() {
        val temperatures = listOf(36.3f, 36.4f, 36.35f, 36.3f, 36.4f, 36.35f, 36.7f, 36.75f, 36.8f)
        val logs = temperatures.mapIndexed { index, value ->
            val offset = if (index >= 6) index + 1L else index.toLong()
            DailyLog(date = start.plusDays(offset), temperature = value)
        }

        assertNull(AdvancedCycleIntelligence.detectOvulationFromBBT(start, logs))
    }

    @Test
    fun `BBT shift normalizes plausible Fahrenheit readings`() {
        val temperatures = listOf(97.3f, 97.4f, 97.3f, 97.4f, 97.3f, 97.4f, 98.1f, 98.2f, 98.1f)
        val logs = temperatures.mapIndexed { index, value ->
            DailyLog(date = start.plusDays(index.toLong()), temperature = value)
        }

        assertEquals(start.plusDays(5), AdvancedCycleIntelligence.detectOvulationFromBBT(start, logs))
    }

    @Test
    fun `mucus peak requires three consecutive lower observations`() {
        val logs = listOf(
            DailyLog(date = start, cervicalMucus = 4),
            DailyLog(date = start.plusDays(1), cervicalMucus = 2),
            DailyLog(date = start.plusDays(2), cervicalMucus = 1),
            DailyLog(date = start.plusDays(3), cervicalMucus = 0)
        )

        assertEquals(start, AdvancedCycleIntelligence.detectPeakMucusDay(start, logs))
    }

    @Test
    fun `mucus peak is not returned while fertile observations are ongoing`() {
        val logs = listOf(
            DailyLog(date = start, cervicalMucus = 4),
            DailyLog(date = start.plusDays(1), cervicalMucus = 4)
        )

        assertNull(AdvancedCycleIntelligence.detectPeakMucusDay(start, logs))
    }
}
