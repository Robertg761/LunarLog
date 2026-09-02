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
import org.junit.Assert.assertNotNull
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

    // --- Validation of the current (v2) backup format -----------------------------------------
    //
    // Every case below must be rejected before clearAllTables() runs: a restore that fails
    // validation must leave the user's existing data untouched.

    private fun v2Json(data: String): String = """
        {
            "version": 2,
            "exportedAtMillis": 0,
            "appVersionName": "1.10.0",
            "data": { $data }
        }
    """.trimIndent()

    private suspend fun assertRejectedBeforeClearing(json: String): String {
        val error = try {
            repository.restoreFromJson(json)
            null
        } catch (e: IllegalArgumentException) {
            e
        }
        assertNotNull("expected the backup to be rejected", error)
        coVerify(exactly = 0) { appDatabase.clearAllTables() }
        return error?.message.orEmpty()
    }

    @Test
    fun `overlapping periods are rejected`() = runTest {
        val message = assertRejectedBeforeClearing(
            v2Json(
                """"cycles": [
                    {"id": 1, "startEpochDay": 20000, "endEpochDay": 20005},
                    {"id": 2, "startEpochDay": 20005, "endEpochDay": 20010}
                ]"""
            )
        )
        assertTrue(message, message.contains("overlap"))
    }

    @Test
    fun `open period followed by a later period is rejected as overlapping`() = runTest {
        assertRejectedBeforeClearing(
            v2Json(
                """"cycles": [
                    {"id": 1, "startEpochDay": 20000},
                    {"id": 2, "startEpochDay": 20030, "endEpochDay": 20034}
                ]"""
            )
        )
    }

    @Test
    fun `period ending before it starts is rejected`() = runTest {
        assertRejectedBeforeClearing(v2Json(""""cycles": [{"id": 1, "startEpochDay": 20010, "endEpochDay": 20005}]"""))
    }

    @Test
    fun `duplicate positive period ids are rejected`() = runTest {
        assertRejectedBeforeClearing(
            v2Json(
                """"cycles": [
                    {"id": 7, "startEpochDay": 20000, "endEpochDay": 20004},
                    {"id": 7, "startEpochDay": 20030, "endEpochDay": 20034}
                ]"""
            )
        )
    }

    @Test
    fun `daily log values outside their scales are rejected`() = runTest {
        assertRejectedBeforeClearing(v2Json(""""dailyLogs": [{"dateEpochDay": 20000, "flowLevel": 9}]"""))
        assertRejectedBeforeClearing(v2Json(""""dailyLogs": [{"dateEpochDay": 20000, "sleepHours": 25}]"""))
        assertRejectedBeforeClearing(v2Json(""""dailyLogs": [{"dateEpochDay": 20000, "sexDrive": -1}]"""))
        assertRejectedBeforeClearing(v2Json(""""dailyLogs": [{"dateEpochDay": 20000, "temperature": 60.0}]"""))
    }

    @Test
    fun `duplicate daily log dates are rejected`() = runTest {
        assertRejectedBeforeClearing(
            v2Json(""""dailyLogs": [{"dateEpochDay": 20000}, {"dateEpochDay": 20000}]""")
        )
    }

    @Test
    fun `log entry with an unknown type or blank value is rejected`() = runTest {
        assertRejectedBeforeClearing(
            v2Json(""""logEntries": [{"id": 1, "dateEpochDay": 20000, "timeEpochMillis": 0, "type": "TELEPATHY", "value": "3"}]""")
        )
        assertRejectedBeforeClearing(
            v2Json(""""logEntries": [{"id": 1, "dateEpochDay": 20000, "timeEpochMillis": 0, "type": "FLOW", "value": "  "}]""")
        )
    }

    @Test
    fun `medication with an unknown frequency or bad reminder time is rejected`() = runTest {
        assertRejectedBeforeClearing(
            v2Json(""""medications": [{"id": 1, "name": "Iron", "frequency": "hourly", "startDateEpochDay": 20000}]""")
        )
        assertRejectedBeforeClearing(
            v2Json(""""medications": [{"id": 1, "name": "Iron", "dosage": "", "frequency": "daily", "startDateEpochDay": 20000, "reminderTimeMinutes": 1500}]""")
        )
        assertRejectedBeforeClearing(
            v2Json(""""medications": [{"id": 1, "name": "   ", "dosage": "", "frequency": "daily", "startDateEpochDay": 20000}]""")
        )
    }

    @Test
    fun `medication log referencing a missing medication is rejected`() = runTest {
        assertRejectedBeforeClearing(
            v2Json(
                """"medications": [{"id": 1, "name": "Iron", "dosage": "", "frequency": "daily", "startDateEpochDay": 20000}],
                "medicationLogs": [{"id": 1, "dateEpochDay": 20001, "medicationId": 2, "timestampMillis": 0}]"""
            )
        )
    }

    @Test
    fun `two doses of one medication on one day are rejected`() = runTest {
        assertRejectedBeforeClearing(
            v2Json(
                """"medications": [{"id": 1, "name": "Iron", "dosage": "", "frequency": "daily", "startDateEpochDay": 20000}],
                "medicationLogs": [
                    {"id": 1, "dateEpochDay": 20001, "medicationId": 1, "timestampMillis": 0},
                    {"id": 2, "dateEpochDay": 20001, "medicationId": 1, "timestampMillis": 1}
                ]"""
            )
        )
    }

    @Test
    fun `symptom definition with an unknown category or duplicate name is rejected`() = runTest {
        assertRejectedBeforeClearing(
            v2Json(""""symptomDefinitions": [{"id": 1, "name": "x", "displayName": "X", "category": "WEATHER"}]""")
        )
        assertRejectedBeforeClearing(
            v2Json(
                """"symptomDefinitions": [
                    {"id": 1, "name": "cramps", "displayName": "Cramps", "category": "PHYSICAL"},
                    {"id": 2, "name": "cramps", "displayName": "Cramps again", "category": "PHYSICAL"}
                ]"""
            )
        )
    }

    @Test
    fun `preference reminder time outside a day is rejected`() = runTest {
        assertRejectedBeforeClearing(v2Json(""""preferences": {"periodLogReminderTimeMinutes": 1440}"""))
    }

    @Test
    fun `date outside the supported range is rejected`() = runTest {
        assertRejectedBeforeClearing(v2Json(""""cycles": [{"id": 1, "startEpochDay": 9223372036854775807}]"""))
    }

    @Test
    fun `unparseable json is rejected before clearing`() = runTest {
        assertRejectedBeforeClearing("{ this is not json")
        assertRejectedBeforeClearing("[]")
        assertRejectedBeforeClearing("")
    }
}
