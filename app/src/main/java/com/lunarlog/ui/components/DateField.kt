package com.lunarlog.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import java.time.LocalDate
import com.lunarlog.ui.util.MediumDate
import com.lunarlog.ui.util.toPickerMillis
import com.lunarlog.ui.util.toPickerLocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: LocalDate?, onChange: (LocalDate?) -> Unit, optional: Boolean = false, enabled: Boolean = true) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column {
        OutlinedButton(enabled = enabled, onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${value?.format(MediumDate) ?: "Not set"}")
        }
        if (optional && value != null) TextButton(enabled = enabled, onClick = { onChange(null) }) { Text("Clear $label") }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (value ?: LocalDate.now()).toPickerMillis(),
            yearRange = 1900..(LocalDate.now().year + 10))
        DatePickerDialog(onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onChange(it.toPickerLocalDate()) }; open = false }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_ok_565339)) } },
            dismissButton = { TextButton(onClick = { open = false }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_cancel_19766e)) } }) { DatePicker(state) }
    }
}
