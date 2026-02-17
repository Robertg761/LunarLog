package com.lunarlog.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.lunarlog.core.model.DailyLog
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction

@Singleton
class DailyLogRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val logEntryDao: LogEntryDao,
    private val appDatabase: AppDatabase
) {
    fun getLogForDate(date: LocalDate): Flow<DailyLog?> {
        return dailyLogDao.getLogForDate(date)
    }

    fun getLogsForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyLog>> {
        return dailyLogDao.getLogsForRange(startDate, endDate)
    }

    suspend fun getLogsForRangeSync(startDate: LocalDate, endDate: LocalDate): List<DailyLog> {
        return dailyLogDao.getLogsForRangeSync(startDate, endDate)
    }

    suspend fun getAllLogsSync(): List<DailyLog> {
        return dailyLogDao.getAllLogsSync()
    }

    suspend fun saveLog(dailyLog: DailyLog) {
        val date = dailyLog.date.toEpochDay()
        val defaultTime = dailyLog.date
            .atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val entries = mutableListOf<LogEntry>()

        if (dailyLog.flowLevel > 0) {
            entries += LogEntry(date = date, time = defaultTime, type = LogEntryType.FLOW, value = dailyLog.flowLevel.toString())
        }
        dailyLog.symptoms.forEach {
            entries += LogEntry(date = date, time = defaultTime, type = LogEntryType.SYMPTOM, value = it)
        }
        dailyLog.mood.forEach {
            entries += LogEntry(date = date, time = defaultTime, type = LogEntryType.MOOD, value = it)
        }
        if (dailyLog.waterIntake > 0) {
            entries += LogEntry(date = date, time = defaultTime, type = LogEntryType.WATER, value = dailyLog.waterIntake.toString())
        }
        if (dailyLog.sleepHours > 0f) {
            entries += LogEntry(date = date, time = defaultTime, type = LogEntryType.SLEEP, value = dailyLog.sleepHours.toString())
        }
        if (dailyLog.sleepQuality > 0) {
            entries += LogEntry(date = date, time = defaultTime, type = LogEntryType.SLEEP_QUALITY, value = dailyLog.sleepQuality.toString())
        }
        if (dailyLog.sexDrive > 0) {
            entries += LogEntry(date = date, time = defaultTime, type = LogEntryType.SEX, value = dailyLog.sexDrive.toString())
        }
        if (dailyLog.notes.isNotBlank()) {
            entries += LogEntry(date = date, time = defaultTime, type = LogEntryType.NOTE, value = dailyLog.notes)
        }
        if (dailyLog.temperature != null) {
            entries += LogEntry(date = date, time = defaultTime, type = LogEntryType.TEMPERATURE, value = dailyLog.temperature.toString())
        }
        if (dailyLog.cervicalMucus > 0) {
            entries += LogEntry(date = date, time = defaultTime, type = LogEntryType.MUCUS, value = dailyLog.cervicalMucus.toString())
        }

        replaceEntriesForDate(date, entries)
    }

    fun getAllLogs(): Flow<List<DailyLog>> {
        return dailyLogDao.getAllLogs()
    }

    fun searchLogs(query: String): Flow<List<DailyLog>> {
        val ftsQuery = sanitizeFtsQuery(query)
        if (ftsQuery.isBlank()) return flowOf(emptyList())
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
        appDatabase.withTransaction {
            ensureLegacyDataHydrated(entry.date)
            logEntryDao.insertEntry(entry)
            updateDailyLogAggregateInTransaction(entry.date)
        }
    }

    /**
     * Variant intended to be called from within a Room `withTransaction {}` block.
     * It must not change coroutine context, otherwise Room's transaction context can be lost.
     */
    suspend fun addEntryInTransaction(entry: LogEntry) {
        ensureLegacyDataHydrated(entry.date)
        logEntryDao.insertEntry(entry)
        updateDailyLogAggregateInTransaction(entry.date)
    }
    
    suspend fun deleteEntry(entry: LogEntry) {
        appDatabase.withTransaction {
            logEntryDao.deleteEntry(entry.id)
            updateDailyLogAggregateInTransaction(entry.date)
        }
    }

    suspend fun updateEntry(entry: LogEntry) {
        appDatabase.withTransaction {
            logEntryDao.updateEntry(entry)
            updateDailyLogAggregateInTransaction(entry.date)
        }
    }

    suspend fun replaceEntriesForDate(date: Long, entries: List<LogEntry>) {
        appDatabase.withTransaction {
            ensureLegacyDataHydrated(date)
            logEntryDao.deleteEntriesForDate(date)
            entries.forEach { entry ->
                logEntryDao.insertEntry(entry)
            }
            updateDailyLogAggregateInTransaction(date)
        }
    }

    suspend fun upsertEntries(
        date: Long,
        payload: Map<LogEntryType, List<String>>,
        time: Long,
        details: String? = null
    ) {
        val entries = payload.flatMap { (type, values) ->
            values
                .filter { it.isNotBlank() }
                .map { value ->
                    LogEntry(
                        date = date,
                        time = time,
                        type = type,
                        value = value,
                        details = details
                    )
                }
        }
        replaceEntriesForDate(date, entries)
    }

    suspend fun ensureLegacyDataHydrated(date: Long) {
        val existingEntries = logEntryDao.getEntriesForDateSync(date)
        if (existingEntries.isNotEmpty()) return

        val legacyLog = dailyLogDao.getLogForDateSync(LocalDate.ofEpochDay(date)) ?: return

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

    suspend fun rebuildDailyLogAggregate(date: Long) {
        appDatabase.withTransaction {
            updateDailyLogAggregateInTransaction(date)
        }
    }

    suspend fun rebuildDailyLogAggregateInTransaction(date: Long) {
        updateDailyLogAggregateInTransaction(date)
    }

    private suspend fun updateDailyLogAggregateInTransaction(date: Long) {
        updateDailyLogAggregateInternal(date)
    }

    private fun sanitizeFtsQuery(raw: String): String {
        val tokens = raw
            .split(Regex("\\s+"))
            .map { token -> token.replace(Regex("[^\\p{L}\\p{N}_-]"), "") }
            .filter { it.isNotBlank() }
            .take(8)
        return tokens.joinToString(" ") { "$it*" }
    }

    private suspend fun updateDailyLogAggregateInternal(date: Long) {
        val entries = logEntryDao.getEntriesForDateSync(date)
        
        if (entries.isEmpty()) {
            dailyLogDao.insertLog(DailyLog(date = LocalDate.ofEpochDay(date)))
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
            date = LocalDate.ofEpochDay(date),
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
