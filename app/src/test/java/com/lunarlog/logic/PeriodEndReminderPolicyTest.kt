package com.lunarlog.logic

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodEndReminderPolicyTest {

    private val start: LocalDate = LocalDate.of(2026, 3, 1)

    private fun notify(
        dayOfPeriod: Long,
        cycleEnd: LocalDate? = null,
        reminderEnabled: Boolean = true,
        averagePeriodLength: Int = 5,
        hasPositiveFlowToday: Boolean = false
    ): Boolean = PeriodEndReminderPolicy.shouldNotify(
        today = start.plusDays(dayOfPeriod - 1),
        cycleStart = start,
        cycleEnd = cycleEnd,
        reminderEnabled = reminderEnabled,
        averagePeriodLength = averagePeriodLength,
        hasPositiveFlowToday = hasPositiveFlowToday
    )

    @Test
    fun `stays quiet through the expected period and grace day`() {
        // Average 5 + grace 2: day 6 is only one day over, first nudge is day 7.
        for (day in 1L..6L) {
            assertEquals("day $day", false, notify(dayOfPeriod = day))
        }
    }

    @Test
    fun `notifies once the period runs past the grace window`() {
        assertEquals(true, notify(dayOfPeriod = 7))
        assertEquals(true, notify(dayOfPeriod = 13))
    }

    @Test
    fun `goes quiet after the reminder window`() {
        assertEquals(false, notify(dayOfPeriod = 14))
        assertEquals(false, notify(dayOfPeriod = 40))
    }

    @Test
    fun `does not notify when period already ended`() {
        assertEquals(false, notify(dayOfPeriod = 7, cycleEnd = start.plusDays(4)))
    }

    @Test
    fun `does not notify when reminders are disabled`() {
        assertEquals(false, notify(dayOfPeriod = 7, reminderEnabled = false))
    }

    @Test
    fun `does not notify while flow is still being logged today`() {
        assertEquals(false, notify(dayOfPeriod = 7, hasPositiveFlowToday = true))
    }

    @Test
    fun `does not notify without a cycle start`() {
        assertEquals(
            false,
            PeriodEndReminderPolicy.shouldNotify(
                today = start,
                cycleStart = null,
                cycleEnd = null,
                reminderEnabled = true,
                averagePeriodLength = 5,
                hasPositiveFlowToday = false
            )
        )
    }

    @Test
    fun `window follows the user's average period length`() {
        // Average 7 + grace 2: first nudge on day 9.
        assertEquals(false, notify(dayOfPeriod = 8, averagePeriodLength = 7))
        assertEquals(true, notify(dayOfPeriod = 9, averagePeriodLength = 7))
    }
}
