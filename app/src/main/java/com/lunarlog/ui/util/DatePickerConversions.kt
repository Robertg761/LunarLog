package com.lunarlog.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 * M3's DatePicker speaks epoch millis at UTC, the app speaks LocalDate. These two conversions were
 * duplicated verbatim in Onboarding and Period Detail; a third screen adding a picker is exactly
 * where one of the copies would have drifted to the system zone and started landing on the wrong
 * day near midnight.
 */

/**
 * UTC, deliberately. `DatePickerState.selectedDateMillis` is documented as a UTC timestamp, so
 * converting through the device zone shifts the result by a day for anyone east or west of it.
 */
private val PickerZone: ZoneId = ZoneId.of("UTC")

/** A calendar date as the epoch-milli value a `DatePickerState` expects. */
fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(PickerZone).toInstant().toEpochMilli()

/** The inverse of [toPickerMillis], for `selectedDateMillis` coming back out. */
fun Long.toPickerLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(PickerZone).toLocalDate()
