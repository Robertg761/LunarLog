package com.lunarlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.UserPreferencesRepository
import com.lunarlog.ui.navigation.Screen
import com.lunarlog.update.UpdateInfo
import com.lunarlog.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainActivityUiState(
    val isLoading: Boolean = true,
    val startDestination: String = Screen.Home.route,
    val isAppLockEnabled: Boolean = false,
    val themeSeedColor: Int? = null,
    val updateInfo: UpdateInfo? = null,
    val isUpdateAvailable: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    private val _installUpdateTrigger = Channel<UpdateInfo>(Channel.CONFLATED)
    val installUpdateTrigger = _installUpdateTrigger.receiveAsFlow()

    val uiState = combine(
        userPreferencesRepository.isFirstRun,
        userPreferencesRepository.isAppLockEnabled,
        userPreferencesRepository.themeSeedColor,
        _updateInfo
    ) { isFirstRun, isAppLockEnabled, themeSeedColor, updateInfo ->
        MainActivityUiState(
            isLoading = false,
            startDestination = if (isFirstRun) Screen.Onboarding.route else Screen.Home.route,
            isAppLockEnabled = isAppLockEnabled,
            themeSeedColor = themeSeedColor?.toInt(),
            updateInfo = updateInfo,
            isUpdateAvailable = updateInfo != null
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = MainActivityUiState(isLoading = true),
        started = SharingStarted.WhileSubscribed(5_000)
    )
    
    private val _isLocked = MutableStateFlow(true)
    val isLocked = _isLocked.asStateFlow()

    fun unlock() {
        _isLocked.value = false
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val info = updateRepository.checkForUpdate(
                    owner = "Robertg761",
                    repo = "LunarLog",
                    currentVersionName = BuildConfig.VERSION_NAME
                )
                _updateInfo.value = info
            } catch (_: Exception) {
                // Network failures should be silent; the app remains functional without update checks.
                _updateInfo.value = null
            }
        }
    }

    fun triggerInstallUpdate() {
        val info = _updateInfo.value ?: return
        _installUpdateTrigger.trySend(info)
    }
}
