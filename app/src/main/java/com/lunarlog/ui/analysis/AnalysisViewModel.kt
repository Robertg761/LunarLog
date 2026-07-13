package com.lunarlog.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.data.LogEntry
import com.lunarlog.di.DefaultDispatcher
import com.lunarlog.logic.CyclePredictionUtils
import com.lunarlog.logic.CycleSummary
import com.lunarlog.logic.NarrativeGenerator
import com.lunarlog.logic.WeeklyDigest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

data class AnalysisUiState(
    val cycleHistory: List<Pair<LocalDate, Int>> = emptyList(),
    val symptomCounts: Map<String, Int> = emptyMap(),
    val moodCounts: Map<String, Int> = emptyMap(),
    val recentCycleSummaries: List<CycleSummary> = emptyList(),
    val weeklyDigest: WeeklyDigest? = null,
    val symptomCorrelations: List<com.lunarlog.logic.SymptomCorrelation> = emptyList(),
    val anomalies: List<com.lunarlog.logic.CycleAnomaly> = emptyList(),
    val periods: List<Cycle> = emptyList(),
    val dailyLogs: List<DailyLog> = emptyList(),
    val logEntries: List<LogEntry> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val recentStart = LocalDate.now().minusMonths(6)

    val uiState: StateFlow<AnalysisUiState> = combine(
        cycleRepository.getAllCycles(),
        dailyLogRepository.getAllLogs(),
        dailyLogRepository.getAllEntries()
    ) { cycles, allLogs, logEntries ->
        withContext(defaultDispatcher) {
            val completedCycles = CyclePredictionUtils.completedCycleIntervals(cycles)
            val recentLogs = allLogs.filter { !it.date.isBefore(recentStart) }
            val cycleHistory = completedCycles
                .filter { !it.endDate.isBefore(recentStart) }
                .map { it.startDate to it.length }

            val symptomCounts = recentLogs.flatMap { it.symptoms }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .toMap()

            val moodCounts = recentLogs.flatMap { it.mood }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .toMap()

            val cycleSummaries = completedCycles
                .sortedByDescending { it.startDate }
                .take(5)
                .map { NarrativeGenerator.generateCycleSummary(it, allLogs) }

            val weeklyDigest = NarrativeGenerator.generateWeeklyDigest(recentLogs)
            val symptomCorrelations = com.lunarlog.logic.SymptomCorrelationEngine.analyzeCorrelations(cycles, recentLogs)
            val anomalies = com.lunarlog.logic.SmartAnomalyDetector.detectAnomalies(cycles)

            AnalysisUiState(
                cycleHistory = cycleHistory,
                symptomCounts = symptomCounts,
                moodCounts = moodCounts,
                recentCycleSummaries = cycleSummaries,
                weeklyDigest = weeklyDigest,
                symptomCorrelations = symptomCorrelations,
                anomalies = anomalies,
                periods = cycles.sortedBy { it.startDate },
                dailyLogs = allLogs.sortedBy { it.date },
                logEntries = logEntries,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalysisUiState(isLoading = true)
    )
}
