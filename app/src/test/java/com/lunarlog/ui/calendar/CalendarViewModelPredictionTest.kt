package com.lunarlog.ui.calendar

import com.lunarlog.core.model.Cycle
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogRepository
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelPredictionTest {

    private val cycleRepository: CycleRepository = mockk()
    private val dailyLogRepository: DailyLogRepository = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `closed long period does not show predicted continuation on following day`() = runTest {
        val today = LocalDate.now()
        val cycle = Cycle(
            id = 1,
            startDate = today.minusDays(28),
            endDate = today
        )

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        val viewModel = CalendarViewModel(cycleRepository, dailyLogRepository)
        val state = viewModel.calendarState.first { it is CalendarDataState.Success } as CalendarDataState.Success

        val tomorrow = state.data[today.plusDays(1).toEpochDay()]
        val adjustedPredictionStart = state.data[today.plusDays(24).toEpochDay()]

        assertFalse(tomorrow?.isPredictedPeriod == true)
        assertEquals(true, state.data[today.toEpochDay()]?.isPeriod)
        assertTrue(adjustedPredictionStart?.isPredictedPeriod == true)
    }
}
