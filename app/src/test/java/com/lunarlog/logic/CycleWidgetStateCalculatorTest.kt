package com.lunarlog.logic

import com.lunarlog.core.config.AppConfig
import com.lunarlog.core.model.Cycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CycleWidgetStateCalculatorTest {

    /** Two 28-day cycles, so the average is real history rather than the default. */
    private val regularCycles = listOf(
        Cycle(startDate = LocalDate.of(2026, 1, 4), endDate = LocalDate.of(2026, 1, 8)),
        Cycle(startDate = LocalDate.of(2026, 2, 1), endDate = LocalDate.of(2026, 2, 5))
    )

    @Test
    fun `reports unknown phase when no cycles are recorded`() {
        val state = CycleWidgetStateCalculator.calculate(emptyList(), LocalDate.of(2026, 2, 17))

        assertEquals(CyclePhase.UNKNOWN, state.phase)
        assertEquals("No data", state.phaseLabel)
        assertEquals(0, state.cycleDay)
        assertEquals(AppConfig.DEFAULT_CYCLE_LENGTH, state.cycleLength)
        assertEquals(0f, state.progress, 0f)
        assertNull(state.daysUntilNextPeriod)
        assertEquals(IntRange.EMPTY, state.periodDays)
        assertEquals(IntRange.EMPTY, state.fertileDays)
        assertNull(state.ovulationDay)
        assertEquals("Log a period to start tracking", state.supporting)
    }

    @Test
    fun `counts period day from an ongoing cycle with no end date`() {
        val cycles = listOf(Cycle(startDate = LocalDate.of(2026, 2, 15), endDate = null))

        val state = CycleWidgetStateCalculator.calculate(cycles, LocalDate.of(2026, 2, 17))

        assertEquals(CyclePhase.PERIOD, state.phase)
        assertEquals("Period", state.phaseLabel)
        assertEquals("Period day 3", state.supporting)
        assertEquals(3, state.cycleDay)
        assertEquals(26, state.daysUntilNextPeriod)
        assertEquals(3f / 28f, state.progress, 0.0001f)
    }

    @Test
    fun `places today in the fertile window and hedges the wording`() {
        val state = CycleWidgetStateCalculator.calculate(regularCycles, LocalDate.of(2026, 2, 12))

        assertEquals(CyclePhase.FERTILE, state.phase)
        assertEquals("Fertile", state.phaseLabel)
        // Never phrased as a fact — these are averages projected forward, not observed ovulation.
        assertEquals("Estimated fertile window", state.supporting)
        assertEquals(12, state.cycleDay)
        assertEquals(17, state.daysUntilNextPeriod)
    }

    @Test
    fun `singles out predicted ovulation from the fertile window around it`() {
        val state = CycleWidgetStateCalculator.calculate(regularCycles, LocalDate.of(2026, 2, 15))

        assertEquals(CyclePhase.OVULATION, state.phase)
        assertEquals("Estimated ovulation today", state.supporting)
        assertEquals(15, state.cycleDay)
    }

    @Test
    fun `converts predicted dates into cycle-day arcs for the ring`() {
        val state = CycleWidgetStateCalculator.calculate(regularCycles, LocalDate.of(2026, 2, 8))

        assertEquals(28, state.cycleLength)
        assertEquals(1..5, state.periodDays)
        assertEquals(10..16, state.fertileDays)
        assertEquals(15, state.ovulationDay)
    }

    @Test
    fun `counts down to the next period before the fertile window opens`() {
        val state = CycleWidgetStateCalculator.calculate(regularCycles, LocalDate.of(2026, 2, 8))

        assertEquals(CyclePhase.FOLLICULAR, state.phase)
        assertEquals("21 days to next period", state.supporting)
    }

    @Test
    fun `switches to singular wording on the eve of the predicted date`() {
        val state = CycleWidgetStateCalculator.calculate(regularCycles, LocalDate.of(2026, 2, 28))

        assertEquals(CyclePhase.LUTEAL, state.phase)
        assertEquals(1, state.daysUntilNextPeriod)
        assertEquals("Period expected tomorrow", state.supporting)
    }

    @Test
    fun `announces the predicted date on the day itself`() {
        val state = CycleWidgetStateCalculator.calculate(regularCycles, LocalDate.of(2026, 3, 1))

        assertEquals(0, state.daysUntilNextPeriod)
        assertEquals("Period expected today", state.supporting)
    }

    @Test
    fun `reports days overdue and fills the ring once the predicted date passes`() {
        val state = CycleWidgetStateCalculator.calculate(regularCycles, LocalDate.of(2026, 3, 4))

        assertEquals(CyclePhase.LUTEAL, state.phase)
        assertEquals(-3, state.daysUntilNextPeriod)
        assertEquals("3 days overdue", state.supporting)
        // Day 32 of a 28-day cycle: the ring fills rather than sweeping past the top.
        assertEquals(32, state.cycleDay)
        assertEquals(1f, state.progress, 0f)
    }

    @Test
    fun `clamps cycle day to one when today predates the latest recorded start`() {
        // A back-dated entry, or a device clock that moved backwards.
        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2026, 2, 20), endDate = LocalDate.of(2026, 2, 24))
        )

        val state = CycleWidgetStateCalculator.calculate(cycles, LocalDate.of(2026, 2, 17))

        assertEquals(1, state.cycleDay)
        assertEquals(1f / 28f, state.progress, 0.0001f)
        assertEquals(CyclePhase.FOLLICULAR, state.phase)
    }

    @Test
    fun `trims a fertile arc that would start before day one of a short cycle`() {
        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 1, 5)),
            Cycle(startDate = LocalDate.of(2026, 1, 17), endDate = LocalDate.of(2026, 1, 21))
        )

        val state = CycleWidgetStateCalculator.calculate(cycles, LocalDate.of(2026, 1, 20))

        assertEquals(16, state.cycleLength)
        // The window opens 19 days before a next period only 16 days out, so it reaches back into
        // the previous cycle; the ring shows the part that belongs to this one.
        assertEquals(1..4, state.fertileDays)
        assertEquals(3, state.ovulationDay)
        assertEquals(1..5, state.periodDays)
    }

    @Test
    fun `drops arcs entirely when the prediction lands beyond the ring`() {
        // A 51-day recorded period is filtered out of the period average but still drives the
        // end-based prediction, pushing the next start far past one average cycle from the start.
        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 2, 20))
        )

        val state = CycleWidgetStateCalculator.calculate(cycles, LocalDate.of(2026, 2, 25))

        assertEquals(28, state.cycleLength)
        assertEquals(IntRange.EMPTY, state.fertileDays)
        assertNull(state.ovulationDay)
        // The date-based phase still reads correctly even with nothing to draw.
        assertEquals(CyclePhase.FERTILE, state.phase)
    }

    @Test
    fun `treats any containing cycle as the period, not only the latest`() {
        // An out-of-order record: the latest start is in the future, today sits inside an earlier one.
        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2026, 2, 1), endDate = LocalDate.of(2026, 2, 5)),
            Cycle(startDate = LocalDate.of(2026, 3, 1), endDate = LocalDate.of(2026, 3, 5))
        )

        val state = CycleWidgetStateCalculator.calculate(cycles, LocalDate.of(2026, 2, 3))

        assertEquals(CyclePhase.PERIOD, state.phase)
        assertEquals("Period day 3", state.supporting)
    }
}
