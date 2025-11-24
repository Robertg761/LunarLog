package com.lunarlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE date = :date")
    fun getLogForDate(date: Long): Flow<DailyLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(dailyLog: DailyLog)
    
    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :startDate AND :endDate")
    fun getLogsForRange(startDate: Long, endDate: Long): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs")
    suspend fun getAllLogsSync(): List<DailyLog>
}
