package com.lunarlog.ui.periodhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunarlog.core.model.Cycle
import com.lunarlog.data.CycleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class PeriodHistoryViewModel @Inject constructor(
    private val cycleRepository: CycleRepository
) : ViewModel() {

    val error = MutableStateFlow<String?>(null)
    val isLoading = MutableStateFlow(true)
    val cycles: StateFlow<List<Cycle>> = cycleRepository.getAllCycles()
        .onStart { isLoading.value = true; error.value = null }.onEach { isLoading.value = false }
        .catch { if (it is kotlinx.coroutines.CancellationException) throw it; error.value = "Unable to load periods. Reopen this screen to retry."; isLoading.value = false; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
