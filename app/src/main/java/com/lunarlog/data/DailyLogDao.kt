package com.lunarlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lunarlog.core.model.DailyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE date = :date")
    fun getLogForDate(date: Long): Flow<DailyLog?>

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    suspend fun getLogForDateSync(date: Long): DailyLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(dailyLog: DailyLog)
    
    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :startDate AND :endDate")
    fun getLogsForRange(startDate: Long, endDate: Long): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs")
    suspend fun getAllLogsSync(): List<DailyLog>

    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<DailyLog>>

    @Query("""
        SELECT daily_logs.* FROM daily_logs 
        JOIN daily_logs_fts ON daily_logs.date = daily_logs_fts.docid 
        WHERE daily_logs_fts MATCH :query
        ORDER BY daily_logs.date DESC
    """)
    fun searchLogsFts(query: String): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs WHERE symptoms LIKE '%' || :symptom || '%' ORDER BY date DESC")
    fun searchLogsBySymptom(symptom: String): Flow<List<DailyLog>>
}
