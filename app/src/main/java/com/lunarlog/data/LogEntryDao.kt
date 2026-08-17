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

    @Query("SELECT * FROM log_entries ORDER BY date ASC, time ASC")
    suspend fun getAllEntriesSync(): List<LogEntry>

    @Query("SELECT * FROM log_entries ORDER BY date ASC, time ASC")
    fun getAllEntries(): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LogEntry): Long

    @Query("SELECT EXISTS(SELECT 1 FROM log_entries WHERE date = :date AND type = :type AND value = :value)")
    suspend fun entryExists(date: Long, type: LogEntryType, value: String): Boolean

    @Update
    suspend fun updateEntry(entry: LogEntry)

    @Query("DELETE FROM log_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)
    
    @Query("DELETE FROM log_entries WHERE date = :date")
    suspend fun deleteEntriesForDate(date: Long)

    @Query("DELETE FROM log_entries WHERE date = :date AND type = :type")
    suspend fun deleteEntriesForDateAndType(date: Long, type: LogEntryType)
}
