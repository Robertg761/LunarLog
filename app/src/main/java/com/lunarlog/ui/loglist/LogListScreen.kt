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

    LaunchedEffect(date) {
        viewModel.loadDate(date)
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
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
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
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.entries) { entry ->
                    LogEntryCard(
                        entry = entry,
                        onClick = {
                            editingEntry = entry
                            showAddSheet = true
                        },
                        onDelete = { viewModel.deleteEntry(entry) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddEntrySheet(
            date = uiState.date,
            initialEntry = editingEntry,
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
