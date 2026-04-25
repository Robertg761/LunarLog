package com.lunarlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.core.config.AppConfig
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import com.lunarlog.data.PeriodChangeResult
import com.lunarlog.logic.CounterMode
import com.lunarlog.logic.CounterPresentationCalculator
import com.lunarlog.logic.CyclePredictionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val counterValue: Int = 0,
    val counterMode: CounterMode = CounterMode.NEXT_PERIOD_COUNTDOWN,
    val counterTitle: String = "Next Period",
    val counterSubtitle: String = "No cycle data yet",
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
            runCatching {
                if (cycles.isEmpty()) {
                    val counter = CounterPresentationCalculator.calculate(emptyList())
                    HomeUiState(
                        counterValue = counter.value,
                        counterMode = counter.mode,
                        counterTitle = counter.title,
                        counterSubtitle = counter.subtitle,
                        isLoading = false
                    )
                } else {
                    val sortedCycles = cycles.sortedByDescending { it.startDate }
                    val lastCycle = sortedCycles.first()
                    val averageLength = CyclePredictionUtils.calculateAverageCycleLength(cycles)
                    val averagePeriodLength = CyclePredictionUtils.calculateAveragePeriodLength(cycles)
                    val nextPeriodStart = CyclePredictionUtils.predictNextPeriodAfterLatestCycle(
                        lastCycle,
                        averageLength,
                        averagePeriodLength
                    )
                    val today = LocalDate.now()
                    val counter = CounterPresentationCalculator.calculate(cycles, today)

                    val daysUntil = ChronoUnit.DAYS.between(today, nextPeriodStart).toInt()
                    val currentCycleDay = ChronoUnit.DAYS.between(lastCycle.startDate, today).toInt() + 1

                    val ovulationByBBT = com.lunarlog.logic.AdvancedCycleIntelligence.detectOvulationFromBBT(lastCycle.startDate, logs)
                    val peakMucus = com.lunarlog.logic.AdvancedCycleIntelligence.detectPeakMucusDay(lastCycle.startDate, logs)
                    val refinedOvulation = ovulationByBBT ?: peakMucus ?: CyclePredictionUtils.predictOvulation(nextPeriodStart)
                    
                    val refinedFertileStart = refinedOvulation.minusDays(AppConfig.FERTILE_WINDOW_OFFSET_START)
                    val refinedFertileEnd = refinedOvulation.plusDays(AppConfig.FERTILE_WINDOW_OFFSET_END)
                    val isFertile = today >= refinedFertileStart && today <= refinedFertileEnd

                    val anomalies = com.lunarlog.logic.SmartAnomalyDetector.detectAnomalies(cycles)

                    val isPeriodOngoing = lastCycle.endDate == null
                    val isPeriodActive = isPeriodOngoing
                    val isEndedToday = lastCycle.endDate == today

                    val daysRemainingInPeriod =
                        if (counter.mode == CounterMode.PERIOD_DAYS_LEFT) counter.value else null

                    val quickLogSymptoms = com.lunarlog.logic.SymptomStatsCalculator.getTopSymptomsForPhase(currentCycleDay, cycles, logs)

                    HomeUiState(
                        daysUntilPeriod = daysUntil,
                        daysRemainingInPeriod = daysRemainingInPeriod,
                        currentCycleDay = currentCycleDay,
                        isFertile = isFertile,
                        isPeriodActive = isPeriodActive,
                        isPeriodOngoing = isPeriodOngoing,
                        isEndedToday = isEndedToday,
                        counterValue = counter.value,
                        counterMode = counter.mode,
                        counterTitle = counter.title,
                        counterSubtitle = counter.subtitle,
                        isLoading = false,
                        quickLogSymptoms = quickLogSymptoms,
                        anomalies = anomalies
                    )
                }
            }.getOrElse {
                HomeUiState(isLoading = false)
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
            val today = LocalDate.now()
            val state = uiState.value
            val result = when {
                state.isPeriodOngoing -> cycleRepository.endOngoingPeriod(today)
                state.isEndedToday -> cycleRepository.resumePeriodEndedOn(today)
                else -> cycleRepository.startPeriod(today)
            }

            when (result) {
                is PeriodChangeResult.Success -> _message.trySend(result.message)
                is PeriodChangeResult.ValidationError -> _message.trySend(result.message)
            }
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

        val counterSummary = when (state.counterMode) {
            CounterMode.PERIOD_DAYS_LEFT -> {
                if (state.counterValue == 0) {
                    "Estimated period status: ending today"
                } else {
                    "Estimated period days left: ${state.counterValue}"
                }
            }
            CounterMode.PERIOD_OVERAGE ->
                "Period is ${state.counterValue} days beyond estimate"
            CounterMode.NEXT_PERIOD_COUNTDOWN -> {
                if (state.counterValue == 0) {
                    "Estimated next period: due today"
                } else {
                    "Estimated days until next period: ${state.counterValue}"
                }
            }
            CounterMode.NEXT_PERIOD_OVERDUE ->
                "Period is ${state.counterValue} days overdue"
        }

        return """
            🌙 LunarLog Status Update
            
            📊 $counterSummary
            📅 Cycle day ${state.currentCycleDay}
            ${if (state.isFertile) "🌿 Likely Fertile Window" else ""}
            
            Sent from my private LunarLog
        """.trimIndent()
    }
}
