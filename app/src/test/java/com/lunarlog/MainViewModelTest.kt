package com.lunarlog

import com.lunarlog.data.AppLockMode
import com.lunarlog.data.UserPreferencesRepository
import com.lunarlog.update.UpdateRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val userPreferencesRepository: UserPreferencesRepository = mockk()
    private val updateRepository: UpdateRepository = mockk(relaxed = true)
    private val isFirstRun = MutableStateFlow(false)
    private val appLockMode = MutableStateFlow(AppLockMode.BIOMETRIC_REQUIRED)
    private val appLockTimeout = MutableStateFlow(0L)
    private val themeSeed = MutableStateFlow<Long?>(null)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { userPreferencesRepository.isFirstRun } returns isFirstRun
        every { userPreferencesRepository.appLockMode } returns appLockMode
        every { userPreferencesRepository.appLockTimeoutSeconds } returns appLockTimeout
        every { userPreferencesRepository.themeSeedColor } returns themeSeed
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onAppResumed locks when lock mode is enabled and timeout is immediate`() = runTest {
        val viewModel = MainViewModel(userPreferencesRepository, updateRepository)
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onAppResumed()

        assertTrue(viewModel.isLocked.value)
    }

    @Test
    fun `onAppResumed stays unlocked within timeout window`() = runTest {
        appLockTimeout.value = 120L
        val viewModel = MainViewModel(userPreferencesRepository, updateRepository)
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.unlock()
        viewModel.onAppResumed()

        assertFalse(viewModel.isLocked.value)
    }
}

