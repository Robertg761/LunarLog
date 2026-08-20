package com.lunarlog.data

import androidx.room.withTransaction
import com.lunarlog.core.model.Cycle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CycleRepositoryTest {

    private val cycleDao: CycleDao = mockk(relaxed = true)
    private val appDatabase: AppDatabase = mockk(relaxed = true)
    private lateinit var repository: CycleRepository

    @Before
    fun setUp() {
        repository = CycleRepository(cycleDao, appDatabase)
        mockkStatic("androidx.room.RoomDatabaseKt")
        val tx = slot<suspend () -> Any?>()
        coEvery { appDatabase.withTransaction(capture(tx)) } coAnswers {
            tx.captured.invoke()
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `updateCycleDates rejects overlap`() = runTest {
        val c1 = Cycle(id = 1, startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 1, 3))
        val c2 = Cycle(id = 2, startDate = LocalDate.of(2026, 1, 5), endDate = LocalDate.of(2026, 1, 7))

        coEvery { cycleDao.getCycleById(2) } returns c2
        coEvery { cycleDao.getAllCyclesSync() } returns listOf(c1, c2)

        val result = repository.updateCycleDates(
            cycleId = 2,
            startDate = LocalDate.of(2026, 1, 3),
            endDate = LocalDate.of(2026, 1, 7)
        )

        assertTrue(result is PeriodChangeResult.ValidationError)
        coVerify(exactly = 0) { cycleDao.updateCycle(any()) }
    }

    @Test
    fun `updateCycleDates rejects future start date`() = runTest {
        val result = repository.updateCycleDates(
            cycleId = 1,
            startDate = LocalDate.now().plusDays(1),
            endDate = null
        )

        assertTrue(result is PeriodChangeResult.ValidationError)
        coVerify(exactly = 0) { cycleDao.getCycleById(any()) }
        coVerify(exactly = 0) { cycleDao.updateCycle(any()) }
    }

    @Test
    fun `updateCycleDates rejects future end date`() = runTest {
        val result = repository.updateCycleDates(
            cycleId = 1,
            startDate = LocalDate.now().minusDays(3),
            endDate = LocalDate.now().plusDays(1)
        )

        assertTrue(result is PeriodChangeResult.ValidationError)
        coVerify(exactly = 0) { cycleDao.getCycleById(any()) }
        coVerify(exactly = 0) { cycleDao.updateCycle(any()) }
    }

    @Test
    fun `setPeriodDay false splits middle day`() = runTest {
        val cycle = Cycle(
            id = 1,
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 5)
        )
        val left = cycle.copy(endDate = LocalDate.of(2026, 1, 2))
        val right = Cycle(
            startDate = LocalDate.of(2026, 1, 4),
            endDate = LocalDate.of(2026, 1, 5)
        )

        coEvery { cycleDao.getAllCyclesSync() } returnsMany listOf(
            listOf(cycle),
            listOf(left, right)
        )

        val result = repository.setPeriodDay(LocalDate.of(2026, 1, 3), false)

        assertTrue(result is PeriodChangeResult.Success)
        coVerify { cycleDao.updateCycle(match { it.id == 1 && it.endDate == LocalDate.of(2026, 1, 2) }) }
        coVerify { cycleDao.insertCycle(match { it.startDate == LocalDate.of(2026, 1, 4) && it.endDate == LocalDate.of(2026, 1, 5) }) }
    }

    @Test
    fun `startPeriod caps stale open cycle at average length and flags the end as estimated`() = runTest {
        // Two confirmed 5-day periods, then one left open for a whole cycle.
        val closed1 = Cycle(id = 1, startDate = LocalDate.of(2025, 11, 1), endDate = LocalDate.of(2025, 11, 5))
        val closed2 = Cycle(id = 2, startDate = LocalDate.of(2025, 11, 29), endDate = LocalDate.of(2025, 12, 3))
        val staleOpen = Cycle(id = 3, startDate = LocalDate.of(2025, 12, 27), endDate = null)
        val newStart = LocalDate.of(2026, 1, 24)

        coEvery { cycleDao.getAllCyclesSync() } returnsMany listOf(
            listOf(staleOpen, closed2, closed1),
            listOf(
                staleOpen.copy(endDate = LocalDate.of(2025, 12, 31), endEstimated = true),
                closed2,
                closed1,
                Cycle(id = 4, startDate = newStart, endDate = null)
            )
        )

        val result = repository.startPeriod(newStart)

        assertTrue(result is PeriodChangeResult.Success)
        // Average period is 5 days, so the stale cycle closes at start + 4, not newStart - 1.
        coVerify {
            cycleDao.updateCycle(
                match { it.id == 3 && it.endDate == LocalDate.of(2025, 12, 31) && it.endEstimated }
            )
        }
        coVerify { cycleDao.insertCycle(match { it.startDate == newStart && it.endDate == null && !it.endEstimated }) }
    }

    @Test
    fun `startPeriod treats a tap within a believable period length as no change`() = runTest {
        // Day 8 of an open period: could still be real bleeding, so don't split it.
        val ongoing = Cycle(id = 1, startDate = LocalDate.now().minusDays(7), endDate = null)
        coEvery { cycleDao.getAllCyclesSync() } returns listOf(ongoing)

        val result = repository.startPeriod(LocalDate.now())

        assertTrue(result is PeriodChangeResult.Success)
        assertTrue((result as PeriodChangeResult.Success).action == PeriodChangeAction.NO_CHANGE)
        coVerify(exactly = 0) { cycleDao.updateCycle(any()) }
        coVerify(exactly = 0) { cycleDao.insertCycle(any()) }
    }

    @Test
    fun `endOngoingPeriod records a confirmed end`() = runTest {
        val ongoing = Cycle(id = 1, startDate = LocalDate.now().minusDays(4), endDate = null)
        coEvery { cycleDao.getAllCyclesSync() } returns listOf(ongoing)

        val result = repository.endOngoingPeriod(LocalDate.now())

        assertTrue(result is PeriodChangeResult.Success)
        coVerify { cycleDao.updateCycle(match { it.id == 1 && it.endDate == LocalDate.now() && !it.endEstimated }) }
    }

    @Test
    fun `updateCycleDates clears the estimated flag`() = runTest {
        val estimated = Cycle(
            id = 1,
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 5),
            endEstimated = true
        )
        coEvery { cycleDao.getCycleById(1) } returns estimated
        coEvery { cycleDao.getAllCyclesSync() } returns listOf(estimated)

        val result = repository.updateCycleDates(
            cycleId = 1,
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 6)
        )

        assertTrue(result is PeriodChangeResult.Success)
        coVerify { cycleDao.updateCycle(match { it.endDate == LocalDate.of(2026, 1, 6) && !it.endEstimated }) }
    }

    @Test
    fun `startPeriod creates new ongoing when no ongoing period exists`() = runTest {
        val closed = Cycle(id = 1, startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 1, 5))
        val newOngoing = Cycle(id = 2, startDate = LocalDate.of(2026, 1, 10), endDate = null)

        coEvery { cycleDao.getAllCyclesSync() } returnsMany listOf(
            listOf(closed),
            listOf(closed, newOngoing)
        )

        val result = repository.startPeriod(LocalDate.of(2026, 1, 10))

        assertTrue(result is PeriodChangeResult.Success)
        coVerify(exactly = 0) { cycleDao.updateCycle(any()) }
        coVerify { cycleDao.insertCycle(match { it.startDate == LocalDate.of(2026, 1, 10) && it.endDate == null }) }
    }
}
