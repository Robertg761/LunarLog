package com.lunarlog.ui.util

/*
 * The words the app uses for the integer levels stored on a DailyLog.
 *
 * These lived as three private `flowLabel` copies (Calendar, DailyLogSummary) plus one screen that
 * skipped the mapping entirely and rendered the raw int — Log History showed "Flow: 3" where every
 * other surface showed "Flow: Medium". One copy means a level can never be worded two ways again.
 */

/** 0..4 as stored on `DailyLog.flowLevel`. */
fun flowLabel(level: Int): String = when (level) {
    1 -> "Spotting"
    2 -> "Light"
    3 -> "Medium"
    4 -> "Heavy"
    else -> "None"
}

/** 0..3 as stored on `DailyLog.sexDrive`. */
fun sexDriveLabel(level: Int): String = when (level) {
    1 -> "Low"
    2 -> "Medium"
    3 -> "High"
    else -> "None"
}

/** 0..4 as stored on `DailyLog.cervicalMucus`. */
fun mucusLabel(level: Int): String = when (level) {
    1 -> "Sticky"
    2 -> "Creamy"
    3 -> "Watery"
    4 -> "Egg White"
    else -> "None/Dry"
}
