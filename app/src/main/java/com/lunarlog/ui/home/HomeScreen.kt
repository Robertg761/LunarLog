package com.lunarlog.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.ui.components.LunarLogCard
import com.lunarlog.ui.components.LunarLogCenterTopAppBar
import com.lunarlog.ui.theme.BrandMoon
import com.lunarlog.ui.theme.BrandRoseDeep
import com.lunarlog.ui.theme.BrandRoseLight
import com.lunarlog.ui.theme.DisplayHuge
import com.lunarlog.ui.theme.Spacing
import com.lunarlog.ui.theme.cycleColors
import com.lunarlog.ui.theme.shimmerEffect
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Below this scroll offset the hero ring has already faded out, so it stops composing and animating. */
private const val HeroFadeDistancePx = 600f

/** Approximate laid-out heights of the two real cards, so the skeleton does not resize on load. */
private val SummaryCardSkeletonHeight = 104.dp
private val FertilityCardSkeletonHeight = 80.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    onLogDetailsClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    isUpdateAvailable: Boolean = false,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showQuickLog by remember { mutableStateOf(false) }
    var showEndPeriodConfirm by remember { mutableStateOf(false) }
    val quickLogSheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        viewModel.message.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Logo-inspired background colors
    val background = MaterialTheme.colorScheme.background
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        background,
                        primaryContainer.copy(alpha = 0.22f),
                        secondaryContainer.copy(alpha = 0.16f),
                        // Full-alpha `background` at the foot, matching the first stop. The nav bar
                        // below this Box is transparent, so what shows through it is MainActivity's
                        // `Surface(background)`; ending on anything else — a 0.96 stop, or `surface`,
                        // which is a shade lighter — leaves a visible band at the seam.
                        background
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        // Was a hardcoded near-white, which washed the dark theme grey.
                        surface.copy(alpha = 0.42f),
                        Color.Transparent,
                        primaryContainer.copy(alpha = 0.20f)
                    ),
                    start = Offset(size.width * 0.08f, 0f),
                    end = Offset(size.width * 0.92f, size.height)
                )
            )
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                LunarLogCenterTopAppBar(
                    actions = {
                        IconButton(onClick = {
                            val status = viewModel.getShareableStatus()
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, status)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Status")
                            // A device with nothing able to handle text/plain would otherwise crash
                            // the app straight off the home screen.
                            runCatching { context.startActivity(shareIntent) }.onFailure {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No app available to share with.")
                                }
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Status")
                        }
                        IconButton(onClick = onSettingsClicked) {
                            if (isUpdateAvailable) {
                                BadgedBox(badge = { Badge() }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            } else {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LunarLogBrandMark(modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("LunarLog", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showQuickLog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    // The adjacent "Log Today" label already names the action.
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    text = { Text("Log Today") },
                    expanded = true
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = Spacing.screenHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(Spacing.screenVertical))

                Crossfade(
                    targetState = uiState.isLoading,
                    animationSpec = tween(220),
                    label = "home_loading"
                ) { isLoading ->
                    if (isLoading) {
                        HomeSkeleton()
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Determine theme color for cycle
                            val cycleColor = when {
                                uiState.isPeriodActive -> cycleColors.period
                                uiState.isEstimatedFertileWindow -> cycleColors.fertile
                                else -> MaterialTheme.colorScheme.primary
                            }
                            // The glow, the arc, the number's shadow and the status pill all read
                            // this, so a phase change eases across the whole hero instead of
                            // snapping in one frame.
                            val animatedCycleColor by animateColorAsState(
                                targetValue = cycleColor,
                                animationSpec = tween(400),
                                label = "cycle_color"
                            )

                            // Cycle Indicator - Responsive
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val circleDiameter = (maxWidth * 0.85f).coerceAtMost(480.dp)
                                CycleStatusCircle(
                                    value = uiState.counterValue,
                                    title = uiState.counterTitle,
                                    subtitle = uiState.counterSubtitle,
                                    progressScaleDays = uiState.counterScaleDays,
                                    activeColor = animatedCycleColor,
                                    scrollState = scrollState,
                                    modifier = Modifier.size(circleDiameter)
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.xl))

                            // Daily Summary Card
                            DailySummaryCard(
                                onLogDetailsClicked = onLogDetailsClicked,
                                modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            sharedContentState = rememberSharedContentState(key = "day_${LocalDate.now().toEpochDay()}"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }
                                } else Modifier
                            )

                            if (uiState.isEstimatedFertileWindow) {
                                Spacer(modifier = Modifier.height(Spacing.lg))
                                FertilityCard()
                            }
                        }
                    }
                }

                // Clearance for the FAB, matching every other FAB-bearing screen.
                Spacer(modifier = Modifier.height(Spacing.fabClearance))
            }
        }

        // A real ModalBottomSheet, so its scrim is drawn in its own full-screen window and covers
        // the bottom navigation bar too — the hand-rolled Box scrim this replaced lived inside the
        // nav graph's padded content area and left the nav bar bright and apparently tappable.
        if (showQuickLog) {
            ModalBottomSheet(
                onDismissRequest = { showQuickLog = false },
                sheetState = quickLogSheetState,
                // Sheets take the same warm `surfaceContainer` as LunarLogCard rather than
                // BottomSheetDefaults' `surfaceContainerLow`, so a sheet reads as the same material
                // as the cards it slides over instead of a second, paler one.
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                dragHandle = {
                    Box(
                        modifier = Modifier.clickable(onClickLabel = "Close quick log") {
                            scope.launch { quickLogSheetState.hide() }.invokeOnCompletion {
                                if (!quickLogSheetState.isVisible) showQuickLog = false
                            }
                        }
                    ) {
                        BottomSheetDefaults.DragHandle()
                    }
                }
            ) {
                QuickLogContent(
                    isPeriodActive = uiState.isPeriodActive,
                    isPeriodOngoing = uiState.isPeriodOngoing,
                    isEndedToday = uiState.isEndedToday,
                    onTogglePeriod = {
                        if (uiState.isPeriodOngoing) {
                            // The sheet is its own window; stacking the confirm dialog on top of it
                            // would put two modals on screen at once, so the sheet steps aside.
                            showQuickLog = false
                            showEndPeriodConfirm = true
                        } else {
                            viewModel.togglePeriod()
                        }
                    },
                    quickSymptoms = uiState.quickLogSymptoms,
                    onSymptomClick = { viewModel.logQuickSymptom(it) },
                    // Close the sheet, then navigate. A ModalBottomSheet draws into its own
                    // WindowManager window above the activity content, so the nav graph's push
                    // transition cannot animate it away: navigating straight from here left the
                    // sheet and its full-screen scrim sitting over the incoming screen — and
                    // owning the back gesture — until Home finally left composition.
                    onFullDetailsClick = {
                        scope.launch { quickLogSheetState.hide() }.invokeOnCompletion {
                            showQuickLog = false
                            onLogDetailsClicked()
                        }
                    }
                )
            }
        }

        if (showEndPeriodConfirm) {
            AlertDialog(
                onDismissRequest = { showEndPeriodConfirm = false },
                title = { Text("End Period?") },
                text = { Text("This will mark today as the end of your current period.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEndPeriodConfirm = false
                            viewModel.togglePeriod()
                        }
                    ) { Text("End") }
                },
                dismissButton = {
                    TextButton(onClick = { showEndPeriodConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun LunarLogBrandMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(BrandRoseLight, BrandRoseDeep),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            ),
            size = size,
            cornerRadius = CornerRadius(size.width * 0.26f, size.height * 0.26f)
        )
        drawCircle(
            color = BrandMoon.copy(alpha = 0.18f),
            radius = size.minDimension * 0.27f,
            center = Offset(size.width * 0.34f, size.height * 0.34f)
        )
        drawCircle(
            color = BrandMoon.copy(alpha = 0.96f),
            radius = size.minDimension * 0.24f,
            center = Offset(size.width * 0.53f, size.height * 0.55f)
        )
        drawCircle(
            color = BrandRoseDeep,
            radius = size.minDimension * 0.14f,
            center = Offset(size.width * 0.53f, size.height * 0.55f)
        )
    }
}

@Composable
fun CycleStatusCircle(
    value: Int,
    title: String,
    subtitle: String,
    progressScaleDays: Int,
    activeColor: Color,
    modifier: Modifier = Modifier,
    scrollState: ScrollState? = null
) {
    val parallaxModifier = if (scrollState != null) {
        Modifier.graphicsLayer {
            translationY = scrollState.value * 0.5f
            alpha = (1f - (scrollState.value / HeroFadeDistancePx)).coerceIn(0f, 1f)
        }
    } else Modifier

    // The parallax above fades the ring to nothing well before the card below fills the screen;
    // derivedStateOf keeps this a single boolean flip rather than a recomposition per scroll frame.
    val heroVisible by remember(scrollState) {
        derivedStateOf { scrollState == null || scrollState.value < HeroFadeDistancePx }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.then(parallaxModifier)
    ) {
        // Once the parallax has faded the ring out there is nothing left to see, so the hero — and
        // with it the infinite breathing transition and two Canvases — leaves the composition
        // instead of animating and redrawing off-screen for as long as Home is open.
        if (heroVisible) {
            CycleStatusCircleContent(
                value = value,
                title = title,
                subtitle = subtitle,
                progressScaleDays = progressScaleDays,
                activeColor = activeColor
            )
        }
    }
}

@Composable
private fun CycleStatusCircleContent(
    value: Int,
    title: String,
    subtitle: String,
    progressScaleDays: Int,
    activeColor: Color
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    // Breathing animation. Symmetric easing because the loop reverses: FastOutSlowIn played
    // backwards is slow-then-fast, which puffs out and snaps in rather than breathing.
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val targetProgress = (value / progressScaleDays.coerceAtLeast(1).toFloat())
        .coerceIn(0.05f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "cycle_progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Soft Glow / Shadow
        Canvas(modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(activeColor.copy(alpha = 0.2f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 1.8f
                ),
                radius = size.minDimension / 1.8f
            )
        }

        // Progress
        Canvas(modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg)
        ) {
            val strokeWidth = size.minDimension * 0.08f // Responsive stroke width
            val radius = (size.minDimension - strokeWidth) / 2

            // Track
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                radius = radius
            )

            // A sweep gradient always begins its ramp at 3 o'clock and wraps back to the first
            // colour there with a hard seam, but this arc begins at 12 o'clock. The two used to be
            // a quarter turn out of step, so the ramp's pale end landed partway along the ring and
            // snapped back to full strength mid-stroke — the abrupt light patch on the right.
            // Rotating the canvas puts the gradient's origin on the arc's origin, and the ramp is
            // symmetric so the colour at the arc's tail matches the colour at its head no matter
            // how far round it has travelled.
            rotate(degrees = -90f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.6f),
                            activeColor,
                            activeColor.copy(alpha = 0.6f)
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 360 * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }
        }

        Surface(
            shape = CircleShape,
            // Opaque, not 72% alpha: the pulsing glow behind this disc showed through it, so the
            // day count's background — and therefore its contrast — changed frame to frame. The
            // halo still reads, because it spills past the disc rather than under it.
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(
                    horizontal = Spacing.xl,
                    vertical = Spacing.xxl
                )
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn() togetherWith
                            slideOutVertically { -it } + fadeOut())
                            .using(SizeTransform(clip = false))
                    },
                    label = "cycle_day"
                ) { day ->
                    Text(
                        text = "$day",
                        style = DisplayHuge.copy(
                            shadow = Shadow(
                                color = activeColor.copy(alpha = 0.3f),
                                offset = Offset(0f, 4f),
                                blurRadius = 8f
                            )
                        ),
                        color = activeColor
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Surface(
                    color = activeColor.copy(alpha = 0.1f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = activeColor.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelLarge,
                        // 14sp needs 4.5:1, which no cycle accent clears against this near-white
                        // pill. The tint and border still carry the colour coding.
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            horizontal = Spacing.lg,
                            vertical = Spacing.sm
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HomeSkeleton() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Cycle Circle Skeleton
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val circleDiameter = (maxWidth * 0.85f).coerceAtMost(480.dp)
            Box(
                modifier = Modifier
                    .size(circleDiameter)
                    .clip(CircleShape)
                    .shimmerEffect()
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        // Summary Card Skeleton — sized to the real card so content does not jump on load.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SummaryCardSkeletonHeight)
                .clip(MaterialTheme.shapes.large)
                .shimmerEffect()
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Fertility Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FertilityCardSkeletonHeight)
                .clip(MaterialTheme.shapes.large)
                .shimmerEffect()
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DailySummaryCard(
    onLogDetailsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    LunarLogCard(
        onClick = onLogDetailsClicked,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daily Insight",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "How are you feeling?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Tap to log symptoms & mood",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Cute icon container
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        // Decorative; the card's own text is the label.
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * [LunarLogCard] in the fertile container tint — same shape and inner padding as every other card,
 * with the colour carrying the one thing that makes this card different.
 * Flat with no border: the 10%-alpha hairline it used to carry read as a half-drawn smudge.
 */
@Composable
fun FertilityCard(modifier: Modifier = Modifier) {
    val cycle = cycleColors
    LunarLogCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = cycle.fertileContainer,
        contentColor = cycle.onFertileContainer
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = cycle.onFertileContainer.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        // The adjacent title already says what this card is.
                        contentDescription = null,
                        tint = cycle.onFertileContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column {
                Text(
                    text = "Estimated Fertile Days",
                    style = MaterialTheme.typography.titleMedium,
                    color = cycle.onFertileContainer
                )
                Text(
                    text = "Calendar estimate only — not birth control",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cycle.onFertileContainer
                )
            }
        }
    }
}
