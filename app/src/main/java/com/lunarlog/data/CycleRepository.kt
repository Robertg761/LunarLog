package com.lunarlog.data

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class CycleRepository @Inject constructor(private val cycleDao: CycleDao) {
    suspend fun insertCycle(cycle: Cycle) = cycleDao.insertCycle(cycle)
    suspend fun updateCycle(cycle: Cycle) = cycleDao.updateCycle(cycle)
    fun getAllCycles(): Flow<List<Cycle>> = cycleDao.getAllCycles()
    suspend fun getAllCyclesSync(): List<Cycle> = cycleDao.getAllCyclesSync()
    fun getCyclesInRange(startDate: Long, endDate: Long): Flow<List<Cycle>> = cycleDao.getCyclesInRange(startDate, endDate)
}
