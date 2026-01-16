package com.lunarlog.ui.periodhistory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PeriodDetailUiState(
    val cycle: Cycle? = null,
    val dailyLogs: List<DailyLog> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PeriodDetailViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cycleId: Int = savedStateHandle["cycleId"] ?: -1

    private val _uiState = MutableStateFlow(PeriodDetailUiState())
    val uiState: StateFlow<PeriodDetailUiState> = _uiState.asStateFlow()

    init {
        loadCycle()
    }

    private fun loadCycle() {
        viewModelScope.launch {
            try {
                val cycle = cycleRepository.getCycleById(cycleId)
                if (cycle != null) {
                    val endDate = cycle.endDate ?: LocalDate.now()
                    val logs = dailyLogRepository.getLogsForRangeSync(cycle.startDate, endDate)
                    _uiState.value = _uiState.value.copy(
                        cycle = cycle,
                        dailyLogs = logs,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Period not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load: ${e.message}"
                )
            }
        }
    }

    fun updateStartDate(date: LocalDate) {
        val cycle = _uiState.value.cycle ?: return
        val endDate = cycle.endDate
        
        // Validate: startDate must be before or equal to endDate
        if (endDate != null && date.isAfter(endDate)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Start date cannot be after end date")
            return
        }
        
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val updatedCycle = cycle.copy(startDate = date)
                cycleRepository.updateCycle(updatedCycle)
                // Reload logs for new date range
                val logs = dailyLogRepository.getLogsForRangeSync(date, endDate ?: LocalDate.now())
                _uiState.value = _uiState.value.copy(
                    cycle = updatedCycle,
                    dailyLogs = logs,
                    isSaving = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to update: ${e.message}"
                )
            }
        }
    }

    fun updateEndDate(date: LocalDate?) {
        val cycle = _uiState.value.cycle ?: return
        
        // Validate: endDate must be after or equal to startDate
        if (date != null && date.isBefore(cycle.startDate)) {
            _uiState.value = _uiState.value.copy(errorMessage = "End date cannot be before start date")
            return
        }
        
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val updatedCycle = cycle.copy(endDate = date)
                cycleRepository.updateCycle(updatedCycle)
                // Reload logs for new date range
                val logs = dailyLogRepository.getLogsForRangeSync(cycle.startDate, date ?: LocalDate.now())
                _uiState.value = _uiState.value.copy(
                    cycle = updatedCycle,
                    dailyLogs = logs,
                    isSaving = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to update: ${e.message}"
                )
            }
        }
    }

    fun deleteCycle() {
        val cycle = _uiState.value.cycle ?: return
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                cycleRepository.deleteCycle(cycle)
                _uiState.value = _uiState.value.copy(isDeleted = true, isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to delete: ${e.message}"
                )
            }
        }
    }

    fun onErrorShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
