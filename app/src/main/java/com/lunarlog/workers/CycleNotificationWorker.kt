package com.lunarlog.workers

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lunarlog.R
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.UserPreferencesRepository
import com.lunarlog.logic.CyclePredictionUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class CycleNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CycleRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val enabled = try {
            userPreferencesRepository.getCycleNotificationEnabledSync()
        } catch (_: Exception) {
            false
        }
        if (!enabled) return Result.success()

        val cycles = repository.getAllCyclesSync()
        if (cycles.isEmpty()) return Result.success()

        val averageLength = CyclePredictionUtils.calculateAverageCycleLength(cycles)
        val averagePeriodLength = CyclePredictionUtils.calculateAveragePeriodLength(cycles)
        // Cycles are ordered by startDate DESC in Dao, so the first one is the latest
        val lastCycle = cycles.first()

        val nextPeriod = CyclePredictionUtils.predictNextPeriodAfterLatestCycle(
            lastCycle,
            averageLength,
            averagePeriodLength
        )
        val fertileWindow = CyclePredictionUtils.predictFertileWindow(nextPeriod)

        val today = LocalDate.now()

        // Period due in 2 days. Home is where the period button lives, so the tap lands there.
        if (today.plusDays(2) == nextPeriod) {
            sendNotification(
                notificationId = ("cycle_update_period_due_${nextPeriod.toEpochDay()}").hashCode(),
                title = "Cycle Update",
                message = "Your period is predicted to start in 2 days.",
                contentIntent = NotificationIntents.launchApp(
                    applicationContext,
                    requestCode = "cycle_update_period_due".hashCode()
                )
            )
        }

        // Fertile window starting (Notification on the start day). The calendar is the one screen
        // that draws the estimated window, so that is where the tap opens.
        if (today == fertileWindow.first) {
            sendNotification(
                notificationId = ("cycle_update_fertile_start_${fertileWindow.first.toEpochDay()}").hashCode(),
                title = "Cycle Update",
                message = "Your estimated fertile days begin today. Predictions are not birth control.",
                contentIntent = NotificationIntents.deepLink(
                    applicationContext,
                    uri = "lunarlog://calendar",
                    requestCode = "cycle_update_fertile_start".hashCode()
                )
            )
        }

        return Result.success()
    }

    private fun sendNotification(
        notificationId: Int,
        title: String,
        message: String,
        contentIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        NotificationChannels.ensureCreated(applicationContext)
        val channelId = NotificationChannels.CYCLE_PREDICTIONS
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        notificationManager.notify(notificationId, notification)
    }
}
