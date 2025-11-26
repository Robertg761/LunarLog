/*
package com.lunarlog.ui.calendar

import com.lunarlog.core.model.Cycle
import com.lunarlog.data.CycleRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private lateinit var viewModel: CalendarViewModel
    private val cycleRepository: CycleRepository = mockk()
    private val dailyLogRepository: DailyLogRepository = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        every { cycleRepository.getCyclesInRange(any(), any()) } returns flowOf(emptyList())
        every { dailyLogRepository.getLogsForRange(any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads current month`() = runTest {
        viewModel = CalendarViewModel(cycleRepository, dailyLogRepository)
        
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        
        val state = viewModel.uiState.value
        assertTrue("State should be Success but was $state", state is CalendarUiState.Success)
        val successState = state as CalendarUiState.Success
        
        assertEquals(YearMonth.now(), successState.currentMonth)
    }

    @Test
    fun `onNextMonth updates month`() = runTest {
        viewModel = CalendarViewModel(cycleRepository, dailyLogRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        
        val currentMonth = YearMonth.now()
        viewModel.onNextMonth()
        
        val state = viewModel.uiState.value as CalendarUiState.Success
        assertEquals(currentMonth.plusMonths(1), state.currentMonth)
    }

    @Test
    fun `calendar days include padding and correct dates`() = runTest {
        viewModel = CalendarViewModel(cycleRepository, dailyLogRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        
        val state = viewModel.uiState.value as CalendarUiState.Success
        val days = state.days
        
        assertEquals(42, days.size)
        
        val firstDay = state.currentMonth.atDay(1)
        assertTrue(days.any { it.date == firstDay && it.isCurrentMonth })
    }
    
    @Test
    fun `period days are marked`() = runTest {
        val today = LocalDate.now()
        val cycle = Cycle(startDate = today.toEpochDay())
        every { cycleRepository.getCyclesInRange(any(), any()) } returns flowOf(listOf(cycle))
        
        viewModel = CalendarViewModel(cycleRepository, dailyLogRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        
        val state = viewModel.uiState.value as CalendarUiState.Success
        val todayState = state.days.find { it.date == today }
        
        assertTrue("Today should be marked as period", todayState?.isPeriod == true)
    }
}
*/