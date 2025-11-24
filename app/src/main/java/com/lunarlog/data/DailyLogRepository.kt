package com.lunarlog.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao
) {
    fun getLogForDate(date: Long): Flow<DailyLog?> {
        return dailyLogDao.getLogForDate(date)
    }

    fun getLogsForRange(startDate: Long, endDate: Long): Flow<List<DailyLog>> {
        return dailyLogDao.getLogsForRange(startDate, endDate)
    }

    suspend fun getAllLogsSync(): List<DailyLog> {
        return dailyLogDao.getAllLogsSync()
    }

    suspend fun saveLog(dailyLog: DailyLog) {
        dailyLogDao.insertLog(dailyLog)
    }

    fun getAllLogs(): Flow<List<DailyLog>> {
        return dailyLogDao.getAllLogs()
    }

    fun searchLogs(query: String): Flow<List<DailyLog>> {
        // Append * to query for prefix matching if not present
        val ftsQuery = if (query.endsWith("*")) query else "$query*"
        return dailyLogDao.searchLogsFts(ftsQuery)
    }

    fun searchLogsBySymptom(symptom: String): Flow<List<DailyLog>> {
        return dailyLogDao.searchLogsBySymptom(symptom)
    }
}
