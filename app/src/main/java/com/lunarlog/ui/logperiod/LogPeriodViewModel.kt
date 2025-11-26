package com.lunarlog.ui.logperiod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.core.model.Cycle
import com.lunarlog.data.CycleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class LogPeriodUiState(
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LogPeriodViewModel @Inject constructor(
    private val cycleRepository: CycleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogPeriodUiState())
    val uiState: StateFlow<LogPeriodUiState> = _uiState.asStateFlow()

    fun savePeriod(startDate: Long, endDate: Long) {
        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

        val startLocalDate = Instant.ofEpochMilli(startDate)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()

        val endLocalDate = Instant.ofEpochMilli(endDate)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()

        viewModelScope.launch {
            try {
                cycleRepository.insertCycle(
                    Cycle(
                        startDate = startLocalDate.toEpochDay(),
                        endDate = endLocalDate.toEpochDay()
                    )
                )
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to save period"
                )
            }
        }
    }

    fun onNavigatedBack() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }

    fun onErrorShown() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
