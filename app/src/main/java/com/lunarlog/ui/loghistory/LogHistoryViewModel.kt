package com.lunarlog.ui.loghistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.DailyLog
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.data.SymptomDefinition
import com.lunarlog.data.SymptomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class LogHistoryViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val symptomRepository: SymptomRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedSymptom = MutableStateFlow<SymptomDefinition?>(null)
    val selectedSymptom = _selectedSymptom.asStateFlow()

    val availableSymptoms = symptomRepository.getAllSymptoms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<DailyLog>> = combine(
        _searchQuery,
        _selectedSymptom
    ) { query, symptom ->
        Pair(query, symptom)
    }.flatMapLatest { (query, symptom) ->
        if (symptom != null) {
            dailyLogRepository.searchLogsBySymptom(symptom.name)
        } else if (query.isNotBlank()) {
            dailyLogRepository.searchLogs(query)
        } else {
            dailyLogRepository.getAllLogs()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            _selectedSymptom.value = null
        }
    }

    fun onSymptomSelected(symptom: SymptomDefinition?) {
        _selectedSymptom.value = symptom
        if (symptom != null) {
            _searchQuery.value = ""
        }
    }
}
