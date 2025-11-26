package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import java.time.LocalDate

object AdvancedCycleIntelligence {

    /**
     * Attempts to detect ovulation date based on Basal Body Temperature (BBT) shift.
     * Uses a simplified "3 over 6" rule:
     * Ovulation is likely occurred the day BEFORE the temperature shift started.
     */
    fun detectOvulationFromBBT(cycleStartDate: LocalDate, logs: List<DailyLog>): LocalDate? {
        // Filter logs for this cycle
        val cycleLogs = logs.filter { !it.date.isBefore(cycleStartDate) }
            .sortedBy { it.date }
            .filter { it.temperature != null }

        if (cycleLogs.size < 10) return null // Need sufficient data points

        // Iterate to find a shift
        for (i in 6 until cycleLogs.size - 2) {
            val preShiftWindow = cycleLogs.subList(i - 6, i).mapNotNull { it.temperature }
            val postShiftWindow = cycleLogs.subList(i, i + 3).mapNotNull { it.temperature }

            if (preShiftWindow.size == 6 && postShiftWindow.size == 3) {
                val minPost = postShiftWindow.minOrNull() ?: continue

                // Check for shift (approx 0.2 C or 0.3 F, assuming user is consistent)
                // We use a generic 0.2 threshold assuming Celsius for now, or 0.4 for F.
                // To be safe, we check if minPost is strictly higher than maxPre
                val maxPre = preShiftWindow.maxOrNull() ?: continue
                
                if (minPost > maxPre) {
                    // Potential shift detected at index i
                    // Ovulation is usually the day of the last low temp (index i-1) or day of shift (index i)
                    // We'll return the day of the last low temp (index i-1)
                    // cycleLogs[i].date is the first high temp day. So day before is index i-1.
                    // But cycleLogs is filtered by temp != null, so index i-1 IS the last low temp day.
                    return cycleLogs[i].date.minusDays(1)
                }
            }
        }
        return null
    }

    /**
     * Detects "Peak Day" based on Cervical Mucus.
     * Peak Day is the last day of "Egg White" (4) or "Watery" (3) mucus before drying up.
     */
    fun detectPeakMucusDay(cycleStartDate: LocalDate, logs: List<DailyLog>): LocalDate? {
        val cycleLogs = logs.filter { !it.date.isBefore(cycleStartDate) }
            .sortedBy { it.date }
        
        // Find the last day with highly fertile mucus (3 or 4)
        // followed by a day of lower fertility (0, 1, 2)
        
        var potentialPeak: LocalDate? = null
        
        for (i in 0 until cycleLogs.size - 1) {
            val current = cycleLogs[i]
            val next = cycleLogs[i+1]
            
            if (current.cervicalMucus >= 3) {
                potentialPeak = current.date
            }
            
            // If we had a peak candidate, and now it's drying up, confirm it
            if (potentialPeak != null && next.cervicalMucus < 3 && next.date == current.date.plusDays(1)) {
                // Confirming this block as a peak
            }
        }
        
        return potentialPeak
    }
}