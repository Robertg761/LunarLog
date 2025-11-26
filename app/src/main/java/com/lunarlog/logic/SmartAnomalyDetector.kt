package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class AnomalyType {
    IRREGULAR, // Random variance
    SUDDEN_SHIFT, // A new stable normal (e.g., changed from 28 to 32 days)
    TRENDING_LONGER, // Getting longer every month
    TRENDING_SHORTER // Getting shorter every month
}

data class CycleAnomaly(
    val type: AnomalyType,
    val description: String,
    val severity: Int // 1 (Info) to 3 (Alert)
)

object SmartAnomalyDetector {

    fun detectAnomalies(cycles: List<Cycle>): List<CycleAnomaly> {
        val anomalies = mutableListOf<CycleAnomaly>()
        if (cycles.size < 4) return anomalies

        // Calculate lengths
        val sortedCycles = cycles.sortedBy { it.startDate }
        val lengths = mutableListOf<Int>()
        for (i in 0 until sortedCycles.size - 1) {
            val start = LocalDate.ofEpochDay(sortedCycles[i].startDate)
            val nextStart = LocalDate.ofEpochDay(sortedCycles[i+1].startDate)
            lengths.add(ChronoUnit.DAYS.between(start, nextStart).toInt())
        }

        if (lengths.size < 3) return anomalies

        // 1. Check for Irregularity (Standard Deviation)
        val sd = CyclePredictionUtils.calculateStandardDeviation(cycles)
        
        if (sd > 5.0) {
            anomalies.add(CycleAnomaly(
                AnomalyType.IRREGULAR,
                "Your cycle length varies significantly (approx +/- ${sd.toInt()} days). Predictions may be less accurate.",
                2
            ))
        }

        // 2. Check for Sudden Shift
        // Compare last 3 cycles vs previous history
        if (lengths.size >= 6) {
            val last3 = lengths.takeLast(3)
            val history = lengths.dropLast(3)
            val historyMean = history.average()
            val recentMean = last3.average()

            if (Math.abs(recentMean - historyMean) >= 4) {
                 val direction = if (recentMean > historyMean) "longer" else "shorter"
                 anomalies.add(CycleAnomaly(
                     AnomalyType.SUDDEN_SHIFT,
                     "Your last 3 cycles have been consistently $direction (${recentMean.toInt()} days) than your usual average (${historyMean.toInt()} days).",
                     3
                 ))
            }
        }

        // 3. Check for Trending (Monotonic increase/decrease)
        // Last 3 cycles
        val last3 = lengths.takeLast(3)
        if (last3[0] < last3[1] && last3[1] < last3[2]) {
             anomalies.add(CycleAnomaly(
                 AnomalyType.TRENDING_LONGER,
                 "Your cycle has been getting longer for the last 3 months.",
                 1
             ))
        } else if (last3[0] > last3[1] && last3[1] > last3[2]) {
             anomalies.add(CycleAnomaly(
                 AnomalyType.TRENDING_SHORTER,
                 "Your cycle has been getting shorter for the last 3 months.",
                 1
             ))
        }

        return anomalies
    }
}
