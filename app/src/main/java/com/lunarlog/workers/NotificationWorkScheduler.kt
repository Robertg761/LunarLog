package com.lunarlog.workers

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object NotificationWorkScheduler {
    const val UNIQUE_CYCLE_WORK_NAME = "CycleNotificationWork"
    const val UNIQUE_PERIOD_LOG_REMINDER_WORK_NAME = "PeriodLogReminderWork"
    private const val UNIQUE_PERIOD_LOG_REMINDER_RESCHEDULE_WORK_NAME = "PeriodLogReminderRescheduleWork"
    private const val DEFAULT_CYCLE_NOTIFICATION_TIME_MINUTES = 9L * 60L

    /**
     * Schedules the [CycleNotificationWorker] to run once every 24h.
     */
    fun scheduleCycleNotifications(
        context: Context
    ) {
        val initialDelayMillis = computeInitialDelayMillis(DEFAULT_CYCLE_NOTIFICATION_TIME_MINUTES)
        val workRequest = PeriodicWorkRequestBuilder<CycleNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_CYCLE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    /**
     * Like [scheduleCycleNotifications], but forces the schedule to re-anchor (useful for time/tz changes).
     */
    fun rescheduleCycleNotifications(context: Context) {
        val initialDelayMillis = computeInitialDelayMillis(DEFAULT_CYCLE_NOTIFICATION_TIME_MINUTES)
        val workRequest = PeriodicWorkRequestBuilder<CycleNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_CYCLE_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }

    /**
     * Schedules the [PeriodLogReminderWorker] to run once every 24h, with an initial delay so the
     * first run happens at the next occurrence of [timeMinutesFromMidnight] in the user's local tz.
     *
     * Note: a periodic 24h cadence will drift across DST/time changes unless we reschedule on
     * `TIME_SET`/`TIMEZONE_CHANGED`; [ReminderRescheduleReceiver] triggers that reschedule.
     */
    fun schedulePeriodLogReminders(
        context: Context,
        timeMinutesFromMidnight: Long
    ) {
        val initialDelayMillis = computeInitialDelayMillis(timeMinutesFromMidnight)
        val workRequest = PeriodicWorkRequestBuilder<PeriodLogReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIOD_LOG_REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }

    fun cancelPeriodLogReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIOD_LOG_REMINDER_WORK_NAME)
    }

    fun enqueuePeriodLogReminderReschedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<PeriodLogReminderRescheduleWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_PERIOD_LOG_REMINDER_RESCHEDULE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun computeInitialDelayMillis(timeMinutesFromMidnight: Long): Long {
        val minutes = timeMinutesFromMidnight.coerceIn(0L, (24L * 60L) - 1L)
        val targetTime = LocalTime.of((minutes / 60L).toInt(), (minutes % 60L).toInt())

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val todayAtTarget = now.toLocalDate().atTime(targetTime).atZone(zone)
        val nextRun = if (now.isBefore(todayAtTarget)) todayAtTarget else todayAtTarget.plusDays(1)
        return Duration.between(now, nextRun).toMillis().coerceAtLeast(0L)
    }
}
