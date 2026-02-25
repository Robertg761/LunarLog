package com.lunarlog.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lunarlog.data.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PeriodLogReminderRescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val enabled = try {
            userPreferencesRepository.getPeriodLogReminderEnabledSync()
        } catch (_: Exception) {
            false
        }

        if (!enabled) {
            NotificationWorkScheduler.cancelPeriodLogReminders(applicationContext)
            return Result.success()
        }

        val minutes = try {
            userPreferencesRepository.getPeriodLogReminderTimeMinutesSync()
        } catch (_: Exception) {
            20L * 60L
        }

        NotificationWorkScheduler.schedulePeriodLogReminders(applicationContext, minutes)
        return Result.success()
    }
}

