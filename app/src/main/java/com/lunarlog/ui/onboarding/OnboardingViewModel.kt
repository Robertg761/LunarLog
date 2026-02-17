package com.lunarlog.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.PeriodChangeResult
import com.lunarlog.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    sealed interface OnboardingState {
        data object Idle : OnboardingState
        data object Saving : OnboardingState
        data object Success : OnboardingState
        data class Error(val message: String) : OnboardingState
    }

    private val _onboardingState = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val onboardingState = _onboardingState.asStateFlow()

    fun completeOnboarding(lastPeriodDate: LocalDate) {
        if (_onboardingState.value == OnboardingState.Saving) return
        viewModelScope.launch {
            _onboardingState.value = OnboardingState.Saving
            try {
                when (val result = cycleRepository.setPeriodDay(lastPeriodDate, true)) {
                    is PeriodChangeResult.Success -> {
                        userPreferencesRepository.setFirstRunComplete()
                        _onboardingState.value = OnboardingState.Success
                    }
                    is PeriodChangeResult.ValidationError -> {
                        _onboardingState.value = OnboardingState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _onboardingState.value = OnboardingState.Error(
                    e.localizedMessage ?: "Couldn't complete setup"
                )
            }
        }
    }

    fun clearError() {
        if (_onboardingState.value is OnboardingState.Error) {
            _onboardingState.value = OnboardingState.Idle
        }
    }
}
