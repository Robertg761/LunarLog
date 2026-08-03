package com.lunarlog.ui.periodhistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.core.model.Cycle
import com.lunarlog.ui.components.EmptyState
import com.lunarlog.ui.components.LunarLogCard
import com.lunarlog.ui.components.LunarLogTopAppBar
import com.lunarlog.ui.theme.Spacing
import com.lunarlog.ui.theme.cycleColors
import com.lunarlog.ui.util.MediumDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodHistoryScreen(
    onCycleClick: (Int) -> Unit,
    onAddPeriodClick: () -> Unit,
    viewModel: PeriodHistoryViewModel = hiltViewModel()
) {
    val cycles by viewModel.cycles.collectAsState()

    Scaffold(
        topBar = { LunarLogTopAppBar(title = "Periods") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPeriodClick,
                containerColor = cycleColors.periodStrong,
                contentColor = cycleColors.onPeriodStrong
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Log New Period"
                )
            }
        }
    ) { padding ->
        if (cycles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.screenHorizontal),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Outlined.DateRange,
                    title = "No periods logged yet",
                    description = "Tap + to log your first period"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                // Keep the last card's tap target clear of the FAB.
                contentPadding = PaddingValues(
                    start = Spacing.screenHorizontal,
                    end = Spacing.screenHorizontal,
                    top = Spacing.screenVertical,
                    bottom = Spacing.fabClearance
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
            ) {
                items(cycles, key = { it.id }) { cycle ->
                    PeriodCard(
                        cycle = cycle,
                        onClick = { onCycleClick(cycle.id) }
                    )
                }
            }
        }
    }
}

/**
 * One row of the history list.
 *
 * Every card is the same `LunarLogCard` tone: a list that alternated between `primaryContainer` and
 * `surfaceVariant` put two unrelated neutral families next to each other and made the ongoing row
 * look like a different kind of object. The ongoing state is carried by the accent-tinted drop and
 * the "Ongoing" badge instead, which is louder than a fill swap anyway.
 *
 * Hierarchy is the date range (`titleMedium`, `onSurface`) over the duration (`bodyMedium`,
 * `onSurfaceVariant`) — previously both resolved to `onSurfaceVariant`, so the card had no focal
 * point.
 */
@Composable
private fun PeriodCard(
    cycle: Cycle,
    onClick: () -> Unit
) {
    val isOngoing = cycle.endDate == null
    val endDate = cycle.endDate ?: LocalDate.now()
    val duration = ChronoUnit.DAYS.between(cycle.startDate, endDate) + 1
    val endLabel = if (isOngoing) "Present" else endDate.format(MediumDate)
    val dateRange = "${cycle.startDate.format(MediumDate)} – $endLabel"

    LunarLogCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = if (isOngoing) cycleColors.period else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(Spacing.lg))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateRange,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        // Without this the badge measures to zero width and vanishes on a long date.
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isOngoing) {
                        Spacer(Modifier.width(Spacing.sm))
                        Badge(
                            containerColor = cycleColors.periodStrong,
                            contentColor = cycleColors.onPeriodStrong
                        ) {
                            Text(
                                "Ongoing",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Text(
                    text = "$duration day${if (duration != 1L) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
