package com.lunarlog.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    isPeriodActive: Boolean,
    onTogglePeriod: () -> Unit,
    quickSymptoms: List<String>,
    onSymptomClick: (String) -> Unit,
    onFullDetailsClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp), // Extra padding for navigation bar
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Quick Log",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Period Toggle Button
            Button(
                onClick = {
                    onTogglePeriod()
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPeriodActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = if (isPeriodActive) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPeriodActive) "End Period" else "Start Period",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (quickSymptoms.isNotEmpty()) {
                Text(
                    text = "Often logged now:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickSymptoms) { symptom ->
                        SuggestionChip(
                            onClick = { 
                                onSymptomClick(symptom)
                            },
                            label = { Text(symptom) },
                            icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Full Details Link
            TextButton(
                onClick = {
                    onFullDetailsClick()
                    onDismissRequest()
                }
            ) {
                Text("Add More Details")
            }
        }
    }
}
