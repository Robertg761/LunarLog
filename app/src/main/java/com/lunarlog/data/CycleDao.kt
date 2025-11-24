package com.lunarlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Query("SELECT * FROM cycles ORDER BY startDate DESC")
    fun getAllCycles(): Flow<List<Cycle>>

    @Query("SELECT * FROM cycles ORDER BY startDate DESC")
    fun getAllCyclesSync(): List<Cycle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: Cycle)
    
    @Update
    suspend fun updateCycle(cycle: Cycle)
    
    @Query("SELECT * FROM cycles WHERE startDate = :date LIMIT 1")
    fun getCycleForDateSync(date: Long): Cycle?
}
