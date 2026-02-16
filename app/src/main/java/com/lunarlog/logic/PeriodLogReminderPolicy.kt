package com.lunarlog.logic

import java.time.LocalDate

object PeriodLogReminderPolicy {
    /**
     * @param today Current local date.
     * @param cycleStart Start date of the active cycle.
     * @param cycleEnd End date of the cycle, or null if still active.
     * @param reminderEnabled Whether the user enabled daily period reminders.
     * @param hasEntriesToday True if there is at least one granular log entry for today.
     * @param dailyLogEmpty True if the aggregate DailyLog is missing or has no user-entered data.
     * @param maxDays Maximum reminder window length (inclusive) starting from [cycleStart].
     */
    fun shouldNotify(
        today: LocalDate,
        cycleStart: LocalDate?,
        cycleEnd: LocalDate?,
        reminderEnabled: Boolean,
        hasEntriesToday: Boolean,
        dailyLogEmpty: Boolean,
        maxDays: Long = 5L
    ): Boolean {
        if (!reminderEnabled) return false
        if (cycleStart == null) return false
        if (cycleEnd != null) return false
        if (maxDays <= 0L) return false

        val lastDayInclusive = cycleStart.plusDays(maxDays - 1L)
        val withinWindow = !today.isBefore(cycleStart) && !today.isAfter(lastDayInclusive)
        if (!withinWindow) return false

        // Any log data for the day means "already logged", so don't nag.
        if (hasEntriesToday) return false
        if (!dailyLogEmpty) return false

        return true
    }
}

