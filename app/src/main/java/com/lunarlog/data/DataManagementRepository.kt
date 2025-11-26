package com.lunarlog.data

import androidx.room.withTransaction
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val cycles = cycleRepository.getAllCycles().first()
        // We need a way to get *all* logs. Currently repo only has range or date.
        // I should add getAllLogs to DailyLogDao/Repository or just use a wide range.
        // Let's use a wide range for now or update DAO. 
        // Updating DAO is cleaner. But for now, let's assume valid range 2020-2030?
        // No, let's update DAO.
        val logs = dailyLogRepository.getAllLogsSync()
        
        val backup = BackupData(cycles, logs)
        gson.toJson(backup)
    }

    suspend fun restoreFromJson(json: String) = withContext(Dispatchers.IO) {
        val backup = gson.fromJson(json, BackupData::class.java)
        
        appDatabase.withTransaction {
            // Nuke first
            appDatabase.clearAllTables() // This is drastic but "Restore" usually implies state replacement
            
            // Re-insert
            backup.cycles.forEach { cycleRepository.insertCycle(it) }
            backup.dailyLogs.forEach { dailyLogRepository.saveLog(it) }
        }
    }

    suspend fun nukeData() = withContext(Dispatchers.IO) {
        appDatabase.clearAllTables()
    }
}
