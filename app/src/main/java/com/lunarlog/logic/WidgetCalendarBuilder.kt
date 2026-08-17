package com.lunarlog.logic

import com.lunarlog.core.config.AppConfig
import com.lunarlog.core.model.Cycle
import java.time.LocalDate
import java.time.YearMonth

/**
 * The single state a widget calendar cell is painted in.
 *
 * `CalendarViewModel` keeps these as independent booleans on `DayData` because the full calendar
 * layers them (a fertile pill *and* a log dot on the same date). A widget cell is 20-odd dp with no
 * room to layer, so this collapses them to one value; [WidgetDayMark.rank] defines which wins.
 */
enum class WidgetDayMark {
    NONE,
    PREDICTED_PERIOD,
    FERTILE,
    OVULATION,
    PERIOD;

    /**
     * Recorded bleeding outranks every estimate, and the single ovulation day outranks the fertile
     * window it sits inside. Declaration order above is the ranking, so this is just the ordinal.
     */
    val rank: Int get() = ordinal
}

data class WidgetCalendarDay(
    val date: LocalDate,
    /** False for the leading/trailing days that pad the grid out to whole weeks. */
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val mark: WidgetDayMark
)

data class WidgetCalendarMonth(
    val yearMonth: YearMonth,
    /** Five or six rows of seven, Sunday-first. */
    val weeks: List<List<WidgetCalendarDay>>
)

/**
 * Builds one month of cycle marks for the calendar widget.
 *
 * This deliberately mirrors `CalendarViewModel.computeCalendarData` — same Sunday-first grid, same
 * prediction sources, same "recorded period beats prediction" rule — so the widget and the in-app
 * calendar never disagree about a date. It differs in two ways, both because a widget rebuild runs
 * on the main-thread-adjacent Glance path on every host refresh: it ignores daily logs (the widget
 * draws no log dots) and it only projects predictions across the requested month instead of the
 * next twelve.
 */
object WidgetCalendarBuilder {

    private const val DAYS_PER_WEEK = 7

    /**
     * How far either side of the grid the projection has to reach.
     *
     * A predicted cycle's marks are not all at or after its start: the fertile window sits
     * [AppConfig.DEFAULT_LUTEAL_PHASE_LENGTH] + [AppConfig.FERTILE_WINDOW_OFFSET_START] days *before*
     * it. So a predicted start up to 19 days past the end of the grid still paints days inside it —
     * stopping the loop at `windowEnd` loses the fertile window of the next period, which is the one
     * the current month is usually leading up to. Rounded up generously; `mark` clamps to the window
     * anyway, so an extra iteration costs nothing but the loop.
     */
    private const val PROJECTION_MARGIN_DAYS = 30L

    fun build(
        cycles: List<Cycle>,
        yearMonth: YearMonth,
        today: LocalDate = LocalDate.now()
    ): WidgetCalendarMonth {
        val firstOfMonth = yearMonth.atDay(1)

        // `dayOfWeek.value` is Monday=1..Sunday=7, so `% 7` maps Sunday to 0 — a Sunday-first grid,
        // matching CalendarScreen.
        val startOffset = firstOfMonth.dayOfWeek.value % DAYS_PER_WEEK
        val weekCount = ceilDiv(startOffset + yearMonth.lengthOfMonth(), DAYS_PER_WEEK)
        val windowStart = firstOfMonth.minusDays(startOffset.toLong())
        val windowEnd = windowStart.plusDays((weekCount * DAYS_PER_WEEK - 1).toLong())

        val marks = buildMarks(cycles, windowStart, windowEnd, today)

        val weeks = (0 until weekCount).map { week ->
            (0 until DAYS_PER_WEEK).map { dayOfWeek ->
                val date = windowStart.plusDays((week * DAYS_PER_WEEK + dayOfWeek).toLong())
                WidgetCalendarDay(
                    date = date,
                    isCurrentMonth = YearMonth.from(date) == yearMonth,
                    isToday = date == today,
                    mark = marks[date.toEpochDay()] ?: WidgetDayMark.NONE
                )
            }
        }

        return WidgetCalendarMonth(yearMonth = yearMonth, weeks = weeks)
    }

    private fun buildMarks(
        cycles: List<Cycle>,
        windowStart: LocalDate,
        windowEnd: LocalDate,
        today: LocalDate
    ): Map<Long, WidgetDayMark> {
        val marks = HashMap<Long, WidgetDayMark>()

        fun mark(date: LocalDate, value: WidgetDayMark) {
            if (date < windowStart || date > windowEnd) return
            val key = date.toEpochDay()
            val existing = marks[key]
            if (existing == null || value.rank > existing.rank) {
                marks[key] = value
            }
        }

        fun markRange(from: LocalDate, to: LocalDate, value: WidgetDayMark) {
            var date = maxOf(from, windowStart)
            val last = minOf(to, windowEnd)
            while (date <= last) {
                mark(date, value)
                date = date.plusDays(1)
            }
        }

        // Recorded periods. An ongoing cycle (no end date) is treated as running through today,
        // which is what the in-app calendar does.
        cycles.forEach { cycle ->
            val end = cycle.endDate ?: maxOf(today, cycle.startDate)
            markRange(cycle.startDate, end, WidgetDayMark.PERIOD)
        }

        if (cycles.isEmpty()) return marks

        val latestCycle = cycles.maxBy { it.startDate }
        val averageCycleLength = CyclePredictionUtils.calculateAverageCycleLength(cycles)
        val averagePeriodLength = CyclePredictionUtils.calculateAveragePeriodLength(cycles)

        var predictedStart = CyclePredictionUtils.predictNextPeriodAfterLatestCycle(
            latestCycle,
            averageCycleLength,
            averagePeriodLength
        )

        // Jump straight to the first prediction that could touch the window instead of stepping
        // through every cycle since the user's first record — otherwise scrolling the widget to a
        // month years ahead walks hundreds of iterations to get there.
        val skipFrom = windowStart.minusDays(PROJECTION_MARGIN_DAYS).toEpochDay()
        val stepsToSkip = (skipFrom - predictedStart.toEpochDay()) / averageCycleLength
        if (stepsToSkip > 0) {
            predictedStart = predictedStart.plusDays(stepsToSkip * averageCycleLength)
        }

        val projectUntil = windowEnd.plusDays(PROJECTION_MARGIN_DAYS)
        while (predictedStart <= projectUntil) {
            markRange(
                predictedStart,
                predictedStart.plusDays((averagePeriodLength - 1).coerceAtLeast(0).toLong()),
                WidgetDayMark.PREDICTED_PERIOD
            )

            val (fertileStart, fertileEnd) =
                CyclePredictionUtils.predictFertileWindow(predictedStart)
            markRange(fertileStart, fertileEnd, WidgetDayMark.FERTILE)
            mark(CyclePredictionUtils.predictOvulation(predictedStart), WidgetDayMark.OVULATION)

            predictedStart = predictedStart.plusDays(averageCycleLength.toLong())
        }

        return marks
    }

    private fun ceilDiv(value: Int, divisor: Int): Int = (value + divisor - 1) / divisor
}
