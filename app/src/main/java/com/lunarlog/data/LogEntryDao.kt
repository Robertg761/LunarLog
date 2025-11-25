package com.lunarlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {
    @Query("SELECT * FROM log_entries WHERE date = :date ORDER BY time ASC")
    fun getEntriesForDate(date: Long): Flow<List<LogEntry>>

    @Query("SELECT * FROM log_entries WHERE date = :date ORDER BY time ASC")
    suspend fun getEntriesForDateSync(date: Long): List<LogEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LogEntry): Long

    @Update
    suspend fun updateEntry(entry: LogEntry)

    @Query("DELETE FROM log_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)
    
    @Query("DELETE FROM log_entries WHERE date = :date")
    suspend fun deleteEntriesForDate(date: Long)
}
