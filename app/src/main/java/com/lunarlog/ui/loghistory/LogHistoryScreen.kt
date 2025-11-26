package com.lunarlog.ui.loghistory

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.core.model.DailyLog
import com.lunarlog.data.SymptomDefinition
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.lunarlog.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LogHistoryScreen(
    onBackClick: () -> Unit,
    onLogClick: (Long) -> Unit, // Navigate to details
    viewModel: LogHistoryViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val logs by viewModel.logs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSymptom by viewModel.selectedSymptom.collectAsState()
    val availableSymptoms by viewModel.availableSymptoms.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log History") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedSymptom != null) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.onSymptomSelected(null) },
                            label = { Text(selectedSymptom!!.displayName) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Clear Filter", modifier = Modifier.size(16.dp))
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter by Symptom")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Log List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedSymptom != null) "No matching logs found." else "No logs yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(logs, key = { it.date }) { log ->
                        LogHistoryItem(
                            log = log,
                            onClick = { onLogClick(log.date.toEpochDay()) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Filter by Symptom", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    
                    // Simple flow row or vertical list if many
                    // Using a lazy column inside sheet for scalability
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(availableSymptoms) { symptom ->
                             ListItem(
                                 headlineContent = { Text(symptom.displayName) },
                                 leadingContent = {
                                     RadioButton(
                                         selected = selectedSymptom?.id == symptom.id,
                                         onClick = null // Handled by ListItem click
                                     )
                                 },
                                 modifier = Modifier.clickable {
                                     viewModel.onSymptomSelected(symptom)
                                     showFilterSheet = false
                                 }
                             )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LogHistoryItem(
    log: DailyLog, 
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val date = log.date
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    ListItem(
        modifier = Modifier
            .then(
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            state = rememberSharedContentState(key = "day_${log.date}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else Modifier
            )
            .clickable(onClick = onClick),
        headlineContent = { Text(date.format(formatter)) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (log.notes.isNotBlank()) {
                    Text(
                        text = log.notes,
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Summary of symptoms/mood
                val summary = buildList {
                    if (log.flowLevel > 0) add("Flow: ${log.flowLevel}")
                    addAll(log.mood)
                    addAll(log.symptoms)
                }.take(5).joinToString(", ")
                
                if (summary.isNotEmpty()) {
                    Text(
                        text = summary,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}
