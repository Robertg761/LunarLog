package com.lunarlog.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object NotificationWorkScheduler {
    const val UNIQUE_WORK_NAME = "CycleNotificationWork"

    /**
     * Schedules the [CycleNotificationWorker] to run once every 24h, with an initial delay so the
     * first run happens at the next occurrence of [timeMinutesFromMidnight] in the user's local tz.
     */
    fun scheduleCycleNotifications(
        context: Context,
        timeMinutesFromMidnight: Long
    ) {
        val minutes = timeMinutesFromMidnight.coerceIn(0L, (24L * 60L) - 1L)
        val targetTime = LocalTime.of((minutes / 60L).toInt(), (minutes % 60L).toInt())

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val todayAtTarget = now.toLocalDate().atTime(targetTime).atZone(zone)
        val nextRun = if (now.isBefore(todayAtTarget)) todayAtTarget else todayAtTarget.plusDays(1)
        val initialDelayMillis = Duration.between(now, nextRun).toMillis().coerceAtLeast(0L)

        val workRequest = PeriodicWorkRequestBuilder<CycleNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}

