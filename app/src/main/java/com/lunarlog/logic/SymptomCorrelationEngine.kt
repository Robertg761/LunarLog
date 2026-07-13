package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
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

        val intervals = CyclePredictionUtils.completedCycleIntervals(cycles)
        if (intervals.isEmpty()) return emptyList()

        val observedCyclesByDay = mutableMapOf<Int, MutableSet<java.time.LocalDate>>()
        val symptomCyclesByDay = mutableMapOf<String, MutableMap<Int, MutableSet<java.time.LocalDate>>>()

        for (log in logs) {
            val interval = intervals.findLast {
                !log.date.isBefore(it.startDate) && !log.date.isAfter(it.endDate)
            } ?: continue
            val dayOfCycle = ChronoUnit.DAYS.between(interval.startDate, log.date).toInt() + 1
            val cycleKey = interval.startDate

            observedCyclesByDay.getOrPut(dayOfCycle) { mutableSetOf() }.add(cycleKey)
            for (symptom in log.symptoms.distinct()) {
                symptomCyclesByDay
                    .getOrPut(symptom) { mutableMapOf() }
                    .getOrPut(dayOfCycle) { mutableSetOf() }
                    .add(cycleKey)
            }
        }

        val results = mutableListOf<SymptomCorrelation>()
        for ((symptom, dayMap) in symptomCyclesByDay) {
            for ((day, matchingCycles) in dayMap) {
                val observedCycles = observedCyclesByDay[day]?.size ?: 0
                if (observedCycles >= 3) {
                    val frequency = matchingCycles.size.toFloat() / observedCycles
                    if (frequency >= 0.5f) {
                        results.add(
                            SymptomCorrelation(
                                symptom = symptom,
                                cycleDay = day,
                                frequency = frequency,
                                totalOccurrences = matchingCycles.size
                            )
                        )
                    }
                }
            }
        }

        return results.sortedByDescending { it.frequency }
    }
}
