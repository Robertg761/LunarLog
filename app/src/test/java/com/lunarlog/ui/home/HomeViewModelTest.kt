package com.lunarlog.ui.home

import com.lunarlog.data.Cycle
import com.lunarlog.data.CycleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    private val cycleRepository = mock(CycleRepository::class.java)
    private lateinit var viewModel: HomeViewModel
    private val testDispatcher = StandardTestDispatcher()

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
        val cycle = Cycle(id = 1, startDate = lastCycleStart)

        `when`(cycleRepository.getAllCycles()).thenReturn(flowOf(listOf(cycle)))

        viewModel = HomeViewModel(cycleRepository)

        // Advance until state is updated
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        // Expected:
        // Current cycle day: 11 (10 days ago was day 1, today is day 11)
        // Days until period: 28 (default avg) - 11 = 17 (approx, depending on logic)

        // Logic in VM:
        // averageLength = 28 (default for single cycle)
        // nextPeriodStart = lastCycleStart + 28 days
        // daysUntil = days between today and nextPeriodStart

        val nextPeriodStart = lastCycleStart.plusDays(28)
        val expectedDaysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, nextPeriodStart).toInt()

        assertEquals(expectedDaysUntil, state.daysUntilPeriod)
        assertEquals(11, state.currentCycleDay)
    }
}
