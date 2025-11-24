package com.lunarlog.ui.calendar

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.ui.theme.FertileGreen
import com.lunarlog.ui.theme.OvulationBlue
import com.lunarlog.ui.theme.PeriodRed
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CalendarScreen(
    onDayClicked: (Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            if (uiState is CalendarUiState.Success) {
                val state = uiState as CalendarUiState.Success
                CalendarHeader(
                    currentMonth = state.currentMonth,
                    onPreviousMonth = viewModel::onPreviousMonth,
                    onNextMonth = viewModel::onNextMonth
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                CalendarUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is CalendarUiState.Success -> {
                    CalendarContent(
                        days = state.days,
                        onDayClicked = onDayClicked,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarHeader(
    currentMonth: java.time.YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
        }
        
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CalendarContent(
    days: List<CalendarDayState>,
    onDayClicked: (Long) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    Column {
        // Weekday Headers
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = days,
                key = { it.date.toEpochDay() }
            ) { dayState ->
                CalendarDay(
                    dayState = dayState,
                    onClick = { onDayClicked(dayState.date.toEpochDay()) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarDay(
    dayState: CalendarDayState,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            val summary = buildString {
                if (dayState.isPeriod) append("Period")
                if (dayState.isOvulation) { if (isNotEmpty()) append(", "); append("Ovulation") }
                if (dayState.isFertile && !dayState.isOvulation) { if (isNotEmpty()) append(", "); append("Fertile") }
                if (dayState.isPredictedPeriod) { if (isNotEmpty()) append(", "); append("Predicted") }
                if (isEmpty()) append("Day ${dayState.date.dayOfMonth}")
            }
            PlainTooltip {
                Text(summary)
            }
        },
        state = tooltipState
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f) // Square cells
                .padding(2.dp)
                .clip(CircleShape)
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                state = rememberSharedContentState(key = "day_${dayState.date.toEpochDay()}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    } else Modifier
                )
                .combinedClickable(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { tooltipState.show() }
                    }
                )
                .then(
                    if (dayState.isPeriod || dayState.flowIntensity > 0) {
                        val alpha = when (dayState.flowIntensity) {
                            1 -> 0.4f
                            2 -> 0.6f
                            3 -> 0.8f
                            4 -> 1.0f
                            else -> 0.6f // Default for "isPeriod" without specific flow log
                        }
                        Modifier.background(PeriodRed.copy(alpha = alpha))
                    } else if (dayState.isPredictedPeriod) {
                        Modifier.border(2.dp, PeriodRed.copy(alpha = 0.6f), CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Background indicators for fertility/ovulation
            if (!dayState.isPeriod && dayState.flowIntensity == 0) { // Don't overlay fertility on period
                if (dayState.isOvulation) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, OvulationBlue, CircleShape)
                    )
                } else if (dayState.isFertile) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(FertileGreen, CircleShape)
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                    )
                }
            }

            // Log indicator
            if (dayState.hasLog) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                )
            }

            Text(
                text = dayState.date.dayOfMonth.toString(),
                color = if (dayState.isPeriod) Color.White 
                       else if (!dayState.isCurrentMonth) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) 
                       else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
