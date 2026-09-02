package com.lunarlog.ui.analysis

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.logic.NarrativeGenerator.EMPTY_WEEK_NARRATIVE
import com.lunarlog.logic.WeeklyDigest
import com.lunarlog.ui.components.EmptyState
import com.lunarlog.ui.components.LoadingState
import com.lunarlog.ui.components.LunarLogCard
import com.lunarlog.ui.components.LunarLogTopAppBar
import com.lunarlog.ui.components.SectionHeader
import com.lunarlog.ui.theme.Spacing
import com.lunarlog.ui.util.MediumDate
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.TextStyle


private const val PDF_MIME_TYPE = "application/pdf"
private const val CSV_MIME_TYPE = "text/csv"

/**
 * Analysis — trends and reports. Named for the app bar title and the bottom-nav label, both of which
 * read "Analysis"; the KDoc used to call it "Insights", which matches nothing the user ever sees.
 *
 * This is one of the four bottom-nav destinations (see `LunarLogNavGraph.bottomNavItems`) and is
 * additionally reachable through the `lunarlog://analysis` deep link. It is never pushed on top of
 * another screen, so there is nothing for a back arrow to pop and none is shown — the same as Home,
 * Calendar and Period History.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onHistoryClick: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(PDF_MIME_TYPE)
    ) { uri ->
        uri?.let {
            val snapshot = uiState
            exportInBackground(
                scope = scope,
                snackbarHostState = snackbarHostState,
                context = context,
                uri = it,
                mimeType = PDF_MIME_TYPE,
                savedMessage = "PDF saved",
                failedMessage = "Unable to save PDF. Please try another location."
            ) { stream ->
                ReportGenerator.generatePdf(
                    stream,
                    snapshot.cycleHistory,
                    snapshot.symptomCounts,
                    snapshot.moodCounts
                )
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(CSV_MIME_TYPE)
    ) { uri ->
        uri?.let {
            val snapshot = uiState
            exportInBackground(
                scope = scope,
                snackbarHostState = snackbarHostState,
                context = context,
                uri = it,
                mimeType = CSV_MIME_TYPE,
                savedMessage = "CSV saved",
                failedMessage = "Unable to save CSV. Please try another location."
            ) { stream ->
                ReportGenerator.generateCsv(
                    stream,
                    snapshot.periods,
                    snapshot.dailyLogs,
                    snapshot.logEntries
                )
            }
        }
    }

    Scaffold(
        topBar = {
            LunarLogTopAppBar(
                title = "Analysis",
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Log History")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Material3's Tab defaults unselectedContentColor to selectedContentColor, so every
            // label rendered in `primary` and only the indicator marked the selection.
            //
            // Transparent container for the same reason the app bar is transparent: TabRow defaults
            // to `surface`, which is lighter than the screen's `background`, so the tab strip drew
            // as a pale band with a visible edge above and below it.
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = { Text("Trends") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = { Text("Reports") }
                )
            }

            if (uiState.isLoading) {
                LoadingState()
            } else {
                when (selectedTab) {
                    0 -> TrendsTab(uiState)
                    1 -> ReportsTab(
                        onGeneratePdf = {
                            pdfLauncher.launch("LunarLog_Report_${LocalDate.now()}.pdf")
                        },
                        onGenerateCsv = {
                            csvLauncher.launch("LunarLog_Data_${LocalDate.now()}.csv")
                        }
                    )
                }
            }
        }
    }
}

/**
 * Both generators walk the whole history, and the PDF additionally lays out and draws every page
 * through a Canvas, so the write runs on the IO dispatcher rather than inside the activity-result
 * callback on the main thread, where it stalled the UI for longer with every month of data. The
 * outcome is announced through the same snackbars as before.
 */
private fun exportInBackground(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    context: Context,
    uri: Uri,
    mimeType: String,
    savedMessage: String,
    failedMessage: String,
    write: (OutputStream) -> Unit
) {
    scope.launch {
        val saved = withContext(Dispatchers.IO) {
            try {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: throw IOException("The selected destination could not be opened")
                write(stream)
                true
            } catch (_: Exception) {
                false
            }
        }
        if (saved) {
            announceExportSaved(scope, snackbarHostState, context, uri, mimeType, savedMessage)
        } else {
            announceExportFailed(scope, snackbarHostState, failedMessage)
        }
    }
}

/**
 * The user has just written a file to a location they picked and otherwise has no route back to it,
 * so the success message carries an "Open" action on the very uri that was written.
 */
private fun announceExportSaved(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    context: Context,
    uri: Uri,
    mimeType: String,
    message: String
) {
    scope.launch {
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = "Open",
            duration = SnackbarDuration.Long
        )
        if (result != SnackbarResult.ActionPerformed) return@launch

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            snackbarHostState.showSnackbar(
                message = "No app on this device can open that file.",
                duration = SnackbarDuration.Short
            )
        }
    }
}

/**
 * Failures are the message the user actually has to read and act on, so they get the long — the
 * inverse of the old Toasts, which gave the success LENGTH_LONG and the failure LENGTH_SHORT.
 */
private fun announceExportFailed(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    message: String
) {
    scope.launch {
        snackbarHostState.showSnackbar(
            message = message,
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )
    }
}

@Composable
fun TrendsTab(uiState: AnalysisUiState) {
    val digest = uiState.weeklyDigest
    val hasDigest = digest != null && digest.narrative != EMPTY_WEEK_NARRATIVE
    val hasNothing = !hasDigest &&
        uiState.recentCycleSummaries.isEmpty() &&
        uiState.cycleHistory.isEmpty() &&
        uiState.symptomCounts.isEmpty()

    // A fresh install used to get a small icon a third of the way down followed by a large void,
    // because the empty state was emitted inline in the scroll column above sections that had no
    // else-branch at all. With genuinely nothing to show, the whole tab is the empty state.
    if (hasNothing) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenHorizontal),
            verticalArrangement = Arrangement.Center
        ) {
            EmptyState(
                icon = Icons.Outlined.Timeline,
                title = "No Trends Yet",
                description = "Log your period for at least 2 cycles to see predictions and history."
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.screenVertical
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)
    ) {
        if (digest != null) {
            WeeklyDigestSection(digest)
        }

        CycleInsightsSection(uiState)

        CycleLengthSection(uiState)

        TopSymptomsSection(uiState)
    }
}

@Composable
private fun WeeklyDigestSection(digest: WeeklyDigest) {
    Column {
        SectionHeader("Weekly Digest")
        if (digest.narrative == EMPTY_WEEK_NARRATIVE) {
            EmptyState(
                icon = Icons.AutoMirrored.Outlined.EventNote,
                title = "No logs recorded this week",
                description = "Log a mood or a symptom and this week's summary appears here."
            )
        } else {
            LunarLogCard(modifier = Modifier.fillMaxWidth()) {
                Text(digest.narrative, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CycleInsightsSection(uiState: AnalysisUiState) {
    Column {
        SectionHeader("Cycle Insights")
        if (uiState.recentCycleSummaries.isEmpty()) {
            SectionPlaceholder("Finish a cycle and its summary shows up here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
                uiState.recentCycleSummaries.forEach { summary ->
                    LunarLogCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Cycle ${summary.cycleId}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                "(${summary.startDate.format(MediumDate)} - " +
                                    "${summary.endDate.format(MediumDate)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(summary.narrative, style = MaterialTheme.typography.bodyMedium)

                        if (summary.keyInsights.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            summary.keyInsights.forEach { insight ->
                                Row(
                                    modifier = Modifier.padding(top = Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.sm))
                                    Text(insight, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CycleLengthSection(uiState: AnalysisUiState) {
    Column {
        SectionHeader("Cycle Length History")
        if (uiState.cycleHistory.isEmpty()) {
            SectionPlaceholder("Two logged cycles are enough to start plotting this.")
        } else {
            val model = remember(uiState.cycleHistory) {
                CartesianChartModel(
                    LineCartesianLayerModel.build {
                        series(uiState.cycleHistory.map { it.second.toFloat() })
                    }
                )
            }

            val locale = LocalLocale.current.platformLocale
            val cycleDates = remember(uiState.cycleHistory, locale) {
                uiState.cycleHistory.map {
                    it.first.month.getDisplayName(TextStyle.SHORT, locale)
                }
            }

            val cycleAxisFormatter = remember(cycleDates) {
                CartesianValueFormatter { _, value, _ ->
                    cycleDates.getOrElse(value.toInt()) { "" }
                }
            }

            val cycleChartDescription = remember(uiState.cycleHistory) {
                "Cycle length history chart. " + uiState.cycleHistory.joinToString(", ") {
                    "${it.first.format(MediumDate)}: ${it.second} days"
                }
            }

            ChartCard {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = cycleAxisFormatter),
                        marker = rememberMarker()
                    ),
                    model = model,
                    modifier = Modifier
                        .fillMaxWidth()
                        // height() pins max as well as min, so at large font scales the
                        // bottom-axis labels clipped mid-glyph.
                        .heightIn(min = 200.dp)
                        .semantics { contentDescription = cycleChartDescription }
                )
            }
        }
    }
}

@Composable
private fun TopSymptomsSection(uiState: AnalysisUiState) {
    Column {
        SectionHeader("Top Symptoms")
        if (uiState.symptomCounts.isEmpty()) {
            SectionPlaceholder("Log a few symptoms and the most frequent ones rank here.")
        } else {
            val counts = remember(uiState.symptomCounts) {
                uiState.symptomCounts.values.map { it.toFloat() }.toTypedArray()
            }
            val symptomNames = remember(uiState.symptomCounts) {
                uiState.symptomCounts.keys.toList()
            }

            val model = remember(counts) {
                CartesianChartModel(
                    ColumnCartesianLayerModel.build { series(counts.toList()) }
                )
            }

            val symptomAxisFormatter = remember(symptomNames) {
                CartesianValueFormatter { _, value, _ ->
                    symptomNames.getOrElse(value.toInt()) { "" }
                }
            }

            val symptomChartDescription = remember(uiState.symptomCounts) {
                "Top symptoms chart. " + uiState.symptomCounts.entries.joinToString(", ") {
                    "${it.key}: ${it.value}"
                }
            }

            ChartCard {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = symptomAxisFormatter),
                        marker = rememberMarker()
                    ),
                    model = model,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .semantics { contentDescription = symptomChartDescription }
                )
            }
        }
    }
}

/**
 * A chart sits on the app's one card surface rather than bare on the page background, and every
 * component built inside this scope — including the axes, whose default label components read the
 * ambient theme — picks up the M3 Vico theme, which pins series colours to
 * primary/secondary/tertiary and axis text and lines to the scheme's surface-variant colours.
 */
@Composable
private fun ChartCard(content: @Composable () -> Unit) {
    LunarLogCard(modifier = Modifier.fillMaxWidth()) {
        ProvideVicoTheme(rememberM3VicoTheme(), content)
    }
}

/** Keeps a section header in place with a one-line reason when the section has nothing to show. */
@Composable
private fun SectionPlaceholder(text: String) {
    LunarLogCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ReportsTab(onGeneratePdf: () -> Unit, onGenerateCsv: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.screenVertical
            ),
        // Top-aligned, like Trends. Arrangement.Center inside a verticalScroll distributes free
        // space that a scrollable column never has, so it only ever made the two tabs disagree
        // about where their content starts.
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        SectionHeader("Export Your Health Data")

        LunarLogCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "A PDF report summarises your cycles for an appointment. The CSV is the raw " +
                    "export — every period, log and entry — for your own records.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Button(
                onClick = onGeneratePdf,
                // height() pins max as well as min, so a wrapped label clipped mid-glyph.
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Generate Doctor's Report (PDF)")
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            OutlinedButton(
                onClick = onGenerateCsv,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Export Data (CSV)")
            }
        }

        Text(
            "You can choose where to save your reports (e.g., Downloads or Google Drive).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
