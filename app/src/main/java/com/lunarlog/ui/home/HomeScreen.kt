package com.lunarlog.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.ui.theme.*

import androidx.compose.material.icons.filled.Share
import android.content.Intent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext

import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    onLogPeriodClicked: () -> Unit,
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
    
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Gradient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            // Make Scaffold background transparent so gradient shows through
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "LunarLog",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
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
                             Icon(Icons.Default.Share, contentDescription = "Share Status", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = onSettingsClicked) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.secondary)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent, // Transparent TopBar
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showBottomSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Edit, "Quick Log") },
                    text = { Text("Log", fontWeight = FontWeight.Bold) }
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
                    // Cycle Indicator
                    CycleStatusCircle(
                        day = uiState.currentCycleDay,
                        daysUntil = uiState.daysUntilPeriod,
                        scrollState = scrollState
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

                    // Optional: Fertility Warning if applicable
                    if (uiState.isFertile) {
                        FertilityCard()
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Bottom padding to avoid FAB overlap
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
        
        if (showBottomSheet) {
            QuickLogBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                isPeriodActive = uiState.isPeriodActive,
                onTogglePeriod = { viewModel.togglePeriod() },
                quickSymptoms = uiState.quickLogSymptoms,
                onSymptomClick = { viewModel.logQuickSymptom(it) },
                onFullDetailsClick = onLogDetailsClicked
            )
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
                .size(300.dp)
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

@Composable
fun CycleStatusCircle(day: Int, daysUntil: Int, scrollState: ScrollState? = null) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val parallaxModifier = if (scrollState != null) {
        Modifier.graphicsLayer {
            translationY = scrollState.value * 0.5f
            alpha = (1f - (scrollState.value / 600f)).coerceIn(0f, 1f)
        }
    } else Modifier

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(300.dp).then(parallaxModifier)
    ) {
        // Soft Glow / Shadow behind
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.1f),
                radius = size.minDimension / 2f + 20.dp.toPx()
            )
        }

        // Progress circle
        Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            // Track
            drawCircle(
                color = trackColor,
                style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round)
            )

            // Progress Gradient
            // Mock logic: 28 day cycle
            val progress = (day / 28f).coerceIn(0.05f, 1f)
            
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(primaryColor, secondaryColor, tertiaryColor, primaryColor)
                ),
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Day",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "$day",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (daysUntil == 0) "Period due today" else "$daysUntil days left",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun DailySummaryCard(
    onLogDetailsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onLogDetailsClicked,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
 // More rounded
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    text = "Today's Log",
                    style = MaterialTheme.typography.headlineSmall, // Bigger title
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Track symptoms, mood, and more",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Cute icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FertilityCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = MaterialTheme.shapes.large,

        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Heart Icon",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = "Fertile Window",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "High chance of conception",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
