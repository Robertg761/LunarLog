package com.lunarlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.UserPreferencesRepository
import com.lunarlog.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
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
    val isUpdateAvailable: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _isUpdateAvailable = MutableStateFlow(false)
    private val _installUpdateTrigger = Channel<Unit>(Channel.CONFLATED)
    val installUpdateTrigger = _installUpdateTrigger.receiveAsFlow()

    val uiState = combine(
        userPreferencesRepository.isFirstRun,
        userPreferencesRepository.isAppLockEnabled,
        userPreferencesRepository.themeSeedColor,
        _isUpdateAvailable
    ) { isFirstRun, isAppLockEnabled, themeSeedColor, isUpdateAvailable ->
        MainActivityUiState(
            isLoading = false,
            startDestination = if (isFirstRun) Screen.Onboarding.route else Screen.Home.route,
            isAppLockEnabled = isAppLockEnabled,
            themeSeedColor = themeSeedColor?.toInt(),
            isUpdateAvailable = isUpdateAvailable
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

    fun setUpdateAvailable(available: Boolean) {
        _isUpdateAvailable.value = available
    }

    fun triggerInstallUpdate() {
        _installUpdateTrigger.trySend(Unit)
    }
}
