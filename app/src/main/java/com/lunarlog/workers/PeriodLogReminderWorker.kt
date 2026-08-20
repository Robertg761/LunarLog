package com.lunarlog.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lunarlog.R
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogDao
import com.lunarlog.data.LogEntryDao
import com.lunarlog.data.LogEntryType
import com.lunarlog.data.UserPreferencesRepository
import com.lunarlog.logic.CyclePredictionUtils
import com.lunarlog.logic.PeriodEndReminderPolicy
import com.lunarlog.logic.PeriodLogReminderPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class PeriodLogReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cycleRepository: CycleRepository,
    private val dailyLogDao: DailyLogDao,
    private val logEntryDao: LogEntryDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val enabled = try {
            userPreferencesRepository.getPeriodLogReminderEnabledSync()
        } catch (_: Exception) {
            false
        }
        if (!enabled) {
            // Best-effort cleanup if a stale periodic job exists.
            WorkManager.getInstance(applicationContext).cancelUniqueWork(NotificationWorkScheduler.UNIQUE_PERIOD_LOG_REMINDER_WORK_NAME)
            return Result.success()
        }

        val cycles = cycleRepository.getAllCyclesSync()
        if (cycles.isEmpty()) return Result.success()
        val lastCycle = cycles.first()

        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()

        val entriesToday = try {
            logEntryDao.getEntriesForDateSync(todayEpochDay)
        } catch (_: Exception) {
            // If we can't read entries, avoid notifying to prevent false positives.
            return Result.success()
        }
        val hasEntriesToday = entriesToday.isNotEmpty()

        // A period left open past its expected end takes precedence over the daily logging
        // nudge: an unrecorded end date is what quietly starves the period-length average.
        val hasPositiveFlowToday = entriesToday.any {
            it.type == LogEntryType.FLOW && (it.value.toFloatOrNull() ?: 0f) > 0f
        }
        val shouldConfirmEnd = PeriodEndReminderPolicy.shouldNotify(
            today = today,
            cycleStart = lastCycle.startDate,
            cycleEnd = lastCycle.endDate,
            reminderEnabled = enabled,
            averagePeriodLength = CyclePredictionUtils.calculateAveragePeriodLength(cycles),
            hasPositiveFlowToday = hasPositiveFlowToday
        )
        if (shouldConfirmEnd) {
            sendNotification(
                title = "LunarLog",
                message = "Is your period over? Open the app to log its end date.",
                notificationId = ("period_end_reminder_$todayEpochDay").hashCode(),
                contentIntent = buildLaunchPendingIntent()
            )
            return Result.success()
        }

        val dailyLogEmpty = if (hasEntriesToday) {
            // Entries imply logging; don't spend time querying the aggregate.
            true
        } else {
            val dl = try {
                dailyLogDao.getLogForDateSync(today)
            } catch (_: Exception) {
                // Avoid false positives if Room throws or the DB is unavailable.
                return Result.success()
            }
            dl == null || isDailyLogEmpty(dl)
        }

        val shouldNotify = PeriodLogReminderPolicy.shouldNotify(
            today = today,
            cycleStart = lastCycle.startDate,
            cycleEnd = lastCycle.endDate,
            reminderEnabled = enabled,
            hasEntriesToday = hasEntriesToday,
            dailyLogEmpty = dailyLogEmpty,
            maxDays = 5L
        )
        if (!shouldNotify) return Result.success()

        val deepLink = "lunarlog://details/$todayEpochDay"
        val requestCode = ("details_$todayEpochDay").hashCode()
        val pendingIntent = buildDeepLinkPendingIntent(deepLink, requestCode)

        val notificationId = ("period_log_reminder_$todayEpochDay").hashCode()
        sendNotification(
            title = "LunarLog",
            message = "Time to log today's details.",
            notificationId = notificationId,
            contentIntent = pendingIntent
        )

        return Result.success()
    }

    private fun isDailyLogEmpty(log: com.lunarlog.core.model.DailyLog): Boolean {
        return log.flowLevel == 0 &&
            log.symptoms.isEmpty() &&
            log.mood.isEmpty() &&
            log.waterIntake == 0 &&
            log.sleepHours == 0f &&
            log.sleepQuality == 0 &&
            log.sexDrive == 0 &&
            log.notes.isEmpty() &&
            log.temperature == null &&
            log.cervicalMucus == 0
    }

    /** Opens the app at its start destination (Home), where the period button lives. */
    private fun buildLaunchPendingIntent(): PendingIntent {
        // Explicit component, so the PendingIntent can't be redirected by another app.
        val intent = Intent(applicationContext, com.lunarlog.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(applicationContext, "period_end_reminder".hashCode(), intent, flags)
    }

    private fun buildDeepLinkPendingIntent(uri: String, requestCode: Int): PendingIntent {
        // Constructed explicit (MainActivity hosts every lunarlog:// deep link), so the
        // PendingIntent can't be redirected by another app.
        val intent = Intent(applicationContext, com.lunarlog.MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri.toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(applicationContext, requestCode, intent, flags)
    }

    private fun sendNotification(
        title: String,
        message: String,
        notificationId: Int,
        contentIntent: PendingIntent
    ) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "lunar_log_channel"

        val channel = NotificationChannel(
            channelId,
            "LunarLog Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(applicationContext, channelId)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("LunarLog reminder")
                    .setContentText("Open LunarLog to view details")
                    .build()
            )
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(notificationId, notification)
            }
        } else {
            notificationManager.notify(notificationId, notification)
        }
    }
}
