package com.lunarlog.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import java.io.IOException
import java.time.LocalDate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.automirrored.filled.List

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onBack: () -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            try {
                val stream = context.contentResolver.openOutputStream(it)
                    ?: throw IOException("The selected destination could not be opened")
                ReportGenerator.generatePdf(
                    stream,
                    uiState.cycleHistory,
                    uiState.symptomCounts,
                    uiState.moodCounts
                )
                Toast.makeText(context, "PDF saved successfully", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to save PDF. Please try another location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                val stream = context.contentResolver.openOutputStream(it)
                    ?: throw IOException("The selected destination could not be opened")
                ReportGenerator.generateCsv(
                    stream,
                    uiState.periods,
                    uiState.dailyLogs,
                    uiState.logEntries
                )
                Toast.makeText(context, "CSV saved successfully", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to save CSV. Please try another location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Log History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Trends") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Reports") })
            }

            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
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

@Composable
fun TrendsTab(uiState: AnalysisUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Weekly Digest
        uiState.weeklyDigest?.let { digest ->
            Text("Weekly Digest", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(digest.narrative, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Cycle Summaries
        if (uiState.recentCycleSummaries.isNotEmpty()) {
            Text("Cycle Insights", style = MaterialTheme.typography.titleMedium)
            
            uiState.recentCycleSummaries.forEach { summary ->
                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Cycle ${summary.cycleId}", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "(${summary.startDate} - ${summary.endDate})", 
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(summary.narrative, style = MaterialTheme.typography.bodyMedium)
                        
                        if (summary.keyInsights.isNotEmpty()) {
                             Spacer(modifier = Modifier.height(8.dp))
                             summary.keyInsights.forEach { insight ->
                                 Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(insight, style = MaterialTheme.typography.bodySmall)
                                 }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (uiState.cycleHistory.isNotEmpty()) {
            Text("Cycle Length History", style = MaterialTheme.typography.titleMedium)
            
            val model = remember(uiState.cycleHistory) {
                CartesianChartModel(
                    LineCartesianLayerModel.build {
                        series(uiState.cycleHistory.map { it.second.toFloat() })
                    }
                )
            }

            val cycleDates = remember(uiState.cycleHistory) {
                 uiState.cycleHistory.map { it.first.month.name.take(3) }
            }

            val cycleAxisFormatter = remember(cycleDates) {
                CartesianValueFormatter { _, value, _ ->
                    cycleDates.getOrElse(value.toInt()) { "" }
                }
            }

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = cycleAxisFormatter),
                    marker = rememberMarker()
                ),
                model = model,
                modifier = Modifier.height(200.dp)
            )
        } else {
            EmptyState(
                title = "No Trends Yet",
                description = "Log your period for at least 2 cycles to see predictions and history."
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.symptomCounts.isNotEmpty()) {
            Text("Top Symptoms", style = MaterialTheme.typography.titleMedium)
             
             val counts = remember(uiState.symptomCounts) {
                 uiState.symptomCounts.values.map { it.toFloat() }.toTypedArray()
             }
             val symptomNames = remember(uiState.symptomCounts) {
                 uiState.symptomCounts.keys.toList()
             }

             if (counts.isNotEmpty()) {
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

                 CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = symptomAxisFormatter),
                        marker = rememberMarker()
                    ),
                    model = model,
                    modifier = Modifier.height(200.dp)
                 )
             }
        }
    }
}

@Composable
fun ReportsTab(onGeneratePdf: () -> Unit, onGenerateCsv: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Export Your Health Data",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onGeneratePdf,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Generate Doctor's Report (PDF)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onGenerateCsv,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Export Data (CSV)")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "You can choose where to save your reports (e.g., Downloads or Google Drive).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyState(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info Icon",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
