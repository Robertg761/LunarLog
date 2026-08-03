package com.lunarlog.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.lunarlog.ui.theme.Spacing

/**
 * Leading icon size inside a button. The app runs two icon sizes and only two: 18dp in buttons,
 * 16dp in chips.
 */
private val ButtonIconSize = 18.dp

/** Leading icon size inside a chip. */
private val ChipIconSize = 16.dp

/**
 * Body of the quick-log bottom sheet. Hosted by a real `ModalBottomSheet` in HomeScreen, so it has
 * no close button of its own: the drag handle, the scrim and back all dismiss it.
 */
@Composable
fun QuickLogContent(
    isPeriodActive: Boolean,
    isPeriodOngoing: Boolean = false,
    isEndedToday: Boolean = false,
    onTogglePeriod: () -> Unit,
    quickSymptoms: List<String>,
    onSymptomClick: (String) -> Unit,
    onFullDetailsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = Spacing.sheetHorizontal,
                end = Spacing.sheetHorizontal,
                bottom = Spacing.sheetHorizontal
            ),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Quick Log",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

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
                // height() would clip "Resume Period" mid-glyph at large font scales.
                .heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = buttonIcon,
                contentDescription = null,
                modifier = Modifier.size(ButtonIconSize)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        if (quickSymptoms.isNotEmpty()) {
            Text(
                text = "Often logged now:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickSymptoms) { symptom ->
                    SuggestionChip(
                        onClick = {
                            onSymptomClick(symptom)
                        },
                        label = { Text(symptom) },
                        icon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(ChipIconSize)
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xl))
        }

        // Full Details Link
        OutlinedButton(
            onClick = onFullDetailsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(ButtonIconSize)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text("Add More Details")
        }
    }
}
