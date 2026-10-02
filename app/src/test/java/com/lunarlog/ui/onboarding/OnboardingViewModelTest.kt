package com.lunarlog.ui.onboarding

import com.lunarlog.data.CycleRepository
import com.lunarlog.data.PeriodChangeAction
import com.lunarlog.data.PeriodChangeResult
import com.lunarlog.data.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val cycleRepository: CycleRepository = mockk()
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private lateinit var viewModel: OnboardingViewModel
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
    fun `skip onboarding creates no period and ongoing start has no invented end`() = runTest {
        viewModel = OnboardingViewModel(cycleRepository, userPreferencesRepository)
        viewModel.completeOnboarding(null)
        advanceUntilIdle()
        coVerify(exactly = 0) { cycleRepository.recordPeriod(any(), any()) }
        coVerify { userPreferencesRepository.setFirstRunComplete() }
        coEvery { cycleRepository.recordPeriod(any(), null) } returns PeriodChangeResult.Success(PeriodChangeAction.PERIOD_DAY_ADDED, "Saved")
        val start = LocalDate.of(2026, 2, 1)
        viewModel.completeOnboarding(start)
        advanceUntilIdle()
        coVerify { cycleRepository.recordPeriod(start, null) }
    }

    @Test
    fun `completeOnboarding emits success and marks first run complete`() = runTest {
        coEvery {
            cycleRepository.recordPeriod(any(), any())
        } returns PeriodChangeResult.Success(
            action = PeriodChangeAction.PERIOD_DAY_ADDED,
            message = "Period day added"
        )

        viewModel = OnboardingViewModel(cycleRepository, userPreferencesRepository)
        viewModel.completeOnboarding(LocalDate.of(2026, 2, 1))
        advanceUntilIdle()

        assertTrue(viewModel.onboardingState.value is OnboardingViewModel.OnboardingState.Success)
        coVerify { userPreferencesRepository.setFirstRunComplete() }
    }

    @Test
    fun `completeOnboarding emits error when cycle update fails`() = runTest {
        coEvery {
            cycleRepository.recordPeriod(any(), any())
        } returns PeriodChangeResult.ValidationError("Cannot modify future days")

        viewModel = OnboardingViewModel(cycleRepository, userPreferencesRepository)
        viewModel.completeOnboarding(LocalDate.of(2099, 1, 1))
        advanceUntilIdle()

        assertTrue(viewModel.onboardingState.value is OnboardingViewModel.OnboardingState.Error)
        coVerify(exactly = 0) { userPreferencesRepository.setFirstRunComplete() }
    }
}
