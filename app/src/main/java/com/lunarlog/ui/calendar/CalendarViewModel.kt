package com.lunarlog.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.Cycle
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLog
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.logic.CyclePredictionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    val uiState: StateFlow<CalendarUiState> = combine(
        _currentMonth,
        cycleRepository.getAllCycles(),
        dailyLogRepository.getLogsForRange(0, Long.MAX_VALUE)
    ) { month, cycles, logs ->
        generateCalendarUiState(month, cycles, logs)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CalendarUiState.Loading
    )

    fun onPreviousMonth() {
        _currentMonth.update { it.minusMonths(1) }
    }

    fun onNextMonth() {
        _currentMonth.update { it.plusMonths(1) }
    }

    private fun generateCalendarUiState(
        month: YearMonth,
        cycles: List<Cycle>,
        logs: List<DailyLog>
    ): CalendarUiState {
        val days = mutableListOf<CalendarDayState>()
        
        val firstDayOfMonth = month.atDay(1)
        val daysInMonth = month.lengthOfMonth()
        
        // Adjust for start of week (Sunday = 0)
        // java.time DayOfWeek: Mon=1 ... Sun=7
        // We want Sun=0, Mon=1 ... Sat=6
        // So: (dayOfWeek.value % 7) gives Sun=0, Mon=1...
        val startOffset = firstDayOfMonth.dayOfWeek.value % 7
        
        // Previous month padding
        val prevMonth = month.minusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()
        for (i in 0 until startOffset) {
            val date = prevMonth.atDay(daysInPrevMonth - startOffset + 1 + i)
            days.add(createDayState(date, cycles, logs, false))
        }

        // Current month
        for (i in 1..daysInMonth) {
            val date = month.atDay(i)
            days.add(createDayState(date, cycles, logs, true))
        }

        // Next month padding
        // Ensure 6 rows (42 cells) to keep layout stable
        val remaining = 42 - days.size
        val nextMonth = month.plusMonths(1)
        for (i in 1..remaining) {
            val date = nextMonth.atDay(i)
            days.add(createDayState(date, cycles, logs, false))
        }

        return CalendarUiState.Success(
            currentMonth = month,
            days = days
        )
    }

    private fun createDayState(
        date: LocalDate, 
        cycles: List<Cycle>, 
        logs: List<DailyLog>, 
        isCurrentMonth: Boolean
    ): CalendarDayState {
        val epochDay = date.toEpochDay()
        
        // Confirmed Period
        val isPeriod = cycles.any { cycle ->
            val start = cycle.startDate
            val end = cycle.endDate
            if (end != null) {
                epochDay in start..end
            } else {
                epochDay == start
            }
        }
        
        // Predictions
        var isPredictedPeriod = false
        var isFertile = false
        var isOvulation = false

        if (cycles.isNotEmpty()) {
             // For simplicity, we just look ahead from the VERY LAST cycle.
             // A more robust implementation would project multiple future cycles.
             val sortedCycles = cycles.sortedBy { it.startDate }
             val latestCycle = sortedCycles.last()
             val avgLength = CyclePredictionUtils.calculateAverageCycleLength(cycles)
             
             // Project up to 3 cycles ahead to cover the displayed month
             var predictedStart = CyclePredictionUtils.predictNextPeriod(latestCycle, avgLength)
             
             // Loop a few times to find if this date matches a future cycle
             for (k in 0..2) {
                 val predictedEnd = predictedStart.plusDays(4) // Assume 5 days
                 
                 if (epochDay in predictedStart.toEpochDay()..predictedEnd.toEpochDay()) {
                     isPredictedPeriod = true
                 }
                 
                 val (fertileStart, fertileEnd) = CyclePredictionUtils.predictFertileWindow(predictedStart)
                 if (date >= fertileStart && date <= fertileEnd) {
                     isFertile = true
                 }
                 
                 // Ovulation is roughly 14 days before period start
                 val ovulationDate = predictedStart.minusDays(14)
                 if (date == ovulationDate) {
                     isOvulation = true
                 }
                 
                 // Move to next cycle for next iteration
                 predictedStart = predictedStart.plusDays(avgLength.toLong())
             }
        }
        
        // Don't predict if it's already a confirmed period
        if (isPeriod) {
            isPredictedPeriod = false
            // Fertility/Ovulation logic usually stops during period, but let's keep it simple
        }

        val hasLog = logs.any { it.date == epochDay }

        return CalendarDayState(
            date = date,
            isCurrentMonth = isCurrentMonth,
            isPeriod = isPeriod,
            isPredictedPeriod = isPredictedPeriod,
            isFertile = isFertile,
            isOvulation = isOvulation,
            hasLog = hasLog
        )
    }
}

sealed interface CalendarUiState {
    object Loading : CalendarUiState
    data class Success(
        val currentMonth: YearMonth,
        val days: List<CalendarDayState>
    ) : CalendarUiState
}

data class CalendarDayState(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isPeriod: Boolean = false,
    val isPredictedPeriod: Boolean = false,
    val isFertile: Boolean = false,
    val isOvulation: Boolean = false,
    val hasLog: Boolean = false
)
