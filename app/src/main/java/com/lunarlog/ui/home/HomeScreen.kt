package com.lunarlog.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.ui.theme.FertileGreen
import com.lunarlog.ui.theme.FertileSurface
import com.lunarlog.ui.theme.OnFertileSurface
import com.lunarlog.ui.theme.PeriodRed
import com.lunarlog.ui.theme.PeriodSurface
import com.lunarlog.ui.theme.shimmerEffect
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    onLogDetailsClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    var showQuickLog by remember { mutableStateOf(false) }

    // Organic Background Colors
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Organic Blobs Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top Left Blob
            drawCircle(
                color = primaryContainer.copy(alpha = 0.4f),
                radius = size.width * 0.6f,
                center = Offset(0f, 0f)
            )
            // Bottom Right Blob
            drawCircle(
                color = secondaryContainer.copy(alpha = 0.4f),
                radius = size.width * 0.5f,
                center = Offset(size.width, size.height)
            )
             // Middle Left Blob
            drawCircle(
                color = tertiaryContainer.copy(alpha = 0.3f),
                radius = size.width * 0.4f,
                center = Offset(0f, size.height * 0.6f)
            )
        }
        
        // Blur effect for blobs (if needed, but drawing soft alpha circles is usually performant enough)
        // A full blur modifier on the box can be expensive.

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "LunarLog",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            val status = viewModel.getShareableStatus()
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, status)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Status")
                            context.startActivity(shareIntent)
                        }) {
                             Icon(Icons.Default.Share, contentDescription = "Share Status", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onSettingsClicked) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) // Frosted glass effect potential
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.isLoading) {
                    HomeSkeleton()
                } else {
                    // Determine theme color for cycle
                    val cycleColor = when {
                        uiState.isPeriodActive -> PeriodRed
                        uiState.isFertile -> FertileGreen
                        else -> MaterialTheme.colorScheme.primary
                    }

                    // Cycle Indicator - Responsive
                    CycleStatusCircle(
                        day = uiState.currentCycleDay,
                        daysUntil = uiState.daysUntilPeriod,
                        daysRemainingInPeriod = uiState.daysRemainingInPeriod,
                        isPeriodActive = uiState.isPeriodActive,
                        activeColor = cycleColor,
                        scrollState = scrollState,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Daily Summary Card
                    DailySummaryCard(
                        onLogDetailsClicked = onLogDetailsClicked,
                        modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    state = rememberSharedContentState(key = "day_${LocalDate.now().toEpochDay()}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        } else Modifier
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (uiState.isFertile) {
                        FertilityCard()
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Bottom padding
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
        
        // Scrim
        if (showQuickLog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showQuickLog = false }
            )
        }

        // Custom FAB / Expanded Card Container
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            AnimatedContent(
                targetState = showQuickLog,
                label = "fab_expand",
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + 
                    expandIn(expandFrom = Alignment.BottomEnd, animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(animationSpec = tween(300)) + 
                    shrinkOut(shrinkTowards = Alignment.BottomEnd, animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
            ) { isExpanded ->
                if (isExpanded) {
                    // Expanded Card
                    Card(
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge, // Rounded corners
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        QuickLogContent(
                            isPeriodActive = uiState.isPeriodActive,
                            onTogglePeriod = { viewModel.togglePeriod() },
                            quickSymptoms = uiState.quickLogSymptoms,
                            onSymptomClick = { viewModel.logQuickSymptom(it) },
                            onFullDetailsClick = onLogDetailsClicked,
                            onClose = { showQuickLog = false }
                        )
                    }
                } else {
                    // FAB
                    ExtendedFloatingActionButton(
                        onClick = { showQuickLog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Default.Edit, "Quick Log") },
                        text = { Text("Log Today", fontWeight = FontWeight.Bold) },
                        expanded = true,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CycleStatusCircle(
    day: Int, 
    daysUntil: Int,
    daysRemainingInPeriod: Int?,
    isPeriodActive: Boolean,
    activeColor: Color,
    scrollState: ScrollState? = null,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    
    // Breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val parallaxModifier = if (scrollState != null) {
        Modifier.graphicsLayer {
            translationY = scrollState.value * 0.5f
            alpha = (1f - (scrollState.value / 600f)).coerceIn(0f, 1f)
        }
    } else Modifier

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.then(parallaxModifier)
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
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val strokeWidth = size.minDimension * 0.08f // Responsive stroke width
            val radius = (size.minDimension - strokeWidth) / 2
            
            // Track
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                radius = radius
            )

            // Dynamic Progress based on 28-day cycle assumption for visual filling
            // Clamp to ensure it looks like a ring
            val progress = (day / 28f).coerceIn(0.05f, 1f)
            
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.5f), 
                        activeColor, 
                        activeColor.copy(alpha = 0.8f)
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Day",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$day",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 80.sp,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = activeColor.copy(alpha = 0.3f),
                        offset = Offset(0f, 4f),
                        blurRadius = 8f
                    )
                ),
                color = activeColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                color = activeColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, activeColor.copy(alpha = 0.2f))
            ) {
                val statusText = if (isPeriodActive) {
                    val days = daysRemainingInPeriod ?: 0
                    if (days <= 0) "Period ending today" else "$days days left in period"
                } else {
                    if (daysUntil <= 0) "Period due today" else "$daysUntil days until period"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = activeColor, // Match ring color
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold
                )
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
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .shimmerEffect()
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Summary Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(MaterialTheme.shapes.large)
                .shimmerEffect()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Fertility Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
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
    ElevatedCard(
        onClick = onLogDetailsClicked,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // More rounded
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
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
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to log symptoms & mood",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Cute icon container
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FertilityCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = FertileSurface
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OnFertileSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = OnFertileSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heart Icon",
                        tint = OnFertileSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Fertile Window",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnFertileSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "High chance of conception",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnFertileSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}