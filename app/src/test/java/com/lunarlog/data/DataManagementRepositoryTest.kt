package com.lunarlog.data

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class DataManagementRepositoryTest {

    private lateinit var repository: DataManagementRepository
    private val cycleRepository: CycleRepository = mockk(relaxed = true)
    private val dailyLogRepository: DailyLogRepository = mockk(relaxed = true)
    private val appDatabase: AppDatabase = mockk(relaxed = true)

    @Before
    fun setUp() {
        repository = DataManagementRepository(cycleRepository, dailyLogRepository, appDatabase)
        mockkStatic("androidx.room.RoomDatabaseKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `nukeData calls clearAllTables`() = runTest {
        repository.nukeData()
        coVerify { appDatabase.clearAllTables() }
    }

    @Test
    fun `restoreFromJson calls clearAllTables and inserts data`() = runTest {
        val json = """
            {
                "cycles": [{"id": 1, "startDate": 123}],
                "dailyLogs": [{"date": 456}],
                "version": 1
            }
        """.trimIndent()

        // Mock withTransaction to just execute the block
        val slot = slot<suspend () -> Unit>()
        coEvery { appDatabase.withTransaction(capture(slot)) } coAnswers {
            slot.captured.invoke()
        }

        repository.restoreFromJson(json)

        coVerify { appDatabase.clearAllTables() }
        coVerify { cycleRepository.insertCycle(any()) }
        coVerify { dailyLogRepository.saveLog(any()) }
    }
}
