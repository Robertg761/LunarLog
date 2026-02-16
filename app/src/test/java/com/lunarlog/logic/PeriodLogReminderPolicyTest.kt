package com.lunarlog.logic

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodLogReminderPolicyTest {

    @Test
    fun `disabled - no notify`() {
        val today = LocalDate.of(2026, 2, 15)
        assertFalse(
            PeriodLogReminderPolicy.shouldNotify(
                today = today,
                cycleStart = today,
                cycleEnd = null,
                reminderEnabled = false,
                hasEntriesToday = false,
                dailyLogEmpty = true,
                maxDays = 5
            )
        )
    }

    @Test
    fun `enabled active period day 1-5 and nothing logged - notify`() {
        val start = LocalDate.of(2026, 2, 10)
        val today = LocalDate.of(2026, 2, 14) // day 5 (inclusive)
        assertTrue(
            PeriodLogReminderPolicy.shouldNotify(
                today = today,
                cycleStart = start,
                cycleEnd = null,
                reminderEnabled = true,
                hasEntriesToday = false,
                dailyLogEmpty = true,
                maxDays = 5
            )
        )
    }

    @Test
    fun `enabled active period but entries exist - no notify`() {
        val start = LocalDate.of(2026, 2, 10)
        val today = LocalDate.of(2026, 2, 12)
        assertFalse(
            PeriodLogReminderPolicy.shouldNotify(
                today = today,
                cycleStart = start,
                cycleEnd = null,
                reminderEnabled = true,
                hasEntriesToday = true,
                dailyLogEmpty = true,
                maxDays = 5
            )
        )
    }

    @Test
    fun `enabled active period but aggregate daily log has data - no notify`() {
        val start = LocalDate.of(2026, 2, 10)
        val today = LocalDate.of(2026, 2, 12)
        assertFalse(
            PeriodLogReminderPolicy.shouldNotify(
                today = today,
                cycleStart = start,
                cycleEnd = null,
                reminderEnabled = true,
                hasEntriesToday = false,
                dailyLogEmpty = false,
                maxDays = 5
            )
        )
    }

    @Test
    fun `enabled active period day 6 - no notify`() {
        val start = LocalDate.of(2026, 2, 10)
        val today = LocalDate.of(2026, 2, 15) // day 6
        assertFalse(
            PeriodLogReminderPolicy.shouldNotify(
                today = today,
                cycleStart = start,
                cycleEnd = null,
                reminderEnabled = true,
                hasEntriesToday = false,
                dailyLogEmpty = true,
                maxDays = 5
            )
        )
    }

    @Test
    fun `no active period - no notify`() {
        val today = LocalDate.of(2026, 2, 15)
        assertFalse(
            PeriodLogReminderPolicy.shouldNotify(
                today = today,
                cycleStart = today.minusDays(1),
                cycleEnd = today, // ended
                reminderEnabled = true,
                hasEntriesToday = false,
                dailyLogEmpty = true,
                maxDays = 5
            )
        )
    }
}

