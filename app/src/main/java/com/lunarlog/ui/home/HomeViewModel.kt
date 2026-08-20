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
    val isEstimatedFertileWindow: Boolean = false,
    val isLoading: Boolean = true,
    val isPeriodActive: Boolean = false, // Visual: Is today a period day?
    val isPeriodOngoing: Boolean = false, // Logic: Is the period open?
    val isEndedToday: Boolean = false, // Logic: Did it end today?
    val counterValue: Int = 0,
    val counterMode: CounterMode = CounterMode.NEXT_PERIOD_COUNTDOWN,
    val counterTitle: String = "Next Period",
    val counterSubtitle: String = "No cycle data yet",
    val counterScaleDays: Int = AppConfig.DEFAULT_CYCLE_LENGTH,
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
                        counterScaleDays = AppConfig.DEFAULT_CYCLE_LENGTH,
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

                    // Calendar predictions are estimates only. Observed BBT and mucus signals are
                    // retrospective and must never be used to claim current contraceptive safety.
                    val (estimatedFertileStart, estimatedFertileEnd) =
                        CyclePredictionUtils.predictFertileWindow(nextPeriodStart)
                    val isEstimatedFertileWindow =
                        today >= estimatedFertileStart && today <= estimatedFertileEnd

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
                        isEstimatedFertileWindow = isEstimatedFertileWindow,
                        isPeriodActive = isPeriodActive,
                        isPeriodOngoing = isPeriodOngoing,
                        isEndedToday = isEndedToday,
                        counterValue = counter.value,
                        counterMode = counter.mode,
                        counterTitle = counter.title,
                        counterSubtitle = counter.subtitle,
                        counterScaleDays = when (counter.mode) {
                            CounterMode.PERIOD_DAYS_LEFT, CounterMode.PERIOD_OVERAGE -> averagePeriodLength
                            CounterMode.NEXT_PERIOD_COUNTDOWN, CounterMode.NEXT_PERIOD_OVERDUE -> averageLength
                        }.coerceAtLeast(1),
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
            
            dailyLogRepository.addEntryIfAbsent(entry)
        }
    }

    fun getShareableStatus(): String {
        val state = uiState.value
        if (state.isLoading) return "Loading..."

        val counterSummary = when (state.counterMode) {
            CounterMode.PERIOD_DAYS_LEFT -> {
                // The counter counts today as remaining, so 1 means the last expected day.
                if (state.counterValue == 1) {
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
            ${if (state.isEstimatedFertileWindow) "🌿 Estimated fertile days (prediction only)" else ""}
            
            Sent from my private LunarLog
        """.trimIndent()
    }
}
