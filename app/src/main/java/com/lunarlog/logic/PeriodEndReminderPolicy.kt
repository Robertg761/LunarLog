package com.lunarlog.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Decides when to ask "is your period over?" for a period that was started but never ended.
 *
 * Open-ended periods are the main way real period lengths go unrecorded: the cycle stays open
 * until the next period starts, at which point the app can only estimate an end. A nudge a
 * couple of days past the expected end captures the true end date while the user still
 * remembers it.
 */
object PeriodEndReminderPolicy {

    /** Days past the expected last period day before the first nudge. */
    const val GRACE_DAYS = 2L

    /** How many consecutive days to keep nudging before going quiet. */
    const val REMINDER_WINDOW_DAYS = 7L

    /**
     * @param today Current local date.
     * @param cycleStart Start date of the latest cycle.
     * @param cycleEnd End date of the latest cycle, or null while it is still open.
     * @param reminderEnabled Whether the user enabled daily period reminders.
     * @param averagePeriodLength The user's average period length in days.
     * @param hasPositiveFlowToday True if a flow level above "none" was logged today — the
     *   period is clearly still going, so asking whether it ended would be noise.
     */
    fun shouldNotify(
        today: LocalDate,
        cycleStart: LocalDate?,
        cycleEnd: LocalDate?,
        reminderEnabled: Boolean,
        averagePeriodLength: Int,
        hasPositiveFlowToday: Boolean
    ): Boolean {
        if (!reminderEnabled) return false
        if (cycleStart == null) return false
        if (cycleEnd != null) return false
        if (hasPositiveFlowToday) return false

        val dayOfPeriod = ChronoUnit.DAYS.between(cycleStart, today) + 1L
        val firstReminderDay = averagePeriodLength.coerceAtLeast(1) + GRACE_DAYS
        val lastReminderDay = firstReminderDay + REMINDER_WINDOW_DAYS - 1L
        return dayOfPeriod in firstReminderDay..lastReminderDay
    }
}
