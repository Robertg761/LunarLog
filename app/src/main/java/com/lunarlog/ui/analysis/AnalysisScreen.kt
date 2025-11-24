package com.lunarlog.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onBack: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            ReportGenerator.generatePdf(
                                context,
                                uiState.cycleHistory,
                                uiState.symptomCounts,
                                uiState.moodCounts
                            )
                        },
                        onGenerateCsv = {
                            ReportGenerator.generateCsv(context, uiState.cycleHistory)
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
        if (uiState.cycleHistory.isNotEmpty()) {
            Text("Cycle Length History", style = MaterialTheme.typography.titleMedium)
            val entries = uiState.cycleHistory.map { it.second.toFloat() }.toTypedArray()
            val model = entryModelOf(*entries)
            
            val cycleDates = uiState.cycleHistory.map { 
                it.first.month.name.take(3) 
            }
            val cycleAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                cycleDates.getOrElse(value.toInt()) { "" }
            }

            Chart(
                chart = lineChart(),
                model = model,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(valueFormatter = cycleAxisFormatter),
                modifier = Modifier.height(200.dp),
                marker = rememberMarker()
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
             val counts = uiState.symptomCounts.values.map { it.toFloat() }.toTypedArray()
             val symptomNames = uiState.symptomCounts.keys.toList()

             if (counts.isNotEmpty()) {
                 val model = entryModelOf(*counts)
                 
                 val symptomAxisFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                    symptomNames.getOrElse(value.toInt()) { "" }
                 }

                 Chart(
                    chart = columnChart(),
                    model = model,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(valueFormatter = symptomAxisFormatter),
                    modifier = Modifier.height(200.dp),
                    marker = rememberMarker()
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
            "Documents are saved to your device's Documents folder.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
