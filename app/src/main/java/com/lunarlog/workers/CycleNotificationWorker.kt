package com.lunarlog.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
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

        // Period due in 2 days
        if (today.plusDays(2) == nextPeriod) {
            sendNotification(
                notificationId = ("cycle_update_period_due_${nextPeriod.toEpochDay()}").hashCode(),
                title = "Cycle Update",
                message = "Your period is predicted to start in 2 days."
            )
        }

        // Fertile window starting (Notification on the start day)
        if (today == fertileWindow.first) {
              sendNotification(
                notificationId = ("cycle_update_fertile_start_${fertileWindow.first.toEpochDay()}").hashCode(),
                title = "Cycle Update",
                message = "Your estimated fertile days begin today. Predictions are not birth control."
            )
        }

        return Result.success()
    }

    private fun sendNotification(notificationId: Int, title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "lunar_log_channel"

        val channel = NotificationChannel(channelId, "LunarLog Notifications", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
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
