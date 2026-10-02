package com.lunarlog.ui.util

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

val MediumDate: DateTimeFormatter get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
val ShortDayDate: DateTimeFormatter get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault())
val FullDayDate: DateTimeFormatter get() = ShortDayDate
