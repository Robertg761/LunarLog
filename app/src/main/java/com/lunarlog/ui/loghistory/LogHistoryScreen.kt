package com.lunarlog.ui.loghistory

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.core.model.DailyLog
import com.lunarlog.ui.components.CardDivider
import com.lunarlog.ui.components.EmptyState
import com.lunarlog.ui.components.LunarLogTopAppBar
import com.lunarlog.ui.theme.Spacing
import com.lunarlog.ui.util.MediumDate
import com.lunarlog.ui.util.flowLabel
import kotlinx.coroutines.launch


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
    val filterSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Flipping `showFilterSheet` straight to false tears the sheet out of composition mid-frame, so
    // it vanishes instead of sliding away. Animate it out first, then drop it.
    fun dismissFilterSheet() {
        scope.launch { filterSheetState.hide() }.invokeOnCompletion {
            if (!filterSheetState.isVisible) showFilterSheet = false
        }
    }
    // Pinned rather than enterAlways: the Scaffold reserves the bar's full height for content, so
    // a collapsing bar would leave a gap. Pinned still tints to surfaceContainer once you scroll.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LunarLogTopAppBar(
                title = "Log History",
                onNavigateBack = onBackClick,
                scrollBehavior = scrollBehavior,
                actions = {
                    // Only the 48dp icon button lives up here now; the active-filter chip moved
                    // into the header strip below, where a 32dp pill does not sit off-centre
                    // against a 48dp target.
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter by Symptom")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search + active filter as one tonal header strip, so the list no longer collides
            // with the text field's bottom border.
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Spacing.screenHorizontal,
                            end = Spacing.screenHorizontal,
                            top = Spacing.sm,
                            bottom = Spacing.sm
                        ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
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

                    selectedSymptom?.let { symptom ->
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.onSymptomSelected(null) },
                            label = { Text(symptom.displayName) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear Filter",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.heightIn(min = Spacing.minTouchTarget)
                        )
                    }
                }
            }
            CardDivider()

            // Log List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.screenVertical)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(horizontal = Spacing.screenHorizontal),
                            contentAlignment = Alignment.Center
                        ) {
                            val isFiltered = searchQuery.isNotEmpty() || selectedSymptom != null
                            EmptyState(
                                icon = if (isFiltered) Icons.Outlined.SearchOff
                                else Icons.AutoMirrored.Outlined.EventNote,
                                title = if (isFiltered) "No matching logs found" else "No logs yet",
                                description = if (isFiltered) {
                                    "Try a different search term or clear the symptom filter."
                                } else {
                                    "Days you log will show up here."
                                }
                            )
                        }
                    }
                } else {
                    itemsIndexed(logs, key = { _, log -> log.date }) { index, log ->
                        LogHistoryItem(
                            log = log,
                            onClick = { onLogClick(log.date.toEpochDay()) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        // Trailing divider after the last row read as a cut-off item.
                        // Inset to the screen gutter so it lines up with the row text.
                        if (index < logs.lastIndex) {
                            CardDivider(
                                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)
                            )
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = filterSheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                // A fixed 400dp cap overflowed short screens and wasted tall ones; half the
                // screen keeps the sheet's own scroll from fighting the list's.
                val maxListHeight = (LocalConfiguration.current.screenHeightDp * 0.5f).dp
                Column(
                    modifier = Modifier.padding(
                        start = Spacing.sheetHorizontal,
                        end = Spacing.sheetHorizontal,
                        bottom = Spacing.sheetHorizontal
                    )
                ) {
                    Text(
                        "Filter by Symptom",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = Spacing.lg)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxListHeight)
                    ) {
                        items(availableSymptoms) { symptom ->
                            ListItem(
                                headlineContent = { Text(symptom.displayName) },
                                leadingContent = {
                                    RadioButton(
                                        selected = selectedSymptom?.id == symptom.id,
                                        onClick = null // Handled by ListItem click
                                    )
                                },
                                modifier = Modifier
                                    .heightIn(min = Spacing.minTouchTarget)
                                    .selectable(
                                        selected = selectedSymptom?.id == symptom.id,
                                        role = Role.RadioButton,
                                        onClick = {
                                            viewModel.onSymptomSelected(symptom)
                                            dismissFilterSheet()
                                        }
                                    )
                            )
                        }
                    }
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

    ListItem(
        modifier = Modifier
            .then(
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            // Epoch day, not the LocalDate's toString: HomeScreen registers the same element as
                            // "day_${LocalDate.now().toEpochDay()}", and the two keys have to be identical
                            // to pair up.
                            sharedContentState = rememberSharedContentState(key = "day_${log.date.toEpochDay()}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else Modifier
            )
            .clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = date.format(MediumDate),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (log.notes.isNotBlank()) {
                    Text(
                        text = log.notes,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Summary of symptoms/mood
                val summary = buildList {
                    if (log.flowLevel > 0) add("Flow: ${flowLabel(log.flowLevel)}")
                    addAll(log.mood)
                    addAll(log.symptoms)
                }.take(5).joinToString(", ")

                if (summary.isNotEmpty()) {
                    Text(
                        text = summary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}
