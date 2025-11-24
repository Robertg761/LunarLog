package com.lunarlog.data

import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(
    val cycles: List<Cycle>,
    val dailyLogs: List<DailyLog>,
    val version: Int = 1
)

@Singleton
class DataManagementRepository @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val appDatabase: AppDatabase // Need direct access for nuking
) {
    private val gson = Gson()

    suspend fun createBackupJson(): String {
        val cycles = cycleRepository.getAllCycles().first()
        // We need a way to get *all* logs. Currently repo only has range or date.
        // I should add getAllLogs to DailyLogDao/Repository or just use a wide range.
        // Let's use a wide range for now or update DAO. 
        // Updating DAO is cleaner. But for now, let's assume valid range 2020-2030?
        // No, let's update DAO.
        val logs = dailyLogRepository.getAllLogsSync()
        
        val backup = BackupData(cycles, logs)
        return gson.toJson(backup)
    }

    suspend fun restoreFromJson(json: String) {
        val backup = gson.fromJson(json, BackupData::class.java)
        
        appDatabase.runInTransaction {
            // Nuke first
            appDatabase.clearAllTables() // This is drastic but "Restore" usually implies state replacement
            // Re-insert
            // We need Dao methods for bulk insert.
            // Using loops for now as we don't have bulk insert methods yet, and data is likely small.
            // Actually, for a Phase 6 polish, we should probably add bulk insert.
            // But strict mandates say "Mimic style". 
            // I will use what I have or extend.
        }
        // Since I can't call suspend functions in runInTransaction easily without blocking, 
        // and Room's clearAllTables is allowed on background thread.
        
        // I'll do manual clear via DAOs if available, or clearAllTables()
        appDatabase.clearAllTables()
        
        backup.cycles.forEach { cycleRepository.insertCycle(it) }
        backup.dailyLogs.forEach { dailyLogRepository.saveLog(it) }
    }

    suspend fun nukeData() {
        appDatabase.clearAllTables()
    }
}
