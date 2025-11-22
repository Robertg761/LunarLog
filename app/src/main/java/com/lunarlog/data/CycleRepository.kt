package com.lunarlog.data

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class CycleRepository @Inject constructor(private val cycleDao: CycleDao) {
    suspend fun insertCycle(cycle: Cycle) = cycleDao.insert(cycle)
    suspend fun updateCycle(cycle: Cycle) = cycleDao.update(cycle)
    fun getAllCycles(): Flow<List<Cycle>> = cycleDao.getAllCycles()
}
