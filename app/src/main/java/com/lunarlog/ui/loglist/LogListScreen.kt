package com.lunarlog.ui.loglist

import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.data.LogEntry
import com.lunarlog.data.displayName
import com.lunarlog.data.Medication
import com.lunarlog.ui.components.EmptyState
import com.lunarlog.ui.components.LunarLogCard
import com.lunarlog.ui.components.LunarLogTopAppBar
import com.lunarlog.ui.components.SectionHeader
import com.lunarlog.ui.theme.Spacing
import com.lunarlog.ui.theme.shimmerEffect
import com.lunarlog.ui.util.ShortDayDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogListScreen(
    date: Long,
    onBack: () -> Unit,
    viewModel: LogListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<LogEntry?>(null) }
    var entryPendingDelete by remember { mutableStateOf<LogEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    // Pinned rather than enterAlways: the Scaffold reserves the bar's full height for content, so
    // a collapsing bar would leave a gap. Pinned still tints to surfaceContainer once you scroll.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val timeFormatter = rememberEntryTimeFormatter()

    LaunchedEffect(date) {
        viewModel.loadDate(date)
    }

    LaunchedEffect(uiState.periodMessage) {
        uiState.periodMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onPeriodMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Single-line titleLarge like every other detail screen; the two-line
            // titleMedium/labelMedium stack made this heading smaller than the sections under it.
            LunarLogTopAppBar(
                title = uiState.date.format(ShortDayDate),
                onNavigateBack = onBack,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingEntry = null
                showAddSheet = true
            }) {
                Icon(Icons.Default.Add, "Add Log")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        // One LazyColumn for all three states. The loading, empty and populated branches used to
        // be three different layouts, so the toggle card changed width and the spinner ignored the
        // Scaffold inset entirely.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = Spacing.screenHorizontal,
                end = Spacing.screenHorizontal,
                top = Spacing.screenVertical,
                // Scaffold does not fold the FAB into content padding, so the last card's
                // Delete button sat underneath it.
                bottom = Spacing.fabClearance
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
        ) {
            if (uiState.isLoading) {
                item { LogListSkeleton() }
            } else {
                item {
                    PeriodToggleCard(
                        isPeriodDay = uiState.isPeriodDay,
                        onToggle = { checked -> viewModel.togglePeriod(checked) }
                    )
                }

                if (uiState.medications.isNotEmpty()) {
                    item {
                        MedicationSection(
                            medications = uiState.medications,
                            takenMedicationIds = uiState.takenMedicationIds,
                            onTakenChange = viewModel::setMedicationTaken
                        )
                    }
                }

                if (uiState.entries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxHeight(0.5f),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon = Icons.Outlined.Timeline,
                                title = "No logs for this day",
                                description = "Tap + to add an entry"
                            )
                        }
                    }
                } else {
                    items(uiState.entries, key = { entry -> entry.id }) { entry ->
                        LogEntryCard(
                            entry = entry,
                            timeFormatter = timeFormatter,
                            onClick = {
                                editingEntry = entry
                                showAddSheet = true
                            },
                            onDelete = { entryPendingDelete = entry }
                        )
                    }
                }
            }
        }
    }

    if (entryPendingDelete != null) {
        val entry = entryPendingDelete!!
        val timeStr = remember(entry.time, timeFormatter) {
            Instant.ofEpochMilli(entry.time)
                .atZone(ZoneId.systemDefault())
                .format(timeFormatter)
        }

        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            title = { Text("Delete log?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = "This action can't be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$timeStr • ${entry.type.displayName}: ${entry.value}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!entry.details.isNullOrBlank()) {
                        Text(
                            text = entry.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        entryPendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddSheet) {
        AddEntrySheet(
            date = uiState.date,
            initialEntry = editingEntry,
            symptomDefinitions = uiState.symptomDefinitions,
            onAddCustomSymptom = viewModel::addCustomSymptom,
            onDismiss = { showAddSheet = false },
            onSave = { payload, time, details ->
                viewModel.saveEntries(
                    payload = payload,
                    time = time,
                    details = details,
                    editingEntry = editingEntry
                )
                showAddSheet = false
                editingEntry = null
            }
        )
    }
}

/**
 * A shimmer stand-in for the real layout — toggle card, then a few entry cards — instead of a bare
 * spinner. Matches the loading language HomeScreen already uses.
 */
@Composable
private fun LogListSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        SkeletonBlock(height = 88.dp)
        repeat(4) {
            SkeletonBlock(height = 96.dp)
        }
    }
}

@Composable
private fun SkeletonBlock(height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.large)
            .shimmerEffect()
    )
}

@Composable
private fun MedicationSection(
    medications: List<Medication>,
    takenMedicationIds: Set<Int>,
    onTakenChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader("Medications")
        LunarLogCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Record doses taken on this day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.sm))
            medications.forEach { medication ->
                val isTaken = medication.id in takenMedicationIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.minTouchTarget)
                        // toggleable on the row merges the checkbox into one labelled,
                        // state-carrying stop instead of two unlabelled ones.
                        .toggleable(
                            value = isTaken,
                            role = Role.Checkbox,
                            onValueChange = { checked -> onTakenChange(medication.id, checked) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isTaken,
                        onCheckedChange = null
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            medication.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        val details = listOfNotNull(
                            medication.dosage.takeIf { it.isNotBlank() },
                            medication.frequency.replace('_', ' ').replaceFirstChar { it.uppercase() }
                        ).joinToString(" • ")
                        Text(
                            details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntryCard(
    entry: LogEntry,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeStr = remember(entry.time, timeFormatter) {
        Instant.ofEpochMilli(entry.time)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    }

    LunarLogCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = entry.type.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        // colorScheme.secondary is a decorative pink; as a foreground on the
                        // card it measured 1.99:1.
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = entry.value,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!entry.details.isNullOrEmpty()) {
                    Text(
                        text = entry.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${entry.type.displayName} entry at $timeStr",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PeriodToggleCard(
    isPeriodDay: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // LunarLogCard with the container overridden: this card's fill *is* the state, which is exactly
    // what `containerColor` is for, so it stays on the same shape and inset as every other card.
    LunarLogCard(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = isPeriodDay,
                role = Role.Switch,
                onValueChange = onToggle
            ),
        containerColor = if (isPeriodDay)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (isPeriodDay)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = if (isPeriodDay)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Period",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (isPeriodDay) "This day is marked as period" else "Tap to mark as period day",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPeriodDay)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isPeriodDay,
                // The whole row is toggleable, so the Switch is a visual readout; a second
                // handler here would give TalkBack two stops for one control.
                onCheckedChange = null
            )
        }
    }
}
