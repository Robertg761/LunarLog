package com.lunarlog.data

import com.lunarlog.core.model.DailyLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class DailyLogRepositoryAggregateTest {

    private val dailyLogDao: DailyLogDao = mockk(relaxed = true)
    private val logEntryDao: LogEntryDao = mockk(relaxed = true)

    @Test
    fun `rebuildDailyLogAggregateInTransaction aggregates without dropping notes or symptoms`() = runTest {
        val repo = DailyLogRepository(dailyLogDao, logEntryDao)
        val dateEpochDay = LocalDate.of(2026, 2, 14).toEpochDay()

        coEvery { logEntryDao.getEntriesForDateSync(dateEpochDay) } returns listOf(
            LogEntry(date = dateEpochDay, time = 1L, type = LogEntryType.SYMPTOM, value = "Headache"),
            LogEntry(date = dateEpochDay, time = 2L, type = LogEntryType.NOTE, value = "Felt off in the morning"),
            LogEntry(date = dateEpochDay, time = 3L, type = LogEntryType.NOTE, value = "Better after lunch")
        )

        val inserted = slot<DailyLog>()
        coEvery { dailyLogDao.insertLog(capture(inserted)) } returns Unit

        repo.rebuildDailyLogAggregateInTransaction(dateEpochDay)

        coVerify { dailyLogDao.insertLog(any()) }
        assertEquals(LocalDate.ofEpochDay(dateEpochDay), inserted.captured.date)
        assertTrue(inserted.captured.symptoms.contains("Headache"))
        assertEquals(
            "Felt off in the morning\nBetter after lunch",
            inserted.captured.notes
        )
    }
}

