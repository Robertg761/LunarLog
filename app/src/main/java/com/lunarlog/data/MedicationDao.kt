package com.lunarlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE endDate IS NULL OR endDate >= :currentDate")
    fun getActiveMedications(currentDate: Long): Flow<List<Medication>>

    @Query("SELECT * FROM medications")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications")
    suspend fun getAllMedicationsSync(): List<Medication>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: Int)

    @Query("SELECT * FROM medication_logs WHERE date = :date")
    fun getLogsForDate(date: Long): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs ORDER BY date ASC, timestamp ASC")
    suspend fun getAllMedicationLogsSync(): List<MedicationLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logMedication(log: MedicationLog)
}
