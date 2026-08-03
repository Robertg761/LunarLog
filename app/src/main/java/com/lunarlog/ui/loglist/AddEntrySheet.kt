package com.lunarlog.ui.loglist

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import com.lunarlog.data.displayName
import com.lunarlog.data.SymptomCategory
import com.lunarlog.data.SymptomDefinition
import com.lunarlog.ui.theme.Spacing
import com.lunarlog.ui.util.flowLabel
import com.lunarlog.ui.util.mucusLabel
import com.lunarlog.ui.util.sexDriveLabel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


/**
 * The one time format the logging surfaces use, following the device's 12/24-hour setting rather
 * than hard-coding `h:mm a`. Shared with [LogListScreen]'s entry rows so a saved time reads back
 * exactly as it was picked.
 */
@Composable
internal fun rememberEntryTimeFormatter(): DateTimeFormatter {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
    }
}

/** The live answer for a field — the number the user just dialled in. */
@Composable
private fun ValueReadout(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** The key that explains a scale, deliberately quieter than the value it explains. */
@Composable
private fun ScaleLegend(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** A prompt that labels a control rather than reporting a value. */
@Composable
private fun FieldPrompt(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * The range each scalar slider offers.
 *
 * Declared once because the range now has two readers — the Slider and the clamp that seeds the
 * editor from an existing entry — and two independent copies of a scale is precisely how
 * out-of-domain sex-drive values reached the database to begin with.
 *
 * Domains follow the fields on `DailyLog`: flow 0..4, sleep quality 0..5, sex drive 0..3,
 * cervical mucus 0..4. Water and sleep hours are free measures rather than coded levels, so their
 * bounds are just what the control offers.
 */
private fun sliderRangeFor(type: LogEntryType): ClosedFloatingPointRange<Float> = when (type) {
    LogEntryType.FLOW -> 0f..4f
    LogEntryType.WATER -> 0f..15f
    LogEntryType.SLEEP -> 0f..12f
    LogEntryType.SLEEP_QUALITY -> 0f..5f
    LogEntryType.SEX -> 0f..3f
    LogEntryType.MUCUS -> 0f..4f
    else -> 0f..1f
}

private val addEntryTypeOrder = listOf(LogEntryType.FLOW) +
    LogEntryType.values().filterNot { it == LogEntryType.FLOW }

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
    var selectedType by remember { mutableStateOf(initialEntry?.type ?: LogEntryType.FLOW) }
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
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val timeFormatter = rememberEntryTimeFormatter()
    val timeLabel = remember(time, timeFormatter) { time.format(timeFormatter) }

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
                        // Clamped to the slider's own range. Builds up to 1.8.1 let the sex-drive
                        // slider reach 4 and 5, which `DailyLog.sexDrive` never defined, and those
                        // values are in real databases (and pass DataManagementRepository's
                        // `require(sexDrive in 0..5)` on restore). A Slider coerces out-of-range
                        // values for *display* only — it never calls onValueChange — so without
                        // this the editor would open on a legacy 5 with the thumb pinned to "High"
                        // while the readout said "None", and saving without touching it would
                        // write the stale 5 straight back.
                        initialEntry.value.toFloatOrNull()
                            ?.takeIf { it > 0f }
                            ?.coerceIn(sliderRangeFor(initialEntry.type))
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

    if (showTimePicker) {
        // Seeded from the current value each time it opens, so re-opening the picker shows the
        // time the sheet is actually holding rather than "now".
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = is24Hour
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        time = LocalTime.of(pickerState.hour, pickerState.minute)
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            // The clock dial is 256dp wide; the platform default dialog width clips it on
            // narrow screens, so let the M3 dialog size itself to its content instead.
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Sheets take the same warm `surfaceContainer` as LunarLogCard rather than
        // BottomSheetDefaults' `surfaceContainerLow`, so a sheet reads as the same material
        // as the cards it slides over instead of a second, paler one.
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                // ModalBottomSheet only applies vertical system-bar insets, so without this the
                // IME covered the Note/Temperature fields and the Save button.
                .imePadding()
                // One sheet gutter across the app (Spacing.sheetHorizontal), instead of the
                // five different insets the sheets used to carry.
                .padding(horizontal = Spacing.sheetHorizontal)
                .padding(top = Spacing.sm)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(if (initialEntry != null) "Edit Log" else "Add Log", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.lg))

            // Summary of Selected Items
            if (entryData.isNotEmpty()) {
                Text("Currently Selected:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Spacing.sm))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(bottom = Spacing.lg)
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
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove ${type.displayName}",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                            else -> {
                                // For scalars, clicking removes the whole entry for that type
                                item {
                                    // Words, not raw numbers. These chips are the running summary of
                                    // what is about to be saved, and "Flow: 3" asks the reader to
                                    // remember a scale the sheet is no longer showing them. SEX and
                                    // MUCUS used to fall through to the `else` branch and render the
                                    // Float itself — "Sex: 2.0".
                                    val displayValue = when(type) {
                                        LogEntryType.FLOW -> "Flow: ${flowLabel((data as Float).toInt())}"
                                        LogEntryType.WATER -> "Water: ${(data as Float).toInt()} cups"
                                        LogEntryType.SLEEP -> "Sleep: ${String.format(Locale.US, "%.1f", data as Float)}h"
                                        LogEntryType.SLEEP_QUALITY -> "Quality: ${(data as Float).toInt()}/5"
                                        LogEntryType.SEX -> "Sex drive: ${sexDriveLabel((data as Float).toInt())}"
                                        LogEntryType.MUCUS -> "Mucus: ${mucusLabel((data as Float).toInt())}"
                                        else -> "${type.displayName}: $data"
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

            // Type Selector. Transparent so the strip sits on the sheet's own colour instead of
            // TabRow's default `surface`, which is a different tone from the sheet container and
            // drew a pale band across it.
            ScrollableTabRow(
                selectedTabIndex = addEntryTypeOrder.indexOf(selectedType),
                edgePadding = 0.dp,
                containerColor = Color.Transparent
            ) {
                addEntryTypeOrder.forEach { type ->
                    Tab(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        // Without this every tab label renders in `primary` (the M3 default is
                        // unselectedContentColor = selectedContentColor), so the ten types looked
                        // uniformly "active".
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text(type.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

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
                    // The readout names the level, so the legend that spelled out the whole 0–4
                    // scale underneath is gone — it was a second copy of the same mapping, and it
                    // had already drifted out of step with `flowLabel` on the other sliders.
                    ValueReadout("Flow: ${flowLabel(currentVal.toInt())}")
                    Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.FLOW)
                            else entryData[LogEntryType.FLOW] = it
                        },
                        valueRange = sliderRangeFor(LogEntryType.FLOW),
                        steps = 3,
                        modifier = Modifier.semantics {
                            contentDescription = "Flow level"
                            stateDescription = flowLabel(currentVal.toInt())
                        }
                    )
                }
                LogEntryType.WATER -> {
                    val currentVal = entryData[LogEntryType.WATER] as? Float ?: 0f
                    ValueReadout("Cups: ${currentVal.toInt()}")
                    Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.WATER)
                            else entryData[LogEntryType.WATER] = it
                        },
                        valueRange = sliderRangeFor(LogEntryType.WATER),
                        steps = 14,
                        modifier = Modifier.semantics {
                            contentDescription = "Water cups"
                            stateDescription = currentVal.toInt().toString()
                        }
                    )
                    ScaleLegend("0: Not recorded")
                }
                LogEntryType.SLEEP -> {
                    val currentVal = entryData[LogEntryType.SLEEP] as? Float ?: 0f
                    ValueReadout("Hours: ${String.format(Locale.US, "%.1f", currentVal)}")
                    Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.SLEEP)
                            else entryData[LogEntryType.SLEEP] = it
                        },
                        valueRange = sliderRangeFor(LogEntryType.SLEEP),
                        steps = 23,
                        modifier = Modifier.semantics {
                            contentDescription = "Sleep hours"
                            stateDescription = String.format(Locale.US, "%.1f", currentVal)
                        }
                    )
                }
                LogEntryType.SLEEP_QUALITY -> {
                    val currentVal = entryData[LogEntryType.SLEEP_QUALITY] as? Float ?: 0f
                    ValueReadout("Stars: ${currentVal.toInt()}")
                    Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.SLEEP_QUALITY)
                            else entryData[LogEntryType.SLEEP_QUALITY] = it
                        },
                        valueRange = sliderRangeFor(LogEntryType.SLEEP_QUALITY),
                        steps = 4,
                        modifier = Modifier.semantics {
                            contentDescription = "Sleep quality stars"
                            stateDescription = currentVal.toInt().toString()
                        }
                    )
                    ScaleLegend("0: Not recorded, 1: Poor, 5: Excellent")
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
                    // 0..3, not 0..5. `DailyLog.sexDrive` defines exactly four levels and every
                    // reader of it — the day summary, the report generator, `sexDriveLabel` — maps
                    // anything above 3 to "None". The slider let you pick 4 or 5, saved them, and
                    // then showed the day as having no sex drive logged at all.
                    ValueReadout("Sex drive: ${sexDriveLabel(currentVal.toInt())}")
                    Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.SEX)
                            else entryData[LogEntryType.SEX] = it
                        },
                        valueRange = sliderRangeFor(LogEntryType.SEX),
                        steps = 2,
                        modifier = Modifier.semantics {
                            contentDescription = "Sex drive"
                            stateDescription = sexDriveLabel(currentVal.toInt())
                        }
                    )
                }
                LogEntryType.MUCUS -> {
                    val currentVal = entryData[LogEntryType.MUCUS] as? Float ?: 0f
                    // This slider had its own list of words — "Not recorded, Dry, Sticky, Watery,
                    // Egg white" — one rung short of `DailyLog.cervicalMucus`, which defines
                    // 0=None/Dry, 1=Sticky, 2=Creamy, 3=Watery, 4=Egg White. Everything from 1 up
                    // was mislabelled: picking "Sticky" here stored a 2 and came back as "Creamy"
                    // in the day summary. The numbers are load-bearing too — AdvancedCycleIntelligence
                    // treats >= 3 as the fertile signal — so the words were what was wrong.
                    val label = mucusLabel(currentVal.toInt())
                    ValueReadout("Cervical mucus: $label")
                    Slider(
                        value = currentVal,
                        onValueChange = {
                            if (it == 0f) entryData.remove(LogEntryType.MUCUS)
                            else entryData[LogEntryType.MUCUS] = it
                        },
                        valueRange = sliderRangeFor(LogEntryType.MUCUS),
                        steps = 3,
                        modifier = Modifier.semantics {
                            contentDescription = "Cervical mucus"
                            stateDescription = label
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

            Spacer(Modifier.height(Spacing.lg))

            // Entries have always carried a time; until now the sheet silently stamped "now"
            // and gave no way to correct a log written after the fact.
            FieldPrompt("Time")
            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick = { showTimePicker = true },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.minTouchTarget)
                    .semantics { contentDescription = "Entry time, $timeLabel" }
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text(timeLabel, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(Spacing.lg))

            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Details (Applied to all)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.xl))

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
            Spacer(Modifier.height(Spacing.sheetHorizontal))
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
        FieldPrompt("Select $title")
        Spacer(Modifier.height(Spacing.sm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(symptoms) { symptom ->
                FilterChip(
                    selected = selected.contains(symptom.name),
                    onClick = { onSelect(symptom.name) },
                    label = { Text(symptom.displayName) },
                    // A bare FilterChip is 32dp tall — under the 48dp minimum touch target.
                    modifier = Modifier.heightIn(min = Spacing.minTouchTarget)
                )
            }
            item {
                AssistChip(
                    onClick = onAddCustom,
                    label = { Text("Custom") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier.heightIn(min = Spacing.minTouchTarget)
                )
            }
        }
    }
}


private fun Any?.asStringSet(): Set<String> =
    (this as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
