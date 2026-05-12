package com.lunarlog.ui.home

import com.lunarlog.core.model.Cycle
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.logic.CounterMode
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
import org.junit.Assert.assertTrue
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
    fun `uiState should show period days left when ongoing period is within estimate`() = runTest {
        val today = LocalDate.now()
        val lastCycleStart = today.minusDays(2) // elapsed 3 days, default estimate 5 => 2 left
        val cycle = Cycle(id = 1, startDate = lastCycleStart)

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository, testDispatcher)
        
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value

        assertEquals(CounterMode.PERIOD_DAYS_LEFT, state.counterMode)
        assertEquals(2, state.counterValue)
        assertEquals("Period", state.counterTitle)
        assertEquals("2 days left in period", state.counterSubtitle)
        assertEquals(true, state.isPeriodActive)
    }

    @Test
    fun `uiState should show ending today when ongoing period reaches estimate`() = runTest {
        val today = LocalDate.now()
        val lastCycleStart = today.minusDays(4) // elapsed 5 days, default estimate 5 => ending today
        val cycle = Cycle(id = 1, startDate = lastCycleStart, endDate = null)

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository, testDispatcher)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value

        assertEquals(CounterMode.PERIOD_DAYS_LEFT, state.counterMode)
        assertEquals(0, state.counterValue)
        assertEquals("Ending today", state.counterSubtitle)
    }

    @Test
    fun `uiState should show period overage when ongoing period exceeds estimate`() = runTest {
        val today = LocalDate.now()
        val lastCycleStart = today.minusDays(6) // elapsed 7 days, default estimate 5 => 2 over
        val cycle = Cycle(id = 1, startDate = lastCycleStart, endDate = null)

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository, testDispatcher)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value

        assertEquals(CounterMode.PERIOD_OVERAGE, state.counterMode)
        assertEquals(2, state.counterValue)
        assertEquals("2 days over estimate", state.counterSubtitle)
        assertEquals(null, state.daysRemainingInPeriod)
        assertEquals(true, state.isPeriodActive)
    }

    @Test
    fun `uiState should show next period countdown when latest cycle is closed and not due`() = runTest {
        val today = LocalDate.now()
        val lastCycleStart = today.minusDays(10)
        val cycle = Cycle(
            id = 1,
            startDate = lastCycleStart,
            endDate = today.minusDays(6)
        )

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository, testDispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value

        assertEquals(CounterMode.NEXT_PERIOD_COUNTDOWN, state.counterMode)
        assertEquals(18, state.counterValue)
        assertEquals("6 days since last period", state.counterSubtitle)
        assertEquals(false, state.isPeriodActive)
    }

    @Test
    fun `uiState should show days since last period when latest closed cycle hits estimate`() = runTest {
        val today = LocalDate.now()
        val lastCycleStart = today.minusDays(28)
        val cycle = Cycle(
            id = 1,
            startDate = lastCycleStart,
            endDate = today.minusDays(24)
        )

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository, testDispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value

        assertEquals(CounterMode.NEXT_PERIOD_COUNTDOWN, state.counterMode)
        assertEquals(0, state.counterValue)
        assertEquals("24 days since last period", state.counterSubtitle)
    }

    @Test
    fun `uiState should show overdue when latest closed cycle passes estimate`() = runTest {
        val today = LocalDate.now()
        val lastCycleStart = today.minusDays(31)
        val cycle = Cycle(
            id = 1,
            startDate = lastCycleStart,
            endDate = today.minusDays(27)
        )

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository, testDispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value

        assertEquals(CounterMode.NEXT_PERIOD_OVERDUE, state.counterMode)
        assertEquals(3, state.counterValue)
        assertEquals("3 days overdue", state.counterSubtitle)
    }

    @Test
    fun `shareable status should use counter-mode wording`() = runTest {
        val today = LocalDate.now()
        val cycle = Cycle(
            id = 1,
            startDate = today.minusDays(31),
            endDate = today.minusDays(27)
        )

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository, testDispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val status = viewModel.getShareableStatus()
        assertTrue(status.contains("Period is 3 days overdue"))
    }

    @Test
    fun `shareable status should include period days left wording`() = runTest {
        val today = LocalDate.now()
        val cycle = Cycle(
            id = 1,
            startDate = today.minusDays(2),
            endDate = null
        )

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository, testDispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val status = viewModel.getShareableStatus()
        assertTrue(status.contains("Estimated period days left: 2"))
    }

    @Test
    fun `shareable status should include period overage wording`() = runTest {
        val today = LocalDate.now()
        val cycle = Cycle(
            id = 1,
            startDate = today.minusDays(6),
            endDate = null
        )

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository, testDispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val status = viewModel.getShareableStatus()
        assertTrue(status.contains("Period is 2 days beyond estimate"))
    }

    @Test
    fun `shareable status should include due today wording`() = runTest {
        val today = LocalDate.now()
        val cycle = Cycle(
            id = 1,
            startDate = today.minusDays(28),
            endDate = today.minusDays(24)
        )

        every { cycleRepository.getAllCycles() } returns flowOf(listOf(cycle))
        every { dailyLogRepository.getAllLogs() } returns flowOf(emptyList())

        viewModel = HomeViewModel(cycleRepository, dailyLogRepository, testDispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val status = viewModel.getShareableStatus()
        assertTrue(status.contains("Estimated next period: due today"))
    }
}
