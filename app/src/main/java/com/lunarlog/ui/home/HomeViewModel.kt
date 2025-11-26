package com.lunarlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.core.model.Cycle
import com.lunarlog.core.config.AppConfig
import com.lunarlog.data.CycleRepository
import com.lunarlog.core.model.DailyLog
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import com.lunarlog.logic.CyclePredictionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class HomeUiState(
    val daysUntilPeriod: Int = 0,
    val daysRemainingInPeriod: Int? = null,
    val currentCycleDay: Int = 0,
    val isFertile: Boolean = false,
    val isLoading: Boolean = true,
    val isPeriodActive: Boolean = false, // Visual: Is today a period day?
    val isPeriodOngoing: Boolean = false, // Logic: Is the period open?
    val isEndedToday: Boolean = false, // Logic: Did it end today?
    val quickLogSymptoms: List<String> = emptyList(),
    val anomalies: List<com.lunarlog.logic.CycleAnomaly> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    @com.lunarlog.di.DefaultDispatcher private val defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher
) : ViewModel() {

    private val _message = Channel<String>(Channel.CONFLATED)
    val message = _message.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        cycleRepository.getAllCycles(),
        dailyLogRepository.getAllLogs()
    ) { cycles, logs ->
        kotlinx.coroutines.withContext(defaultDispatcher) {
            if (cycles.isEmpty()) {
                HomeUiState(isLoading = false)
            } else {
                val sortedCycles = cycles.sortedByDescending { it.startDate }
                val lastCycle = sortedCycles.first()
                val averageLength = CyclePredictionUtils.calculateAverageCycleLength(cycles)
                val averagePeriodLength = CyclePredictionUtils.calculateAveragePeriodLength(cycles)
                val nextPeriodStart = CyclePredictionUtils.predictNextPeriod(lastCycle, averageLength)
                val today = LocalDate.now()

                val daysUntil = ChronoUnit.DAYS.between(today, nextPeriodStart).toInt()
                val currentCycleDay = ChronoUnit.DAYS.between(lastCycle.startDate, today).toInt() + 1

                // Check fertile window (Advanced)
                val ovulationByBBT = com.lunarlog.logic.AdvancedCycleIntelligence.detectOvulationFromBBT(lastCycle.startDate, logs)
                val peakMucus = com.lunarlog.logic.AdvancedCycleIntelligence.detectPeakMucusDay(lastCycle.startDate, logs)
                val refinedOvulation = ovulationByBBT ?: peakMucus ?: CyclePredictionUtils.predictOvulation(nextPeriodStart)
                
                val refinedFertileStart = refinedOvulation.minusDays(AppConfig.FERTILE_WINDOW_OFFSET_START)
                val refinedFertileEnd = refinedOvulation.plusDays(AppConfig.FERTILE_WINDOW_OFFSET_END)
                val isFertile = today >= refinedFertileStart && today <= refinedFertileEnd

                // Check anomalies
                val anomalies = com.lunarlog.logic.SmartAnomalyDetector.detectAnomalies(cycles)

                // Check if period is active (Visual)
                val isPeriodActive = if (lastCycle.endDate != null) {
                    !today.isAfter(lastCycle.endDate)
                } else {
                    currentCycleDay <= averagePeriodLength
                }

                // Check if period is ongoing (Logic: Open)
                val isPeriodOngoing = lastCycle.endDate == null
                
                // Check if ended today
                val isEndedToday = lastCycle.endDate == today

                val daysRemainingInPeriod = if (isPeriodActive) {
                    if (lastCycle.endDate != null) {
                        ChronoUnit.DAYS.between(today, lastCycle.endDate).toInt()
                    } else {
                         averagePeriodLength - currentCycleDay
                    }
                } else {
                    null
                }

                val quickLogSymptoms = com.lunarlog.logic.SymptomStatsCalculator.getTopSymptomsForPhase(currentCycleDay, cycles, logs)

                HomeUiState(
                    daysUntilPeriod = daysUntil,
                    daysRemainingInPeriod = daysRemainingInPeriod,
                    currentCycleDay = currentCycleDay,
                    isFertile = isFertile,
                    isPeriodActive = isPeriodActive,
                    isPeriodOngoing = isPeriodOngoing,
                    isEndedToday = isEndedToday,
                    isLoading = false,
                    quickLogSymptoms = quickLogSymptoms,
                    anomalies = anomalies
                )
            }
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AppConfig.FLOW_SUBSCRIPTION_TIMEOUT),
        initialValue = HomeUiState()
    )

    fun togglePeriod() {
        viewModelScope.launch {
            val msg = cycleRepository.togglePeriod(LocalDate.now())
            _message.trySend(msg)
        }
    }

    fun logQuickSymptom(symptom: String) {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            val time = System.currentTimeMillis()
            
            // Create a granular entry
            val entry = LogEntry(
                date = today,
                time = time,
                type = LogEntryType.SYMPTOM,
                value = symptom
            )
            
            dailyLogRepository.addEntry(entry)
        }
    }

    fun getShareableStatus(): String {
        val state = uiState.value
        if (state.isLoading) return "Loading..."

        return """
            🌙 LunarLog Status Update
            
            📅 Day ${state.currentCycleDay} of Cycle
            ⏳ Period due in ${state.daysUntilPeriod} days
            ${if (state.isFertile) "🌿 Likely Fertile Window" else ""}
            
            Sent from my private LunarLog
        """.trimIndent()
    }
}