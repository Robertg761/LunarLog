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
import com.lunarlog.logic.CyclePredictionUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class CycleNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CycleRepository
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

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "lunar_log_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "LunarLog Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
