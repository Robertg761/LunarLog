package com.lunarlog.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.AppLockMode
import com.lunarlog.data.DataManagementRepository
import com.lunarlog.data.Medication
import com.lunarlog.data.MedicationRepository
import com.lunarlog.data.UserPreferencesRepository
import com.lunarlog.workers.NotificationWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dataManagementRepository: DataManagementRepository,
    private val medicationRepository: MedicationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private companion object {
        const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
    }

    val appLockMode = userPreferencesRepository.appLockMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLockMode.NONE)

    val appLockTimeoutSeconds = userPreferencesRepository.appLockTimeoutSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val isAppLockEnabled = appLockMode
        .map { it != AppLockMode.NONE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val cycleNotificationEnabled = userPreferencesRepository.cycleNotificationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val themeSeedColor = userPreferencesRepository.themeSeedColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val periodReminderEnabled = userPreferencesRepository.periodLogReminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val periodReminderTimeMinutes = userPreferencesRepository.periodLogReminderTimeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20L * 60L)

    val medications = medicationRepository.getAllMedications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message = _message

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAppLockMode(
                if (enabled) AppLockMode.BIOMETRIC_REQUIRED else AppLockMode.NONE
            )
        }
    }

    fun setAppLockTimeoutSeconds(seconds: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setAppLockTimeoutSeconds(seconds)
        }
    }
    
    fun setThemeSeedColor(color: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeSeedColor(color)
        }
    }

    fun setPeriodReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPeriodLogReminderEnabled(enabled)
            if (enabled) {
                val minutes = try {
                    userPreferencesRepository.getPeriodLogReminderTimeMinutesSync()
                } catch (_: Exception) {
                    20L * 60L
                }
                NotificationWorkScheduler.schedulePeriodLogReminders(context, minutes)
            } else {
                NotificationWorkScheduler.cancelPeriodLogReminders(context)
            }
        }
    }

    fun setCycleNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setCycleNotificationEnabled(enabled)
            if (enabled) {
                NotificationWorkScheduler.scheduleCycleNotifications(context)
            } else {
                NotificationWorkScheduler.cancelCycleNotifications(context)
            }
        }
    }

    fun setPeriodReminderTimeMinutes(minutes: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setPeriodLogReminderTimeMinutes(minutes)
            val enabled = try {
                userPreferencesRepository.getPeriodLogReminderEnabledSync()
            } catch (_: Exception) {
                false
            }
            if (enabled) {
                // Reschedule immediately so changes take effect without requiring app restart.
                NotificationWorkScheduler.schedulePeriodLogReminders(context, minutes)
            }
        }
    }

    fun addMedication(
        name: String,
        dosage: String,
        frequency: String,
        reminderTimeMinutes: Long?
    ) {
        val normalizedName = name.trim().replace(Regex("\\s+"), " ").take(80)
        val normalizedDosage = dosage.trim().replace(Regex("\\s+"), " ").take(80)
        if (normalizedName.isBlank()) {
            _message.value = "Medication name is required."
            return
        }
        if (frequency !in setOf("daily", "weekly", "as_needed")) {
            _message.value = "Medication frequency is invalid."
            return
        }
        if (reminderTimeMinutes != null && reminderTimeMinutes !in 0L..1439L) {
            _message.value = "Medication reminder time is invalid."
            return
        }

        viewModelScope.launch {
            medicationRepository.addMedication(
                Medication(
                    name = normalizedName,
                    dosage = normalizedDosage,
                    frequency = frequency,
                    startDate = LocalDate.now().toEpochDay(),
                    reminderTime = reminderTimeMinutes.takeUnless { frequency == "as_needed" }
                )
            )
            NotificationWorkScheduler.scheduleMedicationReminders(context)
            _message.value = "Medication added."
        }
    }

    fun deleteMedication(id: Int) {
        viewModelScope.launch {
            medicationRepository.deleteMedication(id)
            NotificationWorkScheduler.scheduleMedicationReminders(context)
            _message.value = "Medication deleted."
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = dataManagementRepository.createBackupJson()
                withContext(Dispatchers.IO) {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                        ?: throw IOException("The selected destination could not be opened")
                    outputStream.use { it.write(json.toByteArray(StandardCharsets.UTF_8)) }
                }
                _message.value = "Backup saved successfully."
            } catch (e: Exception) {
                _message.value = "Backup failed: ${e.localizedMessage}"
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    val declaredLength = context.contentResolver
                        .openAssetFileDescriptor(uri, "r")
                        ?.use { it.length }
                    if (declaredLength != null && declaredLength > MAX_BACKUP_BYTES) {
                        throw IOException("Backup exceeds the 10 MB import limit")
                    }

                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IOException("The selected backup could not be opened")
                    inputStream.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_BACKUP_BYTES) {
                                throw IOException("Backup exceeds the 10 MB import limit")
                            }
                            output.write(buffer, 0, count)
                        }
                        output.toString(StandardCharsets.UTF_8.name())
                    }
                }
                dataManagementRepository.restoreFromJson(jsonString)
                restoreNotificationSchedules()
                _message.value = "Data restored successfully."
            } catch (e: Exception) {
                _message.value = "Restore failed: ${e.localizedMessage}"
            }
        }
    }

    fun nukeData() {
        viewModelScope.launch {
            try {
                dataManagementRepository.nukeData()
                userPreferencesRepository.clearAll()
                NotificationWorkScheduler.cancelMedicationReminders(context)
                _message.value = "All data cleared."
            } catch (e: Exception) {
                _message.value = "Failed to clear data: ${e.localizedMessage}"
            }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    private suspend fun restoreNotificationSchedules() {
        if (userPreferencesRepository.getCycleNotificationEnabledSync()) {
            NotificationWorkScheduler.scheduleCycleNotifications(context)
        } else {
            NotificationWorkScheduler.cancelCycleNotifications(context)
        }

        if (userPreferencesRepository.getPeriodLogReminderEnabledSync()) {
            NotificationWorkScheduler.schedulePeriodLogReminders(
                context,
                userPreferencesRepository.getPeriodLogReminderTimeMinutesSync()
            )
        } else {
            NotificationWorkScheduler.cancelPeriodLogReminders(context)
        }
        NotificationWorkScheduler.scheduleMedicationReminders(context)
    }
}
