package com.lunarlog.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.core.model.Cycle
import com.lunarlog.data.CycleRepository
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun completeOnboarding(lastPeriodDate: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            // Create the first cycle
            // We assume it ended? No, if it's the *last* period, it might be the start of the current cycle.
            // Usually trackers ask for "Start date of last period".
            cycleRepository.insertCycle(
                Cycle(startDate = lastPeriodDate)
            )
            userPreferencesRepository.setFirstRunComplete()
            _isLoading.value = false
        }
    }
}
