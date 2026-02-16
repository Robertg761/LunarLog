package com.lunarlog.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.DataManagementRepository
import com.lunarlog.data.UserPreferencesRepository
import com.lunarlog.workers.NotificationWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dataManagementRepository: DataManagementRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isAppLockEnabled = userPreferencesRepository.isAppLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val themeSeedColor = userPreferencesRepository.themeSeedColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val periodReminderEnabled = userPreferencesRepository.periodLogReminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val periodReminderTimeMinutes = userPreferencesRepository.periodLogReminderTimeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20L * 60L)

    private val _message = MutableStateFlow<String?>(null)
    val message = _message

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAppLockEnabled(enabled)
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

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = dataManagementRepository.createBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
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
                    val json = StringBuilder()
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var line = reader.readLine()
                            while (line != null) {
                                json.append(line)
                                line = reader.readLine()
                            }
                        }
                    }
                    json.toString()
                }
                dataManagementRepository.restoreFromJson(jsonString)
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
                _message.value = "All data cleared."
            } catch (e: Exception) {
                _message.value = "Failed to clear data: ${e.localizedMessage}"
            }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }
}
