package com.lunarlog.ui.logdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.DailyLog
import com.lunarlog.data.DailyLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LogDetailsUiState(
    val date: LocalDate = LocalDate.now(),
    val flowLevel: Int = 0,
    val selectedSymptoms: List<String> = emptyList(),
    val selectedMoods: List<String> = emptyList(),
    val waterIntake: Int = 0,
    val sleepHours: Float = 0f,
    val sleepQuality: Int = 0,
    val sexDrive: Int = 0,
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LogDetailsViewModel @Inject constructor(
    private val repository: DailyLogRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogDetailsUiState())
    val uiState: StateFlow<LogDetailsUiState> = _uiState.asStateFlow()

    private val dateArg: Long? = savedStateHandle["date"] // Expecting epoch day

    init {
        val date = if (dateArg != null) {
            LocalDate.ofEpochDay(dateArg)
        } else {
            LocalDate.now()
        }
        loadLog(date)
    }

    private fun loadLog(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, date = date)
            try {
                // We use firstOrNull to get the current state from DB once for editing
                // We could also collect flow if we wanted real-time updates, but for a form, one-shot is usually easier to manage state
                val log = repository.getLogForDate(date.toEpochDay()).firstOrNull()
                
                if (log != null) {
                    _uiState.value = _uiState.value.copy(
                        flowLevel = log.flowLevel,
                        selectedSymptoms = log.symptoms,
                        selectedMoods = log.mood,
                        waterIntake = log.waterIntake,
                        sleepHours = log.sleepHours,
                        sleepQuality = log.sleepQuality,
                        sexDrive = log.sexDrive,
                        notes = log.notes,
                        isLoading = false
                    )
                } else {
                    // No existing log, reset to defaults
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load data: ${e.message}"
                )
            }
        }
    }

    fun updateFlowLevel(level: Int) {
        _uiState.value = _uiState.value.copy(flowLevel = level)
    }

    fun toggleSymptom(symptom: String) {
        val current = _uiState.value.selectedSymptoms.toMutableList()
        if (current.contains(symptom)) {
            current.remove(symptom)
        } else {
            current.add(symptom)
        }
        _uiState.value = _uiState.value.copy(selectedSymptoms = current)
    }

    fun toggleMood(mood: String) {
        val current = _uiState.value.selectedMoods.toMutableList()
        if (current.contains(mood)) {
            current.remove(mood)
        } else {
            current.add(mood)
        }
        _uiState.value = _uiState.value.copy(selectedMoods = current)
    }

    fun updateWaterIntake(cups: Int) {
        _uiState.value = _uiState.value.copy(waterIntake = cups)
    }

    fun updateSleepHours(hours: Float) {
        _uiState.value = _uiState.value.copy(sleepHours = hours)
    }

    fun updateSleepQuality(quality: Int) {
        _uiState.value = _uiState.value.copy(sleepQuality = quality)
    }

    fun updateSexDrive(level: Int) {
        _uiState.value = _uiState.value.copy(sexDrive = level)
    }

    fun updateNotes(text: String) {
        _uiState.value = _uiState.value.copy(notes = text)
    }

    fun saveLog() {
        val state = _uiState.value
        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            try {
                repository.saveLog(
                    DailyLog(
                        date = state.date.toEpochDay(),
                        flowLevel = state.flowLevel,
                        symptoms = state.selectedSymptoms,
                        mood = state.selectedMoods,
                        waterIntake = state.waterIntake,
                        sleepHours = state.sleepHours,
                        sleepQuality = state.sleepQuality,
                        sexDrive = state.sexDrive,
                        notes = state.notes
                    )
                )
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to save: ${e.message}"
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
