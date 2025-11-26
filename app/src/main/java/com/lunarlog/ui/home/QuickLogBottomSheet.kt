package com.lunarlog.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun QuickLogContent(
    isPeriodActive: Boolean,
    isPeriodOngoing: Boolean = false,
    isEndedToday: Boolean = false,
    onTogglePeriod: () -> Unit,
    quickSymptoms: List<String>,
    onSymptomClick: (String) -> Unit,
    onFullDetailsClick: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Log",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Period Toggle Button
        val buttonText = when {
            isPeriodOngoing -> "End Period"
            isEndedToday -> "Resume Period" // Or "Resume Period"
            else -> "Start Period"
        }
        
        val buttonIcon = when {
             isPeriodOngoing -> Icons.Default.Close
             isEndedToday -> Icons.Default.Edit // Icon for editing/resuming
             else -> Icons.Default.WaterDrop
        }
        
        val buttonColor = when {
            isPeriodOngoing -> MaterialTheme.colorScheme.error
            isEndedToday -> MaterialTheme.colorScheme.tertiary // Distinct color for ended state
            else -> MaterialTheme.colorScheme.primary
        }

        Button(
            onClick = onTogglePeriod,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = buttonIcon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (quickSymptoms.isNotEmpty()) {
            Text(
                text = "Often logged now:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
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
        OutlinedButton(
            onClick = onFullDetailsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add More Details")
        }
    }
}