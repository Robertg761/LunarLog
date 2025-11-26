package com.lunarlog.ui.calendar

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CalendarScreen(
    onDayClicked: (Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    // Collect the GLOBAL data state
    val calendarState by viewModel.calendarState.collectAsState()

    // Pager Setup
    val initialPage = 5000
    val pagerState = rememberPagerState(initialPage = initialPage) { 10000 }
    val scope = rememberCoroutineScope()
    
    // Calculate current visible month based on Pager
    // Optimization: Derive state to avoid unnecessary recompositions
    val currentMonth by remember {
        derivedStateOf {
            YearMonth.now().plusMonths((pagerState.currentPage - initialPage).toLong())
        }
    }

    Scaffold(
        topBar = {
            CalendarHeader(
                currentMonth = currentMonth,
                onPrevious = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                },
                onNext = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Day Headers (S M T W T F S)
            CalendarWeekDaysHeader()
            
            // The Infinite Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f) // Fill remaining space
            ) { page ->
                val pageMonth = YearMonth.now().plusMonths((page - initialPage).toLong())
                
                // Fetch the 42 days for this page from the global state
                // This is a fast CPU operation
                val days = remember(calendarState, pageMonth) {
                    viewModel.getPageData(pageMonth, calendarState)
                }

                if (calendarState is CalendarDataState.Loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    CalendarMonthPage(
                        days = days,
                        onDayClicked = onDayClicked
                    )
                }
            }
            
            // Legend
            CalendarLegend(modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun CalendarLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(text = "Period") {
            drawCircle(color = PeriodSurface)
        }
        LegendItem(text = "Predicted") {
            drawCircle(
                color = PeriodRed.copy(alpha = 0.5f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }
        LegendItem(text = "Fertile") {
            drawCircle(
                color = FertileGreen,
                radius = size.minDimension / 4
            )
        }
        LegendItem(text = "Ovulation") {
            drawCircle(color = OvulationBlue.copy(alpha = 0.2f))
            drawCircle(
                color = OvulationBlue,
                radius = size.minDimension / 4,
                center = Offset(center.x, center.y - size.minDimension / 3)
            )
        }
    }
}

@Composable
fun LegendItem(
    text: String,
    icon: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            icon()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentMonth.year.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
        }
    }
}

@Composable
fun CalendarWeekDaysHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
        weekDays.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CalendarMonthPage(
    days: List<CalendarDayUiModel>,
    onDayClicked: (Long) -> Unit
) {
    // Custom Layout: fixed 6 rows x 7 cols
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        for (weekIndex in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (dayIndex in 0 until 7) {
                    val day = days.getOrNull(weekIndex * 7 + dayIndex)
                    if (day != null) {
                        CalendarDayCell(
                            day = day,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onDayClicked(day.date.toEpochDay()) }
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: CalendarDayUiModel,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isToday = day.date == LocalDate.now()
    
    // Theme Colors
    val periodColor = PeriodRed
    val periodBgColor = PeriodSurface
    val fertileColor = FertileGreen
    val ovulationColor = OvulationBlue
    val onPeriodSurface = OnPeriodSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }),
        contentAlignment = Alignment.Center
    ) {
        // Custom Drawing for Connected Periods
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2
            val cy = h / 2
            // Adjusted Diameter for better spacing (0.8f instead of 0.85f)
            val diameter = minOf(w, h) * 0.8f
            val radius = diameter / 2
            val barTop = cy - radius
            val barHeight = diameter

            // 1. Draw Period Background (Connected Pill)
            if (day.data.isPeriod) {
                when (day.periodType) {
                    PeriodType.START -> {
                        // Rounded Left (Circle), Flat Right (Rect)
                        drawCircle(
                            color = periodBgColor,
                            radius = radius,
                            center = center
                        )
                        drawRect(
                            color = periodBgColor,
                            topLeft = Offset(cx, barTop),
                            size = Size(w - cx, barHeight)
                        )
                    }
                    PeriodType.MIDDLE -> {
                        // Flat Left, Flat Right
                        drawRect(
                            color = periodBgColor,
                            topLeft = Offset(0f, barTop),
                            size = Size(w, barHeight)
                        )
                    }
                    PeriodType.END -> {
                        // Flat Left (Rect), Rounded Right (Circle)
                        drawRect(
                            color = periodBgColor,
                            topLeft = Offset(0f, barTop),
                            size = Size(cx, barHeight)
                        )
                        drawCircle(
                            color = periodBgColor,
                            radius = radius,
                            center = center
                        )
                    }
                    PeriodType.SINGLE -> {
                        // Rounded All (Circle)
                        drawCircle(
                            color = periodBgColor,
                            radius = radius,
                            center = center
                        )
                    }
                    PeriodType.NONE -> {}
                }
            }

            // 2. Draw Today Ring (Refined)
            if (isToday) {
                // If period active, draw a ring outside? Or just a solid ring behind?
                // Let's do a solid circle behind the text but distinct from period logic
                if (!day.data.isPeriod) {
                    drawCircle(
                        color = TodayRing.copy(alpha = 0.2f),
                        radius = radius
                    )
                    drawCircle(
                        color = TodayRing,
                        style = Stroke(width = 2.dp.toPx()),
                        radius = radius
                    )
                } else {
                    // If on period, just a white ring to contrast
                    drawCircle(
                        color = Color.White,
                        style = Stroke(width = 2.dp.toPx()),
                        radius = radius * 0.9f
                    )
                }
            }

            // 3. Draw Indicators (Fertile/Ovulation/Predicted)
            if (!day.data.isPeriod) {
                if (day.data.isOvulation) {
                    drawCircle(
                        color = ovulationColor.copy(alpha = 0.2f),
                        radius = radius
                    )
                    drawCircle(
                        color = ovulationColor,
                        radius = 4.dp.toPx(),
                        center = Offset(cx, cy - radius - 6.dp.toPx()) // Top Dot
                    )
                } else if (day.data.isFertile) {
                    drawCircle(
                        color = fertileColor,
                        radius = 3.dp.toPx(),
                        center = Offset(cx, cy - radius - 6.dp.toPx())
                    )
                } else if (day.data.isPredictedPeriod) {
                    drawCircle(
                        color = periodColor.copy(alpha = 0.5f),
                        radius = radius,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }
            }

            // 4. Log Indicator (Bottom Dot)
            if (day.data.hasLog) {
                drawCircle(
                    color = if (day.data.isPeriod) onPeriodSurface else onSurfaceVariant,
                    radius = 2.dp.toPx(),
                    center = Offset(cx, cy + radius - 6.dp.toPx())
                )
            }
        }

        // 5. Date Text
        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (day.data.isPeriod) OnPeriodSurface 
                   else if (!day.isCurrentMonth) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) 
                   else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (day.data.isPeriod || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}
