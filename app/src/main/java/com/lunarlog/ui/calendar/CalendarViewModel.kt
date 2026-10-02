package com.lunarlog.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.core.model.Cycle
import com.lunarlog.core.config.AppConfig
import com.lunarlog.data.CycleRepository
import com.lunarlog.core.model.DailyLog
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.logic.CyclePredictionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    cycleRepository: CycleRepository,
    dailyLogRepository: DailyLogRepository
) : ViewModel() {

    private val today = kotlinx.coroutines.flow.MutableStateFlow(LocalDate.now())
    fun refreshDate(date: LocalDate = LocalDate.now()) { today.value = date }

    private val visibleMonth = kotlinx.coroutines.flow.MutableStateFlow(YearMonth.now())
    fun setVisibleMonth(month: YearMonth) { visibleMonth.value = month }
    private val visibleLogs = combine(today, visibleMonth) { date, month -> date to month }
        .flatMapLatest { (date, month) -> dailyLogRepository.getLogsForWindows(
            month.minusMonths(1).atDay(1), month.plusMonths(1).atEndOfMonth(), date.minusDays(35), date.plusDays(395)) }

    val calendarState: StateFlow<CalendarDataState> = combine(
        cycleRepository.getAllCycles(), visibleLogs, today
    ) { cycles, logs, date ->
        val state = CalendarDataState.Success(
            data = logs.associate { it.date.toEpochDay() to DayData(hasLog = true,
                flowIntensity = it.flowLevel, symptoms = it.symptoms, moods = it.mood, notes = it.notes) },
            cycles = cycles.sortedBy { it.startDate }, today = date
        )
        // Only a bounded current window is materialized. Historical months are resolved on demand.
        val current = (0L..430L).associate { offset ->
            val day = date.minusDays(35).plusDays(offset)
            day.toEpochDay() to resolveDay(day, state)
        }
        state.copy(data = state.data + current)
    }.catch { if (it is kotlinx.coroutines.CancellationException) throw it; emit(CalendarDataState.Success(emptyMap(), error = "Unable to load calendar. Reopen this screen to retry.")) }.flowOn(Dispatchers.Default).stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(AppConfig.FLOW_SUBSCRIPTION_TIMEOUT), CalendarDataState.Loading)

    private fun resolveDay(date: LocalDate, state: CalendarDataState.Success): DayData {
        val logged = state.data[date.toEpochDay()] ?: DayData()
        val period = state.cycles.firstOrNull { date >= it.startDate && date <= (it.endDate ?: state.today) }
        if (period != null) return logged.copy(isPeriod = true, isEstimatedPeriod = period.endEstimated)
        val latest = state.cycles.lastOrNull() ?: return logged
        if (date > state.today.plusMonths(12)) return logged
        val length = CyclePredictionUtils.calculateAverageCycleLength(state.cycles).coerceAtLeast(1)
        val periodLength = CyclePredictionUtils.calculateAveragePeriodLength(state.cycles)
        val base = CyclePredictionUtils.predictNextPeriodAfterLatestCycle(latest, length, periodLength)
        val delta = java.time.temporal.ChronoUnit.DAYS.between(base, date)
        val predictedPeriod = delta >= 0 && delta % length < periodLength
        val nextIndex = if (delta <= 0) 0 else (delta + length - 1) / length
        val next = base.plusDays(nextIndex * length)
        val fertile = CyclePredictionUtils.predictFertileWindow(next)
        return logged.copy(isPredictedPeriod = predictedPeriod,
            isFertile = date >= fertile.first && date <= fertile.second,
            isOvulation = date == CyclePredictionUtils.predictOvulation(next))
    }

    /**
     * Helper to get data for a specific month page (42 days)
     */
    fun getPageData(yearMonth: YearMonth, state: CalendarDataState): List<CalendarDayUiModel> {
        if (state !is CalendarDataState.Success) return emptyList()

        val firstDayOfMonth = yearMonth.atDay(1)
        val firstWeekday = java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).firstDayOfWeek
        val startOffset = (firstDayOfMonth.dayOfWeek.value - firstWeekday.value + 7) % 7
        val startDate = firstDayOfMonth.minusDays(startOffset.toLong())

        val days = ArrayList<CalendarDayUiModel>(42)

        for (i in 0 until 42) {
            val date = startDate.plusDays(i.toLong())
            val epoch = date.toEpochDay()
            val data = resolveDay(date, state)

            // Calculate connectivity here for rendering
            val type = if (data.isPeriod) {
                val prev = resolveDay(date.minusDays(1), state).isPeriod
                val next = resolveDay(date.plusDays(1), state).isPeriod
                when {
                    !prev && !next -> PeriodType.SINGLE
                    !prev && next -> PeriodType.START
                    prev && next -> PeriodType.MIDDLE
                    prev && !next -> PeriodType.END
                    else -> PeriodType.SINGLE
                }
            } else {
                PeriodType.NONE
            }

            val predictedType = if (data.isPredictedPeriod) {
                val prev = resolveDay(date.minusDays(1), state).isPredictedPeriod
                val next = resolveDay(date.plusDays(1), state).isPredictedPeriod
                when {
                    !prev && !next -> PeriodType.SINGLE
                    !prev && next -> PeriodType.START
                    prev && next -> PeriodType.MIDDLE
                    prev && !next -> PeriodType.END
                    else -> PeriodType.SINGLE
                }
            } else {
                PeriodType.NONE
            }

            days.add(
                CalendarDayUiModel(
                    date = date,
                    isCurrentMonth = date.month == yearMonth.month,
                    data = data,
                    periodType = type,
                    predictedPeriodType = predictedType
                )
            )
        }
        return days
    }
}

// Optimized Data Structures
sealed interface CalendarDataState {
    data object Loading : CalendarDataState
    data class Success(val data: Map<Long, DayData>, val cycles: List<Cycle> = emptyList(), val today: LocalDate = LocalDate.now(), val error: String? = null) : CalendarDataState
}

data class DayData(
    val isPeriod: Boolean = false,
    val isEstimatedPeriod: Boolean = false,
    val isPredictedPeriod: Boolean = false,
    val isFertile: Boolean = false,
    val isOvulation: Boolean = false,
    val hasLog: Boolean = false,
    val flowIntensity: Int = 0,
    val symptoms: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val notes: String = ""
)

data class CalendarDayUiModel(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val data: DayData,
    val periodType: PeriodType,
    val predictedPeriodType: PeriodType
)

enum class PeriodType { NONE, START, MIDDLE, END, SINGLE }
