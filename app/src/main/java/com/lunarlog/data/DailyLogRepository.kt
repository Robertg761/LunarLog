package com.lunarlog.data

import kotlinx.coroutines.flow.Flow
import com.lunarlog.core.model.DailyLog
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val logEntryDao: LogEntryDao
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
        // Legacy support: We allow saving the aggregate directly, 
        // but ideally we should update entries. 
        // For now, we trust the caller knows what they are doing (e.g. Migration or Legacy UI).
        dailyLogDao.insertLog(dailyLog)
    }

    fun getAllLogs(): Flow<List<DailyLog>> {
        return dailyLogDao.getAllLogs()
    }

    fun searchLogs(query: String): Flow<List<DailyLog>> {
        val ftsQuery = if (query.endsWith("*")) query else "$query*"
        return dailyLogDao.searchLogsFts(ftsQuery)
    }

    fun searchLogsBySymptom(symptom: String): Flow<List<DailyLog>> {
        return dailyLogDao.searchLogsBySymptom(symptom)
    }

    // --- Granular Log Entry Support ---

    fun getEntriesForDate(date: Long): Flow<List<LogEntry>> {
        return logEntryDao.getEntriesForDate(date)
    }

    suspend fun addEntry(entry: LogEntry) {
        ensureLegacyDataHydrated(entry.date)
        logEntryDao.insertEntry(entry)
        updateDailyLogAggregate(entry.date)
    }
    
    suspend fun deleteEntry(entry: LogEntry) {
        logEntryDao.deleteEntry(entry.id)
        updateDailyLogAggregate(entry.date)
    }

    suspend fun updateEntry(entry: LogEntry) {
        logEntryDao.updateEntry(entry)
        updateDailyLogAggregate(entry.date)
    }

    suspend fun ensureLegacyDataHydrated(date: Long) {
        val existingEntries = logEntryDao.getEntriesForDateSync(date)
        if (existingEntries.isNotEmpty()) return

        val legacyLog = dailyLogDao.getLogForDateSync(date) ?: return

        // Create default timestamp (Noon)
        val defaultTime = LocalDate.ofEpochDay(date)
            .atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Hydrate Flow
        if (legacyLog.flowLevel > 0) {
            logEntryDao.insertEntry(LogEntry(date = date, time = defaultTime, type = LogEntryType.FLOW, value = legacyLog.flowLevel.toString()))
        }
        // Hydrate Symptoms
        legacyLog.symptoms.forEach {
            logEntryDao.insertEntry(LogEntry(date = date, time = defaultTime, type = LogEntryType.SYMPTOM, value = it))
        }
        // Hydrate Moods
        legacyLog.mood.forEach {
            logEntryDao.insertEntry(LogEntry(date = date, time = defaultTime, type = LogEntryType.MOOD, value = it))
        }
        // Hydrate Water
        if (legacyLog.waterIntake > 0) {
            logEntryDao.insertEntry(LogEntry(date = date, time = defaultTime, type = LogEntryType.WATER, value = legacyLog.waterIntake.toString()))
        }
        // Hydrate Sleep
        if (legacyLog.sleepHours > 0) {
            logEntryDao.insertEntry(LogEntry(date = date, time = defaultTime, type = LogEntryType.SLEEP, value = legacyLog.sleepHours.toString()))
        }
        if (legacyLog.sleepQuality > 0) {
            logEntryDao.insertEntry(LogEntry(date = date, time = defaultTime, type = LogEntryType.SLEEP_QUALITY, value = legacyLog.sleepQuality.toString()))
        }
        // Hydrate Sex
        if (legacyLog.sexDrive > 0) {
            logEntryDao.insertEntry(LogEntry(date = date, time = defaultTime, type = LogEntryType.SEX, value = legacyLog.sexDrive.toString()))
        }
        // Hydrate Notes
        if (legacyLog.notes.isNotEmpty()) {
            logEntryDao.insertEntry(LogEntry(date = date, time = defaultTime, type = LogEntryType.NOTE, value = legacyLog.notes))
        }
        // Hydrate Temp
        if (legacyLog.temperature != null) {
            logEntryDao.insertEntry(LogEntry(date = date, time = defaultTime, type = LogEntryType.TEMPERATURE, value = legacyLog.temperature.toString()))
        }
        // Hydrate Mucus
        if (legacyLog.cervicalMucus > 0) {
            logEntryDao.insertEntry(LogEntry(date = date, time = defaultTime, type = LogEntryType.MUCUS, value = legacyLog.cervicalMucus.toString()))
        }
    }

    private suspend fun updateDailyLogAggregate(date: Long) {
        val entries = logEntryDao.getEntriesForDateSync(date)
        
        if (entries.isEmpty()) {
            dailyLogDao.insertLog(DailyLog(date = date))
            return
        }

        val symptoms = entries.filter { it.type == LogEntryType.SYMPTOM }.map { it.value }.distinct()
        val moods = entries.filter { it.type == LogEntryType.MOOD }.map { it.value }.distinct()
        
        val flowEntries = entries.filter { it.type == LogEntryType.FLOW }
        val flowLevel = if (flowEntries.isNotEmpty()) {
            flowEntries.maxOfOrNull { it.value.toIntOrNull() ?: 0 } ?: 0
        } else 0

        val waterEntries = entries.filter { it.type == LogEntryType.WATER }
        val waterIntake = waterEntries.sumOf { it.value.toIntOrNull() ?: 0 }

        val sleepEntries = entries.filter { it.type == LogEntryType.SLEEP }
        val sleepHours = sleepEntries.sumOf { it.value.toDoubleOrNull() ?: 0.0 }.toFloat()

        val sleepQualityEntries = entries.filter { it.type == LogEntryType.SLEEP_QUALITY }
        val sleepQuality = sleepQualityEntries.lastOrNull()?.value?.toIntOrNull() ?: 0

        val sexEntries = entries.filter { it.type == LogEntryType.SEX }
        val sexDrive = sexEntries.maxOfOrNull { it.value.toIntOrNull() ?: 0 } ?: 0

        val noteEntries = entries.filter { it.type == LogEntryType.NOTE }
        val notes = noteEntries.joinToString("\n") { it.value }

        val tempEntries = entries.filter { it.type == LogEntryType.TEMPERATURE }
        val temperature = tempEntries.lastOrNull()?.value?.toFloatOrNull()

        val mucusEntries = entries.filter { it.type == LogEntryType.MUCUS }
        val cervicalMucus = mucusEntries.maxOfOrNull { it.value.toIntOrNull() ?: 0 } ?: 0

        val aggregate = DailyLog(
            date = date,
            flowLevel = flowLevel,
            mood = moods,
            symptoms = symptoms,
            waterIntake = waterIntake,
            sleepHours = sleepHours,
            sleepQuality = sleepQuality,
            sexDrive = sexDrive,
            notes = notes,
            temperature = temperature,
            cervicalMucus = cervicalMucus
        )
        dailyLogDao.insertLog(aggregate)
    }
}