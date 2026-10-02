package com.lunarlog.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lunarlog.data.SymptomDefinition

@Composable
fun CustomSymptomManager(items: List<SymptomDefinition>, rename: (Long, String) -> Unit, archive: (Long, Boolean) -> Unit, close: () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_custom_symptoms_and_moods_c00b35)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_renaming_changes_the_choice_label_existing_entries_keep_a13743))
            if (items.none { it.isCustom }) Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_no_custom_choices_yet_add_them_from_the_daily_entry_she_1e5bee))
            items.filter { it.isCustom }.forEach { symptom -> key(symptom.id) {
                var label by rememberSaveable(symptom.displayName) { mutableStateOf(symptom.displayName) }
                OutlinedTextField(label, { label = it.take(50) }, label = { Text(symptom.name) })
                Row {
                    TextButton(enabled = label.isNotBlank() && label.trim() != symptom.displayName, onClick = { rename(symptom.id, label) }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_rename_3064d7)) }
                    TextButton(onClick = { archive(symptom.id, !symptom.isArchived) }) { Text(if (symptom.isArchived) "Restore choice" else "Archive") }
                }
            } }
        }
    }, confirmButton = { TextButton(onClick = close) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_done_11a676)) } })
}
