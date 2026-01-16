package com.lunarlog.ui.periodhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.core.model.Cycle
import com.lunarlog.data.CycleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PeriodHistoryViewModel @Inject constructor(
    private val cycleRepository: CycleRepository
) : ViewModel() {

    val cycles: StateFlow<List<Cycle>> = cycleRepository.getAllCycles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
