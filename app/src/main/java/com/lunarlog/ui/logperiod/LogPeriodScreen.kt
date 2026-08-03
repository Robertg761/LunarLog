package com.lunarlog.ui.logperiod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.ui.components.LunarLogTopAppBar
import com.lunarlog.ui.components.SuccessOverlay
import com.lunarlog.ui.theme.Spacing
import com.lunarlog.ui.theme.cycleColors
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogPeriodScreen(
    onBack: () -> Unit,
    viewModel: LogPeriodViewModel = hiltViewModel()
) {
    val cycle = cycleColors
    val datePickerState = rememberDateRangePickerState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSuccess by remember { mutableStateOf(false) }
    val isReady = datePickerState.selectedStartDateMillis != null

    // Calculate duration text
    val durationText = remember(datePickerState.selectedStartDateMillis, datePickerState.selectedEndDateMillis) {
        val start = datePickerState.selectedStartDateMillis
        val end = datePickerState.selectedEndDateMillis
        
        if (start != null) {
            if (end != null) {
                val s = Instant.ofEpochMilli(start).atZone(ZoneId.of("UTC")).toLocalDate()
                val e = Instant.ofEpochMilli(end).atZone(ZoneId.of("UTC")).toLocalDate()
                val days = ChronoUnit.DAYS.between(s, e) + 1
                "$days days selected"
            } else {
                "Select end date"
            }
        } else {
            "Select dates"
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            showSuccess = true
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                LunarLogTopAppBar(
                    title = "Log Period",
                    onNavigateBack = onBack
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!isReady || uiState.isSaving) return@ExtendedFloatingActionButton
                        val startDate = datePickerState.selectedStartDateMillis
                        val endDate = datePickerState.selectedEndDateMillis

                        if (startDate != null) {
                            val finalEndDate = endDate ?: startDate
                            viewModel.savePeriod(startDate, finalEndDate)
                        }
                    },
                    expanded = true,
                    icon = { 
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(2.dp),
                                // Whatever the FAB is painting its icon with — which is now
                                // `cycle.onPeriodStrong`, not `onPrimary`. The two used to be the
                                // same colour; since the FAB moved onto the cycle palette they are
                                // not, and a hardcoded `onPrimary` spinner sat at 2.25:1 against
                                // the button in dark mode. Deriving it means the next palette
                                // change can't desynchronise them again.
                                color = LocalContentColor.current,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    text = { Text(if (uiState.isSaving) "Saving..." else "Save Period") },
                    containerColor = if (isReady) cycle.periodStrong else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isReady) cycle.onPeriodStrong else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header Summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xl, vertical = Spacing.sm)
                        .background(cycle.periodContainer, MaterialTheme.shapes.medium)
                        .padding(Spacing.cardPadding)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = cycle.onPeriodContainer)
                        Spacer(modifier = Modifier.width(Spacing.lg))
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.titleMedium,
                            color = cycle.onPeriodContainer
                        )
                    }
                }
                if (!isReady) {
                    Text(
                        text = "Select a start date to enable saving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.xl)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                DateRangePicker(
                    state = datePickerState,
                    modifier = Modifier.weight(1f),
                    title = null,
                    headline = null, // Using custom header above
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        headlineContentColor = MaterialTheme.colorScheme.onSurface,
                        weekdayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        subheadContentColor = MaterialTheme.colorScheme.onSurface,
                        yearContentColor = MaterialTheme.colorScheme.onSurface,
                        currentYearContentColor = MaterialTheme.colorScheme.primary,
                        selectedYearContentColor = cycle.onPeriodStrong,
                        selectedYearContainerColor = cycle.periodStrong,
                        dayContentColor = MaterialTheme.colorScheme.onSurface,
                        disabledDayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        selectedDayContentColor = cycle.onPeriodStrong,
                        disabledSelectedDayContentColor = cycle.onPeriodStrong.copy(alpha = 0.38f),
                        selectedDayContainerColor = cycle.periodStrong,
                        disabledSelectedDayContainerColor = cycle.periodStrong.copy(alpha = 0.38f),
                        todayDateBorderColor = cycle.period,
                        // Was onSurface on PeriodSurface — 1.14:1 in dark mode, i.e. invisible.
                        dayInSelectionRangeContentColor = cycle.onPeriodContainer,
                        dayInSelectionRangeContainerColor = cycle.periodContainer
                    )
                )
            }
        }

        if (showSuccess) {
            SuccessOverlay(onAnimationFinished = {
                viewModel.onNavigatedBack()
                onBack()
            })
        }
    }
}
