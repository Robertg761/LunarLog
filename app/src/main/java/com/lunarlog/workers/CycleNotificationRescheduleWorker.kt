package com.lunarlog.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lunarlog.data.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CycleNotificationRescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val enabled = try {
            userPreferencesRepository.getCycleNotificationEnabledSync()
        } catch (_: Exception) {
            false
        }

        if (!enabled) {
            NotificationWorkScheduler.cancelCycleNotifications(applicationContext)
            return Result.success()
        }

        NotificationWorkScheduler.rescheduleCycleNotifications(applicationContext)
        return Result.success()
    }
}

