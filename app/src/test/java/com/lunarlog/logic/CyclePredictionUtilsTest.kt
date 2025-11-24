package com.lunarlog.logic

import com.lunarlog.data.Cycle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CyclePredictionUtilsTest {

    @Test
    fun calculateAverageCycleLength_returnsDefault_whenNotEnoughData() {
        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2023, 1, 1).toEpochDay())
        )
        val result = CyclePredictionUtils.calculateAverageCycleLength(cycles)
        assertEquals(28, result)
    }

    @Test
    fun calculateAverageCycleLength_calculatesCorrectly() {
        // Cycle 1: Jan 1
        // Cycle 2: Feb 1 (31 days later)
        // Cycle 3: Mar 1 (28 days later)
        // Average: (31 + 28) / 2 = 29.5 -> 29

        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2023, 3, 1).toEpochDay()),
            Cycle(startDate = LocalDate.of(2023, 2, 1).toEpochDay()),
            Cycle(startDate = LocalDate.of(2023, 1, 1).toEpochDay())
        )

        val result = CyclePredictionUtils.calculateAverageCycleLength(cycles)
        assertEquals(29, result)
    }

    @Test
    fun predictNextPeriod_addsAverageLength() {
        val lastCycle = Cycle(startDate = LocalDate.of(2023, 1, 1).toEpochDay())
        val averageLength = 30

        val result = CyclePredictionUtils.predictNextPeriod(lastCycle, averageLength)
        assertEquals(LocalDate.of(2023, 1, 31), result)
    }

    @Test
    fun predictFertileWindow_calculatesCorrectWindow() {
        // Next period: Jan 20
        // Ovulation: Jan 20 - 14 = Jan 6
        // Window start: Jan 6 - 5 = Jan 1
        // Window end: Jan 6 + 1 = Jan 7

        val nextPeriod = LocalDate.of(2023, 1, 20)
        val (start, end) = CyclePredictionUtils.predictFertileWindow(nextPeriod)

        assertEquals(LocalDate.of(2023, 1, 1), start)
        assertEquals(LocalDate.of(2023, 1, 7), end)
    }

    @Test
    fun calculateStandardDeviation_returnsZero_forSingleOrNoCycle() {
        val cycles = listOf(Cycle(startDate = LocalDate.of(2023, 1, 1).toEpochDay()))
        assertEquals(0.0, CyclePredictionUtils.calculateStandardDeviation(cycles), 0.01)
    }

    @Test
    fun calculateStandardDeviation_calculatesCorrectly() {
        // Cycles: Jan 1, Feb 1 (31), Mar 1 (28), Apr 1 (31)
        // Lengths: 31, 28, 31
        // Mean: 30
        // Variance: ((31-30)^2 + (28-30)^2 + (31-30)^2) / (3-1)
        // = (1 + 4 + 1) / 2 = 6 / 2 = 3
        // SD = sqrt(3) ~= 1.732

        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2023, 4, 1).toEpochDay()),
            Cycle(startDate = LocalDate.of(2023, 3, 1).toEpochDay()),
            Cycle(startDate = LocalDate.of(2023, 2, 1).toEpochDay()),
            Cycle(startDate = LocalDate.of(2023, 1, 1).toEpochDay())
        )

        assertEquals(1.732, CyclePredictionUtils.calculateStandardDeviation(cycles), 0.01)
    }

    @Test
    fun isCycleIrregular_detectsIrregularity() {
        // High variance
        // Lengths: 21, 35, 21, 35
        // Mean: 28
        // Variance: (49 + 49 + 49 + 49) / 3 = 196 / 3 = 65.33
        // SD: 8.08
        // Threshold: 5.0 (Assumption)

        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2023, 5, 1).toEpochDay()), // Gap
            Cycle(startDate = LocalDate.of(2023, 3, 27).toEpochDay()), // 35 days from Feb 20
            Cycle(startDate = LocalDate.of(2023, 2, 20).toEpochDay()), // 21 days from Jan 30
            Cycle(startDate = LocalDate.of(2023, 1, 30).toEpochDay()), // 35 days from Dec 26
            Cycle(startDate = LocalDate.of(2022, 12, 26).toEpochDay())
        )
        // Note: The logic inside utils filters 15..50. 21 and 35 are valid.

        // We need to implement isIrregular in Utils to return true here.
        assertEquals(true, CyclePredictionUtils.isCycleIrregular(cycles))
    }

    @Test
    fun isCycleIrregular_returnsFalseForRegularCycles() {
        // Low variance: 28, 29, 28
        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2023, 3, 29).toEpochDay()),
            Cycle(startDate = LocalDate.of(2023, 2, 28).toEpochDay()),
            Cycle(startDate = LocalDate.of(2023, 1, 31).toEpochDay()),
            Cycle(startDate = LocalDate.of(2023, 1, 3).toEpochDay())
        )
        assertEquals(false, CyclePredictionUtils.isCycleIrregular(cycles))
    }
}
