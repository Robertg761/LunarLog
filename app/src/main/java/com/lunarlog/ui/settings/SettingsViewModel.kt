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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val symptomRepository: com.lunarlog.data.SymptomRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private companion object {
        const val MAX_BACKUP_BYTES = com.lunarlog.data.LogValidation.MAX_BACKUP_BYTES
    }

    val redactWidgets = userPreferencesRepository.redactWidgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun setRedactWidgets(value: Boolean) = launchSafely {
        userPreferencesRepository.setRedactWidgets(value)
        com.lunarlog.ui.widget.WidgetRefresher.updateAll(context)
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

    /**
     * False until DataStore has answered for every preference the screen renders as a Switch.
     *
     * Each `stateIn` above hands out its default (app lock off, alerts off, 8:00 PM) until the first
     * real read lands, so composing the rows immediately draws them wrong and then animates each
     * Switch across. Combining the *repository* flows — which emit nothing until DataStore reads —
     * gives the screen a real signal to gate on instead of counting frames.
     */
    val isLoaded: StateFlow<Boolean> = combine(
        userPreferencesRepository.appLockMode,
        userPreferencesRepository.appLockTimeoutSeconds,
        userPreferencesRepository.cycleNotificationEnabled,
        userPreferencesRepository.themeSeedColor,
        userPreferencesRepository.periodLogReminderEnabled
    ) { _, _, _, _, _ -> true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // restoreFromJson can run for seconds on a large backup with nothing on screen to say so.
    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message

    fun toggleAppLock(enabled: Boolean) {
        launchSafely {
            userPreferencesRepository.setAppLockMode(
                if (enabled) AppLockMode.BIOMETRIC_REQUIRED else AppLockMode.NONE
            )
        }
    }

    fun setAppLockTimeoutSeconds(seconds: Long) {
        launchSafely {
            userPreferencesRepository.setAppLockTimeoutSeconds(seconds)
        }
    }

    fun setThemeSeedColor(color: Long) {
        launchSafely {
            userPreferencesRepository.setThemeSeedColor(color)
        }
    }

    fun setPeriodReminderEnabled(enabled: Boolean) {
        launchSafely {
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
        launchSafely {
            userPreferencesRepository.setCycleNotificationEnabled(enabled)
            if (enabled) {
                NotificationWorkScheduler.scheduleCycleNotifications(context)
            } else {
                NotificationWorkScheduler.cancelCycleNotifications(context)
            }
        }
    }

    fun setPeriodReminderTimeMinutes(minutes: Long) {
        launchSafely {
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

    val managedSymptoms = symptomRepository.getManagedSymptoms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _medicationSaving = MutableStateFlow(false)
    val medicationSaving = _medicationSaving.asStateFlow()
    private val _medicationSaved = MutableStateFlow(false)
    val medicationSaved = _medicationSaved.asStateFlow()
    private val _medicationError = MutableStateFlow<String?>(null)
    val medicationError = _medicationError.asStateFlow()
    fun resetMedicationSave() { _medicationSaved.value = false; _medicationError.value = null }

    fun saveMedication(medication: Medication) {
        if (_medicationSaving.value) return
        _medicationSaving.value = true
        _medicationError.value = null
        viewModelScope.launch {
            try {
                medicationRepository.addMedication(medication)
                NotificationWorkScheduler.scheduleMedicationReminders(context)
                _medicationSaved.value = true
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { _medicationError.value = error.message ?: "Unable to save medication" }
            finally { _medicationSaving.value = false }
        }
    }

    fun setMedicationArchived(medication: Medication, archived: Boolean) = launchSafely {
        medicationRepository.setArchived(medication.id, archived)
        NotificationWorkScheduler.scheduleMedicationReminders(context)
        _message.value = if (archived) "Medication archived. Dose history kept." else "Medication resumed."
    }

    fun deleteMedication(id: Int) = launchSafely {
        medicationRepository.deleteMedication(id)
        NotificationWorkScheduler.scheduleMedicationReminders(context)
        _message.value = "Medication deleted."
    }

    fun renameSymptom(id: Long, label: String) = launchSafely { symptomRepository.renameCustom(id, label) }
    fun archiveSymptom(id: Long, archived: Boolean) = launchSafely { symptomRepository.archiveCustom(id, archived) }

    private fun launchSafely(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { _message.value = error.message ?: "Unable to save. Please try again." }
    }

    fun exportData(uri: Uri) {
        if (_isRestoring.value) return
        _isRestoring.value = true
        viewModelScope.launch {
            try {
                val json = dataManagementRepository.createBackupJson()
                withContext(Dispatchers.IO) {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                        ?: throw IOException("The selected destination could not be opened")
                    outputStream.use { it.write(json.toByteArray(StandardCharsets.UTF_8)) }
                }
                _message.value = "Backup saved successfully."
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                _message.value = "Backup failed: ${e.localizedMessage}"
            } finally { _isRestoring.value = false }
        }
    }

    val restorePreview = MutableStateFlow<String?>(null)
    private var pendingRestore: String? = null
    private val recoveryFile get() = java.io.File(context.noBackupFilesDir, "pre-restore.json")
    val hasRecoveryBackup = MutableStateFlow(recoveryFile.exists())

    fun cancelRestore() { pendingRestore = null; restorePreview.value = null }

    fun importData(uri: Uri) {
        if (_isRestoring.value) return
        _isRestoring.value = true
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
                restorePreview.value = dataManagementRepository.previewBackup(jsonString)
                pendingRestore = jsonString
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _message.value = "Cannot restore: ${e.localizedMessage}" }
            finally { _isRestoring.value = false }
        }
    }

    fun confirmRestore() {
        if (_isRestoring.value) return
        val json = pendingRestore ?: return
        _isRestoring.value = true
        viewModelScope.launch {
            try {
                val previous = dataManagementRepository.createBackupJson()
                withContext(Dispatchers.IO) {
                    val atomic = android.util.AtomicFile(recoveryFile)
                    val stream = atomic.startWrite()
                    try { stream.write(previous.toByteArray(Charsets.UTF_8)); atomic.finishWrite(stream) }
                    catch (error: Exception) { atomic.failWrite(stream); throw error }
                }
                hasRecoveryBackup.value = true
                dataManagementRepository.restoreFromJson(json)
                restoreNotificationSchedules()
                cancelRestore()
                _message.value = "Data restored. The previous data is available in the recovery backup."
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _message.value = "Restore failed: ${e.localizedMessage}" }
            finally { _isRestoring.value = false }
        }
    }

    fun exportRecovery(uri: Uri) {
        launchSafely {
            withContext(Dispatchers.IO) {
                val output = context.contentResolver.openOutputStream(uri) ?: error("Destination cannot be opened")
                output.use { recoveryFile.inputStream().use { input -> input.copyTo(it) } }
            }
            _message.value = "Recovery backup saved. You can restore this file using Restore backup."
        }
    }

    fun nukeData() {
        if (_isRestoring.value) return
        _isRestoring.value = true
        viewModelScope.launch {
            try {
                dataManagementRepository.nukeData()
                withContext(Dispatchers.IO) { android.util.AtomicFile(recoveryFile).delete() }
                hasRecoveryBackup.value = false
                userPreferencesRepository.clearAll()
                NotificationWorkScheduler.cancelMedicationReminders(context)
                NotificationWorkScheduler.cancelCycleNotifications(context)
                NotificationWorkScheduler.cancelPeriodLogReminders(context)
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).cancelAll()
                _message.value = "All data cleared."
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                _message.value = "Failed to clear data: ${e.localizedMessage}"
            } finally { _isRestoring.value = false }
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
