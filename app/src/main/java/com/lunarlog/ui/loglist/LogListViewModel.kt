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
import kotlinx.coroutines.CancellationException
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
        val isSaving: Boolean = false,
        val saveCompleted: Boolean = false,
        val saveError: String? = null,
        val deletedEntry: LogEntry? = null,
        val isPeriodDay: Boolean = false,
        val periodMessage: String? = null,
        val symptomDefinitions: List<SymptomDefinition> = emptyList(),
        val medications: List<Medication> = emptyList(),
        val takenMedicationIds: Set<Int> = emptySet(),
        val medicationLogs: List<com.lunarlog.data.MedicationLog> = emptyList()
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
                    medicationRepository.getAllMedications(),
                    medicationRepository.getLogsForDate(date)
                ) { entries, activeMedications, medicationLogs ->
                    Triple(
                        entries,
                        activeMedications.filter { medication ->
                            medicationLogs.any { it.medicationId == medication.id } ||
                                (!medication.isArchived && medication.startDate <= date && (medication.endDate == null || medication.endDate >= date) && (medication.frequency == "as_needed" ||
                                MedicationScheduler.isMedicationDueToday(medication, localDate)))
                        },
                        medicationLogs.filter { it.taken }
                    )
                }.collect { (entries, medications, medicationLogs) ->
                    _uiState.value = _uiState.value.copy(
                        entries = entries,
                        medications = medications,
                        takenMedicationIds = medicationLogs.mapTo(mutableSetOf()) { it.medicationId },
                        medicationLogs = medicationLogs,
                        isLoading = false
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    periodMessage = "Unable to load this day. Please try again."
                )
            }
        }
    }

    fun deleteEntry(entry: LogEntry) {
        runWrite {
            repository.deleteEntry(entry)
            _uiState.value = _uiState.value.copy(deletedEntry = entry)
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
        if (_uiState.value.isSaving) return
        val date = _uiState.value.date.toEpochDay()
        _uiState.value = _uiState.value.copy(isSaving = true, saveError = null, saveCompleted = false)
        viewModelScope.launch {
            try {
                val entries = payload.flatMap { (type, values) -> values.map { value ->
                    LogEntry(date = date, time = time, type = type, value = value, details = details)
                } }
                repository.saveEntries(date, entries, editingEntry)
                _uiState.value = _uiState.value.copy(isSaving = false, saveCompleted = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, saveError = error.message ?: "Unable to save. Your draft is still here.")
            }
        }
    }

    fun acknowledgeSave() {
        _uiState.value = _uiState.value.copy(saveCompleted = false, saveError = null)
    }

    fun undoDelete() {
        val entry = _uiState.value.deletedEntry ?: return
        runWrite {
            repository.restoreDeletedEntry(entry)
            _uiState.value = _uiState.value.copy(deletedEntry = null)
        }
    }

    fun clearUndo() { _uiState.value = _uiState.value.copy(deletedEntry = null) }

    private fun runWrite(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                _uiState.value = _uiState.value.copy(periodMessage = error.message ?: "Unable to save. Please try again.")
            }
        }
    }

    private suspend fun checkPeriodStatus(date: LocalDate): Boolean {
        return cycleRepository.isPeriodDay(date)
    }

    fun togglePeriod(isPeriodDay: Boolean) {
        runWrite {
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
        runWrite {
            symptomRepository.addCustomSymptom(normalized, category)
        }
    }

    fun logDose(medicationId: Int, hour: Int, minute: Int) {
        val date = _uiState.value.date
        val timestamp = date.atTime(hour, minute).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        runWrite { medicationRepository.logDose(date.toEpochDay(), medicationId, timestamp) }
    }

    fun removeDose(id: Long) { runWrite { medicationRepository.removeDose(id) } }
}
