package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class WidgetCalendarBuilderTest {

    private val regularCycles = listOf(
        Cycle(startDate = LocalDate.of(2026, 1, 4), endDate = LocalDate.of(2026, 1, 8)),
        Cycle(startDate = LocalDate.of(2026, 2, 1), endDate = LocalDate.of(2026, 2, 5))
    )

    @Test
    fun `lays the grid out Sunday-first in whole weeks`() {
        // 1 April 2026 is a Wednesday and April has 30 days, so the grid needs five rows and pads at
        // both ends.
        val month = WidgetCalendarBuilder.build(emptyList(), YearMonth.of(2026, 4), LocalDate.of(2026, 4, 15))

        assertEquals(5, month.weeks.size)
        assertTrue(month.weeks.all { it.size == 7 })
        assertTrue(month.weeks.all { week -> week.first().date.dayOfWeek == DayOfWeek.SUNDAY })
        assertEquals(LocalDate.of(2026, 3, 29), month.weeks.first().first().date)
        assertEquals(LocalDate.of(2026, 5, 2), month.weeks.last().last().date)
    }

    @Test
    fun `flags padding days as outside the month`() {
        val month = WidgetCalendarBuilder.build(emptyList(), YearMonth.of(2026, 4), LocalDate.of(2026, 4, 15))

        assertFalse(month.dayOn(LocalDate.of(2026, 3, 31)).isCurrentMonth)
        assertTrue(month.dayOn(LocalDate.of(2026, 4, 1)).isCurrentMonth)
        assertTrue(month.dayOn(LocalDate.of(2026, 4, 30)).isCurrentMonth)
        assertFalse(month.dayOn(LocalDate.of(2026, 5, 1)).isCurrentMonth)
    }

    @Test
    fun `marks today once and only today`() {
        val today = LocalDate.of(2026, 4, 15)
        val month = WidgetCalendarBuilder.build(emptyList(), YearMonth.of(2026, 4), today)

        assertEquals(listOf(today), month.weeks.flatten().filter { it.isToday }.map { it.date })
    }

    @Test
    fun `leaves every day unmarked when nothing is recorded`() {
        val month = WidgetCalendarBuilder.build(emptyList(), YearMonth.of(2026, 4), LocalDate.of(2026, 4, 15))

        assertTrue(month.weeks.flatten().all { it.mark == WidgetDayMark.NONE })
    }

    @Test
    fun `marks recorded period days`() {
        val month = WidgetCalendarBuilder.build(regularCycles, YearMonth.of(2026, 2), LocalDate.of(2026, 2, 12))

        assertEquals(
            (1..5).map { LocalDate.of(2026, 2, it) },
            month.datesMarked(WidgetDayMark.PERIOD)
        )
    }

    @Test
    fun `keeps a fertile window whose next period starts after the grid ends`() {
        // Regression: the projection used to stop at the end of the grid, but a predicted start up to
        // 19 days past it still owns a fertile window inside it. Here the next period is 1 March and
        // its window — 10 to 16 February — is the whole reason February looks the way it does.
        val month = WidgetCalendarBuilder.build(regularCycles, YearMonth.of(2026, 2), LocalDate.of(2026, 2, 12))

        assertEquals(
            listOf(10, 11, 12, 13, 14, 16).map { LocalDate.of(2026, 2, it) },
            month.datesMarked(WidgetDayMark.FERTILE)
        )
        assertEquals(
            listOf(LocalDate.of(2026, 2, 15)),
            month.datesMarked(WidgetDayMark.OVULATION)
        )
    }

    @Test
    fun `marks every predicted period that falls inside the grid`() {
        val month = WidgetCalendarBuilder.build(regularCycles, YearMonth.of(2026, 3), LocalDate.of(2026, 3, 20))

        // The recorded history stops in February, so 1 March is the predicted start — and the cycle
        // after it lands on 29 March, inside the same grid.
        assertEquals(
            (1..5).map { LocalDate.of(2026, 3, it) } +
                listOf(29, 30, 31).map { LocalDate.of(2026, 3, it) } +
                listOf(1, 2).map { LocalDate.of(2026, 4, it) },
            month.datesMarked(WidgetDayMark.PREDICTED_PERIOD)
        )
    }

    @Test
    fun `lets recorded bleeding outrank predictions on the same date`() {
        // An ongoing cycle running well past its predicted end, so the recorded range covers both the
        // predicted period starting 1 March and the fertile window's first day.
        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2026, 1, 4), endDate = LocalDate.of(2026, 1, 8)),
            Cycle(startDate = LocalDate.of(2026, 2, 1), endDate = null)
        )

        val month = WidgetCalendarBuilder.build(cycles, YearMonth.of(2026, 3), LocalDate.of(2026, 3, 10))

        // Recorded through today, which is also where the prediction says the fertile window opens.
        assertEquals(
            (1..10).map { LocalDate.of(2026, 3, it) },
            month.datesMarked(WidgetDayMark.PERIOD)
        )
        assertEquals(
            listOf(11, 12, 13, 14, 16).map { LocalDate.of(2026, 3, it) },
            month.datesMarked(WidgetDayMark.FERTILE)
        )
        assertEquals(WidgetDayMark.OVULATION, month.markOn(LocalDate.of(2026, 3, 15)))
    }

    @Test
    fun `projects forward without losing marks when the month is a year ahead`() {
        // The builder skips whole cycles to reach a distant month rather than stepping through every
        // one; the skip has to land far enough back that the first projected cycle still reaches in.
        val month = WidgetCalendarBuilder.build(regularCycles, YearMonth.of(2027, 2), LocalDate.of(2026, 2, 12))

        assertEquals(
            listOf(
                LocalDate.of(2027, 1, 31), // padding day, still marked
                LocalDate.of(2027, 2, 1),
                LocalDate.of(2027, 2, 2),
                LocalDate.of(2027, 2, 3),
                LocalDate.of(2027, 2, 4),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 1),
                LocalDate.of(2027, 3, 2),
                LocalDate.of(2027, 3, 3),
                LocalDate.of(2027, 3, 4)
            ),
            month.datesMarked(WidgetDayMark.PREDICTED_PERIOD)
        )
        assertEquals(
            listOf(9, 10, 11, 12, 13, 15).map { LocalDate.of(2027, 2, it) },
            month.datesMarked(WidgetDayMark.FERTILE)
        )
        assertEquals(
            listOf(LocalDate.of(2027, 2, 14)),
            month.datesMarked(WidgetDayMark.OVULATION)
        )
    }

    @Test
    fun `ranks recorded bleeding above every estimate`() {
        val ranked = WidgetDayMark.entries.sortedBy { it.rank }

        assertEquals(
            listOf(
                WidgetDayMark.NONE,
                WidgetDayMark.PREDICTED_PERIOD,
                WidgetDayMark.FERTILE,
                WidgetDayMark.OVULATION,
                WidgetDayMark.PERIOD
            ),
            ranked
        )
    }

    private fun WidgetCalendarMonth.dayOn(date: LocalDate): WidgetCalendarDay =
        weeks.flatten().first { it.date == date }

    private fun WidgetCalendarMonth.markOn(date: LocalDate): WidgetDayMark = dayOn(date).mark

    private fun WidgetCalendarMonth.datesMarked(mark: WidgetDayMark): List<LocalDate> =
        weeks.flatten().filter { it.mark == mark }.map { it.date }
}
