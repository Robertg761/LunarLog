package com.lunarlog.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lunarlog.core.model.DailyLog
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RecoveryTransactionTest {
    private lateinit var db: AppDatabase
    private lateinit var logs: DailyLogRepository
    private lateinit var backups: DataManagementRepository
    private val day = LocalDate.of(2026, 1, 10).toEpochDay()
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        logs = DailyLogRepository(db.dailyLogDao(), db.logEntryDao(), db)
        backups = DataManagementRepository(logs, db, UserPreferencesRepository(context))
    }
    @After fun close() { db.close() }
    private fun entry(value: String, type: LogEntryType = LogEntryType.NOTE) = LogEntry(date = day, time = 1000, type = type, value = value)

    @Test fun undoRestoresLegacyExcessSleepAndIsIdempotent() = runBlocking {
        repeat(3) { db.logEntryDao().insertEntry(entry("12", LogEntryType.SLEEP)) }
        val deleted = db.logEntryDao().getEntriesForDateSync(day).first()
        logs.deleteEntry(deleted)
        logs.restoreDeletedEntry(deleted)
        logs.restoreDeletedEntry(deleted)
        assertEquals(3, db.logEntryDao().getEntriesForDateSync(day).size)
        assertEquals(36f, db.dailyLogDao().getLogForDate(LocalDate.ofEpochDay(day)).first()!!.sleepHours)
    }

    @Test fun everyEntryTypeAndInputScaleRoundTripsWithStableIds() = runBlocking {
        val values = mapOf(LogEntryType.SYMPTOM to "Custom, symptom", LogEntryType.MOOD to "Happy",
            LogEntryType.FLOW to "2", LogEntryType.WATER to "500", LogEntryType.SLEEP to "8.5",
            LogEntryType.SLEEP_QUALITY to "4", LogEntryType.NOTE to "Quotes \" and newline\nkept",
            LogEntryType.SEX to "0", LogEntryType.TEMPERATURE to "98.1", LogEntryType.MUCUS to "0")
        logs.saveEntries(day, values.map { (type, value) -> entry(value, type) }, null)
        val before = db.logEntryDao().getEntriesForDateSync(day)
        backups.restoreFromJson(backups.createBackupJson())
        assertEquals(before, db.logEntryDao().getEntriesForDateSync(day))
        assertEquals(values.keys, before.map { it.type }.toSet())
    }

    @Test fun batchFailureRollsBackOriginalAndEveryNewEntry() = runBlocking {
        logs.addEntry(entry("original"))
        val original = db.logEntryDao().getEntriesForDateSync(day).single()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_note BEFORE INSERT ON log_entries WHEN NEW.value = 'fail' BEGIN SELECT RAISE(ABORT, 'injected write failure'); END")
        try { logs.saveEntries(day, listOf(entry("first"), entry("fail")), original); fail("Expected injected failure") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(listOf(original), db.logEntryDao().getEntriesForDateSync(day))
        assertEquals("original", db.dailyLogDao().getLogForDate(LocalDate.ofEpochDay(day)).first()!!.notes)
    }

    @Test fun backupRoundTripsLegacyExcessSleepExplicitZeroAndMultipleDoses() = runBlocking {
        repeat(3) { db.logEntryDao().insertEntry(entry("12", LogEntryType.SLEEP)) }
        db.logEntryDao().insertEntry(entry("0", LogEntryType.MUCUS))
        logs.rebuildDailyLogAggregateInTransaction(day)
        val medication = Medication(id = 1, name = "Example", startDate = day, dosesPerDay = 2, reminderTimes = listOf(480, 1200))
        db.medicationDao().insertMedication(medication)
        repeat(2) { db.medicationDao().logMedication(MedicationLog(date = day, medicationId = 1, timestamp = 1000L + it)) }
        val json = backups.createBackupJson()
        backups.restoreFromJson(json)
        assertEquals(4, db.logEntryDao().getEntriesForDateSync(day).size)
        assertEquals(36f, db.dailyLogDao().getLogForDate(LocalDate.ofEpochDay(day)).first()!!.sleepHours)
        assertEquals(2, db.medicationDao().getLogsForDateSync(day).size)
        assertEquals(medication, db.medicationDao().getAllMedicationsSync().single())
        val repository = MedicationRepository(db.medicationDao())
        repository.addMedication(medication.copy(name = "Renamed"))
        assertEquals(2, db.medicationDao().getLogsForDateSync(day).size)
        repository.removeDose(db.medicationDao().getLogsForDateSync(day).first().id)
        assertEquals(1, db.medicationDao().getLogsForDateSync(day).size)
    }

    @Test fun failedRestoreRollsBackReplacement() = runBlocking {
        logs.addEntry(entry("backup"))
        val json = backups.createBackupJson()
        logs.saveEntries(day, listOf(entry("current")), db.logEntryDao().getEntriesForDateSync(day).single())
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_restore BEFORE INSERT ON log_entries WHEN NEW.value = 'backup' BEGIN SELECT RAISE(ABORT, 'injected restore failure'); END")
        try { backups.restoreFromJson(json); fail("Expected restore failure") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals("current", db.logEntryDao().getEntriesForDateSync(day).single().value)
    }

    @Test fun resetReseedsBuiltInChoicesAndMoodFilterMatchesOnlyMoods() = runBlocking {
        logs.addEntry(entry("Happy", LogEntryType.MOOD))
        assertEquals(1, logs.searchLogsBySymptom("Happy", LogEntryType.MOOD).first().size)
        assertTrue(logs.searchLogsBySymptom("Happy", LogEntryType.SYMPTOM).first().isEmpty())
        backups.nukeData()
        assertTrue(db.symptomDefinitionDao().getAllSymptomsSync().isNotEmpty())
        assertTrue(db.logEntryDao().getEntriesForDateSync(day).isEmpty())
    }
}
