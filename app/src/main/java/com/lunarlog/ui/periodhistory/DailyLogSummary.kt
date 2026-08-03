package com.lunarlog.ui.periodhistory

import com.lunarlog.core.model.DailyLog
import com.lunarlog.ui.util.flowLabel
import com.lunarlog.ui.util.mucusLabel
import com.lunarlog.ui.util.sexDriveLabel
import java.util.Locale

data class DailyLogSummaryLines(
    val primary: String,
    val secondary: String
)

fun buildDailyLogSummaryLines(log: DailyLog): DailyLogSummaryLines {
    val primaryParts = mutableListOf<String>()
    if (log.flowLevel > 0) primaryParts += "Flow: ${flowLabel(log.flowLevel)}"
    if (log.mood.isNotEmpty()) primaryParts += "Mood: ${formatTopTwo(log.mood)}"
    if (log.symptoms.isNotEmpty()) primaryParts += "Symptoms: ${formatTopTwo(log.symptoms)}"

    val secondaryParts = mutableListOf<String>()
    if (log.waterIntake > 0) secondaryParts += "Water: ${log.waterIntake}"

    if (log.sleepHours > 0f || log.sleepQuality > 0) {
        val hoursPart = if (log.sleepHours > 0f) "${formatFloat1(log.sleepHours)}h" else ""
        val qualityPart = if (log.sleepQuality > 0) "(${log.sleepQuality}/5)" else ""
        val combined = listOf(hoursPart, qualityPart).filter { it.isNotBlank() }.joinToString(" ")
        secondaryParts += "Sleep: $combined"
    }

    if (log.sexDrive > 0) secondaryParts += "Libido: ${sexDriveLabel(log.sexDrive)}"
    if (log.temperature != null) secondaryParts += "Temp: ${formatFloat1(log.temperature)}"
    if (log.cervicalMucus > 0) secondaryParts += "Mucus: ${mucusLabel(log.cervicalMucus)}"
    if (log.notes.isNotBlank()) secondaryParts += "Notes"

    return DailyLogSummaryLines(
        primary = primaryParts.joinToString(" \u2022 "),
        secondary = secondaryParts.joinToString(" \u2022 ")
    )
}

private fun formatTopTwo(items: List<String>): String {
    if (items.isEmpty()) return ""
    val topTwo = items.take(2)
    val remaining = items.size - topTwo.size
    val base = topTwo.joinToString(", ")
    return if (remaining > 0) "$base +$remaining" else base
}

private fun formatFloat1(value: Float): String {
    val s = String.format(Locale.US, "%.1f", value)
    return if (s.endsWith(".0")) s.dropLast(2) else s
}

