package com.lunarlog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.data.Cycle
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLog
import com.lunarlog.data.DailyLogRepository
import com.lunarlog.logic.CyclePredictionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class HomeUiState(
    val daysUntilPeriod: Int = 0,
    val currentCycleDay: Int = 0,
    val isFertile: Boolean = false,
    val isLoading: Boolean = true,
    val isPeriodActive: Boolean = false,
    val quickLogSymptoms: List<String> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        cycleRepository.getAllCycles(),
        dailyLogRepository.getAllLogs()
    ) { cycles, logs ->
        if (cycles.isEmpty()) {
            HomeUiState(isLoading = false)
        } else {
            val sortedCycles = cycles.sortedByDescending { it.startDate }
            val lastCycle = sortedCycles.first()
            val averageLength = CyclePredictionUtils.calculateAverageCycleLength(cycles)
            val nextPeriodStart = CyclePredictionUtils.predictNextPeriod(lastCycle, averageLength)
            val today = LocalDate.now()

            val daysUntil = ChronoUnit.DAYS.between(today, nextPeriodStart).toInt()
            val currentCycleDay = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(lastCycle.startDate), today).toInt() + 1

            // Check fertile window
            val (fertileStart, fertileEnd) = CyclePredictionUtils.predictFertileWindow(nextPeriodStart)
            val isFertile = today >= fertileStart && today <= fertileEnd

            // Simple check if period is active (assuming 5 days default or checking endDate if exists)
            val isPeriodActive = if (lastCycle.endDate != null) {
                !today.isAfter(LocalDate.ofEpochDay(lastCycle.endDate))
            } else {
                currentCycleDay <= 5
            }

            val quickLogSymptoms = getTopSymptomsForPhase(currentCycleDay, cycles, logs)

            HomeUiState(
                daysUntilPeriod = daysUntil,
                currentCycleDay = currentCycleDay,
                isFertile = isFertile,
                isPeriodActive = isPeriodActive,
                isLoading = false,
                quickLogSymptoms = quickLogSymptoms
            )
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    private fun getTopSymptomsForPhase(currentDay: Int, cycles: List<Cycle>, logs: List<DailyLog>): List<String> {
        if (cycles.isEmpty() || logs.isEmpty()) return emptyList()

        val phaseRange = when (currentDay) {
            in 1..5 -> 1..5       // Menstrual
            in 6..13 -> 6..13     // Follicular
            in 14..17 -> 14..17   // Ovulation (Approx)
            else -> 18..35        // Luteal
        }

        val symptomCounts = mutableMapOf<String, Int>()
        val sortedCycles = cycles.sortedBy { it.startDate }

        for (log in logs) {
            val logDate = LocalDate.ofEpochDay(log.date)
            // Find the cycle this log belongs to
            val cycle = sortedCycles.findLast { it.startDate <= log.date } ?: continue
            
            val cycleStart = LocalDate.ofEpochDay(cycle.startDate)
            val dayOfCycle = ChronoUnit.DAYS.between(cycleStart, logDate).toInt() + 1

            if (dayOfCycle in phaseRange) {
                for (symptom in log.symptoms) {
                    symptomCounts[symptom] = symptomCounts.getOrDefault(symptom, 0) + 1
                }
            }
        }

        return symptomCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }

    fun togglePeriod() {
        viewModelScope.launch {
            val isActive = uiState.value.isPeriodActive
            val today = LocalDate.now().toEpochDay()
            
            if (isActive) {
                // End period
                val cycles = cycleRepository.getAllCyclesSync()
                val latestCycle = cycles.maxByOrNull { it.startDate }
                
                if (latestCycle != null && latestCycle.endDate == null) {
                    val updatedCycle = latestCycle.copy(endDate = today)
                    cycleRepository.updateCycle(updatedCycle)
                }
            } else {
                // Start period
                val newCycle = Cycle(startDate = today, endDate = null)
                cycleRepository.insertCycle(newCycle)
            }
        }
    }

    fun logQuickSymptom(symptom: String) {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            // We need to collect the first value from the flow
            val existingLog = dailyLogRepository.getLogForDate(today).first()
            
            val newLog = if (existingLog != null) {
                if (existingLog.symptoms.contains(symptom)) return@launch
                existingLog.copy(symptoms = existingLog.symptoms + symptom)
            } else {
                DailyLog(date = today, symptoms = listOf(symptom))
            }
            
            dailyLogRepository.saveLog(newLog)
        }
    }

    fun getShareableStatus(): String {
        val state = uiState.value
        if (state.isLoading) return "Loading..."

        return """
            🌙 LunarLog Status Update
            
            📅 Day ${state.currentCycleDay} of Cycle
            ⏳ Period due in ${state.daysUntilPeriod} days
            ${if (state.isFertile) "🌿 Likely Fertile Window" else ""}
            
            Sent from my private LunarLog
        """.trimIndent()
    }
}
