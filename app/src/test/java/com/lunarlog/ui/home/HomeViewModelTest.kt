package com.lunarlog.ui.home

import com.lunarlog.data.Cycle
import com.lunarlog.data.CycleRepository
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

        viewModel = HomeViewModel(cycleRepository)
        
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value

        val nextPeriodStart = lastCycleStart.plusDays(28)
        val expectedDaysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, nextPeriodStart).toInt()

        assertEquals(expectedDaysUntil, state.daysUntilPeriod)
        assertEquals(11, state.currentCycleDay)
    }
}
