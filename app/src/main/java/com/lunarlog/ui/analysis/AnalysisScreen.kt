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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            .padding(16.dp)
    ) {
        if (uiState.cycleHistory.isNotEmpty()) {
            Text("Cycle Length History", style = MaterialTheme.typography.titleMedium)
            val entries = uiState.cycleHistory.map { it.second.toFloat() }.toTypedArray()
            val model = entryModelOf(*entries)
            
            Chart(
                chart = lineChart(),
                model = model,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier.height(200.dp)
            )
        } else {
            Text("No enough cycle data for charts.")
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.symptomCounts.isNotEmpty()) {
            Text("Top Symptoms", style = MaterialTheme.typography.titleMedium)
            // For symptoms, we might want a bar chart, but mapping string labels to Vico is tricky without a custom axis.
            // For now, let's list them or use a simple ColumnChart with index
             val counts = uiState.symptomCounts.values.map { it.toFloat() }.toTypedArray()
             if (counts.isNotEmpty()) {
                 val model = entryModelOf(*counts)
                 Chart(
                    chart = columnChart(),
                    model = model,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(), // Labels would need custom formatter
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
            "Documents are saved to your device's Documents folder.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
