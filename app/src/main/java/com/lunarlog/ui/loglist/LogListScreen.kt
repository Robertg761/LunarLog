package com.lunarlog.ui.loglist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import com.lunarlog.data.Medication
import java.time.Instant
import java.time.LocalDate
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
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = uiState.date.format(DateTimeFormatter.ofPattern("EEEE")),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = uiState.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
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
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Period Toggle Card
                PeriodToggleCard(
                    isPeriodDay = uiState.isPeriodDay,
                    onToggle = { checked -> viewModel.togglePeriod(checked) }
                )

                if (uiState.medications.isNotEmpty()) {
                    MedicationSection(
                        medications = uiState.medications,
                        takenMedicationIds = uiState.takenMedicationIds,
                        onTakenChange = viewModel::setMedicationTaken,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                // Empty State
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Timeline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No logs for this day",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap + to add an entry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PeriodToggleCard(
                        isPeriodDay = uiState.isPeriodDay,
                        onToggle = { checked -> viewModel.togglePeriod(checked) },
                        modifier = Modifier.padding(bottom = 8.dp)
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
                items(uiState.entries) { entry ->
                    LogEntryCard(
                        entry = entry,
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

    if (entryPendingDelete != null) {
        val entry = entryPendingDelete!!
        val timeStr = remember(entry.time) {
            Instant.ofEpochMilli(entry.time)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("h:mm a"))
        }

        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            title = { Text("Delete log?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This action can't be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$timeStr \u2022 ${entry.type.name}: ${entry.value}",
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

@Composable
private fun MedicationSection(
    medications: List<Medication>,
    takenMedicationIds: Set<Int>,
    onTakenChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Medications", style = MaterialTheme.typography.titleMedium)
            Text(
                "Record doses taken on this day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            medications.forEach { medication ->
                val isTaken = medication.id in takenMedicationIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTakenChange(medication.id, !isTaken) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isTaken,
                        onCheckedChange = { checked -> onTakenChange(medication.id, checked) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(medication.name)
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
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeStr = Instant.ofEpochMilli(entry.time)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = entry.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
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
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
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
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPeriodDay) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = "Period",
                tint = if (isPeriodDay) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Period",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPeriodDay) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (isPeriodDay) "This day is marked as period" else "Tap to mark as period day",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPeriodDay) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = isPeriodDay,
                onCheckedChange = onToggle
            )
        }
    }
}
