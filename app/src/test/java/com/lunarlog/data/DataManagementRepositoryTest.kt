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
import org.junit.Assert.assertTrue

class DataManagementRepositoryTest {

    private lateinit var repository: DataManagementRepository
    private val dailyLogRepository: DailyLogRepository = mockk(relaxed = true)
    private val appDatabase: AppDatabase = mockk(relaxed = true)
    private val cycleDao: CycleDao = mockk(relaxed = true)
    private val medicationDao: MedicationDao = mockk(relaxed = true)
    private val symptomDao: SymptomDefinitionDao = mockk(relaxed = true)
    private val logEntryDao: LogEntryDao = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { appDatabase.cycleDao() } returns cycleDao
        every { appDatabase.medicationDao() } returns medicationDao
        every { appDatabase.symptomDefinitionDao() } returns symptomDao
        every { appDatabase.logEntryDao() } returns logEntryDao

        repository = DataManagementRepository(dailyLogRepository, appDatabase, userPreferencesRepository)
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
                "version": 2,
                "exportedAtMillis": 0,
                "appVersionName": "1.0.0",
                "data": {
                    "cycles": [],
                    "dailyLogs": [{"dateEpochDay": 456}],
                    "logEntries": [],
                    "medications": [],
                    "medicationLogs": [],
                    "symptomDefinitions": []
                }
            }
        """.trimIndent()

        // Mock withTransaction to just execute the block
        val slot = slot<suspend () -> Unit>()
        coEvery { appDatabase.withTransaction(capture(slot)) } coAnswers {
            slot.captured.invoke()
        }

        repository.restoreFromJson(json)

        coVerify { appDatabase.clearAllTables() }
        coVerify { dailyLogRepository.rebuildDailyLogAggregateInTransaction(any()) }
    }

    @Test
    fun `invalid legacy record is rejected before current data is cleared`() = runTest {
        val json = """
            {
                "cycles": [{"id": 1, "startDate": "not-a-date"}],
                "dailyLogs": []
            }
        """.trimIndent()

        var rejected = false
        try {
            repository.restoreFromJson(json)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        coVerify(exactly = 0) { appDatabase.clearAllTables() }
    }
}
