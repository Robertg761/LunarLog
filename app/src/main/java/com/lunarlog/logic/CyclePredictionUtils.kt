package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import com.lunarlog.core.config.AppConfig
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class CompletedCycleInterval(
    val period: Cycle,
    val nextPeriodStart: LocalDate
) {
    val startDate: LocalDate = period.startDate
    val endDate: LocalDate = nextPeriodStart.minusDays(1)
    val length: Int = ChronoUnit.DAYS.between(startDate, nextPeriodStart).toInt()
}

object CyclePredictionUtils {

    fun completedCycleIntervals(cycles: List<Cycle>): List<CompletedCycleInterval> {
        val sortedCycles = cycles.sortedBy { it.startDate }
        return sortedCycles.zipWithNext()
            .map { (period, nextPeriod) ->
                CompletedCycleInterval(period, nextPeriod.startDate)
            }
            .filter { it.length > 0 }
    }

    fun calculateAverageCycleLength(cycles: List<Cycle>): Int {
        // Keep the historical prediction guardrail so a mistaken or unusually long
        // gap does not dominate the next-date estimate. Other analysis still sees it.
        val lengths = completedCycleIntervals(cycles)
            .map { it.length }
            .filter { it in 15..50 }
        return if (lengths.isEmpty()) {
            AppConfig.DEFAULT_CYCLE_LENGTH
        } else {
            // Round to nearest rather than truncate: flooring 29.5 to 29 systematically
            // biased predictions early, by up to a day.
            lengths.average().roundToInt()
        }
    }

    fun calculateAveragePeriodLength(cycles: List<Cycle>): Int {
        val lengths = cycles.mapNotNull { cycle ->
            cycle.endDate?.let { endDate ->
                val start = cycle.startDate
                val end = endDate
                ChronoUnit.DAYS.between(start, end).toInt() + 1
            }
        }.filter { it in 2..10 } // Basic sanity check for valid period lengths

        return if (lengths.isEmpty()) {
            AppConfig.AVERAGE_PERIOD_LENGTH_DEFAULT
        } else {
            // Round to nearest rather than truncate, so periods that usually run
            // 5.7 days count as 6, not 5.
            lengths.average().roundToInt()
        }
    }

    fun calculateStandardDeviation(cycles: List<Cycle>): Double {
        val lengths = completedCycleIntervals(cycles).map { it.length }
        if (lengths.size < 2) return 0.0

        val mean = lengths.average()
        val sumSquaredDiffs = lengths.map { (it - mean).pow(2) }.sum()
        val variance = sumSquaredDiffs / (lengths.size - 1)
        return sqrt(variance)
    }

    fun isCycleIrregular(cycles: List<Cycle>): Boolean {
        // Threshold: If SD > 5 days, consider it irregular.
        return calculateStandardDeviation(cycles) > 5.0
    }

    fun predictNextPeriod(lastCycle: Cycle, averageLength: Int): LocalDate {
        return lastCycle.startDate.plusDays(averageLength.toLong())
    }

    fun predictNextPeriodAfterLatestCycle(
        lastCycle: Cycle,
        averageCycleLength: Int,
        averagePeriodLength: Int
    ): LocalDate {
        val startBasedPrediction = predictNextPeriod(lastCycle, averageCycleLength)
        val endDate = lastCycle.endDate ?: return startBasedPrediction

        val expectedNonPeriodDays = (averageCycleLength - averagePeriodLength + 1).coerceAtLeast(1)
        val endBasedPrediction = endDate.plusDays(expectedNonPeriodDays.toLong())

        return maxOf(startBasedPrediction, endBasedPrediction)
    }

    fun predictOvulation(nextPeriodStart: LocalDate): LocalDate {
        return nextPeriodStart.minusDays(AppConfig.DEFAULT_LUTEAL_PHASE_LENGTH.toLong())
    }

    fun predictFertileWindow(nextPeriodStart: LocalDate): Pair<LocalDate, LocalDate> {
        val ovulation = predictOvulation(nextPeriodStart)
        val start = ovulation.minusDays(AppConfig.FERTILE_WINDOW_OFFSET_START)
        val end = ovulation.plusDays(AppConfig.FERTILE_WINDOW_OFFSET_END)
        return Pair(start, end)
    }
}
