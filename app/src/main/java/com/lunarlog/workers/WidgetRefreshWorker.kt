package com.lunarlog.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lunarlog.ui.widget.WidgetRefresher
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Redraws the widgets once a day, just after local midnight.
 *
 * Every LunarLog widget is dated: the ring's day number, the calendar's "today" underline, the
 * medication list, the counter. All four are wrong the moment the clock rolls over, and nothing in
 * the database changed to tell [com.lunarlog.ui.widget.WidgetDataObserver] about it.
 *
 * This is WorkManager rather than `updatePeriodMillis` in the provider XML, and rather than an alarm.
 * `updatePeriodMillis` is capped at 30 minutes by the platform, so it would wake the app 48 times a
 * day to redraw content that changes once — and on One UI it is the first thing "Sleeping apps"
 * suppresses, which is exactly the case this needs to survive. An exact alarm would need
 * `SCHEDULE_EXACT_ALARM` on API 31+ for a refresh that does not need to be to-the-second. Deferred
 * work that always eventually runs is the honest fit.
 *
 * The 24h cadence drifts across DST the same way [NotificationWorkScheduler]'s periodic work does,
 * and is re-anchored the same way: [com.lunarlog.ui.widget.WidgetDateChangeReceiver] calls
 * [reschedule] on `TIME_SET`/`TIMEZONE_CHANGED`.
 */
class WidgetRefreshWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = runCatching {
        WidgetRefresher.updateAll(applicationContext)
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        private const val UNIQUE_WORK_NAME = "WidgetMidnightRefreshWork"

        /**
         * A minute past midnight, not midnight exactly. `LocalDate.now()` inside the widgets has to
         * agree that the day has turned; landing a hair early would repaint yesterday and then wait
         * 24 hours to correct itself.
         */
        private const val MIDNIGHT_GUARD_MINUTES = 1L

        /** Idempotent — safe to call on every `Application.onCreate`. */
        fun schedule(context: Context) {
            enqueue(context, ExistingPeriodicWorkPolicy.UPDATE)
        }

        /** Forces the schedule to re-anchor on the new local midnight after a clock or timezone change. */
        fun reschedule(context: Context) {
            enqueue(context, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE)
        }

        private fun enqueue(context: Context, policy: ExistingPeriodicWorkPolicy) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayUntilAfterMidnightMillis(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                policy,
                request
            )
        }

        private fun delayUntilAfterMidnightMillis(): Long {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            // atStartOfDay(zone) rather than atTime(MIDNIGHT) so a DST transition that skips or
            // repeats the hour still resolves to a real instant.
            val target = now.toLocalDate()
                .plusDays(1)
                .atStartOfDay(zone)
                .plusMinutes(MIDNIGHT_GUARD_MINUTES)
            return Duration.between(now, target).toMillis().coerceAtLeast(0L)
        }
    }
}
