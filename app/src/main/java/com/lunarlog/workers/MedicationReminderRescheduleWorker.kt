package com.lunarlog.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lunarlog.data.MedicationRepository
import com.lunarlog.logic.MedicationScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.ZoneId

@HiltWorker
class MedicationReminderRescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val medicationRepository: MedicationRepository
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = runCatching {
        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()
        val nextReminders = medicationRepository.getAllMedicationsSync()
            .mapNotNull { medication ->
                MedicationScheduler.getNextReminderTime(medication, now, zoneId)
                    ?.let { reminderTime -> medication.id to reminderTime }
            }

        val nextTime = nextReminders.minOfOrNull { it.second }
        if (nextTime == null) {
            NotificationWorkScheduler.cancelPendingMedicationReminder(applicationContext)
            return@runCatching Result.success()
        }

        val medicationIds = nextReminders
            .filter { it.second == nextTime }
            .map { it.first }
            .toIntArray()
        NotificationWorkScheduler.enqueueMedicationReminder(
            context = applicationContext,
            scheduledEpochMillis = nextTime,
            medicationIds = medicationIds
        )
        Result.success()
    }.getOrElse { Result.retry() }
}
