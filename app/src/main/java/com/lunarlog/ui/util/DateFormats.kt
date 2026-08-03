package com.lunarlog.ui.util

import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared date patterns for the UI layer.
 *
 * Each screen used to build its own `DateTimeFormatter.ofPattern(...)`, which drifted (`MMM dd,
 * yyyy` vs `MMM d, yyyy` vs raw ISO) and pinned some screens to `Locale.US`. These use the default
 * locale so dates follow the device.
 */

/** e.g. "Jun 01, 2026" — for list rows and headers. */
val MediumDate: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())

/** e.g. "Mon, Jun 01" — where the weekday matters more than the year. */
val ShortDayDate: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM dd", Locale.getDefault())

/** e.g. "Monday, Jun 1" — for a single focused date, such as the calendar's day preview. */
val FullDayDate: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
