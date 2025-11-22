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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cycle: Cycle)

    @Update
    suspend fun update(cycle: Cycle)
}
