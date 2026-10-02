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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.lunarlog.R
import com.lunarlog.logic.MedicationScheduler
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
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var editingEntry by rememberSaveable(stateSaver = listSaver<LogEntry?, Any>(
        save = { it?.let { e -> listOf(e.id, e.date, e.time, e.type.name, e.value, e.details.orEmpty()) } ?: emptyList() },
        restore = { if (it.isEmpty()) null else LogEntry(it[0] as Long, it[1] as Long, it[2] as Long, com.lunarlog.data.LogEntryType.valueOf(it[3] as String), it[4] as String, (it[5] as String).ifEmpty { null }) }
    )) { mutableStateOf<LogEntry?>(null) }
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

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            showAddSheet = false
            editingEntry = null
            viewModel.acknowledgeSave()
        }
    }
    LaunchedEffect(uiState.deletedEntry) {
        if (uiState.deletedEntry != null) {
            if (snackbarHostState.showSnackbar("Entry deleted", "Undo", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) viewModel.undoDelete()
            else viewModel.clearUndo()
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
                            date = java.time.LocalDate.ofEpochDay(date),
                            medications = uiState.medications,
                            logs = uiState.medicationLogs,
                            onLogDose = viewModel::logDose,
                            onRemoveDose = viewModel::removeDose
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
            title = { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_delete_log_4669af)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_you_can_undo_this_deletion_using_the_message_below_6961cf),
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
                    Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_delete_e2d0a5), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) {
                    Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_cancel_19766e))
                }
            }
        )
    }

    if (showAddSheet) {
        AddEntrySheet(
            date = uiState.date,
            initialEntry = editingEntry,
            isSaving = uiState.isSaving,
            saveError = uiState.saveError,
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
    date: java.time.LocalDate,
    medications: List<Medication>,
    logs: List<com.lunarlog.data.MedicationLog>,
    onLogDose: (Int, Int, Int) -> Unit,
    onRemoveDose: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var removeId by rememberSaveable { mutableStateOf<Long?>(null) }
    selectedId?.let { id ->
        val time = java.time.LocalTime.now()
        com.lunarlog.ui.settings.LunarLogTimePickerDialog("Dose time", time.hour, time.minute,
            onDismiss = { selectedId = null }, onConfirm = { hour, minute -> onLogDose(id, hour, minute); selectedId = null })
    }
    removeId?.let { id -> AlertDialog(onDismissRequest = { removeId = null }, title = { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_remove_this_dose_186f4e)) },
        text = { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_only_this_dose_event_will_be_removed_411deb)) },
        confirmButton = { TextButton(onClick = { onRemoveDose(id); removeId = null }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_remove_dose_ece327)) } },
        dismissButton = { TextButton(onClick = { removeId = null }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_cancel_19766e)) } }) }
    Column(modifier.fillMaxWidth()) {
        SectionHeader("Medications")
        val formatter = rememberEntryTimeFormatter()
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            medications.forEach { medication ->
                val doses = logs.filter { it.medicationId == medication.id && it.taken }.sortedBy { it.timestamp }
                val scheduled = medication.frequency != "as_needed"
                val remaining = (medication.dosesPerDay - doses.size).coerceAtLeast(0)
                LunarLogCard(modifier = Modifier.fillMaxWidth()) {
                    Text(medication.name, style = MaterialTheme.typography.titleMedium)
                    if (medication.dosage.isNotBlank()) Text(medication.dosage,
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(if (scheduled) stringResource(R.string.dose_progress, doses.size, medication.dosesPerDay)
                        else pluralStringResource(R.plurals.doses_logged, doses.size, doses.size),
                        style = MaterialTheme.typography.titleSmall)
                    if (scheduled) {
                        Spacer(Modifier.height(Spacing.sm))
                        LinearProgressIndicator(progress = { (doses.size.toFloat() / medication.dosesPerDay).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(Spacing.sm))
                        Text(if (remaining == 0) stringResource(R.string.dose_target_met)
                            else pluralStringResource(R.plurals.doses_remaining, remaining, remaining),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (date == java.time.LocalDate.now() && MedicationScheduler.isMedicationDueToday(medication, date)) {
                            val minute = java.time.LocalTime.now().let { it.hour * 60L + it.minute }
                            MedicationScheduler.reminderTimes(medication).firstOrNull {
                                it > minute && MedicationScheduler.needsReminder(medication, doses.size, it)
                            }?.let { next ->
                                Text(stringResource(R.string.next_dose_reminder,
                                    java.time.LocalTime.MIDNIGHT.plusMinutes(next).format(formatter)),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else Text(stringResource(R.string.as_needed_help), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    doses.forEachIndexed { index, dose ->
                        val time = Instant.ofEpochMilli(dose.timestamp).atZone(ZoneId.systemDefault()).format(formatter)
                        val removeLabel = stringResource(R.string.remove_named_dose, medication.name, time)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.logged_dose_time, index + 1, time), Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { removeId = dose.id }) {
                                Icon(Icons.Default.Delete, contentDescription = removeLabel,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    val logLabel = stringResource(R.string.log_named_dose, medication.name)
                    FilledTonalButton(onClick = { selectedId = medication.id },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = logLabel }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(stringResource(R.string.ui_log_dose_a665b9))
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
                    text = if (entry.type == com.lunarlog.data.LogEntryType.TEMPERATURE)
                        entry.value.toFloatOrNull()?.let { com.lunarlog.ui.util.formatTemperature(it) } ?: entry.value
                        else entry.value,
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
                    androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_period_6e795d),
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
