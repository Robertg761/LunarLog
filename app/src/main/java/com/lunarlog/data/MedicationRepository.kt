package com.lunarlog.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao
) {
    fun getActiveMedications(currentDate: Long): Flow<List<Medication>> =
        medicationDao.getActiveMedications(currentDate)

    fun getAllMedications(): Flow<List<Medication>> =
        medicationDao.getAllMedications()

    suspend fun addMedication(medication: Medication) {
        medicationDao.insertMedication(medication)
    }

    suspend fun deleteMedication(id: Int) {
        medicationDao.deleteMedication(id)
    }

    fun getLogsForDate(date: Long): Flow<List<MedicationLog>> =
        medicationDao.getLogsForDate(date)

    suspend fun logMedication(log: MedicationLog) {
        medicationDao.logMedication(log)
    }
}
