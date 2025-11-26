package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SymptomCorrelation(
    val symptom: String,
    val cycleDay: Int,
    val frequency: Float, // 0.0 to 1.0 (e.g., 0.8 means 80% of cycles have this symptom on this day)
    val totalOccurrences: Int
)

object SymptomCorrelationEngine {

    fun analyzeCorrelations(cycles: List<Cycle>, logs: List<DailyLog>): List<SymptomCorrelation> {
        if (cycles.isEmpty() || logs.isEmpty()) return emptyList()

        val sortedCycles = cycles.sortedBy { it.startDate }
        val symptomMap = mutableMapOf<String, MutableMap<Int, Int>>() // Symptom -> Day -> Count
        val cycleCountPerDay = mutableMapOf<Int, Int>() // Day -> How many cycles reached this day

        // 1. Map logs to cycle days
        for (log in logs) {
            val logDate = LocalDate.ofEpochDay(log.date)
            // Find the cycle this log belongs to
            val cycle = sortedCycles.findLast { it.startDate <= log.date } ?: continue
            
            val cycleStart = LocalDate.ofEpochDay(cycle.startDate)
            val dayOfCycle = ChronoUnit.DAYS.between(cycleStart, logDate).toInt() + 1

            if (dayOfCycle > 50) continue // Ignore extremely long outlier days

            // 2. Tally symptoms
            for (symptom in log.symptoms) {
                val dayMap = symptomMap.getOrPut(symptom) { mutableMapOf() }
                dayMap[dayOfCycle] = dayMap.getOrDefault(dayOfCycle, 0) + 1
            }
        }

        // 3. Calculate denominator (How many cycles actually *have* a Day X?)
        // E.g. If I have 5 cycles, but only 2 of them were 30 days long, then Day 30 statistics should be based on 2, not 5.
        // However, this is complex because we need to know if the cycle *ended* before that day.
        // Simplified approach: Count how many cycles have a duration >= X.
        val cycleLengths = sortedCycles.map { 
             val start = LocalDate.ofEpochDay(it.startDate)
             val end = if (it.endDate != null) LocalDate.ofEpochDay(it.endDate) else LocalDate.now()
             ChronoUnit.DAYS.between(start, end).toInt() + 1
        }
        
        // Find max cycle length to iterate
        val maxDay = cycleLengths.maxOrNull() ?: 28
        for (d in 1..maxDay) {
            cycleCountPerDay[d] = cycleLengths.count { it >= d }
        }

        // 4. Generate results
        val results = mutableListOf<SymptomCorrelation>()
        for ((symptom, dayMap) in symptomMap) {
            for ((day, count) in dayMap) {
                val denominator = cycleCountPerDay[day] ?: 0
                if (denominator >= 3) { // Minimum 3 cycles to form a pattern
                    val frequency = count.toFloat() / denominator
                    if (frequency >= 0.5f) { // Only report if it happens > 50% of the time
                        results.add(SymptomCorrelation(symptom, day, frequency, count))
                    }
                }
            }
        }

        return results.sortedByDescending { it.frequency }
    }
}
