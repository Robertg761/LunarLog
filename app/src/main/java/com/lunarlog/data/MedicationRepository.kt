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

    suspend fun getAllMedicationsSync(): List<Medication> =
        medicationDao.getAllMedicationsSync()

    suspend fun addMedication(medication: Medication) {
        medicationDao.insertMedication(medication)
    }

    suspend fun deleteMedication(id: Int) {
        medicationDao.deleteMedication(id)
    }

    fun getLogsForDate(date: Long): Flow<List<MedicationLog>> =
        medicationDao.getLogsForDate(date)

    suspend fun getLogsForDateSync(date: Long): List<MedicationLog> =
        medicationDao.getLogsForDateSync(date)

    suspend fun logMedication(log: MedicationLog) {
        medicationDao.logMedication(log)
    }

    suspend fun setMedicationTaken(date: Long, medicationId: Int, taken: Boolean) {
        if (!taken) {
            medicationDao.deleteMedicationLog(date, medicationId)
            return
        }
        val existing = medicationDao.getLogForMedicationOnDate(date, medicationId)
        medicationDao.logMedication(
            MedicationLog(
                id = existing?.id ?: 0,
                date = date,
                medicationId = medicationId,
                taken = true,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
