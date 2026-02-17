package com.lunarlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.AppLockMode
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainActivityUiState(
    val isLoading: Boolean = true,
    val startDestination: String = Screen.Home.route,
    val isAppLockEnabled: Boolean = false,
    val appLockMode: AppLockMode = AppLockMode.NONE,
    val appLockTimeoutSeconds: Long = 0L,
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
        userPreferencesRepository.appLockMode,
        userPreferencesRepository.appLockTimeoutSeconds,
        userPreferencesRepository.themeSeedColor,
        _updateInfo
    ) { isFirstRun, appLockMode, appLockTimeoutSeconds, themeSeedColor, updateInfo ->
        MainActivityUiState(
            isLoading = false,
            startDestination = if (isFirstRun) Screen.Onboarding.route else Screen.Home.route,
            isAppLockEnabled = appLockMode != AppLockMode.NONE,
            appLockMode = appLockMode,
            appLockTimeoutSeconds = appLockTimeoutSeconds,
            themeSeedColor = themeSeedColor?.toInt(),
            updateInfo = updateInfo,
            isUpdateAvailable = updateInfo != null
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = MainActivityUiState(isLoading = true),
        started = SharingStarted.WhileSubscribed(5_000)
    )
    
    private val _isLocked = MutableStateFlow(false)
    val isLocked = _isLocked.asStateFlow()
    private var lastUnlockAtMillis: Long? = null

    init {
        viewModelScope.launch {
            userPreferencesRepository.appLockMode.collectLatest { mode ->
                if (mode == AppLockMode.NONE) {
                    _isLocked.value = false
                    return@collectLatest
                }
                if (lastUnlockAtMillis == null) {
                    _isLocked.value = true
                }
            }
        }
    }

    fun unlock() {
        _isLocked.value = false
        lastUnlockAtMillis = System.currentTimeMillis()
    }

    fun lock() {
        if (uiState.value.isAppLockEnabled) {
            _isLocked.value = true
        }
    }

    fun onAppResumed() {
        val state = uiState.value
        if (!state.isAppLockEnabled) {
            _isLocked.value = false
            return
        }

        val timeoutSeconds = state.appLockTimeoutSeconds.coerceAtLeast(0L)
        if (timeoutSeconds == 0L) {
            _isLocked.value = true
            return
        }

        val lastUnlock = lastUnlockAtMillis
        val elapsedMillis = if (lastUnlock == null) Long.MAX_VALUE else (System.currentTimeMillis() - lastUnlock)
        _isLocked.value = elapsedMillis >= timeoutSeconds * 1000L
    }

    fun onAppBackgrounded() {
        if (uiState.value.isAppLockEnabled && uiState.value.appLockTimeoutSeconds == 0L) {
            _isLocked.value = true
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            if (BuildConfig.DEBUG) {
                // Debug builds are often signed differently than release APK assets.
                _updateInfo.value = null
                return@launch
            }
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
