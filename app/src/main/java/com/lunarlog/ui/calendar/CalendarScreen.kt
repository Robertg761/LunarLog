package com.lunarlog.ui.calendar

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import androidx.compose.ui.platform.LocalLocale
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
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
    var previewDay by remember { mutableStateOf<CalendarDayUiModel?>(null) }
    
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
                },
                onToday = {
                    scope.launch { pagerState.animateScrollToPage(initialPage) }
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
                        onDaySelected = { previewDay = it }
                    )
                }
            }
            
            // Legend
            CalendarLegend(modifier = Modifier.padding(16.dp))
        }
    }

    previewDay?.let { day ->
        ModalBottomSheet(
            onDismissRequest = { previewDay = null }
        ) {
            CalendarDayPreviewSheet(
                day = day,
                onEdit = {
                    previewDay = null
                    onDayClicked(day.date.toEpochDay())
                }
            )
        }
    }
}

@Composable
fun CalendarLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.82f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(text = "Period") {
                drawCircle(color = PeriodSurface)
            }
            LegendItem(text = "Predicted") {
                drawRoundRect(
                    color = PeriodRed.copy(alpha = 0.08f),
                    cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                )
                drawRoundRect(
                    color = PeriodRed.copy(alpha = 0.55f),
                    cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(0.82f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        FlowIntensityLegendItem(modifier = Modifier.padding(top = 2.dp))
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
fun FlowIntensityLegendItem(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "Flow intensity",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Light",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Canvas(modifier = Modifier.size(width = 76.dp, height = 18.dp)) {
                val radius = size.height / 2
                val spacing = size.width / 4
                for (level in 1..4) {
                    drawCircle(
                        color = lerp(PeriodSurface, PeriodRed, level / 4f),
                        radius = radius,
                        center = Offset(spacing * (level - 0.5f), center.y)
                    )
                }
            }
            Text(
                text = "Heavy",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
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
                text = currentMonth.month.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentMonth.year.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onToday) {
                Text("Today")
            }
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
    onDaySelected: (CalendarDayUiModel) -> Unit
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
                            onClick = { onDaySelected(day) }
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarDayPreviewSheet(
    day: CalendarDayUiModel,
    onEdit: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d") }
    val statusLabels = remember(day) {
        buildList {
            if (day.data.isPeriod) add("Period")
            if (day.data.isPredictedPeriod) add("Predicted")
            if (day.data.isFertile) add("Fertile")
            if (day.data.isOvulation) add("Ovulation")
            if (day.date == LocalDate.now()) add("Today")
        }
    }
    val details = day.data.symptoms + day.data.moods

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = day.date.format(dateFormatter),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = day.date.year.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit")
            }
        }

        if (statusLabels.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusLabels.forEach { label ->
                    CalendarPreviewChip(text = label)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = if (day.data.flowIntensity > 0) PeriodRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    text = "Flow",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = flowLabel(day.data.flowIntensity),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Symptoms & mood",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (details.isEmpty()) {
                Text(
                    text = "No symptoms logged",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    details.take(8).forEach { detail ->
                        CalendarPreviewChip(text = detail)
                    }
                    if (details.size > 8) {
                        CalendarPreviewChip(text = "+${details.size - 8} more")
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = day.data.notes.ifBlank { "No notes logged" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (day.data.notes.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun CalendarPreviewChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun flowLabel(flowIntensity: Int): String {
    return when (flowIntensity) {
        1 -> "Spotting"
        2 -> "Light"
        3 -> "Medium"
        4 -> "Heavy"
        else -> "None"
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
    val isDark = isSystemInDarkTheme()
    
    // Theme Colors
    val periodColor = PeriodRed
    // Dynamic Base Color
    val periodBaseColor = if (isDark) PeriodSurfaceDark else PeriodSurface
    // Interpolate Color based on Flow
    val finalPeriodColor = if (day.data.hasLog && day.data.flowIntensity > 0) {
        val t = day.data.flowIntensity / 4f // 0.25, 0.5, 0.75, 1.0
        lerp(periodBaseColor, periodColor, t)
    } else {
        periodBaseColor
    }
    
    val fertileColor = FertileGreen
    val ovulationColor = OvulationBlue
    
    // Text Color Logic
    val onPeriodSurface = if (isDark && (day.data.flowIntensity < 3)) {
        OnPeriodSurfaceDark 
    } else {
        OnPeriodSurface
    }
    
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val pressedFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val pressedStrokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .semantics {
                val statuses = buildList {
                    if (day.data.isPeriod) add("period")
                    if (day.data.isPredictedPeriod) add("predicted period")
                    if (day.data.isFertile) add("fertile")
                    if (day.data.isOvulation) add("ovulation")
                    if (isToday) add("today")
                }
                contentDescription = if (statuses.isEmpty()) {
                    "${day.date.dayOfMonth}"
                } else {
                    "${day.date.dayOfMonth}, ${statuses.joinToString(", ")}"
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
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
            fun drawConnectedFill(type: PeriodType, color: Color) {
                when (type) {
                    PeriodType.START -> {
                        drawCircle(
                            color = color,
                            radius = radius,
                            center = center
                        )
                        drawRect(
                            color = color,
                            topLeft = Offset(cx, barTop),
                            size = Size(w - cx, barHeight)
                        )
                    }
                    PeriodType.MIDDLE -> {
                        drawRect(
                            color = color,
                            topLeft = Offset(0f, barTop),
                            size = Size(w, barHeight)
                        )
                    }
                    PeriodType.END -> {
                        drawRect(
                            color = color,
                            topLeft = Offset(0f, barTop),
                            size = Size(cx, barHeight)
                        )
                        drawCircle(
                            color = color,
                            radius = radius,
                            center = center
                        )
                    }
                    PeriodType.SINGLE -> {
                        drawCircle(
                            color = color,
                            radius = radius,
                            center = center
                        )
                    }
                    PeriodType.NONE -> {}
                }
            }

            // 1. Draw Period Background (Connected Pill)
            if (day.data.isPeriod) {
                // Use finalPeriodColor here
                drawConnectedFill(day.periodType, finalPeriodColor)
            } else if (day.data.isPredictedPeriod) {
                drawConnectedFill(day.predictedPeriodType, periodColor.copy(alpha = 0.08f))
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
                    when (day.predictedPeriodType) {
                        PeriodType.START -> {
                            drawArc(
                                color = periodColor.copy(alpha = 0.55f),
                                startAngle = 90f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(cx - radius, barTop),
                                size = Size(diameter, diameter),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                        }
                        PeriodType.END -> {
                            drawArc(
                                color = periodColor.copy(alpha = 0.55f),
                                startAngle = 270f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(cx - radius, barTop),
                                size = Size(diameter, diameter),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                        }
                        PeriodType.SINGLE -> {
                            drawCircle(
                                color = periodColor.copy(alpha = 0.55f),
                                radius = radius,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                        }
                        PeriodType.MIDDLE,
                        PeriodType.NONE -> {}
                    }
                    val lineBounds = when (day.predictedPeriodType) {
                        PeriodType.START -> cx to w
                        PeriodType.MIDDLE -> 0f to w
                        PeriodType.END -> 0f to cx
                        PeriodType.SINGLE,
                        PeriodType.NONE -> null
                    }
                    if (lineBounds != null) {
                        val (lineStart, lineEnd) = lineBounds
                        drawLine(
                            color = periodColor.copy(alpha = 0.55f),
                            start = Offset(lineStart, barTop),
                            end = Offset(lineEnd, barTop),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        drawLine(
                            color = periodColor.copy(alpha = 0.55f),
                            start = Offset(lineStart, barTop + barHeight),
                            end = Offset(lineEnd, barTop + barHeight),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
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

            if (isPressed) {
                drawCircle(
                    color = pressedFillColor,
                    radius = radius * 1.08f
                )
                drawCircle(
                    color = pressedStrokeColor,
                    radius = radius * 1.08f,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 5. Date Text
        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (day.data.isPeriod) onPeriodSurface 
                   else if (!day.isCurrentMonth) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) 
                   else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (day.data.isPeriod || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}
