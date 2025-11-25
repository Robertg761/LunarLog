package com.lunarlog.ui.loglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val repository: DailyLogRepository
) : ViewModel() {

    // UI State
    data class UiState(
        val date: LocalDate = LocalDate.now(),
        val entries: List<LogEntry> = emptyList(),
        val isLoading: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun loadDate(date: Long) {
        val localDate = LocalDate.ofEpochDay(date)
        _uiState.value = _uiState.value.copy(
            date = localDate, 
            entries = emptyList(), // Clear old entries
            isLoading = true
        )
        
        viewModelScope.launch {
            try {
                repository.ensureLegacyDataHydrated(date)
                
                repository.getEntriesForDate(date).collect { list ->
                    _uiState.value = _uiState.value.copy(entries = list, isLoading = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // In a real app, we would expose this error to the UI
                _uiState.value = _uiState.value.copy(isLoading = false)
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
                }
                // Note: If user removed all values for the editing type, we strictly strictly don't delete it here
                // to be safe, unless we want to support deletion via empty list.
                // Current UI removes the type from map if empty, so it just won't be updated.
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
}
