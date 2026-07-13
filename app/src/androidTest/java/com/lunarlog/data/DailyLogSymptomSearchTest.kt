package com.lunarlog.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lunarlog.core.model.DailyLog
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyLogSymptomSearchTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: DailyLogRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DailyLogRepository(
            dailyLogDao = database.dailyLogDao(),
            logEntryDao = database.logEntryDao(),
            appDatabase = database
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exactSymptomSearch_hydratesLegacyRows_withoutSubstringMatches() = runBlocking {
        val exactDate = LocalDate.of(2026, 7, 12)
        val substringDate = LocalDate.of(2026, 7, 13)
        database.dailyLogDao().insertLog(
            DailyLog(date = exactDate, symptoms = listOf("Pain"))
        )
        database.dailyLogDao().insertLog(
            DailyLog(date = substringDate, symptoms = listOf("Painful"))
        )

        val matches = repository.searchLogsBySymptom("Pain").first()

        assertEquals(listOf(exactDate), matches.map { it.date })
        assertEquals(
            listOf("Pain"),
            database.logEntryDao()
                .getEntriesForDateSync(exactDate.toEpochDay())
                .map { it.value }
        )
        assertEquals(
            listOf("Painful"),
            database.logEntryDao()
                .getEntriesForDateSync(substringDate.toEpochDay())
                .map { it.value }
        )
    }
}
