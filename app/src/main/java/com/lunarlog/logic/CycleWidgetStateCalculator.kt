package com.lunarlog.logic

import com.lunarlog.core.config.AppConfig
import com.lunarlog.core.model.Cycle
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Which part of the cycle today falls in, as far as the recorded data and the app's own estimates
 * can tell.
 *
 * [FERTILE] and [OVULATION] are *predictions* derived from average cycle length, never observed
 * signals — see the note on [CycleRingState.supporting].
 */
enum class CyclePhase {
    PERIOD,
    FOLLICULAR,
    FERTILE,
    OVULATION,
    LUTEAL,

    /** No cycles recorded yet, so there is nothing to place today against. */
    UNKNOWN
}

/**
 * Everything the cycle-ring widget draws, computed away from Glance so it can be unit-tested.
 *
 * [CounterPresentationCalculator] already answers "how many days until/left", which is what the
 * log-period widget and the home counter show. This adds the two things a ring needs and that one
 * does not carry: where today sits as a *fraction* of the whole cycle, and which phase that is.
 */
data class CycleRingState(
    /** 1-based day of the current cycle; 0 only when [phase] is [CyclePhase.UNKNOWN]. */
    val cycleDay: Int,
    /** The user's average cycle length, or [AppConfig.DEFAULT_CYCLE_LENGTH] before there is enough history. */
    val cycleLength: Int,
    val phase: CyclePhase,
    val phaseLabel: String,
    /** How far through the expected cycle today sits, clamped to 0f..1f so an overdue period fills the ring rather than overflowing it. */
    val progress: Float,
    /** Negative once the predicted date has passed; null when [phase] is [CyclePhase.UNKNOWN]. */
    val daysUntilNextPeriod: Int?,
    /**
     * Cycle-day ranges the ring paints as coloured arcs, all 1-based and clamped into
     * `1..cycleLength` so an unusual average can't sweep an arc past the top of the circle.
     * Empty ranges mean "don't draw that arc".
     */
    val periodDays: IntRange,
    val fertileDays: IntRange,
    /** Cycle day of predicted ovulation, or null when it falls outside this cycle. */
    val ovulationDay: Int?,
    /**
     * One line of context under the day number.
     *
     * Fertility wording stays hedged ("Estimated ...") for the same reason the calendar and home
     * screens do: these are averages projected forward, not observed ovulation, and must not read
     * as a statement about contraceptive safety.
     */
    val supporting: String
)

object CycleWidgetStateCalculator {

    fun calculate(cycles: List<Cycle>, today: LocalDate = LocalDate.now()): CycleRingState {
        if (cycles.isEmpty()) {
            return CycleRingState(
                cycleDay = 0,
                cycleLength = AppConfig.DEFAULT_CYCLE_LENGTH,
                phase = CyclePhase.UNKNOWN,
                phaseLabel = "No data",
                progress = 0f,
                daysUntilNextPeriod = null,
                periodDays = IntRange.EMPTY,
                fertileDays = IntRange.EMPTY,
                ovulationDay = null,
                supporting = "Log a period to start tracking"
            )
        }

        val latestCycle = cycles.maxBy { it.startDate }
        val averageCycleLength = CyclePredictionUtils.calculateAverageCycleLength(cycles)
        val averagePeriodLength = CyclePredictionUtils.calculateAveragePeriodLength(cycles)
        val nextPeriodStart = CyclePredictionUtils.predictNextPeriodAfterLatestCycle(
            latestCycle,
            averageCycleLength,
            averagePeriodLength
        )

        // Day 1 is the first day of bleeding, so a `today` that predates the latest recorded start
        // — a back-dated entry, or a device clock that moved backwards — has no meaningful day
        // number. Clamp rather than render a 0 or a negative in a 2x2 cell.
        val cycleDay = (ChronoUnit.DAYS.between(latestCycle.startDate, today).toInt() + 1)
            .coerceAtLeast(1)
        val daysUntilNextPeriod = ChronoUnit.DAYS.between(today, nextPeriodStart).toInt()

        // An ongoing cycle has no end date; the calendar treats it as running through today, and so
        // does this. Checking every cycle rather than just the latest keeps an out-of-order or
        // overlapping record from reading as "not bleeding".
        val containingCycle = cycles.firstOrNull { cycle ->
            today >= cycle.startDate && today <= (cycle.endDate ?: today)
        }

        val ovulation = CyclePredictionUtils.predictOvulation(nextPeriodStart)
        val (fertileStart, fertileEnd) = CyclePredictionUtils.predictFertileWindow(nextPeriodStart)

        val phase = when {
            containingCycle != null -> CyclePhase.PERIOD
            today == ovulation -> CyclePhase.OVULATION
            today >= fertileStart && today <= fertileEnd -> CyclePhase.FERTILE
            today < fertileStart -> CyclePhase.FOLLICULAR
            else -> CyclePhase.LUTEAL
        }

        val supporting = when (phase) {
            CyclePhase.PERIOD -> {
                val periodDay =
                    ChronoUnit.DAYS.between(containingCycle!!.startDate, today).toInt() + 1
                "Period day $periodDay"
            }
            CyclePhase.OVULATION -> "Estimated ovulation today"
            CyclePhase.FERTILE -> "Estimated fertile window"
            CyclePhase.FOLLICULAR, CyclePhase.LUTEAL -> when {
                daysUntilNextPeriod > 1 -> "$daysUntilNextPeriod days to next period"
                daysUntilNextPeriod == 1 -> "Period expected tomorrow"
                daysUntilNextPeriod == 0 -> "Period expected today"
                else -> "${abs(daysUntilNextPeriod)} days overdue"
            }
            CyclePhase.UNKNOWN -> ""
        }

        val cycleLength = averageCycleLength.coerceAtLeast(1)

        // The ring's arcs are positions within *this* cycle, so convert the predicted dates into
        // 1-based cycle days. `predictNextPeriodAfterLatestCycle` can push the next start past
        // `startDate + averageCycleLength` when the recorded end date implies a longer gap, which
        // would place ovulation beyond the ring — hence the clamp in `cycleDaysBetween`.
        val cycleDayOf: (LocalDate) -> Int = { date ->
            ChronoUnit.DAYS.between(latestCycle.startDate, date).toInt() + 1
        }

        return CycleRingState(
            cycleDay = cycleDay,
            cycleLength = cycleLength,
            phase = phase,
            phaseLabel = phase.label,
            progress = (cycleDay.toFloat() / cycleLength).coerceIn(0f, 1f),
            daysUntilNextPeriod = daysUntilNextPeriod,
            periodDays = clampToCycle(1, averagePeriodLength, cycleLength),
            fertileDays = clampToCycle(
                cycleDayOf(fertileStart),
                cycleDayOf(fertileEnd),
                cycleLength
            ),
            ovulationDay = cycleDayOf(ovulation).takeIf { it in 1..cycleLength },
            supporting = supporting
        )
    }

    /**
     * Intersects a cycle-day range with `1..cycleLength`, returning [IntRange.EMPTY] when the two
     * do not overlap at all.
     */
    private fun clampToCycle(first: Int, last: Int, cycleLength: Int): IntRange {
        val start = first.coerceAtLeast(1)
        val end = last.coerceAtMost(cycleLength)
        return if (start > end) IntRange.EMPTY else start..end
    }

    private val CyclePhase.label: String
        get() = when (this) {
            CyclePhase.PERIOD -> "Period"
            CyclePhase.FOLLICULAR -> "Follicular"
            CyclePhase.FERTILE -> "Fertile"
            CyclePhase.OVULATION -> "Ovulation"
            CyclePhase.LUTEAL -> "Luteal"
            CyclePhase.UNKNOWN -> "No data"
        }
}
