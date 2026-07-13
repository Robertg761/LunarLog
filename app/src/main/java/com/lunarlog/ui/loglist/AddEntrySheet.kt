package com.lunarlog.ui.loglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import com.lunarlog.data.SymptomCategory
import com.lunarlog.data.SymptomDefinition
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    date: LocalDate,
    initialEntry: LogEntry? = null,
    symptomDefinitions: List<SymptomDefinition> = emptyList(),
    onAddCustomSymptom: (String, SymptomCategory) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onSave: (Map<LogEntryType, List<String>>, Long, String?) -> Unit
) {
    // Current active tab
    var selectedType by remember { mutableStateOf(initialEntry?.type ?: LogEntryType.SYMPTOM) }
    var customCategory by remember { mutableStateOf<SymptomCategory?>(null) }
    var customName by remember { mutableStateOf("") }
    
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
                        if (initialEntry.value.isNotBlank()) {
                            put(initialEntry.type, setOf(initialEntry.value))
                        }
                    }
                    LogEntryType.FLOW, LogEntryType.WATER, LogEntryType.SLEEP,
                    LogEntryType.SLEEP_QUALITY, LogEntryType.SEX, LogEntryType.MUCUS -> {
                        initialEntry.value.toFloatOrNull()
                            ?.takeIf { it > 0f }
                            ?.let { put(initialEntry.type, it) }
                    }
                    else -> {
                        if (initialEntry.value.isNotBlank()) {
                            put(initialEntry.type, initialEntry.value)
                        }
                    }
                }
            }
        }
    }

    if (customCategory != null) {
        AlertDialog(
            onDismissRequest = {
                customCategory = null
                customName = ""
            },
            title = { Text(if (customCategory == SymptomCategory.EMOTIONAL) "Add custom mood" else "Add custom symptom") },
            text = {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it.take(50) },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = customName.isNotBlank(),
                    onClick = {
                        val category = customCategory ?: return@TextButton
                        val normalized = customName.trim().replace(Regex("\\s+"), " ")
                        onAddCustomSymptom(normalized, category)
                        val type = if (category == SymptomCategory.EMOTIONAL) LogEntryType.MOOD else LogEntryType.SYMPTOM
                        val current = entryData[type].asStringSet()
                        entryData[type] = current + normalized
                        customCategory = null
                        customName = ""
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    customCategory = null
                    customName = ""
                }) { Text("Cancel") }
            }
        )
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
                                val set = data.asStringSet()
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
                                        LogEntryType.SLEEP -> "Sleep: ${String.format(Locale.US, "%.1f", data as Float)}h"
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
                    val currentSet = entryData[LogEntryType.SYMPTOM].asStringSet()
                    SymptomSelector(
                        title = "symptoms",
                        symptoms = symptomDefinitions.filter { it.category != SymptomCategory.EMOTIONAL },
                        onSelect = { symptom ->
                            val newSet = if (currentSet.contains(symptom)) currentSet - symptom else currentSet + symptom
                            if (newSet.isEmpty()) entryData.remove(LogEntryType.SYMPTOM)
                            else entryData[LogEntryType.SYMPTOM] = newSet
                        },
                        selected = currentSet,
                        onAddCustom = { customCategory = SymptomCategory.PHYSICAL }
                    )
                }
                LogEntryType.MOOD -> {
                    val currentSet = entryData[LogEntryType.MOOD].asStringSet()
                     SymptomSelector(
                        title = "moods",
                        symptoms = symptomDefinitions.filter { it.category == SymptomCategory.EMOTIONAL },
                        onSelect = { mood ->
                            val newSet = if (currentSet.contains(mood)) currentSet - mood else currentSet + mood
                            if (newSet.isEmpty()) entryData.remove(LogEntryType.MOOD)
                            else entryData[LogEntryType.MOOD] = newSet
                        },
                        selected = currentSet,
                        onAddCustom = { customCategory = SymptomCategory.EMOTIONAL }
                    )
                }
                LogEntryType.FLOW -> {
                    val currentVal = entryData[LogEntryType.FLOW] as? Float ?: 0f
                    Text("Flow Level: ${currentVal.toInt()}")
                    Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.FLOW)
                            else entryData[LogEntryType.FLOW] = it
                        },
                        valueRange = 0f..4f,
                        steps = 3,
                        modifier = Modifier.semantics {
                            contentDescription = "Flow level"
                            stateDescription = currentVal.toInt().toString()
                        }
                    )
                    Text("0: None, 1: Spotting, 2: Light, 3: Medium, 4: Heavy")
                }
                LogEntryType.WATER -> {
                    val currentVal = entryData[LogEntryType.WATER] as? Float ?: 0f
                     Text("Cups: ${currentVal.toInt()}")
                     Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.WATER)
                            else entryData[LogEntryType.WATER] = it
                        },
                        valueRange = 0f..15f,
                        steps = 14,
                        modifier = Modifier.semantics {
                            contentDescription = "Water cups"
                            stateDescription = currentVal.toInt().toString()
                        }
                    )
                    Text("0: Not recorded")
                }
                LogEntryType.SLEEP -> {
                    val currentVal = entryData[LogEntryType.SLEEP] as? Float ?: 0f
                    Text("Hours: ${String.format(Locale.US, "%.1f", currentVal)}")
                    Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.SLEEP)
                            else entryData[LogEntryType.SLEEP] = it
                        },
                        valueRange = 0f..12f,
                        steps = 23,
                        modifier = Modifier.semantics {
                            contentDescription = "Sleep hours"
                            stateDescription = String.format(Locale.US, "%.1f", currentVal)
                        }
                    )
                }
                LogEntryType.SLEEP_QUALITY -> {
                    val currentVal = entryData[LogEntryType.SLEEP_QUALITY] as? Float ?: 0f
                    Text("Stars: ${currentVal.toInt()}")
                    Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.SLEEP_QUALITY)
                            else entryData[LogEntryType.SLEEP_QUALITY] = it
                        },
                        valueRange = 0f..5f,
                        steps = 4,
                        modifier = Modifier.semantics {
                            contentDescription = "Sleep quality stars"
                            stateDescription = currentVal.toInt().toString()
                        }
                    )
                    Text("0: Not recorded, 1: Poor, 5: Excellent")
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
                LogEntryType.SEX -> {
                    val currentVal = entryData[LogEntryType.SEX] as? Float ?: 0f
                    Text("Sex drive: ${currentVal.toInt()}")
                    Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.SEX)
                            else entryData[LogEntryType.SEX] = it
                        },
                        valueRange = 0f..5f,
                        steps = 4,
                        modifier = Modifier.semantics {
                            contentDescription = "Sex drive"
                            stateDescription = currentVal.toInt().toString()
                        }
                    )
                    Text("0: Not recorded, 1: Very low, 5: Very high")
                }
                LogEntryType.MUCUS -> {
                    val currentVal = entryData[LogEntryType.MUCUS] as? Float ?: 0f
                    val labels = listOf("Not recorded", "Dry", "Sticky", "Watery", "Egg white")
                    Text("Cervical mucus: ${labels[currentVal.toInt().coerceIn(0, 4)]}")
                    Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.MUCUS)
                            else entryData[LogEntryType.MUCUS] = it
                        },
                        valueRange = 0f..4f,
                        steps = 3,
                        modifier = Modifier.semantics {
                            contentDescription = "Cervical mucus"
                            stateDescription = labels[currentVal.toInt().coerceIn(0, 4)]
                        }
                    )
                }
                LogEntryType.TEMPERATURE -> {
                    val currentVal = entryData[LogEntryType.TEMPERATURE] as? String ?: ""
                    val parsed = currentVal.replace(',', '.').toFloatOrNull()
                    val isValid = currentVal.isBlank() || parsed != null &&
                        (parsed in 34f..43f || parsed in 90f..110f)
                    OutlinedTextField(
                        value = currentVal,
                        onValueChange = { value ->
                            val normalized = value.replace(',', '.').take(6)
                            if (normalized.isBlank()) entryData.remove(LogEntryType.TEMPERATURE)
                            else entryData[LogEntryType.TEMPERATURE] = normalized
                        },
                        label = { Text("Basal temperature (°C or °F)") },
                        supportingText = {
                            Text(if (isValid) "Enter 34–43 °C or 90–110 °F" else "Enter a plausible °C or °F temperature")
                        },
                        isError = !isValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
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
                                data.asStringSet().toList()
                            }
                            LogEntryType.FLOW, LogEntryType.WATER, LogEntryType.SLEEP_QUALITY,
                            LogEntryType.SEX, LogEntryType.MUCUS -> {
                                listOf((data as Float).toInt().toString())
                            }
                            LogEntryType.SLEEP -> {
                                listOf(String.format(Locale.US, "%.1f", data as Float))
                            }
                            else -> {
                                listOf(data.toString())
                            }
                        }
                    }
                    
                    onSave(payload, timestamp, details.ifBlank { null })
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (entryData.isNotEmpty() || initialEntry != null) &&
                    ((entryData[LogEntryType.TEMPERATURE] as? String)?.let { value ->
                        value.toFloatOrNull()?.let { it in 34f..43f || it in 90f..110f } == true
                    } ?: true)
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
    title: String,
    symptoms: List<SymptomDefinition>,
    onSelect: (String) -> Unit,
    selected: Set<String>,
    onAddCustom: () -> Unit
) {
    Column {
        Text("Select $title")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(symptoms) { symptom ->
                FilterChip(
                    selected = selected.contains(symptom.name),
                    onClick = { onSelect(symptom.name) },
                    label = { Text(symptom.displayName) }
                )
            }
            item {
                AssistChip(
                    onClick = onAddCustom,
                    label = { Text("Custom") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                )
            }
        }
    }
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private fun Any?.asStringSet(): Set<String> =
    (this as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
