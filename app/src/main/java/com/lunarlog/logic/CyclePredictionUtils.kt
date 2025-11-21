package com.lunarlog.logic

import com.lunarlog.data.Cycle
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object CyclePredictionUtils {

    fun calculateAverageCycleLength(cycles: List<Cycle>): Int {
        if (cycles.size < 2) return 28 // Default to 28 if not enough data

        // We need consecutive cycles to calculate lengths.
        // However, standard calculation usually just looks at completed cycles.
        // A cycle length is typically defined as days from start of one period to the start of the next.
        // So if we have N cycles sorted by date, we can calculate N-1 intervals.

        // Assuming cycles are passed ordered by startDate DESC (most recent first)
        val sortedCycles = cycles.sortedByDescending { it.startDate }

        val lengths = mutableListOf<Int>()
        for (i in 0 until sortedCycles.size - 1) {
            val currentCycleStart = sortedCycles[i].startDate
            val previousCycleStart = sortedCycles[i+1].startDate

            val length = ChronoUnit.DAYS.between(previousCycleStart, currentCycleStart).toInt()
            // Filter out unreasonable lengths (e.g. < 10 or > 100 might be skipped intervals or errors,
            // but for basic logic we'll keep it simple or cap it)
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
        return lastCycle.startDate.plusDays(averageLength.toLong())
    }

    fun predictFertileWindow(nextPeriodStart: LocalDate): Pair<LocalDate, LocalDate> {
        // Fertile window is typically 12-16 days before next period.
        // Ovulation is around 14 days before next period.
        // Window: 16 days before (start) to 12 days before (end)

        val start = nextPeriodStart.minusDays(16)
        val end = nextPeriodStart.minusDays(12)
        return Pair(start, end)
    }
}
