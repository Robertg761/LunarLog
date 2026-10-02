package com.lunarlog.ui.settings

import android.text.format.DateFormat
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.*
import androidx.compose.ui.res.stringResource
import com.lunarlog.R
import com.lunarlog.ui.components.FormSectionTitle
import com.lunarlog.ui.components.InlineError
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lunarlog.data.Medication
import com.lunarlog.ui.components.DateField
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationEditor(initial: Medication?, isSaving: Boolean, error: String?, onDismiss: () -> Unit, onSave: (Medication) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var dosage by rememberSaveable { mutableStateOf(initial?.dosage.orEmpty()) }
    var frequency by rememberSaveable { mutableStateOf(initial?.frequency ?: "daily") }
    var startDay by rememberSaveable { mutableLongStateOf(initial?.startDate ?: LocalDate.now().toEpochDay()) }
    var endDay by rememberSaveable { mutableStateOf(initial?.endDate) }
    var reminderEnabled by rememberSaveable { mutableStateOf(initial?.let { com.lunarlog.logic.MedicationScheduler.reminderTimes(it).isNotEmpty() } == true) }
    var reminderMinutes by rememberSaveable { mutableLongStateOf(initial?.reminderTime ?: 540L) }
    var dosesText by rememberSaveable { mutableStateOf((initial?.dosesPerDay ?: 1).toString()) }
    var timesText by rememberSaveable { mutableStateOf(com.lunarlog.logic.MedicationScheduler.reminderTimes(initial ?: Medication(name = "", startDate = 0)).joinToString(",") { it.toString() }) }
    val times = timesText.split(',').mapNotNull { it.toLongOrNull() }
    var editingTime by rememberSaveable { mutableIntStateOf(-1) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val dismiss = { if (!isSaving) { if (dirty) discard = true else onDismiss() } }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_discard_unsaved_medication_650472)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_discard_eb1a70)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_keep_editing_e76fd2)) } })
    val doseCount = dosesText.toIntOrNull()
    val validDoseCount = frequency == "as_needed" || (doseCount != null && doseCount in 1..24)
    val validReminders = frequency == "as_needed" || !reminderEnabled || (times.isNotEmpty() && times.size == doseCount)
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    if (showTime) {
        LunarLogTimePickerDialog("Reminder time", (reminderMinutes / 60).toInt(), (reminderMinutes % 60).toInt(),
            onDismiss = { showTime = false }, onConfirm = { hour, minute -> reminderMinutes = hour * 60L + minute
                val updated = times.toMutableList()
                if (editingTime in updated.indices) updated[editingTime] = reminderMinutes else updated.add(reminderMinutes)
                timesText = updated.distinct().sorted().joinToString(","); dirty = true; showTime = false })
    } else AlertDialog(onDismissRequest = dismiss, title = { Text(if (initial == null) "Add medication" else "Edit medication") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormSectionTitle(stringResource(R.string.medication_details_heading))
                OutlinedTextField(name, { name = it.take(80); dirty = true }, label = { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_name_dcd1d5)) }, singleLine = true, enabled = !isSaving, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(dosage, { dosage = it.take(80); dirty = true }, label = { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_dose_optional_f45b02)) }, singleLine = true, enabled = !isSaving, modifier = Modifier.fillMaxWidth())
                FormSectionTitle(stringResource(R.string.schedule_heading))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("daily" to "Daily", "weekly" to "Weekly", "as_needed" to "As needed").forEach { (key, label) ->
                        FilterChip(frequency == key, { frequency = key; dirty = true; if (key == "as_needed") reminderEnabled = false }, label = { Text(label) }, enabled = !isSaving)
                    }
                }
                DateField(if (frequency == "weekly") "First scheduled dose" else "Start date", LocalDate.ofEpochDay(startDay), { it?.let { startDay = it.toEpochDay(); dirty = true } }, enabled = !isSaving)
                if (frequency == "weekly") Text("Repeats every ${LocalDate.ofEpochDay(startDay).dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, androidx.compose.ui.platform.LocalLocale.current.platformLocale)}.")
                DateField("End date", endDay?.let(LocalDate::ofEpochDay), { endDay = it?.toEpochDay(); dirty = true }, optional = true, enabled = !isSaving)
                if (endDay != null && endDay!! < startDay) InlineError(stringResource(R.string.ui_end_date_must_follow_the_start_date_09529c))
                if (frequency != "as_needed") {
                    OutlinedTextField(dosesText, { dosesText = it.filter(Char::isDigit).take(2); dirty = true },
                        label = { Text(stringResource(R.string.doses_per_day_label)) },
                        supportingText = { Text(stringResource(R.string.doses_per_day_help)) },
                        isError = !validDoseCount, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, enabled = !isSaving, modifier = Modifier.fillMaxWidth())
                    FormSectionTitle(stringResource(R.string.reminders_heading))
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(reminderEnabled, enabled = !isSaving, role = Role.Switch,
                            onValueChange = { reminderEnabled = it; dirty = true }),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.ui_reminder_31757b), Modifier.weight(1f))
                        Switch(reminderEnabled, onCheckedChange = null, enabled = !isSaving)
                    }
                    if (reminderEnabled) {
                        Text(stringResource(R.string.reminder_count_help, times.size, doseCount ?: 0),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (validReminders) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                        times.forEachIndexed { index, time -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val timeLabel = LocalTime.MIDNIGHT.plusMinutes(time).format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", androidx.compose.ui.platform.LocalLocale.current.platformLocale))
                            val editLabel = stringResource(R.string.edit_reminder_at, timeLabel)
                            val removeLabel = stringResource(R.string.remove_reminder_at, timeLabel)
                            TextButton(modifier = Modifier.semantics { contentDescription = editLabel }, enabled = !isSaving, onClick = { editingTime = index; reminderMinutes = time; showTime = true }) {
                                Text(timeLabel)
                            }
                            TextButton(modifier = Modifier.semantics { contentDescription = removeLabel }, enabled = !isSaving, onClick = { timesText = times.filterIndexed { i, _ -> i != index }.joinToString(","); dirty = true }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_remove_reminder_aab682)) }
                        } }
                        TextButton(enabled = !isSaving && times.size < (dosesText.toIntOrNull() ?: 0), onClick = { editingTime = -1; showTime = true }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_add_reminder_time_901120)) }
                    }
                }
                Text(stringResource(if (frequency == "as_needed") R.string.as_needed_help else R.string.reminder_delivery_help),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { InlineError(it) }
                if (isSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }, confirmButton = {
            Button(enabled = !isSaving && validDoseCount && validReminders && name.isNotBlank() && (endDay == null || endDay!! >= startDay), onClick = {
                onSave(Medication(id = initial?.id ?: 0, name = name.trim(), dosage = dosage.trim(), frequency = frequency,
                    startDate = startDay, endDate = endDay, dosesPerDay = doseCount?.takeIf { it in 1..24 } ?: 1, reminderTimes = times.takeIf { reminderEnabled && frequency != "as_needed" }.orEmpty(), reminderTime = times.firstOrNull().takeIf { reminderEnabled && frequency != "as_needed" }, isArchived = initial?.isArchived ?: false))
            }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_save_1509f5)) }
        }, dismissButton = { TextButton(onClick = dismiss, enabled = !isSaving) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_cancel_19766e)) } })
}
