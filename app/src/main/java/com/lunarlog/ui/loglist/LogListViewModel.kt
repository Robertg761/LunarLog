package com.lunarlog.ui.loglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import com.lunarlog.data.Medication
import com.lunarlog.data.MedicationRepository
import com.lunarlog.data.PeriodChangeResult
import com.lunarlog.data.SymptomCategory
import com.lunarlog.data.SymptomDefinition
import com.lunarlog.data.SymptomRepository
import com.lunarlog.logic.MedicationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LogListViewModel @Inject constructor(
    private val repository: DailyLogRepository,
    private val cycleRepository: CycleRepository,
    private val symptomRepository: SymptomRepository,
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    // UI State
    data class UiState(
        val date: LocalDate = LocalDate.now(),
        val entries: List<LogEntry> = emptyList(),
        val isLoading: Boolean = false,
        val isPeriodDay: Boolean = false,
        val periodMessage: String? = null,
        val symptomDefinitions: List<SymptomDefinition> = emptyList(),
        val medications: List<Medication> = emptyList(),
        val takenMedicationIds: Set<Int> = emptySet()
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())
    private var loadDateJob: Job? = null

    init {
        viewModelScope.launch {
            symptomRepository.getAllSymptoms().collect { definitions ->
                _uiState.value = _uiState.value.copy(symptomDefinitions = definitions)
            }
        }
    }

    fun loadDate(date: Long) {
        val localDate = try {
            LocalDate.ofEpochDay(date)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                entries = emptyList(),
                periodMessage = "This link contains an invalid date."
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            date = localDate, 
            entries = emptyList(), // Clear old entries
            isLoading = true
        )

        loadDateJob?.cancel()
        loadDateJob = viewModelScope.launch {
            try {
                // Load period status first
                val isPeriod = checkPeriodStatus(localDate)
                _uiState.value = _uiState.value.copy(isPeriodDay = isPeriod)
                
                repository.ensureLegacyDataHydrated(date)
                
                combine(
                    repository.getEntriesForDate(date),
                    medicationRepository.getActiveMedications(date),
                    medicationRepository.getLogsForDate(date)
                ) { entries, activeMedications, medicationLogs ->
                    Triple(
                        entries,
                        activeMedications.filter { medication ->
                            medication.frequency == "as_needed" ||
                                MedicationScheduler.isMedicationDueToday(medication, localDate)
                        },
                        medicationLogs.filter { it.taken }.mapTo(mutableSetOf()) { it.medicationId }
                    )
                }.collect { (entries, medications, takenMedicationIds) ->
                    _uiState.value = _uiState.value.copy(
                        entries = entries,
                        medications = medications,
                        takenMedicationIds = takenMedicationIds,
                        isLoading = false
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    periodMessage = "Unable to load this day. Please try again."
                )
            }
        }
    }

    fun deleteEntry(entry: LogEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    fun addEntry(type: LogEntryType, value: String, time: Long, details: String? = null) {
        viewModelScope.launch {
            val date = _uiState.value.date.toEpochDay()
            val entry = LogEntry(
                date = date,
                time = time,
                type = type,
                value = value,
                details = details
            )
            repository.addEntry(entry)
        }
    }

    fun updateEntry(entry: LogEntry) {
        viewModelScope.launch {
            repository.updateEntry(entry)
        }
    }

    fun saveEntries(
        payload: Map<LogEntryType, List<String>>,
        time: Long,
        details: String?,
        editingEntry: LogEntry?
    ) {
        viewModelScope.launch {
            val date = _uiState.value.date.toEpochDay()

            // 1. Handle Editing Entry if it exists
            if (editingEntry != null) {
                val editingTypeValues = payload[editingEntry.type]
                if (!editingTypeValues.isNullOrEmpty()) {
                    // Update the existing entry with the first value from the list for this type
                    repository.updateEntry(editingEntry.copy(
                        type = editingEntry.type,
                        value = editingTypeValues[0],
                        time = time,
                        details = details
                    ))
                    
                    // Add any EXTRA values for this type as new entries
                    for (i in 1 until editingTypeValues.size) {
                        repository.addEntry(LogEntry(
                            date = date,
                            time = time,
                            type = editingEntry.type,
                            value = editingTypeValues[i],
                            details = details
                        ))
                    }
                } else {
                    repository.deleteEntry(editingEntry)
                }
            }

            // 2. Handle all other types (New Entries)
            payload.forEach { (type, values) ->
                // If we already handled this type for the editing entry, skip the first one (as it updated the entry)
                // and we already added the rest.
                if (editingEntry != null && type == editingEntry.type) {
                    return@forEach
                }

                // Create new entries for everything else
                values.forEach { value ->
                    repository.addEntry(LogEntry(
                        date = date,
                        time = time,
                        type = type,
                        value = value,
                        details = details
                    ))
                }
            }
        }
    }

    private suspend fun checkPeriodStatus(date: LocalDate): Boolean {
        return cycleRepository.isPeriodDay(date)
    }

    fun togglePeriod(isPeriodDay: Boolean) {
        viewModelScope.launch {
            val date = _uiState.value.date
            val result = cycleRepository.setPeriodDay(date, isPeriodDay)
            val isPeriod = checkPeriodStatus(date)
            val message = when (result) {
                is PeriodChangeResult.Success -> result.message
                is PeriodChangeResult.ValidationError -> result.message
            }
            _uiState.value = _uiState.value.copy(
                isPeriodDay = isPeriod,
                periodMessage = message
            )
        }
    }

    fun onPeriodMessageShown() {
        _uiState.value = _uiState.value.copy(periodMessage = null)
    }

    fun addCustomSymptom(name: String, category: SymptomCategory) {
        val normalized = name.trim().replace(Regex("\\s+"), " ").take(50)
        if (normalized.isBlank()) return
        viewModelScope.launch {
            symptomRepository.addCustomSymptom(normalized, category)
        }
    }

    fun setMedicationTaken(medicationId: Int, taken: Boolean) {
        val state = _uiState.value
        if (state.medications.none { it.id == medicationId }) return
        viewModelScope.launch {
            medicationRepository.setMedicationTaken(
                date = state.date.toEpochDay(),
                medicationId = medicationId,
                taken = taken
            )
        }
    }
}
