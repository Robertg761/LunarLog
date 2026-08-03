package com.lunarlog.ui.periodhistory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.core.model.DailyLog
import com.lunarlog.ui.components.EmptyState
import com.lunarlog.ui.components.CardDivider
import com.lunarlog.ui.components.LoadingState
import com.lunarlog.ui.components.LunarLogCard
import com.lunarlog.ui.components.LunarLogTopAppBar
import com.lunarlog.ui.components.SectionHeader
import com.lunarlog.ui.theme.Spacing
import com.lunarlog.ui.theme.cycleColors
import com.lunarlog.ui.util.MediumDate
import com.lunarlog.ui.util.ShortDayDate
import com.lunarlog.ui.util.toPickerLocalDate
import com.lunarlog.ui.util.toPickerMillis
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodDetailScreen(
    onBack: () -> Unit,
    onDayClick: (Long) -> Unit,
    viewModel: PeriodDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    Scaffold(
        topBar = {
            LunarLogTopAppBar(
                title = "Period Details",
                onNavigateBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Period") },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        val cycle = uiState.cycle
        if (uiState.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
        } else if (cycle != null) {
            val isOngoing = cycle.endDate == null
            val endDate = cycle.endDate ?: LocalDate.now()
            val duration = ChronoUnit.DAYS.between(cycle.startDate, endDate) + 1

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.screenVertical
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Date Range Card
                item {
                    // Hand-rolled rather than LunarLogCard because this hero keeps the
                    // primaryContainer fill; the onPrimaryContainer pairing below is measured
                    // contrast work. Geometry and inner padding still match LunarLogCard.
                    //
                    // The "Duration"/"Start Date"/"End Date" labels are full-strength
                    // onPrimaryContainer, not 70% alpha: at labelMedium's 12sp they need 4.5:1, and
                    // fading the only content colour this container has a measured pairing for gave
                    // up roughly a third of it. Their lower emphasis comes from the type scale.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                            Text(
                                "Duration",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                "$duration day${if (duration != 1L) "s" else ""}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(Spacing.lg))

                            // Start Date Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable(onClickLabel = "Change start date") {
                                        showStartDatePicker = true
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = cycleColors.period
                                )
                                Spacer(Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Start Date",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        cycle.startDate.format(MediumDate),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(Modifier.height(Spacing.md))
                            CardDivider()
                            Spacer(Modifier.height(Spacing.md))

                            // End Date Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable(onClickLabel = "Change end date") {
                                        showEndDatePicker = true
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = cycleColors.period
                                )
                                Spacer(Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "End Date",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        if (isOngoing) "Ongoing" else cycle.endDate?.format(MediumDate) ?: "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        // PeriodRed on primaryContainer measured 2.25:1.
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = if (isOngoing) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Daily Logs Section
                item {
                    // SectionHeader owns the 8dp down to its content; the 8dp here balances it
                    // against the list's 12dp arrangement so the break reads 20/20 rather than
                    // the old 20/12.
                    SectionHeader(
                        title = "Daily Logs",
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }

                if (uiState.dailyLogs.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.WaterDrop,
                            title = "No logs recorded",
                            description = "Nothing was logged during this period."
                        )
                    }
                } else {
                    items(uiState.dailyLogs, key = { it.date }) { log ->
                        DailyLogCard(
                            log = log,
                            onClick = { onDayClick(log.date.toEpochDay()) }
                        )
                    }
                }
            }
        } else {
            // The cycle id can outlive the cycle — deleted from another screen, restored into a
            // killed process, or a stale deep link. Without this the user got a bare app bar over
            // an empty rectangle, which is indistinguishable from a crash.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.screenHorizontal),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(
                        icon = Icons.Outlined.Info,
                        title = "This period is no longer available",
                        description = "It may have been deleted from another screen."
                    )
                    Button(onClick = onBack) {
                        Text("Back")
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Period?") },
            text = { Text("This will permanently delete this period record. Daily logs will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCycle()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val cycle = uiState.cycle

    // Start Date Picker Dialog
    if (showStartDatePicker && cycle != null) {
        CycleDatePickerDialog(
            initialDate = cycle.startDate,
            // The repository rejects a start after the end date and any future date; grey those
            // out rather than letting the user pick one and answering with a snackbar.
            minDate = null,
            maxDate = minOf(cycle.endDate ?: LocalDate.now(), LocalDate.now()),
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { date -> viewModel.updateStartDate(date) }
        )
    }

    // End Date Picker Dialog
    if (showEndDatePicker && cycle != null) {
        CycleDatePickerDialog(
            initialDate = cycle.endDate ?: LocalDate.now(),
            // Mirror of the start picker: never before the start, never in the future.
            minDate = cycle.startDate,
            maxDate = LocalDate.now(),
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { date -> viewModel.updateEndDate(date) }
        )
    }
}

/**
 * Restricts the calendar to `[minDate, maxDate]`, both inclusive and either open-ended.
 *
 * The two pickers bound each other — a start date cannot land after the end date and an end date
 * cannot land before the start — and `CycleRepository.updateCycleDates` also refuses any future
 * day. Encoding those rules here means the invalid days are visibly disabled instead of being
 * accepted and then rejected with an error snackbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
private class CycleSelectableDates(
    private val minDate: LocalDate?,
    private val maxDate: LocalDate?
) : SelectableDates {

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = utcTimeMillis.toPickerLocalDate()
        if (minDate != null && date.isBefore(minDate)) return false
        if (maxDate != null && date.isAfter(maxDate)) return false
        return true
    }

    override fun isSelectableYear(year: Int): Boolean {
        if (minDate != null && year < minDate.year) return false
        if (maxDate != null && year > maxDate.year) return false
        return true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleDatePickerDialog(
    initialDate: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val selectableDates = remember(minDate, maxDate) { CycleSelectableDates(minDate, maxDate) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toPickerMillis(),
        selectableDates = selectableDates
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(millis.toPickerLocalDate())
                    }
                    onDismiss()
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun DailyLogCard(
    log: DailyLog,
    onClick: () -> Unit
) {
    LunarLogCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Text(
            text = log.date.format(ShortDayDate),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xs))

        val lines = buildDailyLogSummaryLines(log)
        if (lines.primary.isNotBlank() || lines.secondary.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (lines.primary.isNotBlank()) {
                    Text(
                        text = lines.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (lines.secondary.isNotBlank()) {
                    Text(
                        text = lines.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "No details logged",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
