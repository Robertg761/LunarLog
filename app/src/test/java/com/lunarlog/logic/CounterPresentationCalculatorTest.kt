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

        // Day 3 of a 5-day average: today plus two more expected days.
        assertEquals(CounterMode.PERIOD_DAYS_LEFT, result.mode)
        assertEquals(3, result.value)
        assertEquals("3 days left in period", result.subtitle)
    }

    @Test
    fun `counts today as remaining on first day of period`() {
        val cycles = listOf(
            Cycle(startDate = today, endDate = null)
        )

        val result = CounterPresentationCalculator.calculate(cycles, today)

        assertEquals(CounterMode.PERIOD_DAYS_LEFT, result.mode)
        assertEquals(5, result.value)
        assertEquals("5 days left in period", result.subtitle)
    }

    @Test
    fun `shows full rounded average on first day regardless of predicted start`() {
        // Three closed periods of 6, 6 and 5 days (average 5.67, rounds to 6), then a
        // period logged on its actual start date. Day 1 must show the whole 6-day
        // estimate — this reproduced as "4 days left" when the average truncated to 5
        // and day 1 already counted as spent.
        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 1, 6)),
            Cycle(startDate = LocalDate.of(2026, 1, 29), endDate = LocalDate.of(2026, 2, 3)),
            Cycle(startDate = LocalDate.of(2026, 2, 26), endDate = LocalDate.of(2026, 3, 2)),
            Cycle(startDate = LocalDate.of(2026, 3, 28), endDate = null)
        )

        val result = CounterPresentationCalculator.calculate(cycles, LocalDate.of(2026, 3, 28))

        assertEquals(CounterMode.PERIOD_DAYS_LEFT, result.mode)
        assertEquals(6, result.value)
        assertEquals("6 days left in period", result.subtitle)
    }

    @Test
    fun `returns period ending today when ongoing cycle reaches estimate`() {
        val cycles = listOf(
            Cycle(startDate = today.minusDays(4), endDate = null)
        )

        val result = CounterPresentationCalculator.calculate(cycles, today)

        assertEquals(CounterMode.PERIOD_DAYS_LEFT, result.mode)
        assertEquals(1, result.value)
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
        assertEquals("6 days since last period", result.subtitle)
    }

    @Test
    fun `returns next period countdown after long period ends today`() {
        val cycles = listOf(
            Cycle(startDate = today.minusDays(28), endDate = today)
        )

        val result = CounterPresentationCalculator.calculate(cycles, today)

        assertEquals(CounterMode.NEXT_PERIOD_COUNTDOWN, result.mode)
        assertEquals(24, result.value)
        assertEquals("0 days since last period", result.subtitle)
    }

    @Test
    fun `returns due today when closed cycle hits estimate`() {
        val cycles = listOf(
            Cycle(startDate = today.minusDays(28), endDate = today.minusDays(24))
        )

        val result = CounterPresentationCalculator.calculate(cycles, today)

        assertEquals(CounterMode.NEXT_PERIOD_COUNTDOWN, result.mode)
        assertEquals(0, result.value)
        assertEquals("24 days since last period", result.subtitle)
    }

    @Test
    fun `uses singular day wording for one day since last period`() {
        val cycles = listOf(
            Cycle(startDate = today.minusDays(5), endDate = today.minusDays(1))
        )

        val result = CounterPresentationCalculator.calculate(cycles, today)

        assertEquals(CounterMode.NEXT_PERIOD_COUNTDOWN, result.mode)
        assertEquals("1 day since last period", result.subtitle)
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
