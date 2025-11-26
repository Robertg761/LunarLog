package com.lunarlog.data

import javax.inject.Inject
import com.lunarlog.core.model.Cycle
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class CycleRepository @Inject constructor(private val cycleDao: CycleDao) {
    suspend fun insertCycle(cycle: Cycle) = cycleDao.insertCycle(cycle)
    suspend fun updateCycle(cycle: Cycle) = cycleDao.updateCycle(cycle)
    fun getAllCycles(): Flow<List<Cycle>> = cycleDao.getAllCycles()
    suspend fun getAllCyclesSync(): List<Cycle> = cycleDao.getAllCyclesSync()
    fun getCyclesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Cycle>> = cycleDao.getCyclesInRange(startDate, endDate)

    suspend fun togglePeriod(date: LocalDate): String {
        val cycles = cycleDao.getAllCyclesSync()
        val latestCycle = cycles.maxByOrNull { it.startDate }

        return if (latestCycle != null && latestCycle.endDate == null) {
            // Case 1: Open -> End it
            val updatedCycle = latestCycle.copy(endDate = date)
            cycleDao.updateCycle(updatedCycle)
            "Period ended"
        } else if (latestCycle != null && latestCycle.endDate == date) {
            // Case 2: Closed Today -> Re-open (Resume)
            val updatedCycle = latestCycle.copy(endDate = null)
            cycleDao.updateCycle(updatedCycle)
            "Period resumed"
        } else {
            // Case 3: Start New
            val newCycle = Cycle(startDate = date, endDate = null)
            cycleDao.insertCycle(newCycle)
            "Period started"
        }
    }
}
