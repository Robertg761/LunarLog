package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CounterPresentationCalculatorTest {

    private val today: LocalDate = LocalDate.of(2026, 2, 17)

    @Test
    fun `returns period days left for ongoing cycle within estimate`() {
        val cycles = listOf(
            Cycle(startDate = today.minusDays(2), endDate = null)
        )

        val result = CounterPresentationCalculator.calculate(cycles, today)

        assertEquals(CounterMode.PERIOD_DAYS_LEFT, result.mode)
        assertEquals(2, result.value)
        assertEquals("2 days left in period", result.subtitle)
    }

    @Test
    fun `returns period ending today when ongoing cycle reaches estimate`() {
        val cycles = listOf(
            Cycle(startDate = today.minusDays(4), endDate = null)
        )

        val result = CounterPresentationCalculator.calculate(cycles, today)

        assertEquals(CounterMode.PERIOD_DAYS_LEFT, result.mode)
        assertEquals(0, result.value)
        assertEquals("Ending today", result.subtitle)
    }

    @Test
    fun `returns period overage when ongoing cycle exceeds estimate`() {
        val cycles = listOf(
            Cycle(startDate = today.minusDays(6), endDate = null)
        )

        val result = CounterPresentationCalculator.calculate(cycles, today)

        assertEquals(CounterMode.PERIOD_OVERAGE, result.mode)
        assertEquals(2, result.value)
        assertEquals("2 days over estimate", result.subtitle)
    }

    @Test
    fun `returns next period countdown when closed cycle is before estimate`() {
        val cycles = listOf(
            Cycle(startDate = today.minusDays(10), endDate = today.minusDays(6))
        )

        val result = CounterPresentationCalculator.calculate(cycles, today)

        assertEquals(CounterMode.NEXT_PERIOD_COUNTDOWN, result.mode)
        assertEquals(18, result.value)
        assertEquals("18 days until period", result.subtitle)
    }

    @Test
    fun `returns due today when closed cycle hits estimate`() {
        val cycles = listOf(
            Cycle(startDate = today.minusDays(28), endDate = today.minusDays(24))
        )

        val result = CounterPresentationCalculator.calculate(cycles, today)

        assertEquals(CounterMode.NEXT_PERIOD_COUNTDOWN, result.mode)
        assertEquals(0, result.value)
        assertEquals("Due today", result.subtitle)
    }

    @Test
    fun `returns next period overdue when closed cycle passes estimate`() {
        val cycles = listOf(
            Cycle(startDate = today.minusDays(31), endDate = today.minusDays(27))
        )

        val result = CounterPresentationCalculator.calculate(cycles, today)

        assertEquals(CounterMode.NEXT_PERIOD_OVERDUE, result.mode)
        assertEquals(3, result.value)
        assertEquals("3 days overdue", result.subtitle)
    }
}
