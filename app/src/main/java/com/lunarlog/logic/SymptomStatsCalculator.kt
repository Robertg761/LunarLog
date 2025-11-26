package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object SymptomStatsCalculator {

    fun getTopSymptomsForPhase(currentDay: Int, cycles: List<Cycle>, logs: List<DailyLog>): List<String> {
        if (cycles.isEmpty() || logs.isEmpty()) return emptyList()

        val phaseRange = when (currentDay) {
            in 1..5 -> 1..5       // Menstrual
            in 6..13 -> 6..13     // Follicular
            in 14..17 -> 14..17   // Ovulation (Approx)
            else -> 18..35        // Luteal
        }

        // Optimization: Index logs by date for O(1) lookup
        // Only include logs that actually have symptoms to save memory/time
        val logsByDate = logs.asSequence()
            .filter { it.symptoms.isNotEmpty() }
            .associateBy { it.date }

        val symptomCounts = mutableMapOf<String, Int>()
        
        // Iterate through all past cycles
        for (cycle in cycles) {
            val cycleStartDate = cycle.startDate
            
            // For each day in the target phase
            for (day in phaseRange) {
                // Calculate the specific date for this cycle day (Day 1 is start date, so plus days is day-1)
                val dateToCheck = cycleStartDate.plusDays((day - 1).toLong())

                // Check if this date is within the cycle's actual duration (if ended)
                // or generally valid if open (though we usually only look at past data)
                val cycleEndDate = cycle.endDate
                if (cycleEndDate != null && dateToCheck.isAfter(cycleEndDate)) {
                    continue // This day didn't exist in this cycle (short cycle)
                }

                // Lookup log
                val log = logsByDate[dateToCheck]
                if (log != null) {
                    for (symptom in log.symptoms) {
                        symptomCounts[symptom] = symptomCounts.getOrDefault(symptom, 0) + 1
                    }
                }
            }
        }

        return symptomCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }
}