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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.ui.components.SuccessOverlay
import com.lunarlog.ui.theme.PeriodRed
import com.lunarlog.ui.theme.PeriodSurface
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogPeriodScreen(
    onBack: () -> Unit,
    viewModel: LogPeriodViewModel = hiltViewModel()
) {
    val datePickerState = rememberDateRangePickerState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSuccess by remember { mutableStateOf(false) }

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
                CenterAlignedTopAppBar(
                    title = { Text("Log Period", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            floatingActionButton = {
                val isReady = datePickerState.selectedStartDateMillis != null
                ExtendedFloatingActionButton(
                    onClick = {
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
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    text = { Text(if (uiState.isSaving) "Saving..." else "Save Period") },
                    containerColor = if (isReady) PeriodRed else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isReady) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .background(PeriodSurface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = PeriodRed)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                DateRangePicker(
                    state = datePickerState,
                    modifier = Modifier.weight(1f),
                    title = null,
                    headline = null, // Using custom header above
                    showModeToggle = false,
                    colors = androidx.compose.material3.DatePickerDefaults.colors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        headlineContentColor = MaterialTheme.colorScheme.onSurface,
                        weekdayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        subheadContentColor = MaterialTheme.colorScheme.onSurface,
                        yearContentColor = MaterialTheme.colorScheme.onSurface,
                        currentYearContentColor = MaterialTheme.colorScheme.primary,
                        selectedYearContentColor = Color.White,
                        selectedYearContainerColor = PeriodRed,
                        dayContentColor = MaterialTheme.colorScheme.onSurface,
                        disabledDayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        selectedDayContentColor = Color.White,
                        disabledSelectedDayContentColor = Color.White.copy(alpha = 0.38f),
                        selectedDayContainerColor = PeriodRed,
                        disabledSelectedDayContainerColor = PeriodRed.copy(alpha = 0.38f),
                        todayDateBorderColor = PeriodRed,
                        dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onSurface,
                        dayInSelectionRangeContainerColor = PeriodSurface
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
