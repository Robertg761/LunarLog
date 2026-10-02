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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
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
    val bbtObservation: LocalDate? = null,
    val mucusObservation: LocalDate? = null,
    val rangeMonths: Int = 6,
    val error: String? = null,
    val isLoading: Boolean = true
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val medicationRepository: com.lunarlog.data.MedicationRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    val rangeMonths = MutableStateFlow(6)
    val exporting = MutableStateFlow(false)
    val exportMessage = MutableStateFlow<String?>(null)
    val exportDestination = MutableStateFlow<Pair<android.net.Uri, String>?>(null)

    fun export(uri: android.net.Uri, csv: Boolean) {
        if (exporting.value) return
        val snapshot = uiState.value
        if (snapshot.isLoading || snapshot.error != null || snapshot.rangeMonths != rangeMonths.value) {
            exportMessage.value = "Wait for the selected range to finish loading, then export again."
            return
        }
        val months = snapshot.rangeMonths
        exporting.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val firstDay = if (months == 0) LocalDate.of(1900, 1, 1).toEpochDay() else LocalDate.now().minusMonths(months.toLong()).toEpochDay()
                    val lastDay = if (months == 0) Long.MAX_VALUE else LocalDate.now().toEpochDay()
                    val doses = medicationRepository.getLogsForRangeSync(firstDay, lastDay)
                    val medications = medicationRepository.getAllMedicationsSync().filter { medication ->
                        (medication.startDate <= lastDay && (medication.endDate == null || medication.endDate >= firstDay)) || doses.any { it.medicationId == medication.id }
                    }
                    val stream = context.contentResolver.openOutputStream(uri) ?: error("The destination could not be opened")
                    stream.use {
                        if (csv) ReportGenerator.generateCsv(it, snapshot.periods, snapshot.dailyLogs, snapshot.logEntries, medications, doses)
                        else ReportGenerator.generatePdf(it, snapshot.cycleHistory, snapshot.symptomCounts, snapshot.moodCounts, medications, doses, snapshot.dailyLogs)
                    }
                }
                exportDestination.value = uri to if (csv) "text/csv" else "application/pdf"
                exportMessage.value = "Report saved"
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { exportMessage.value = "Unable to save report. ${e.message ?: "Try another location."}" }
            finally { exporting.value = false }
        }
    }

    val uiState: StateFlow<AnalysisUiState> = rangeMonths.flatMapLatest { months ->
        combine(
            cycleRepository.getAllCycles(),
            if (months == 0) dailyLogRepository.getAllLogs() else dailyLogRepository.getLogsForRange(LocalDate.now().minusMonths(months.toLong()), LocalDate.now()),
            if (months == 0) dailyLogRepository.getAllEntries() else dailyLogRepository.getEntriesForRange(LocalDate.now().minusMonths(months.toLong()).toEpochDay(), LocalDate.now().toEpochDay())
        ) { cycles, allLogs, logEntries ->
        withContext(defaultDispatcher) {
            val recentStart = if (months == 0) LocalDate.of(1900, 1, 1) else LocalDate.now().minusMonths(months.toLong())
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
                rangeMonths = months,
                cycleHistory = cycleHistory,
                symptomCounts = symptomCounts,
                moodCounts = moodCounts,
                recentCycleSummaries = cycleSummaries,
                weeklyDigest = weeklyDigest,
                symptomCorrelations = symptomCorrelations,
                anomalies = anomalies,
                periods = cycles.filter { (it.endDate ?: LocalDate.now()) >= recentStart }.sortedBy { it.startDate },
                dailyLogs = recentLogs.sortedBy { it.date },
                logEntries = logEntries.filter { it.date >= recentStart.toEpochDay() },
                bbtObservation = cycles.maxByOrNull { it.startDate }?.let { com.lunarlog.logic.AdvancedCycleIntelligence.detectOvulationFromBBT(it.startDate, recentLogs) },
                mucusObservation = cycles.maxByOrNull { it.startDate }?.let { com.lunarlog.logic.AdvancedCycleIntelligence.detectPeakMucusDay(it.startDate, recentLogs,
                    logEntries.filter { it.type == com.lunarlog.data.LogEntryType.MUCUS }.mapTo(mutableSetOf()) { LocalDate.ofEpochDay(it.date) } + recentLogs.filter { it.cervicalMucus > 0 }.map { it.date }) },
                isLoading = false
            )
        }
        }.onStart { emit(AnalysisUiState(isLoading = true, rangeMonths = months)) }
    }.catch { if (it is CancellationException) throw it; emit(AnalysisUiState(isLoading = false, error = "Unable to load analysis. Reopen this screen to retry.")) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalysisUiState(isLoading = true)
    )
}
