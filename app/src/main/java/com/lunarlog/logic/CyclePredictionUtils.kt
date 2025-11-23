package com.lunarlog.logic

import com.lunarlog.data.Cycle
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object CyclePredictionUtils {

    fun calculateAverageCycleLength(cycles: List<Cycle>): Int {
        if (cycles.size < 2) return 28 // Default to 28 if not enough data

        val sortedCycles = cycles.sortedByDescending { it.startDate }

        val lengths = mutableListOf<Int>()
        for (i in 0 until sortedCycles.size - 1) {
            val currentCycleStart = LocalDate.ofEpochDay(sortedCycles[i].startDate)
            val previousCycleStart = LocalDate.ofEpochDay(sortedCycles[i+1].startDate)

            val length = ChronoUnit.DAYS.between(previousCycleStart, currentCycleStart).toInt()
            if (length in 15..50) {
                 lengths.add(length)
            }
        }

        return if (lengths.isEmpty()) {
            28
        } else {
            lengths.average().toInt()
        }
    }

    fun predictNextPeriod(lastCycle: Cycle, averageLength: Int): LocalDate {
        return LocalDate.ofEpochDay(lastCycle.startDate).plusDays(averageLength.toLong())
    }

    fun predictFertileWindow(nextPeriodStart: LocalDate): Pair<LocalDate, LocalDate> {
        val start = nextPeriodStart.minusDays(16)
        val end = nextPeriodStart.minusDays(12)
        return Pair(start, end)
    }
}
