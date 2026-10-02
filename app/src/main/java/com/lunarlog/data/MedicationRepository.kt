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
        require(medication.dosesPerDay in 1..24)
        require((medication.reminderTimes.isEmpty() || medication.reminderTimes.size == medication.dosesPerDay) && medication.reminderTimes.distinct().size == medication.reminderTimes.size && medication.reminderTimes.all { it in 0L..1439L })
        require(medication.name.isNotBlank()) { "Medication name is required" }
        LogValidation.date(medication.startDate)
        medication.endDate?.let { LogValidation.date(it); require(it >= medication.startDate) { "End date must follow the start date" } }
        require(medication.frequency in setOf("daily", "weekly", "as_needed"))
        require(medication.reminderTime == null || medication.reminderTime in 0L..1439L)
        if (medication.id == 0) medicationDao.insertMedication(medication)
        else medicationDao.updateMedication(medication) // REPLACE would cascade-delete dose history.
    }

    suspend fun setArchived(id: Int, archived: Boolean) = medicationDao.setArchived(id, archived)

    suspend fun getAllMedicationLogsSync(): List<MedicationLog> = medicationDao.getAllMedicationLogsSync()

    suspend fun getLogsForRangeSync(start: Long, end: Long) = medicationDao.getLogsForRangeSync(start, end)

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

    suspend fun logDose(date: Long, medicationId: Int, timestamp: Long = System.currentTimeMillis()) {
        LogValidation.date(date)
        medicationDao.logMedication(MedicationLog(date = date, medicationId = medicationId, timestamp = timestamp))
    }

    suspend fun removeDose(id: Long) = medicationDao.deleteDose(id)

}
