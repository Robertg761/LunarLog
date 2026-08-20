package com.lunarlog.data

import javax.inject.Inject
import com.lunarlog.core.config.AppConfig
import com.lunarlog.core.model.Cycle
import com.lunarlog.logic.CyclePredictionUtils
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.room.withTransaction

enum class PeriodChangeAction {
    PERIOD_STARTED,
    PERIOD_ENDED,
    PERIOD_RESUMED,
    PERIOD_DAY_ADDED,
    PERIOD_DAY_REMOVED,
    CYCLE_DATES_UPDATED,
    NO_CHANGE
}

sealed class PeriodChangeResult {
    data class Success(val action: PeriodChangeAction, val message: String) : PeriodChangeResult()
    data class ValidationError(val message: String) : PeriodChangeResult()
}

class CycleRepository @Inject constructor(
    private val cycleDao: CycleDao,
    private val appDatabase: AppDatabase
) {
    suspend fun insertCycle(cycle: Cycle) = cycleDao.insertCycle(cycle)
    suspend fun updateCycle(cycle: Cycle) = cycleDao.updateCycle(cycle)
    suspend fun deleteCycle(cycle: Cycle) = cycleDao.deleteCycle(cycle)
    suspend fun getCycleById(id: Int): Cycle? = cycleDao.getCycleById(id)
    fun getAllCycles(): Flow<List<Cycle>> = cycleDao.getAllCycles()
    suspend fun getAllCyclesSync(): List<Cycle> = cycleDao.getAllCyclesSync()
    fun getCyclesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Cycle>> = cycleDao.getCyclesInRange(startDate, endDate)

    suspend fun startPeriod(date: LocalDate): PeriodChangeResult = runValidatedTransaction {
        if (date.isAfter(LocalDate.now())) {
            return@runValidatedTransaction PeriodChangeResult.ValidationError("Cannot start a period in the future")
        }

        val cycles = cycleDao.getAllCyclesSync()
        val containedInClosedCycle = cycles.any { cycle ->
            val end = cycle.endDate
            end != null && !date.isBefore(cycle.startDate) && !date.isAfter(end)
        }
        if (containedInClosedCycle) {
            return@runValidatedTransaction PeriodChangeResult.Success(
                action = PeriodChangeAction.NO_CHANGE,
                message = "Period already marked for this day"
            )
        }

        val ongoing = cycles.firstOrNull { it.endDate == null }
        if (ongoing != null) {
            if (date.isBefore(ongoing.startDate)) {
                return@runValidatedTransaction PeriodChangeResult.ValidationError("Start date cannot be before an ongoing period start")
            }

            val dayOfOngoingPeriod = ChronoUnit.DAYS.between(ongoing.startDate, date) + 1L
            if (dayOfOngoingPeriod <= AppConfig.MAX_PERIOD_LENGTH_DAYS) {
                // A believable continuation of the open period, not a new one.
                return@runValidatedTransaction PeriodChangeResult.Success(
                    action = PeriodChangeAction.NO_CHANGE,
                    message = "Period already marked for this day"
                )
            }

            // The open period was never ended and this start is too far out to be the same
            // bleeding episode, so it must be a new period. Cap the guessed end at the typical
            // period length instead of stretching it to the day before the new start —
            // otherwise the forgotten tap paints a whole cycle of period days on the calendar.
            // The end stays flagged as estimated so it never feeds the period-length average.
            val averagePeriodLength = CyclePredictionUtils.calculateAveragePeriodLength(cycles)
            val estimatedEnd = ongoing.startDate.plusDays((averagePeriodLength - 1).coerceAtLeast(0).toLong())
            val closed = ongoing.copy(
                endDate = minOf(date.minusDays(1), estimatedEnd),
                endEstimated = true
            )
            if (!isValidCycle(closed)) {
                return@runValidatedTransaction PeriodChangeResult.ValidationError("Invalid period range")
            }
            cycleDao.updateCycle(closed)
        }

        cycleDao.insertCycle(Cycle(startDate = date, endDate = null))
        requireValidCycleInvariants(cycleDao.getAllCyclesSync())

        PeriodChangeResult.Success(
            action = PeriodChangeAction.PERIOD_STARTED,
            message = "Period started"
        )
    }

    suspend fun endOngoingPeriod(date: LocalDate): PeriodChangeResult = runValidatedTransaction {
        if (date.isAfter(LocalDate.now())) {
            return@runValidatedTransaction PeriodChangeResult.ValidationError("Cannot end a period in the future")
        }

        val cycles = cycleDao.getAllCyclesSync()
        val ongoing = cycles.maxByOrNull { it.startDate }?.takeIf { it.endDate == null }
            ?: return@runValidatedTransaction PeriodChangeResult.Success(
                action = PeriodChangeAction.NO_CHANGE,
                message = "No ongoing period to end"
            )

        if (date.isBefore(ongoing.startDate)) {
            return@runValidatedTransaction PeriodChangeResult.ValidationError("End date cannot be before start date")
        }

        val updated = ongoing.copy(endDate = date, endEstimated = false)
        if (!isValidCycle(updated)) {
            return@runValidatedTransaction PeriodChangeResult.ValidationError("Invalid period range")
        }
        cycleDao.updateCycle(updated)

        PeriodChangeResult.Success(
            action = PeriodChangeAction.PERIOD_ENDED,
            message = "Period ended"
        )
    }

    suspend fun resumePeriodEndedOn(date: LocalDate): PeriodChangeResult = runValidatedTransaction {
        if (date.isAfter(LocalDate.now())) {
            return@runValidatedTransaction PeriodChangeResult.ValidationError("Cannot resume a future period")
        }

        val cycles = cycleDao.getAllCyclesSync()
        val target = cycles
            .filter { it.endDate == date }
            .maxByOrNull { it.startDate }
            ?: return@runValidatedTransaction PeriodChangeResult.Success(
                action = PeriodChangeAction.NO_CHANGE,
                message = "No period ending today to resume"
            )

        if (cycles.any { it.id != target.id && it.endDate == null }) {
            return@runValidatedTransaction PeriodChangeResult.ValidationError("Cannot resume while another period is ongoing")
        }

        val updated = target.copy(endDate = null, endEstimated = false)
        cycleDao.updateCycle(updated)
        requireValidCycleInvariants(cycleDao.getAllCyclesSync())

        PeriodChangeResult.Success(
            action = PeriodChangeAction.PERIOD_RESUMED,
            message = "Period resumed"
        )
    }

    suspend fun setPeriodDay(date: LocalDate, isPeriodDay: Boolean): PeriodChangeResult =
        runValidatedTransaction {
            if (date.isAfter(LocalDate.now())) {
                return@runValidatedTransaction PeriodChangeResult.ValidationError("Cannot modify future days")
            }
            if (isPeriodDay) addPeriodDay(date) else removePeriodDay(date)
        }

    suspend fun setPeriodRange(startDate: LocalDate, endDate: LocalDate): PeriodChangeResult =
        runValidatedTransaction {
            if (startDate.isAfter(endDate)) {
                return@runValidatedTransaction PeriodChangeResult.ValidationError("Start date must be on or before end date")
            }
            if (endDate.isAfter(LocalDate.now())) {
                return@runValidatedTransaction PeriodChangeResult.ValidationError("Cannot modify future days")
            }

            var day = startDate
            while (!day.isAfter(endDate)) {
                val dayResult = addPeriodDay(day)
                if (dayResult is PeriodChangeResult.ValidationError) {
                    throw CycleInvariantViolation(dayResult.message)
                }
                day = day.plusDays(1)
            }

            PeriodChangeResult.Success(
                action = PeriodChangeAction.PERIOD_DAY_ADDED,
                message = "Period logged"
            )
        }

    suspend fun updateCycleDates(cycleId: Int, startDate: LocalDate, endDate: LocalDate?): PeriodChangeResult =
        runValidatedTransaction {
            val today = LocalDate.now()
            if (startDate.isAfter(today) || endDate?.isAfter(today) == true) {
                return@runValidatedTransaction PeriodChangeResult.ValidationError("Cannot modify future days")
            }

            val cycle = cycleDao.getCycleById(cycleId)
                ?: return@runValidatedTransaction PeriodChangeResult.ValidationError("Period not found")

            val updated = cycle.copy(startDate = startDate, endDate = endDate, endEstimated = false)
            if (!isValidCycle(updated)) {
                return@runValidatedTransaction PeriodChangeResult.ValidationError("Start date must be on or before end date")
            }

            val others = cycleDao.getAllCyclesSync().filter { it.id != cycleId }
            if (hasOverlap(updated, others)) {
                return@runValidatedTransaction PeriodChangeResult.ValidationError("Updated dates overlap another period")
            }

            cycleDao.updateCycle(updated)
            PeriodChangeResult.Success(
                action = PeriodChangeAction.CYCLE_DATES_UPDATED,
                message = "Period dates updated"
            )
        }

    suspend fun isPeriodDay(date: LocalDate): Boolean {
        val cycles = cycleDao.getAllCyclesSync()
        return findCycleContainingDate(cycles, date) != null
    }

    private suspend fun addPeriodDay(date: LocalDate): PeriodChangeResult {
        val cycles = cycleDao.getAllCyclesSync()
        if (findCycleContainingDate(cycles, date) != null) {
            return PeriodChangeResult.Success(
                action = PeriodChangeAction.NO_CHANGE,
                message = "Day already marked as period"
            )
        }

        val previous = cycles.firstOrNull { it.endDate != null && it.endDate == date.minusDays(1) }
        val next = cycles.firstOrNull { it.startDate == date.plusDays(1) }

        when {
            previous != null && next != null && previous.id != next.id -> {
                val merged = previous.copy(endDate = next.endDate, endEstimated = next.endEstimated)
                if (!isValidCycle(merged)) {
                    return PeriodChangeResult.ValidationError("Invalid merged period")
                }
                cycleDao.updateCycle(merged)
                cycleDao.deleteCycle(next)
            }
            previous != null -> {
                val extended = previous.copy(endDate = date, endEstimated = false)
                if (!isValidCycle(extended)) {
                    return PeriodChangeResult.ValidationError("Invalid period range")
                }
                cycleDao.updateCycle(extended)
            }
            next != null -> {
                val extended = next.copy(startDate = date)
                if (!isValidCycle(extended)) {
                    return PeriodChangeResult.ValidationError("Invalid period range")
                }
                cycleDao.updateCycle(extended)
            }
            else -> {
                cycleDao.insertCycle(Cycle(startDate = date, endDate = date))
            }
        }

        requireValidCycleInvariants(cycleDao.getAllCyclesSync())

        return PeriodChangeResult.Success(
            action = PeriodChangeAction.PERIOD_DAY_ADDED,
            message = "Period day added"
        )
    }

    private suspend fun removePeriodDay(date: LocalDate): PeriodChangeResult {
        val cycles = cycleDao.getAllCyclesSync()
        val cycle = findCycleContainingDate(cycles, date)
            ?: return PeriodChangeResult.Success(
                action = PeriodChangeAction.NO_CHANGE,
                message = "Day is not marked as period"
            )

        val effectiveEnd = cycle.endDate ?: LocalDate.now()

        when {
            cycle.startDate == date && effectiveEnd == date -> {
                cycleDao.deleteCycle(cycle)
            }
            cycle.startDate == date -> {
                val updated = cycle.copy(startDate = date.plusDays(1))
                if (!isValidCycle(updated)) {
                    return PeriodChangeResult.ValidationError("Invalid period range")
                }
                cycleDao.updateCycle(updated)
            }
            effectiveEnd == date -> {
                val newEnd = date.minusDays(1)
                val updated = cycle.copy(
                    endDate = if (newEnd < cycle.startDate) cycle.startDate else newEnd,
                    endEstimated = false
                )
                if (!isValidCycle(updated)) {
                    return PeriodChangeResult.ValidationError("Invalid period range")
                }
                cycleDao.updateCycle(updated)
            }
            date.isAfter(cycle.startDate) && date.isBefore(effectiveEnd) -> {
                val left = cycle.copy(endDate = date.minusDays(1), endEstimated = false)
                val rightEnd: LocalDate? = cycle.endDate
                val right = Cycle(startDate = date.plusDays(1), endDate = rightEnd, endEstimated = cycle.endEstimated)
                if (!isValidCycle(left) || !isValidCycle(right)) {
                    return PeriodChangeResult.ValidationError("Invalid split period")
                }
                cycleDao.updateCycle(left)
                cycleDao.insertCycle(right)
            }
        }

        requireValidCycleInvariants(cycleDao.getAllCyclesSync())

        return PeriodChangeResult.Success(
            action = PeriodChangeAction.PERIOD_DAY_REMOVED,
            message = "Period day removed"
        )
    }

    private fun findCycleContainingDate(cycles: List<Cycle>, date: LocalDate): Cycle? {
        return cycles.firstOrNull { cycle ->
            val effectiveEnd = cycle.endDate ?: LocalDate.now()
            !date.isBefore(cycle.startDate) && !date.isAfter(effectiveEnd)
        }
    }

    private fun isValidCycle(cycle: Cycle): Boolean {
        val end = cycle.endDate ?: return true
        return !end.isBefore(cycle.startDate)
    }

    private fun hasOverlap(target: Cycle, others: List<Cycle>): Boolean {
        return others.any { overlaps(target, it) }
    }

    private fun overlaps(a: Cycle, b: Cycle): Boolean {
        val aEnd = a.endDate ?: LocalDate.MAX
        val bEnd = b.endDate ?: LocalDate.MAX
        return !aEnd.isBefore(b.startDate) && !bEnd.isBefore(a.startDate)
    }

    private fun validateAllCycleInvariants(cycles: List<Cycle>): PeriodChangeResult.ValidationError? {
        cycles.forEach { cycle ->
            if (!isValidCycle(cycle)) {
                return PeriodChangeResult.ValidationError("Invalid period range detected")
            }
        }

        val sorted = cycles.sortedBy { it.startDate }
        for (i in 0 until sorted.size - 1) {
            if (overlaps(sorted[i], sorted[i + 1])) {
                return PeriodChangeResult.ValidationError("Overlapping periods detected")
            }
        }
        return null
    }

    private fun requireValidCycleInvariants(cycles: List<Cycle>) {
        validateAllCycleInvariants(cycles)?.let { error ->
            throw CycleInvariantViolation(error.message)
        }
    }

    private suspend fun runValidatedTransaction(
        block: suspend () -> PeriodChangeResult
    ): PeriodChangeResult = try {
        appDatabase.withTransaction { block() }
    } catch (error: CycleInvariantViolation) {
        PeriodChangeResult.ValidationError(error.message ?: "Period data validation failed")
    }

    private class CycleInvariantViolation(message: String) : IllegalStateException(message)
}
