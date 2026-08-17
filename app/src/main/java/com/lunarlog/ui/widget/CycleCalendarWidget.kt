package com.lunarlog.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lunarlog.di.WidgetEntryPoint
import com.lunarlog.logic.WidgetCalendarBuilder
import com.lunarlog.logic.WidgetCalendarDay
import com.lunarlog.logic.WidgetCalendarMonth
import com.lunarlog.logic.WidgetDayMark
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * The current month as a compact grid, marked with recorded periods and the app's predictions.
 *
 * Marks come from [WidgetCalendarBuilder], which mirrors `CalendarViewModel` so this and the in-app
 * calendar cannot disagree about a date.
 *
 * The whole grid is one tap target rather than 42. Per-day taps into `lunarlog://details/{epochDay}`
 * would be nice, but each one is a separate `PendingIntent` inside the RemoteViews, and this widget
 * is already the heaviest of the five — One UI enforces the RemoteViews transaction budget more
 * tightly than AOSP, and a widget that exceeds it does not degrade, it fails to draw.
 */
class CycleCalendarWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        val cycles = withContext(Dispatchers.IO) {
            runCatching { entryPoint.cycleRepository().getAllCyclesSync() }.getOrDefault(emptyList())
        }

        val today = LocalDate.now()
        val month = WidgetCalendarBuilder.build(cycles, YearMonth.from(today), today)

        provideContent { CalendarContent(month) }
    }

    @Composable
    private fun CalendarContent(month: WidgetCalendarMonth) {
        val context = LocalContext.current
        val locale = context.resources.configuration.locales[0]
        val isLarge = LocalSize.current.height >= LARGE.height
        val dayFontSize = if (isLarge) 12 else 10

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .widgetSurface()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .clickable(deepLinkAction(context, "calendar"))
        ) {
            Text(
                text = monthTitle(month.yearMonth, locale),
                maxLines = 1,
                style = TextStyle(
                    color = WidgetColors.primary,
                    fontSize = if (isLarge) 15.sp else 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            WeekdayHeader(locale, dayFontSize)

            month.weeks.forEach { week ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    week.forEach { day -> DayCell(day, dayFontSize) }
                }
            }
        }
    }

    @Composable
    private fun WeekdayHeader(locale: Locale, fontSize: Int) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            // Sunday-first, matching CalendarScreen's `dayOfWeek.value % 7` offset.
            (0 until DAYS_PER_WEEK).forEach { index ->
                val label = DayOfWeek.SUNDAY.plus(index.toLong())
                    .getDisplayName(java.time.format.TextStyle.NARROW, locale)
                Text(
                    text = label,
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = WidgetColors.muted,
                        fontSize = (fontSize - 1).sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }

    /**
     * Two views per cell — a weighted wrapper that supplies the inter-cell gap, and the number
     * itself carrying the mark fill as its own background.
     *
     * Glance has no margin, only padding, and padding sits *inside* the background. So the gap has to
     * come from the wrapper: putting it on the number would paint the fill right up to its
     * neighbours' and turn the grid into one solid block. Unmarked days skip the background and
     * corner-radius modifiers entirely rather than painting surface-on-surface, which keeps ~30
     * needless RemoteViews actions out of a tree that already holds 42 cells.
     *
     * A [RowScope] extension because `defaultWeight` is scoped to the parent's content lambda — a
     * plain composable called from inside the `Row` cannot see it.
     */
    @Composable
    private fun RowScope.DayCell(day: WidgetCalendarDay, fontSize: Int) {
        val base = GlanceModifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .semantics { contentDescription = day.describe() }
        val modifier = if (day.mark == WidgetDayMark.NONE) {
            base
        } else {
            base.background(day.mark.container()).cornerRadius(10.dp)
        }

        Box(
            modifier = GlanceModifier.defaultWeight().padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${day.date.dayOfMonth}",
                maxLines = 1,
                modifier = modifier,
                style = TextStyle(
                    color = day.textColor(),
                    fontSize = fontSize.sp,
                    // Glance has no border modifier, so "today" is carried by weight and an
                    // underline instead of a ring — which also survives landing on a marked day,
                    // where a coloured ring would fight the fill.
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (day.isToday) TextDecoration.Underline else null,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    private fun monthTitle(yearMonth: YearMonth, locale: Locale): String {
        val month = yearMonth.month.getDisplayName(java.time.format.TextStyle.FULL, locale)
        return "$month ${yearMonth.year}"
    }

    private companion object {
        const val DAYS_PER_WEEK = 7

        val MEDIUM = DpSize(250.dp, 180.dp)
        val LARGE = DpSize(250.dp, 250.dp)

        fun WidgetDayMark.container(): ColorProvider = when (this) {
            WidgetDayMark.PERIOD -> WidgetColors.period
            WidgetDayMark.PREDICTED_PERIOD -> WidgetColors.predictedPeriod
            WidgetDayMark.FERTILE -> WidgetColors.fertile
            WidgetDayMark.OVULATION -> WidgetColors.ovulation
            // Never actually drawn — DayCell omits the background modifier for unmarked days.
            WidgetDayMark.NONE -> WidgetColors.surface
        }

        fun WidgetCalendarDay.textColor(): ColorProvider = when {
            // Padding days from the neighbouring months stay recessive whatever their mark, so the
            // month being shown is the one that reads first.
            !isCurrentMonth -> WidgetColors.outline
            mark == WidgetDayMark.PERIOD -> WidgetColors.onPeriod
            mark == WidgetDayMark.FERTILE -> WidgetColors.onFertile
            mark == WidgetDayMark.OVULATION -> WidgetColors.onOvulation
            mark == WidgetDayMark.PREDICTED_PERIOD -> WidgetColors.muted
            isToday -> WidgetColors.today
            else -> WidgetColors.onSurface
        }

        /**
         * A bare day number tells a screen reader nothing about the colour it is sitting on, and the
         * predictions in particular have to be announced as estimates.
         */
        fun WidgetCalendarDay.describe(): String {
            val state = when (mark) {
                WidgetDayMark.PERIOD -> "period"
                WidgetDayMark.PREDICTED_PERIOD -> "predicted period"
                WidgetDayMark.FERTILE -> "estimated fertile window"
                WidgetDayMark.OVULATION -> "estimated ovulation"
                WidgetDayMark.NONE -> null
            }
            return listOfNotNull(
                date.dayOfMonth.toString(),
                "today".takeIf { isToday },
                state
            ).joinToString(", ")
        }
    }
}

class CycleCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CycleCalendarWidget()
}
