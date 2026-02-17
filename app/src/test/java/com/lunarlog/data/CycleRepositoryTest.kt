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
