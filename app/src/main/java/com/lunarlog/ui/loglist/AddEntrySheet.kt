package com.lunarlog.ui.loglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import com.lunarlog.data.SymptomCategory
import com.lunarlog.data.SymptomData
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    date: LocalDate,
    initialEntry: LogEntry? = null,
    onDismiss: () -> Unit,
    onSave: (Map<LogEntryType, List<String>>, Long, String?) -> Unit
) {
    // Current active tab
    var selectedType by remember { mutableStateOf(initialEntry?.type ?: LogEntryType.SYMPTOM) }
    
    // Shared Details/Time
    var details by remember { mutableStateOf(initialEntry?.details ?: "") }
    var time by remember { 
        mutableStateOf(
            if (initialEntry != null) {
                Instant.ofEpochMilli(initialEntry.time).atZone(ZoneId.systemDefault()).toLocalTime()
            } else {
                LocalTime.now()
            }
        ) 
    }
    
    // Unified State Container
    // Types map to:
    // SYMPTOM/MOOD -> Set<String>
    // FLOW/WATER/SLEEP/SLEEP_QUALITY -> Float
    // NOTE/etc -> String
    val entryData = remember { 
        mutableStateMapOf<LogEntryType, Any>().apply {
            if (initialEntry != null) {
                when (initialEntry.type) {
                    LogEntryType.SYMPTOM, LogEntryType.MOOD -> {
                        put(initialEntry.type, setOf(initialEntry.value))
                    }
                    LogEntryType.FLOW, LogEntryType.WATER, LogEntryType.SLEEP, LogEntryType.SLEEP_QUALITY -> {
                        put(initialEntry.type, initialEntry.value.toFloatOrNull() ?: 0f)
                    }
                    else -> {
                        put(initialEntry.type, initialEntry.value)
                    }
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(if (initialEntry != null) "Edit Log" else "Add Log", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            // Summary of Selected Items
            if (entryData.isNotEmpty()) {
                Text("Currently Selected:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    entryData.forEach { (type, data) ->
                        when (type) {
                            LogEntryType.SYMPTOM, LogEntryType.MOOD -> {
                                val set = data as? Set<String> ?: emptySet()
                                items(set.toList()) { item ->
                                    InputChip(
                                        selected = true,
                                        onClick = { 
                                            // Remove this specific item
                                            val newSet = set - item
                                            if (newSet.isEmpty()) {
                                                entryData.remove(type)
                                            } else {
                                                entryData[type] = newSet
                                            }
                                        },
                                        label = { Text(item) },
                                        trailingIcon = { Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                            else -> {
                                // For scalars, clicking removes the whole entry for that type
                                item {
                                    val displayValue = when(type) {
                                        LogEntryType.FLOW -> "Flow: ${(data as Float).toInt()}"
                                        LogEntryType.WATER -> "Water: ${(data as Float).toInt()}"
                                        LogEntryType.SLEEP -> "Sleep: ${String.format("%.1f", data as Float)}h"
                                        LogEntryType.SLEEP_QUALITY -> "Quality: ${(data as Float).toInt()}"
                                        else -> "${type.name.lowercase().capitalize()}: $data"
                                    }
                                    InputChip(
                                        selected = true,
                                        onClick = { entryData.remove(type) },
                                        label = { Text(displayValue) },
                                        trailingIcon = { Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Type Selector
            ScrollableTabRow(
                selectedTabIndex = LogEntryType.values().indexOf(selectedType),
                edgePadding = 0.dp
            ) {
                LogEntryType.values().forEach { type ->
                    Tab(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        text = { Text(type.name.lowercase().capitalize()) }
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))

            // Value Input based on Type
            when (selectedType) {
                LogEntryType.SYMPTOM -> {
                    val currentSet = entryData[LogEntryType.SYMPTOM] as? Set<String> ?: emptySet()
                    SymptomSelector(
                        category = SymptomCategory.PHYSICAL, 
                        onSelect = { symptom ->
                            val newSet = if (currentSet.contains(symptom)) currentSet - symptom else currentSet + symptom
                            if (newSet.isEmpty()) entryData.remove(LogEntryType.SYMPTOM)
                            else entryData[LogEntryType.SYMPTOM] = newSet
                        },
                        selected = currentSet
                    )
                }
                LogEntryType.MOOD -> {
                    val currentSet = entryData[LogEntryType.MOOD] as? Set<String> ?: emptySet()
                     SymptomSelector(
                        category = SymptomCategory.EMOTIONAL, 
                        onSelect = { mood ->
                            val newSet = if (currentSet.contains(mood)) currentSet - mood else currentSet + mood
                            if (newSet.isEmpty()) entryData.remove(LogEntryType.MOOD)
                            else entryData[LogEntryType.MOOD] = newSet
                        },
                        selected = currentSet
                    )
                }
                LogEntryType.FLOW -> {
                    val currentVal = entryData[LogEntryType.FLOW] as? Float ?: 0f
                    Text("Flow Level: ${currentVal.toInt()}")
                    Slider(
                        value = currentVal,
                        onValueChange = { entryData[LogEntryType.FLOW] = it },
                        valueRange = 0f..4f,
                        steps = 3
                    )
                    Text("0: None, 1: Spotting, 2: Light, 3: Medium, 4: Heavy")
                }
                LogEntryType.WATER -> {
                    val currentVal = entryData[LogEntryType.WATER] as? Float ?: 0f
                     Text("Cups: ${currentVal.toInt()}")
                     Slider(
                        value = currentVal,
                        onValueChange = { entryData[LogEntryType.WATER] = it },
                        valueRange = 1f..15f,
                        steps = 13
                    )
                }
                LogEntryType.SLEEP -> {
                    val currentVal = entryData[LogEntryType.SLEEP] as? Float ?: 0f
                    Text("Hours: ${String.format("%.1f", currentVal)}")
                    Slider(
                        value = currentVal,
                        onValueChange = { entryData[LogEntryType.SLEEP] = it },
                        valueRange = 0f..12f,
                        steps = 23
                    )
                }
                LogEntryType.SLEEP_QUALITY -> {
                    val currentVal = entryData[LogEntryType.SLEEP_QUALITY] as? Float ?: 0f
                    Text("Stars: ${currentVal.toInt()}")
                    Slider(
                        value = currentVal,
                        onValueChange = { entryData[LogEntryType.SLEEP_QUALITY] = it },
                        valueRange = 1f..5f,
                        steps = 3
                    )
                }
                LogEntryType.NOTE -> {
                    val currentVal = entryData[LogEntryType.NOTE] as? String ?: ""
                    OutlinedTextField(
                        value = currentVal,
                        onValueChange = { 
                            if (it.isBlank()) entryData.remove(LogEntryType.NOTE)
                            else entryData[LogEntryType.NOTE] = it
                        },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    val currentVal = entryData[selectedType] as? String ?: ""
                    OutlinedTextField(
                        value = currentVal,
                        onValueChange = { 
                             if (it.isBlank()) entryData.remove(selectedType)
                             else entryData[selectedType] = it
                        },
                        label = { Text("Value") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Details (Applied to all)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val timestamp = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    
                    // Convert raw map to simplified (Type -> List<String>) format for the ViewModel
                    val payload = entryData.mapValues { (type, data) ->
                        when(type) {
                            LogEntryType.SYMPTOM, LogEntryType.MOOD -> {
                                (data as Set<String>).toList()
                            }
                            LogEntryType.FLOW, LogEntryType.WATER, LogEntryType.SLEEP_QUALITY -> {
                                listOf((data as Float).toInt().toString())
                            }
                            LogEntryType.SLEEP -> {
                                listOf(String.format("%.1f", data as Float))
                            }
                            else -> {
                                listOf(data.toString())
                            }
                        }
                    }
                    
                    onSave(payload, timestamp, details.ifBlank { null })
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = entryData.isNotEmpty()
            ) {
                Text("Save (${entryData.values.sumOf { if (it is Set<*>) it.size else 1 }} items)")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomSelector(
    category: SymptomCategory,
    onSelect: (String) -> Unit,
    selected: Set<String>
) {
    val symptoms = SymptomData.defaultSymptoms.filter { it.category == category }
    Column {
        Text("Select ${category.name.lowercase().capitalize()}")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(symptoms) { symptom ->
                FilterChip(
                    selected = selected.contains(symptom.name),
                    onClick = { onSelect(symptom.name) },
                    label = { Text(symptom.displayName) }
                )
            }
        }
    }
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }