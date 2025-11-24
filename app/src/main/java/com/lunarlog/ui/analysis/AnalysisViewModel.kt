package com.lunarlog.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.logic.CycleSummary
import com.lunarlog.logic.NarrativeGenerator
import com.lunarlog.logic.WeeklyDigest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AnalysisUiState(
    val cycleHistory: List<Pair<LocalDate, Int>> = emptyList(),
    val symptomCounts: Map<String, Int> = emptyMap(),
    val moodCounts: Map<String, Int> = emptyMap(),
    val recentCycleSummaries: List<CycleSummary> = emptyList(),
    val weeklyDigest: WeeklyDigest? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Load Cycles
            val cycles = cycleRepository.getAllCycles().first()
            val cycleHistory = cycles.filter { it.endDate != null }
                .map { 
                    val start = LocalDate.ofEpochDay(it.startDate)
                    val length = (it.endDate!! - it.startDate + 1).toInt()
                    start to length
                }
                .sortedBy { it.first }

            // Load Logs (Last 6 months)
            val endDay = LocalDate.now().toEpochDay()
            val startDay = LocalDate.now().minusMonths(6).toEpochDay()
            val logs = dailyLogRepository.getLogsForRange(startDay, endDay).first()

            val symptomCounts = logs.flatMap { it.symptoms }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .toMap()

            val moodCounts = logs.flatMap { it.mood }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .toMap()
            
            // Generate Narratives
            val cycleSummaries = cycles.filter { it.endDate != null }
                .sortedByDescending { it.startDate }
                .take(5) // Last 5 cycles
                .mapNotNull { NarrativeGenerator.generateCycleSummary(it, logs) }

            val weeklyDigest = NarrativeGenerator.generateWeeklyDigest(logs)

            _uiState.value = AnalysisUiState(
                cycleHistory = cycleHistory,
                symptomCounts = symptomCounts,
                moodCounts = moodCounts,
                recentCycleSummaries = cycleSummaries,
                weeklyDigest = weeklyDigest,
                isLoading = false
            )
        }
    }
}
