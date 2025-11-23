package com.lunarlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.CycleRepository
import com.lunarlog.logic.CyclePredictionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class HomeUiState(
    val daysUntilPeriod: Int = 0,
    val currentCycleDay: Int = 0,
    val isFertile: Boolean = false,
    val isLoading: Boolean = true,
    val isPeriodActive: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    cycleRepository: CycleRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = cycleRepository.getAllCycles()
        .map { cycles ->
            if (cycles.isEmpty()) {
                HomeUiState(isLoading = false)
            } else {
                val sortedCycles = cycles.sortedByDescending { it.startDate }
                val lastCycle = sortedCycles.first()
                val averageLength = CyclePredictionUtils.calculateAverageCycleLength(cycles)
                val nextPeriodStart = CyclePredictionUtils.predictNextPeriod(lastCycle, averageLength)
                val today = LocalDate.now()

                val daysUntil = ChronoUnit.DAYS.between(today, nextPeriodStart).toInt()
                val currentCycleDay = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(lastCycle.startDate), today).toInt() + 1

                // Check fertile window
                val (fertileStart, fertileEnd) = CyclePredictionUtils.predictFertileWindow(nextPeriodStart)
                val isFertile = today >= fertileStart && today <= fertileEnd

                // Simple check if period is active (assuming 5 days default or checking endDate if exists)
                val isPeriodActive = if (lastCycle.endDate != null) {
                    !today.isAfter(LocalDate.ofEpochDay(lastCycle.endDate))
                } else {
                    // Fallback if no end date logged yet, assume active if within 7 days for safety?
                    // Or just strictly rely on data.
                    // If endDate is null, it might mean the period is still ongoing or user forgot to close it.
                    // Let's assume ongoing if < 7 days for now or just strictly ongoing.
                    // But for now, let's say it's active if the current day is reasonably small and no end date.
                    currentCycleDay <= 5
                }

                HomeUiState(
                    daysUntilPeriod = daysUntil,
                    currentCycleDay = currentCycleDay,
                    isFertile = isFertile,
                    isPeriodActive = isPeriodActive,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )
}
