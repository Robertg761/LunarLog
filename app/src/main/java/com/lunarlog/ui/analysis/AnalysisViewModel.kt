package com.lunarlog.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.di.DefaultDispatcher
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
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class AnalysisUiState(
    val cycleHistory: List<Pair<LocalDate, Int>> = emptyList(),
    val symptomCounts: Map<String, Int> = emptyMap(),
    val moodCounts: Map<String, Int> = emptyMap(),
    val recentCycleSummaries: List<CycleSummary> = emptyList(),
    val weeklyDigest: WeeklyDigest? = null,
    val symptomCorrelations: List<com.lunarlog.logic.SymptomCorrelation> = emptyList(),
    val anomalies: List<com.lunarlog.logic.CycleAnomaly> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val recentLogsFlow = dailyLogRepository.getLogsForRange(
        startDate = LocalDate.now().minusMonths(6),
        endDate = LocalDate.now()
    )

    val uiState: StateFlow<AnalysisUiState> = combine(
        cycleRepository.getAllCycles(),
        recentLogsFlow
    ) { cycles, logs ->
        withContext(defaultDispatcher) {
            val cycleHistory = cycles.filter { it.endDate != null }
                .map {
                    val start = it.startDate
                    val length = (ChronoUnit.DAYS.between(start, it.endDate!!) + 1).toInt()
                    start to length
                }
                .sortedBy { it.first }

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

            val cycleSummaries = cycles.filter { it.endDate != null }
                .sortedByDescending { it.startDate }
                .take(5)
                .mapNotNull { NarrativeGenerator.generateCycleSummary(it, logs) }

            val weeklyDigest = NarrativeGenerator.generateWeeklyDigest(logs)
            val symptomCorrelations = com.lunarlog.logic.SymptomCorrelationEngine.analyzeCorrelations(cycles, logs)
            val anomalies = com.lunarlog.logic.SmartAnomalyDetector.detectAnomalies(cycles)

            AnalysisUiState(
                cycleHistory = cycleHistory,
                symptomCounts = symptomCounts,
                moodCounts = moodCounts,
                recentCycleSummaries = cycleSummaries,
                weeklyDigest = weeklyDigest,
                symptomCorrelations = symptomCorrelations,
                anomalies = anomalies,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalysisUiState(isLoading = true)
    )
}
