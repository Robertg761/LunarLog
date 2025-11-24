package com.lunarlog.ui.logdetails

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.view.HapticFeedbackConstants
import com.lunarlog.data.SymptomCategory
import com.lunarlog.data.SymptomDefinition
import com.lunarlog.ui.components.ConfettiExplosion
import com.lunarlog.ui.components.SuccessOverlay
import com.lunarlog.ui.components.rememberShakeController
import com.lunarlog.ui.components.shake
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LogDetailsScreen(
    onBack: () -> Unit,
    viewModel: LogDetailsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSuccess by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var showAddSymptomDialog by remember { mutableStateOf(false) }
    val shakeController = rememberShakeController()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            showSuccess = true
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                 Modifier.sharedElement(
                     state = rememberSharedContentState(key = "day_${uiState.date.toEpochDay()}"),
                     animatedVisibilityScope = animatedVisibilityScope
                 )
            }
        } else Modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveLog() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Flow Section
                SectionHeader("Flow Intensity")
                Text(
                    text = when (uiState.flowLevel) {
                        0 -> "None"
                        1 -> "Spotting"
                        2 -> "Light"
                        3 -> "Medium"
                        4 -> "Heavy"
                        else -> "None"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = uiState.flowLevel.toFloat(),
                    onValueChange = { viewModel.updateFlowLevel(it.toInt()) },
                    valueRange = 0f..4f,
                    steps = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Physical Symptoms
                uiState.availableSymptoms[SymptomCategory.PHYSICAL]?.let { symptoms ->
                    SymptomSection(
                        title = "Physical Symptoms",
                        symptoms = symptoms,
                        selectedSymptoms = uiState.selectedSymptoms,
                        onToggle = { viewModel.toggleSymptom(it) }
                    )
                }

                // Discharge
                uiState.availableSymptoms[SymptomCategory.DISCHARGE]?.let { symptoms ->
                    SymptomSection(
                        title = "Discharge",
                        symptoms = symptoms,
                        selectedSymptoms = uiState.selectedSymptoms,
                        onToggle = { viewModel.toggleSymptom(it) }
                    )
                }

                // Mood (Emotional)
                if (uiState.availableMoods.isNotEmpty()) {
                    SymptomSection(
                        title = "Mood & Emotions",
                        symptoms = uiState.availableMoods,
                        selectedSymptoms = uiState.selectedMoods, // Note: using selectedMoods here
                        onToggle = { viewModel.toggleMood(it) }
                    )
                }

                // Other / Custom
                uiState.availableSymptoms[SymptomCategory.OTHER]?.let { symptoms ->
                    SymptomSection(
                        title = "Other",
                        symptoms = symptoms,
                        selectedSymptoms = uiState.selectedSymptoms,
                        onToggle = { viewModel.toggleSymptom(it) }
                    )
                }
                
                // Add Custom Symptom Button
                OutlinedButton(
                    onClick = { showAddSymptomDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp)) // Using Spacer logic inside Button
                    Text("Add Custom Symptom")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Wellness Section
                SectionHeader("Wellness")
                
                // Water
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Water Intake (Cups): ", modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (uiState.waterIntake > 0) viewModel.updateWaterIntake(uiState.waterIntake - 1) }) {
                            Icon(Icons.Filled.Remove, "Decrease Water")
                        }
                        Text(text = uiState.waterIntake.toString(), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = {
                            viewModel.updateWaterIntake(uiState.waterIntake + 1)
                            if (uiState.waterIntake + 1 == 8) {
                                showConfetti = true
                            }
                        }) {
                            Icon(Icons.Filled.Add, "Increase Water")
                        }
                    }
                    if (showConfetti) {
                        ConfettiExplosion(
                            modifier = Modifier.matchParentSize(),
                            durationMillis = 2000
                        )
                        LaunchedEffect(Unit) {
                            delay(2000)
                            showConfetti = false
                        }
                    }
                }
                
                val waterProgress by animateFloatAsState(targetValue = (uiState.waterIntake / 12f).coerceIn(0f, 1f), label = "water_anim")
                LinearProgressIndicator(
                    progress = { waterProgress },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sleep
                Text("Sleep: ${uiState.sleepHours} hrs")
                Slider(
                    value = uiState.sleepHours,
                    onValueChange = { viewModel.updateSleepHours(it) },
                    valueRange = 0f..12f,
                    steps = 23 // 0.5 increments
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quality:")
                    (1..5).forEach { star ->
                        IconButton(onClick = { viewModel.updateSleepQuality(star) }) {
                            Icon(
                                imageVector = if (star <= uiState.sleepQuality) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Rate $star stars",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Libido
                Text("Libido: " + when(uiState.sexDrive) { 0 -> "None"; 1 -> "Low"; 2 -> "Medium"; 3 -> "High"; else -> "None" })
                Slider(
                    value = uiState.sexDrive.toFloat(),
                    onValueChange = { viewModel.updateSexDrive(it.toInt()) },
                    valueRange = 0f..3f,
                    steps = 2
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Notes
                SectionHeader("Notes")
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = {
                        if (it.length <= 500) viewModel.updateNotes(it)
                        else shakeController.shake()
                    },
                    modifier = Modifier.fillMaxWidth().shake(shakeController),
                    label = { Text("Daily notes... (max 500)") },
                    minLines = 3,
                    supportingText = { Text("${uiState.notes.length}/500") }
                )
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showAddSymptomDialog) {
        AddCustomSymptomDialog(
            onDismiss = { showAddSymptomDialog = false },
            onConfirm = { name, category ->
                viewModel.addCustomSymptom(name, category)
                showAddSymptomDialog = false
            }
        )
    }

    if (showSuccess) {
        SuccessOverlay(onAnimationFinished = {
            viewModel.onNavigatedBack()
            onBack()
        })
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomSection(
    title: String,
    symptoms: List<SymptomDefinition>,
    selectedSymptoms: List<String>,
    onToggle: (String) -> Unit
) {
    val view = LocalView.current
    SectionHeader(title)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        symptoms.forEach { symptom ->
            val selected = selectedSymptoms.contains(symptom.name)
            FilterChip(
                selected = selected,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onToggle(symptom.name)
                },
                label = { Text(symptom.displayName) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                } else null
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomSymptomDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, SymptomCategory) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SymptomCategory.OTHER) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Symptom") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Symptom Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = category.name,
                        onValueChange = {},
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SymptomCategory.values().forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption.name) },
                                onClick = {
                                    category = selectionOption
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, category) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}