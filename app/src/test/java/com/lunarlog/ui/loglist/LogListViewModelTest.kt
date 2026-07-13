package com.lunarlog.ui.loglist

import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.data.MedicationRepository
import com.lunarlog.data.SymptomRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invalid external epoch day is rejected without crashing`() = runTest {
        val viewModel = LogListViewModel(
            repository = mockk<DailyLogRepository>(relaxed = true),
            cycleRepository = mockk<CycleRepository>(relaxed = true),
            symptomRepository = mockk<SymptomRepository>().also {
                every { it.getAllSymptoms() } returns flowOf(emptyList())
            },
            medicationRepository = mockk<MedicationRepository>(relaxed = true)
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.loadDate(Long.MAX_VALUE)
        advanceUntilIdle()

        assertEquals("This link contains an invalid date.", viewModel.uiState.value.periodMessage)
    }
}
