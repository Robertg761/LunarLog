package com.lunarlog.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.core.model.Cycle
import com.lunarlog.data.CycleRepository
import com.lunarlog.core.model.DailyLog
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.logic.CyclePredictionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    cycleRepository: CycleRepository,
    dailyLogRepository: DailyLogRepository
) : ViewModel() {

    // Global Source of Truth for the Calendar
    val calendarState: StateFlow<CalendarDataState> = combine(
        cycleRepository.getAllCycles(),
        dailyLogRepository.getAllLogs()
    ) { cycles, logs ->
        // Heavy computation on Default dispatcher
        computeCalendarData(cycles, logs)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CalendarDataState.Loading
    )

    private fun computeCalendarData(cycles: List<Cycle>, logs: List<DailyLog>): CalendarDataState {
        val dayMap = HashMap<Long, DayData>()

        // 1. Map Logs (Fast lookup)
        logs.forEach { log ->
            dayMap[log.date] = DayData(
                hasLog = true,
                flowIntensity = log.flowLevel
            )
        }

        // 2. Map Cycles (Actual)
        val sortedCycles = cycles.sortedBy { it.startDate }
        sortedCycles.forEach { cycle ->
            val start = cycle.startDate
            val end = cycle.endDate ?: start // Default to start if null (in progress)
            
            // If in progress, assume active for today (or handled by logic), 
            // but for historical view, we just render what's there.
            // For the "Current" cycle that has no end, let's assume it goes until Today if Today > Start
            val effectiveEnd = if (cycle.endDate == null) {
                maxOf(LocalDate.now().toEpochDay(), start)
            } else {
                end
            }

            for (day in start..effectiveEnd) {
                val current = dayMap[day] ?: DayData()
                dayMap[day] = current.copy(isPeriod = true)
            }
        }

        // 3. Predictions (Future)
        if (sortedCycles.isNotEmpty()) {
            val lastCycle = sortedCycles.last()
            val avgLength = CyclePredictionUtils.calculateAverageCycleLength(cycles)
            
            // Predict for next 12 months
            val predictionLimit = LocalDate.now().plusMonths(12).toEpochDay()
            
            var currentStart = CyclePredictionUtils.predictNextPeriod(lastCycle, avgLength)
            
            while (currentStart.toEpochDay() < predictionLimit) {
                // Period Prediction (Assuming 5 days for prediction visualization)
                val pStart = currentStart.toEpochDay()
                val pEnd = currentStart.plusDays(4).toEpochDay()
                
                for (day in pStart..pEnd) {
                    val current = dayMap[day] ?: DayData()
                    // Don't overwrite actual period data
                    if (!current.isPeriod) {
                        dayMap[day] = current.copy(isPredictedPeriod = true)
                    }
                }

                // Fertile Window Prediction
                val (fStart, fEnd) = CyclePredictionUtils.predictFertileWindow(currentStart)
                val ovDate = currentStart.minusDays(14).toEpochDay()
                
                for (day in fStart.toEpochDay()..fEnd.toEpochDay()) {
                    val current = dayMap[day] ?: DayData()
                    if (!current.isPeriod) { // Don't show fertility on period days
                        dayMap[day] = current.copy(
                            isFertile = true,
                            isOvulation = (day == ovDate)
                        )
                    }
                }

                currentStart = currentStart.plusDays(avgLength.toLong())
            }
        }

        // 4. Calculate PeriodType (Connectivity)
        // We iterate through the map's keys that have isPeriod = true
        // Optimally: Just iterate the cycles again? No, we need the map for random access
        // actually, we can do this lazily or in a second pass over the known period days.
        // For simplicity and speed in this cache building:
        // We will just let the UI determine connectivity based on neighbors in the map.
        // It's fast enough to look up (day-1) and (day+1) in the map.

        return CalendarDataState.Success(dayMap)
    }

    /**
     * Helper to get data for a specific month page (42 days)
     */
    fun getPageData(yearMonth: YearMonth, state: CalendarDataState): List<CalendarDayUiModel> {
        if (state !is CalendarDataState.Success) return emptyList()

        val firstDayOfMonth = yearMonth.atDay(1)
        val startOffset = firstDayOfMonth.dayOfWeek.value % 7
        val startDate = firstDayOfMonth.minusDays(startOffset.toLong())

        val days = ArrayList<CalendarDayUiModel>(42)
        
        for (i in 0 until 42) {
            val date = startDate.plusDays(i.toLong())
            val epoch = date.toEpochDay()
            val data = state.data[epoch] ?: DayData()

            // Calculate connectivity here for rendering
            val type = if (data.isPeriod) {
                val prev = state.data[epoch - 1]?.isPeriod == true
                val next = state.data[epoch + 1]?.isPeriod == true
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
                    periodType = type
                )
            )
        }
        return days
    }
}

// Optimized Data Structures
sealed interface CalendarDataState {
    data object Loading : CalendarDataState
    data class Success(val data: Map<Long, DayData>) : CalendarDataState
}

data class DayData(
    val isPeriod: Boolean = false,
    val isPredictedPeriod: Boolean = false,
    val isFertile: Boolean = false,
    val isOvulation: Boolean = false,
    val hasLog: Boolean = false,
    val flowIntensity: Int = 0
)

data class CalendarDayUiModel(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val data: DayData,
    val periodType: PeriodType
)

enum class PeriodType { NONE, START, MIDDLE, END, SINGLE }
