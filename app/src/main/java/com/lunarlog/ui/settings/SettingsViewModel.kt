package com.lunarlog.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.DataManagementRepository
import com.lunarlog.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileOutputStream
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

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = dataManagementRepository.createBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
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
                dataManagementRepository.restoreFromJson(json.toString())
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
