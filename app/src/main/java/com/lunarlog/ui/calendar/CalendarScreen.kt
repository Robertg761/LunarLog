package com.lunarlog.ui.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.ui.components.LunarLogCenterTopAppBar
import com.lunarlog.ui.components.CardDivider
import com.lunarlog.ui.components.LoadingState
import com.lunarlog.ui.theme.*
import com.lunarlog.ui.util.FullDayDate
import com.lunarlog.ui.util.flowLabel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import androidx.compose.ui.platform.LocalLocale
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * The horizontal gutter shared by the weekday header and the six-week grid.
 *
 * One value for both is what makes the columns line up: the header's seven `weight(1f)` slots and
 * the grid's seven cells resolve against the same available width. Deliberately 8dp rather than the
 * usual 16dp screen inset — seven columns inside a 360dp screen leaves each cell at 49dp, which is
 * still a valid touch target; a 16dp gutter would push it under 48dp.
 */
private val CalendarGridInset = Spacing.sm

/** Fill and stroke alphas for the predicted-period pill, shared by the grid and the legend swatch. */
private const val PredictedFillAlpha = 0.08f
private const val PredictedStrokeAlpha = 0.55f

/** The predicted-period dash, defined once so the legend swatch cannot drift from the real cell. */
private val PredictedDash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

private fun DrawScope.predictedStroke(): Stroke =
    Stroke(width = 2.dp.toPx(), pathEffect = PredictedDash)

/** The legend's stand-in for a single-day predicted pill, drawn from the same alphas and dash. */
private fun DrawScope.drawPredictedSwatch(accent: Color) {
    val corner = CornerRadius(size.height / 2, size.height / 2)
    drawRoundRect(
        color = accent.copy(alpha = PredictedFillAlpha),
        cornerRadius = corner
    )
    drawRoundRect(
        color = accent.copy(alpha = PredictedStrokeAlpha),
        cornerRadius = corner,
        style = predictedStroke()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onDayClicked: (Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    // Collect the GLOBAL data state
    val calendarState by viewModel.calendarState.collectAsState()

    // Pager Setup
    val initialPage = 5000
    val pagerState = rememberPagerState(initialPage = initialPage) { 10000 }
    val scope = rememberCoroutineScope()
    var previewDay by remember { mutableStateOf<CalendarDayUiModel?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Calculate current visible month based on Pager.
    // targetPage rather than currentPage: currentPage only updates once a page *settles*, so the
    // title used to lag a swipe by the whole fling animation. targetPage flips as the gesture
    // commits, which is when the user expects the month name to change.
    val currentMonth by remember {
        derivedStateOf {
            YearMonth.now().plusMonths((pagerState.targetPage - initialPage).toLong())
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
                    scope.launch {
                        // The pager has 10000 pages; animating from an arbitrary one to page 5000
                        // composes every page in between. Only animate a near jump.
                        if (abs(pagerState.currentPage - initialPage) > 2) {
                            pagerState.scrollToPage(initialPage)
                        } else {
                            pagerState.animateScrollToPage(initialPage)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Day Headers (S M T W T F S) — same gutter as the grid, so the columns align.
            CalendarWeekDaysHeader(modifier = Modifier.padding(horizontal = CalendarGridInset))

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
                    LoadingState()
                } else {
                    CalendarMonthPage(
                        days = days,
                        onDaySelected = { previewDay = it },
                        modifier = Modifier.padding(horizontal = CalendarGridInset)
                    )
                }
            }

            // Legend
            CalendarLegend(
                modifier = Modifier.padding(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.md
                )
            )
        }
    }

    previewDay?.let { day ->
        ModalBottomSheet(
            onDismissRequest = { previewDay = null },
            sheetState = sheetState,
            // Sheets take the same warm `surfaceContainer` as LunarLogCard rather than
            // BottomSheetDefaults' `surfaceContainerLow`, so a sheet reads as the same material
            // as the cards it slides over instead of a second, paler one.
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            CalendarDayPreviewSheet(
                day = day,
                onEdit = {
                    // Let the sheet animate down before the nav push; clearing previewDay first
                    // yanks it out of composition and the user sees a blink instead of a transition.
                    val epochDay = day.date.toEpochDay()
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        previewDay = null
                        onDayClicked(epochDay)
                    }
                }
            )
        }
    }
}

/**
 * Four evenly weighted swatch columns over a flow ramp.
 *
 * The old layout was two 2-item rows at 82% width plus a third ramp row on its own vertical rhythm,
 * which read as three unrelated blocks and ate a sixth of the grid's height. One row of four equal
 * columns is both shorter and self-evidently a set.
 */
@Composable
fun CalendarLegend(modifier: Modifier = Modifier) {
    val cycle = cycleColors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            LegendItem(text = "Period", modifier = Modifier.weight(1f)) {
                drawCircle(color = cycle.periodContainer)
            }
            LegendItem(text = "Predicted", modifier = Modifier.weight(1f)) {
                drawPredictedSwatch(cycle.period)
            }
            LegendItem(text = "Fertile", modifier = Modifier.weight(1f)) {
                drawCircle(
                    color = cycle.fertile,
                    radius = size.minDimension / 4
                )
            }
            LegendItem(text = "Ovulation", modifier = Modifier.weight(1f)) {
                drawCircle(color = cycle.ovulation.copy(alpha = 0.2f))
                drawCircle(
                    color = cycle.ovulation,
                    radius = size.minDimension / 4,
                    center = Offset(center.x, center.y - size.minDimension / 3)
                )
            }
        }

        FlowIntensityLegendItem(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun LegendItem(
    text: String,
    modifier: Modifier = Modifier,
    icon: DrawScope.() -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            icon()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            // Two lines, not one. A quarter of a 360dp phone is ~76dp, which "Ovulation" already
            // fills at the default text size — pinned to one line with the default Clip overflow it
            // lost its tail mid-word as soon as the user raised the font scale. Wrapping is the
            // graceful degradation here; the ellipsis is only the backstop past two lines.
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FlowIntensityLegendItem(modifier: Modifier = Modifier) {
    val cycle = cycleColors
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The weight lives on the caption rather than on a Spacer between the two groups. A Row
        // measures its unweighted children first and hands each the space the previous ones left,
        // so with the slack parked in a Spacer the *last* child paid for any overflow: at a raised
        // font scale "Heavy" was measured against whatever remained and squeezed to nothing, and
        // the ramp lost the half of its key that says which end is which. Carrying the slack here
        // instead means this caption is what gives — it wraps to a second line, and "Light",
        // the swatches and "Heavy" keep their intrinsic widths.
        Text(
            text = "Flow intensity",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = labelColor
        )
        Text(
            text = "Light",
            style = MaterialTheme.typography.labelSmall,
            color = labelColor
        )
        Canvas(modifier = Modifier.size(width = 80.dp, height = 16.dp)) {
            val radius = size.height / 2
            val spacing = size.width / 4
            for (level in 1..4) {
                drawCircle(
                    color = lerp(cycle.periodContainer, cycle.period, level / 4f),
                    radius = radius,
                    center = Offset(spacing * (level - 0.5f), center.y)
                )
            }
        }
        Text(
            text = "Heavy",
            style = MaterialTheme.typography.labelSmall,
            color = labelColor
        )
    }
}

/**
 * The month bar: [prev] [month over year] [today][next].
 *
 * A real `CenterAlignedTopAppBar` rather than the Row this used to be, so Calendar starts its
 * content at the same 64dp as every other screen instead of ~105dp — switching tabs no longer makes
 * the grid jump. "Today" moves out of the centre column onto the bar's own baseline, and is disabled
 * rather than hidden while the current month is showing so the actions row does not reflow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val isViewingCurrentMonth = currentMonth == YearMonth.now()
    LunarLogCenterTopAppBar(
        navigationIcon = {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
            }
        },
        actions = {
            IconButton(onClick = onToday, enabled = !isViewingCurrentMonth) {
                Icon(Icons.Filled.Today, contentDescription = "Jump to today")
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
            }
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentMonth.month.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1
            )
            Text(
                text = currentMonth.year.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CalendarWeekDaysHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.sm)
    ) {
        val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
        weekDays.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CalendarMonthPage(
    days: List<CalendarDayUiModel>,
    onDaySelected: (CalendarDayUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    // Fixed 6 rows x 7 cols. Every row takes an equal share of the remaining height and every cell
    // an equal share of the row, with an explicit 4dp between rows — SpaceEvenly on top of
    // weight(1f) children was a no-op, so the rows used to sit flush against each other.
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        for (weekIndex in 0 until 6) {
            // No horizontal gap: the period pill is drawn across cell boundaries and any spacing
            // between columns would cut it into segments.
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f)
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
                        Spacer(Modifier.weight(1f).fillMaxHeight())
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
    val dateFormatter = FullDayDate
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.sheetHorizontal)
            .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = day.date.format(dateFormatter),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = day.date.year.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Edit")
            }
        }

        if (statusLabels.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                statusLabels.forEach { label ->
                    CalendarPreviewChip(text = label)
                }
            }
        }

        CardDivider()

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.WaterDrop,
                contentDescription = null,
                tint = if (day.data.flowIntensity > 0) cycleColors.period else MaterialTheme.colorScheme.onSurfaceVariant
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

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
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

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            style = MaterialTheme.typography.labelMedium
        )
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
    val cycle = cycleColors

    // Theme Colors
    val periodColor = cycle.period
    val periodBaseColor = cycle.periodContainer
    // Interpolate Color based on Flow
    val finalPeriodColor = if (day.data.hasLog && day.data.flowIntensity > 0) {
        val t = day.data.flowIntensity / 4f // 0.25, 0.5, 0.75, 1.0
        lerp(periodBaseColor, periodColor, t)
    } else {
        periodBaseColor
    }

    val fertileColor = cycle.fertile
    val ovulationColor = cycle.ovulation

    // The fill ramps from the soft container to the saturated accent, so the day number has to
    // follow it rather than being pinned to one on-colour: at flow 4 the on-container value drops
    // to ~3.2:1. Picking black/white per rendered fill keeps every step above 4.5:1.
    val onPeriodSurface = bestContentColor(finalPeriodColor)

    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val locale = LocalLocale.current.platformLocale
    val pressedFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val pressedStrokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // The ripple is suppressed on purpose (a rectangular ripple across a connected period pill
    // looks wrong), so the hand-drawn indicator has to supply the fade a ripple would have given
    // it — otherwise a scroll-cancelled press flickers on and off in a single frame.
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "dayCellPress"
    )

    Box(
        modifier = modifier
            .semantics {
                val statuses = buildList {
                    if (!day.isCurrentMonth) {
                        add(day.date.month.getDisplayName(TextStyle.FULL, locale))
                    }
                    if (day.data.isPeriod) {
                        add("period")
                        if (day.data.flowIntensity > 0) add(flowLabel(day.data.flowIntensity))
                    }
                    if (day.data.isPredictedPeriod) add("predicted period")
                    if (day.data.isFertile) add("fertile")
                    if (day.data.isOvulation) add("ovulation")
                    if (day.data.hasLog) add("entry logged")
                    if (isToday) add("today")
                }
                contentDescription = if (statuses.isEmpty()) {
                    "${day.date.dayOfMonth}"
                } else {
                    "${day.date.dayOfMonth}, ${statuses.joinToString(", ")}"
                }
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = "Open day details",
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
                drawConnectedFill(day.predictedPeriodType, periodColor.copy(alpha = PredictedFillAlpha))
            }

            // 2. Draw Today Ring
            if (isToday) {
                if (!day.data.isPeriod) {
                    // Ring only. This used to also fill the cell with the marker colour at 20%,
                    // which turned today into a tan disc — the one warm-orange object in a pink
                    // app, and heavier than the period days it sat beside. As an outline it stays
                    // legible and matches the grid's own grammar: a fill means the day has data,
                    // an outline means it is being pointed at.
                    drawCircle(
                        color = cycle.today,
                        style = Stroke(width = 2.dp.toPx()),
                        radius = radius
                    )
                } else {
                    // On a period fill, ring in whatever the day number uses — a hardcoded white
                    // ring sat at 1.29:1 on the light pink fill.
                    drawCircle(
                        color = onPeriodSurface,
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
                    val predictedOutline = periodColor.copy(alpha = PredictedStrokeAlpha)
                    when (day.predictedPeriodType) {
                        PeriodType.START -> {
                            drawArc(
                                color = predictedOutline,
                                startAngle = 90f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(cx - radius, barTop),
                                size = Size(diameter, diameter),
                                style = predictedStroke()
                            )
                        }
                        PeriodType.END -> {
                            drawArc(
                                color = predictedOutline,
                                startAngle = 270f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(cx - radius, barTop),
                                size = Size(diameter, diameter),
                                style = predictedStroke()
                            )
                        }
                        PeriodType.SINGLE -> {
                            drawCircle(
                                color = predictedOutline,
                                radius = radius,
                                style = predictedStroke()
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
                            color = predictedOutline,
                            start = Offset(lineStart, barTop),
                            end = Offset(lineEnd, barTop),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PredictedDash
                        )
                        drawLine(
                            color = predictedOutline,
                            start = Offset(lineStart, barTop + barHeight),
                            end = Offset(lineEnd, barTop + barHeight),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PredictedDash
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

            if (pressProgress > 0f) {
                drawCircle(
                    color = pressedFillColor,
                    radius = radius * 1.08f,
                    alpha = pressProgress
                )
                drawCircle(
                    color = pressedStrokeColor,
                    radius = radius * 1.08f,
                    alpha = pressProgress,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 5. Date Text
        Text(
            text = day.date.dayOfMonth.toString(),
            // Out-of-month days used to be onSurface at 38% alpha — under 3:1 on the page and
            // barely visible where they fall inside a predicted-period fill. onSurfaceVariant at
            // full alpha is still clearly the secondary tier but stays legible on every fill.
            // The period branch keeps bestContentColor(), which tracks the flow-ramped fill.
            color = when {
                day.data.isPeriod -> onPeriodSurface
                !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            },
            // labelLarge, not bodyMedium: body roles carry 0.25sp tracking, which visibly pushes a
            // numeral off the centre of a circular fill. One weight for every state, because
            // swapping to Bold changes each numeral's optical width and the columns shimmer as you
            // page — period and today emphasis comes from the fill and the ring instead.
            style = MaterialTheme.typography.labelLarge
        )
    }
}
