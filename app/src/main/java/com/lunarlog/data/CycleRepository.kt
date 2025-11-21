package com.lunarlog.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CycleRepository @Inject constructor(
    private val cycleDao: CycleDao
) {
    suspend fun insertCycle(cycle: Cycle) {
        cycleDao.insertCycle(cycle)
    }

    suspend fun updateCycle(cycle: Cycle) {
        cycleDao.updateCycle(cycle)
    }

    fun getAllCycles(): Flow<List<Cycle>> {
        return cycleDao.getAllCycles()
    }
}
