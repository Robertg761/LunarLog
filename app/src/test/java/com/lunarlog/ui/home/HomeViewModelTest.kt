package com.lunarlog.ui.home

import com.lunarlog.core.model.Cycleimport com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    private val cycleRepository = mockk<CycleRepository>()
    private val dailyLogRepository = mockk<DailyLogRepository>()
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState should calculate correct values`() = runTest {
        val today = LocalDate.now()
        // Create a cycle starting 10 days ago
        val lastCycleStart = today.minusDays(10)
        val cycle = Cycle(id = 1, startDate = lastCycleStart.toEpochDay())

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository)
        
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value

        val nextPeriodStart = lastCycleStart.plusDays(28)
        val expectedDaysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, nextPeriodStart).toInt()

        assertEquals(expectedDaysUntil, state.daysUntilPeriod)
        assertEquals(11, state.currentCycleDay)
    }

    @Test
    fun `uiState should calculate daysRemainingInPeriod for active period`() = runTest {
        val today = LocalDate.now()
        // Cycle started 2 days ago (Day 3 of cycle)
        val lastCycleStart = today.minusDays(2)
        val cycle = Cycle(id = 1, startDate = lastCycleStart.toEpochDay(), endDate = null)

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        
        // Default period length is 5. Current day is 3. Remaining: 5 - 3 = 2
        assertEquals(true, state.isPeriodActive)
        assertEquals(2, state.daysRemainingInPeriod)
    }

    @Test
    fun `uiState should have null daysRemainingInPeriod for inactive period`() = runTest {
        val today = LocalDate.now()
        // Cycle started 20 days ago (Day 21 of cycle)
        val lastCycleStart = today.minusDays(20)
        val cycle = Cycle(id = 1, startDate = lastCycleStart.toEpochDay(), endDate = null)

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value

        assertEquals(false, state.isPeriodActive)
        assertEquals(null, state.daysRemainingInPeriod)
    }
}
