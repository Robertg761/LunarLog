package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import com.lunarlog.core.config.AppConfig
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.sqrt

object CyclePredictionUtils {

    private fun getValidCycleLengths(cycles: List<Cycle>): List<Int> {
        if (cycles.size < 2) return emptyList()

        val sortedCycles = cycles.sortedByDescending { it.startDate }

        val lengths = mutableListOf<Int>()
        for (i in 0 until sortedCycles.size - 1) {
            val currentCycleStart = sortedCycles[i].startDate
            val previousCycleStart = sortedCycles[i+1].startDate

            val length = ChronoUnit.DAYS.between(previousCycleStart, currentCycleStart).toInt()
            if (length in 15..50) {
                 lengths.add(length)
            }
        }
        return lengths
    }

    fun calculateAverageCycleLength(cycles: List<Cycle>): Int {
        val lengths = getValidCycleLengths(cycles)
        return if (lengths.isEmpty()) {
            AppConfig.DEFAULT_CYCLE_LENGTH
        } else {
            lengths.average().toInt()
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
            lengths.average().toInt()
        }
    }

    fun calculateStandardDeviation(cycles: List<Cycle>): Double {
        val lengths = getValidCycleLengths(cycles)
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
