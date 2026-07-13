package com.lunarlog.logic

import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import java.time.LocalDate

object AdvancedCycleIntelligence {

    private const val MINIMUM_SHIFT_CELSIUS = 0.2

    /**
     * Attempts to detect ovulation date based on Basal Body Temperature (BBT) shift.
     * Uses a simplified "3 over 6" rule:
     * Ovulation is likely occurred the day BEFORE the temperature shift started.
     */
    fun detectOvulationFromBBT(cycleStartDate: LocalDate, logs: List<DailyLog>): LocalDate? {
        val cycleLogs = logs.filter { !it.date.isBefore(cycleStartDate) }
            .sortedBy { it.date }
            .mapNotNull { log ->
                normalizeToCelsius(log.temperature)?.let { normalized -> log to normalized }
            }

        if (cycleLogs.size < 9) return null

        for (i in 6 until cycleLogs.size - 2) {
            val window = cycleLogs.subList(i - 6, i + 3)
            val firstDate = window.first().first.date
            val isConsecutive = window.indices.all { index ->
                window[index].first.date == firstDate.plusDays(index.toLong())
            }
            if (!isConsecutive) continue

            val preShiftWindow = window.take(6).map { it.second }
            val postShiftWindow = window.takeLast(3).map { it.second }
            val coverLine = preShiftWindow.maxOrNull() ?: continue

            if (postShiftWindow.all { it >= coverLine + MINIMUM_SHIFT_CELSIUS }) {
                return window[5].first.date
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
        
        for (i in cycleLogs.indices.reversed()) {
            val candidate = cycleLogs[i]
            if (candidate.cervicalMucus < 3) continue

            val confirmation = cycleLogs.drop(i + 1).take(3)
            val hasThreeConsecutiveLowerDays = confirmation.size == 3 &&
                confirmation.indices.all { index ->
                    confirmation[index].date == candidate.date.plusDays((index + 1).toLong()) &&
                        confirmation[index].cervicalMucus < 3
                }
            if (hasThreeConsecutiveLowerDays) {
                return candidate.date
            }
        }

        return null
    }

    private fun normalizeToCelsius(value: Float?): Double? {
        val temperature = value?.toDouble() ?: return null
        return when (temperature) {
            in 34.0..43.0 -> temperature
            in 90.0..110.0 -> (temperature - 32.0) * 5.0 / 9.0
            else -> null
        }
    }
}
