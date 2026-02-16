package com.lunarlog.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lunarlog.R
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogDao
import com.lunarlog.data.LogEntryDao
import com.lunarlog.data.UserPreferencesRepository
import com.lunarlog.logic.PeriodLogReminderPolicy
import com.lunarlog.logic.CyclePredictionUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class CycleNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CycleRepository,
    private val dailyLogDao: DailyLogDao,
    private val logEntryDao: LogEntryDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val cycles = repository.getAllCyclesSync()
        if (cycles.isEmpty()) return Result.success()

        val averageLength = CyclePredictionUtils.calculateAverageCycleLength(cycles)
        // Cycles are ordered by startDate DESC in Dao, so the first one is the latest
        val lastCycle = cycles.first()

        val nextPeriod = CyclePredictionUtils.predictNextPeriod(lastCycle, averageLength)
        val fertileWindow = CyclePredictionUtils.predictFertileWindow(nextPeriod)

        val today = LocalDate.now()

        // Period due in 2 days
        if (today.plusDays(2) == nextPeriod) {
            sendNotification(
                "Cycle Update",
                "Your period is predicted to start in 2 days."
            )
        }

        // Fertile window starting (Notification on the start day)
        if (today == fertileWindow.first) {
             sendNotification(
                "Cycle Update",
                "Your fertile window starts today."
            )
        }

        maybeSendDailyPeriodLogReminder(lastCycle = lastCycle, today = today)

        return Result.success()
    }

    private suspend fun maybeSendDailyPeriodLogReminder(lastCycle: com.lunarlog.core.model.Cycle, today: LocalDate) {
        val enabled = try {
            userPreferencesRepository.getPeriodLogReminderEnabledSync()
        } catch (_: Exception) {
            false
        }
        if (!enabled) return

        val todayEpochDay = today.toEpochDay()
        val hasEntriesToday = try {
            logEntryDao.getEntriesForDateSync(todayEpochDay).isNotEmpty()
        } catch (_: Exception) {
            false
        }

        val dailyLogEmpty = if (hasEntriesToday) {
            // Entries imply logging; don't spend time querying the aggregate.
            true
        } else {
            val dl = dailyLogDao.getLogForDateSync(today)
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
        if (!shouldNotify) return

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

    private fun buildDeepLinkPendingIntent(uri: String, requestCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setPackage(applicationContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(applicationContext, requestCode, intent, flags)
    }

    private fun sendNotification(
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt(),
        contentIntent: PendingIntent? = null
    ) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "lunar_log_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "LunarLog Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
        }
        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             if (ContextCompat.checkSelfPermission(
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
