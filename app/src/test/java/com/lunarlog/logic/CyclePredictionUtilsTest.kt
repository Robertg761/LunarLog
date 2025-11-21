package com.lunarlog.logic

import com.lunarlog.data.Cycle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CyclePredictionUtilsTest {

    @Test
    fun calculateAverageCycleLength_returnsDefault_whenNotEnoughData() {
        val cycles = listOf(
            Cycle(startDate = LocalDate.of(2023, 1, 1))
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
            Cycle(startDate = LocalDate.of(2023, 3, 1)),
            Cycle(startDate = LocalDate.of(2023, 2, 1)),
            Cycle(startDate = LocalDate.of(2023, 1, 1))
        )

        val result = CyclePredictionUtils.calculateAverageCycleLength(cycles)
        assertEquals(29, result)
    }

    @Test
    fun predictNextPeriod_addsAverageLength() {
        val lastCycle = Cycle(startDate = LocalDate.of(2023, 1, 1))
        val averageLength = 30

        val result = CyclePredictionUtils.predictNextPeriod(lastCycle, averageLength)
        assertEquals(LocalDate.of(2023, 1, 31), result)
    }

    @Test
    fun predictFertileWindow_calculatesCorrectWindow() {
        // Next period: Jan 20
        // Window start: Jan 20 - 16 = Jan 4
        // Window end: Jan 20 - 12 = Jan 8

        val nextPeriod = LocalDate.of(2023, 1, 20)
        val (start, end) = CyclePredictionUtils.predictFertileWindow(nextPeriod)

        assertEquals(LocalDate.of(2023, 1, 4), start)
        assertEquals(LocalDate.of(2023, 1, 8), end)
    }
}
