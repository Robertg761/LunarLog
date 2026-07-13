package com.lunarlog.logic

import com.lunarlog.core.model.DailyLog
import java.time.LocalDate
import java.util.Locale

data class CycleSummary(
    val cycleId: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val length: Int,
    val narrative: String,
    val keyInsights: List<String>
)

data class WeeklyDigest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val narrative: String,
    val dominantMood: String?,
    val dominantSymptom: String?
)

object NarrativeGenerator {

    fun generateCycleSummary(
        interval: CompletedCycleInterval,
        logs: List<DailyLog>
    ): CycleSummary {
        val period = interval.period
        val startDate = interval.startDate
        val endDate = interval.endDate
        val length = interval.length
        
        // Filter logs specifically for this cycle
        val cycleLogs = logs.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }

        val narrativeBuilder = StringBuilder()
        val insights = mutableListOf<String>()

        // 1. Length Analysis
        narrativeBuilder.append("Cycle beginning $startDate lasted $length days. ")
        when {
            length < 21 -> insights.add("Short cycle length ($length days).")
            length > 35 -> insights.add("Long cycle length ($length days).")
            else -> narrativeBuilder.append("This is within the typical range. ")
        }

        // 2. Flow Analysis
        val heavyDays = cycleLogs.count { it.flowLevel >= 3 }
        if (heavyDays > 2) {
            insights.add("Experienced $heavyDays days of heavy flow.")
        }

        // 3. Symptom Highlights
        val commonSymptoms = cycleLogs.flatMap { it.symptoms }
            .groupingBy { it }
            .eachCount()
            .entries.sortedByDescending { it.value }
            .take(2)
        
        if (commonSymptoms.isNotEmpty()) {
            val symptomNames = commonSymptoms.joinToString(" and ") { it.key.lowercase() }
            narrativeBuilder.append("Frequent symptoms included $symptomNames. ")
        }

        // 4. Mood Highlights
        val commonMoods = cycleLogs.flatMap { it.mood }
            .groupingBy { it }
            .eachCount()
            .entries.sortedByDescending { it.value }
            .take(1)
        
        if (commonMoods.isNotEmpty()) {
            insights.add("Dominant mood: ${commonMoods.first().key}.")
        }

        return CycleSummary(
            cycleId = period.id,
            startDate = startDate,
            endDate = endDate,
            length = length,
            narrative = narrativeBuilder.toString(),
            keyInsights = insights
        )
    }

    fun generateWeeklyDigest(logs: List<DailyLog>, referenceDate: LocalDate = LocalDate.now()): WeeklyDigest {
        val endDay = referenceDate
        val startDay = referenceDate.minusDays(6)
        
        val weeklyLogs = logs.filter { !it.date.isBefore(startDay) && !it.date.isAfter(endDay) }
        
        if (weeklyLogs.isEmpty()) {
            return WeeklyDigest(
                startDate = startDay,
                endDate = referenceDate,
                narrative = "No logs recorded this week.",
                dominantMood = null,
                dominantSymptom = null
            )
        }

        val narrativeBuilder = StringBuilder()
        
        // Mood Analysis
        val moodCounts = weeklyLogs.flatMap { it.mood }
            .groupingBy { it }
            .eachCount()
        val dominantMood = moodCounts.maxByOrNull { it.value }?.key
        
        if (dominantMood != null) {
            narrativeBuilder.append("You mostly felt $dominantMood this week. ")
        } else {
            narrativeBuilder.append("Your mood was balanced. ")
        }

        // Symptom Analysis
        val symptomCounts = weeklyLogs.flatMap { it.symptoms }
            .groupingBy { it }
            .eachCount()
        val dominantSymptom = symptomCounts.maxByOrNull { it.value }?.key

        if (dominantSymptom != null) {
            narrativeBuilder.append("Top symptom was $dominantSymptom. ")
        }

        // Sleep Analysis
        val recordedSleep = weeklyLogs.map { it.sleepHours }.filter { it > 0f }
        if (recordedSleep.isNotEmpty()) {
            val avgSleep = recordedSleep.average()
            narrativeBuilder.append(String.format(Locale.getDefault(), "Average sleep: %.1f hours.", avgSleep))
        }

        return WeeklyDigest(
            startDate = startDay,
            endDate = referenceDate,
            narrative = narrativeBuilder.toString(),
            dominantMood = dominantMood,
            dominantSymptom = dominantSymptom
        )
    }
}
