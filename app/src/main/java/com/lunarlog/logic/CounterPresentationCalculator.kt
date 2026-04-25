package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class CounterMode {
    PERIOD_DAYS_LEFT,
    PERIOD_OVERAGE,
    NEXT_PERIOD_COUNTDOWN,
    NEXT_PERIOD_OVERDUE
}

data class CounterPresentation(
    val value: Int,
    val mode: CounterMode,
    val title: String,
    val subtitle: String
)

object CounterPresentationCalculator {

    fun calculate(cycles: List<Cycle>, today: LocalDate = LocalDate.now()): CounterPresentation {
        if (cycles.isEmpty()) {
            return CounterPresentation(
                value = 0,
                mode = CounterMode.NEXT_PERIOD_COUNTDOWN,
                title = "Next Period",
                subtitle = "No cycle data yet"
            )
        }

        val sortedCycles = cycles.sortedByDescending { it.startDate }
        val latestCycle = sortedCycles.first()
        val averageCycleLength = CyclePredictionUtils.calculateAverageCycleLength(cycles)
        val averagePeriodLength = CyclePredictionUtils.calculateAveragePeriodLength(cycles)

        return if (latestCycle.endDate == null) {
            val elapsedPeriodDays = ChronoUnit.DAYS.between(latestCycle.startDate, today).toInt() + 1
            val daysLeft = averagePeriodLength - elapsedPeriodDays

            if (daysLeft >= 0) {
                CounterPresentation(
                    value = daysLeft,
                    mode = CounterMode.PERIOD_DAYS_LEFT,
                    title = "Period",
                    subtitle = if (daysLeft == 0) "Ending today" else "$daysLeft days left in period"
                )
            } else {
                val overage = abs(daysLeft)
                CounterPresentation(
                    value = overage,
                    mode = CounterMode.PERIOD_OVERAGE,
                    title = "Period",
                    subtitle = "$overage days over estimate"
                )
            }
        } else {
            val nextPeriodStart = CyclePredictionUtils.predictNextPeriodAfterLatestCycle(
                latestCycle,
                averageCycleLength,
                averagePeriodLength
            )
            val daysUntilNextPeriod = ChronoUnit.DAYS.between(today, nextPeriodStart).toInt()

            if (daysUntilNextPeriod >= 0) {
                CounterPresentation(
                    value = daysUntilNextPeriod,
                    mode = CounterMode.NEXT_PERIOD_COUNTDOWN,
                    title = "Next Period",
                    subtitle = if (daysUntilNextPeriod == 0) "Due today" else "$daysUntilNextPeriod days until period"
                )
            } else {
                val overdueDays = abs(daysUntilNextPeriod)
                CounterPresentation(
                    value = overdueDays,
                    mode = CounterMode.NEXT_PERIOD_OVERDUE,
                    title = "Next Period",
                    subtitle = "$overdueDays days overdue"
                )
            }
        }
    }
}
