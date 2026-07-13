package com.lunarlog.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lunarlog.R
import com.lunarlog.data.MedicationRepository
import com.lunarlog.logic.MedicationScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

@HiltWorker
class MedicationReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val medicationRepository: MedicationRepository
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result = runCatching {
        val scheduledEpochMillis = inputData.getLong(KEY_SCHEDULED_EPOCH_MILLIS, -1L)
        val medicationIds = (inputData.getIntArray(KEY_MEDICATION_IDS) ?: intArrayOf()).toSet()
        if (scheduledEpochMillis <= 0L || medicationIds.isEmpty()) {
            NotificationWorkScheduler.scheduleMedicationReminders(applicationContext)
            return@runCatching Result.success()
        }

        val scheduledInstant = Instant.ofEpochMilli(scheduledEpochMillis)
        val isStale = abs(Duration.between(scheduledInstant, Instant.now()).toHours()) > 6L
        if (!isStale) {
            val zoneId = ZoneId.systemDefault()
            val scheduledDate = scheduledInstant.atZone(zoneId).toLocalDate()
            val takenMedicationIds = medicationRepository
                .getLogsForDateSync(scheduledDate.toEpochDay())
                .filter { it.taken }
                .mapTo(mutableSetOf()) { it.medicationId }

            medicationRepository.getAllMedicationsSync()
                .filter { medication ->
                    medication.id in medicationIds &&
                        medication.id !in takenMedicationIds &&
                        MedicationScheduler.isMedicationDueToday(medication, scheduledDate)
                }
                .forEach(::sendNotification)
        }

        NotificationWorkScheduler.scheduleMedicationReminders(applicationContext)
        Result.success()
    }.getOrElse { Result.retry() }

    private fun sendNotification(medication: com.lunarlog.data.Medication) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Medication reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val privateText = listOfNotNull(
            medication.name,
            medication.dosage.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
        val publicVersion = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("LunarLog reminder")
            .setContentText("Open LunarLog to view details")
            .build()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Medication reminder")
            .setContentText(privateText)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build()

        notificationManager.notify(("medication_${medication.id}").hashCode(), notification)
    }

    companion object {
        const val KEY_SCHEDULED_EPOCH_MILLIS = "scheduled_epoch_millis"
        const val KEY_MEDICATION_IDS = "medication_ids"
        private const val CHANNEL_ID = "lunar_log_medications"
    }
}
