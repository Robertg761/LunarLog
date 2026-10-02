package com.lunarlog.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.BuildConfig
import com.lunarlog.R
import com.lunarlog.data.Medication
import com.lunarlog.ui.components.LunarLogCard
import com.lunarlog.ui.components.LunarLogTopAppBar
import com.lunarlog.ui.components.SectionHeader
import com.lunarlog.ui.theme.DefaultThemeSeedColor
import com.lunarlog.ui.theme.Spacing
import com.lunarlog.ui.theme.ThemeSwatches
import com.lunarlog.ui.theme.bestContentColor
import com.lunarlog.ui.theme.shimmerEffect
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MINUTES_PER_DAY = 24L * 60L

/** Label for the snackbar action that deep-links to this app's system settings page. */
private const val SYSTEM_SETTINGS_ACTION = "Settings"

/**
 * Renders minutes-since-midnight, following the device's 12/24-hour setting.
 *
 * [is24Hour] has to be passed in from `DateFormat.is24HourFormat(context)` — the same source
 * [LunarLogTimePickerDialog] picks its dial from, and the same one `rememberEntryTimeFormatter`
 * uses. This used `DateTimeFormatter.ofLocalizedTime(SHORT)`, which resolves from the JVM locale
 * alone and cannot see Android's "Use 24-hour format" toggle, so on any device where the two
 * disagree the label read "8:00 PM" while the picker it sat next to opened on a 24-hour dial at
 * 20:00.
 */
private fun formatMinutes(minutes: Long, is24Hour: Boolean): String {
    val clamped = minutes.coerceIn(0L, MINUTES_PER_DAY - 1L)
    return LocalTime.of((clamped / 60L).toInt(), (clamped % 60L).toInt())
        .format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault()))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    isUpdateAvailable: Boolean = false,
    onInstallUpdate: () -> Unit = {},
    onCheckUpdates: () -> Unit = {}
) {
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val themeSeedColor by viewModel.themeSeedColor.collectAsState()
    val periodReminderEnabled by viewModel.periodReminderEnabled.collectAsState()
    val periodReminderTimeMinutes by viewModel.periodReminderTimeMinutes.collectAsState()
    val cycleNotificationEnabled by viewModel.cycleNotificationEnabled.collectAsState()
    val appLockTimeoutSeconds by viewModel.appLockTimeoutSeconds.collectAsState()
    val redactWidgets by viewModel.redactWidgets.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val message by viewModel.message.collectAsState()
    val isLoaded by viewModel.isLoaded.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val context = LocalContext.current
    // Read once for the whole screen: every reminder time rendered here has to agree with the dial
    // LunarLogTimePickerDialog opens, which reads the same setting.
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showNukeDialog by remember { mutableStateOf(false) }
    var showAddMedicationDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var medicationPendingDelete by remember { mutableStateOf<Medication?>(null) }
    var medicationPendingNotificationPermission by rememberSaveable { mutableStateOf<String?>(null) }
    var editingMedicationId by rememberSaveable { mutableStateOf<Int?>(null) }
    val medicationSaving by viewModel.medicationSaving.collectAsState()
    val medicationSaved by viewModel.medicationSaved.collectAsState()
    val medicationError by viewModel.medicationError.collectAsState()
    LaunchedEffect(medicationSaved) {
        if (medicationSaved) { showAddMedicationDialog = false; editingMedicationId = null; viewModel.resetMedicationSave() }
    }
    val restorePreview by viewModel.restorePreview.collectAsState()
    val hasRecovery by viewModel.hasRecoveryBackup.collectAsState()
    val recoveryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportRecovery) }
    var manageSymptoms by rememberSaveable { mutableStateOf(false) }
    val managedSymptoms by viewModel.managedSymptoms.collectAsState()
    if (manageSymptoms) CustomSymptomManager(managedSymptoms, viewModel::renameSymptom, viewModel::archiveSymptom, { manageSymptoms = false })
    var notificationsBlocked by remember { mutableStateOf(false) }

    val privacyPolicyFailedMessage = stringResource(id = R.string.privacy_policy_open_failed)
    val privacyPolicyUrl = stringResource(id = R.string.privacy_policy_url)

    fun showMessage(text: String) {
        scope.launch { snackbarHostState.showSnackbar(text) }
    }

    /**
     * A denial the user cannot undo from here, so the snackbar carries them to the one place they
     * can — a plain transient message about a state you must leave the app to fix is useless.
     */
    fun showBlockedMessage(text: String) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = text,
                actionLabel = SYSTEM_SETTINGS_ACTION,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                val intent = Intent(
                    AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            }
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportData(it) }
    }

    // Import Launcher. Restoring clears every table first, so it goes through a confirmation the
    // same way the other destructive actions in the app do.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let(viewModel::importData)
    }

    // Biometric Logic for enabling
    fun checkBiometric(onSuccess: () -> Unit) {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(context as FragmentActivity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        showMessage("Authentication error: $errString")
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm Identity")
                .setSubtitle("Authenticate to enable App Lock")
                .setAllowedAuthenticators(authenticators)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            showMessage("Biometric hardware not available or set up.")
        }
    }

    fun formatLockTimeout(seconds: Long): String {
        return when (seconds) {
            0L -> "Immediately"
            30L -> "After 30 seconds"
            120L -> "After 2 minutes"
            else -> "Immediately"
        }
    }

    // Notification permission (Android 13+). Granting needs no confirmation — the Switch settling in
    // its new position is the confirmation — but a denial silently un-flips the Switch, so that one
    // is worth saying out loud.
    val reminderNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setPeriodReminderEnabled(granted)
        notificationsBlocked = !granted
        if (!granted) {
            showBlockedMessage("Notifications are blocked, so reminders can't be sent.")
        }
    }

    val cycleNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setCycleNotificationEnabled(granted)
        notificationsBlocked = !granted
        if (!granted) {
            showBlockedMessage("Notifications are blocked, so cycle alerts can't be sent.")
        }
    }

    val medicationNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        medicationPendingNotificationPermission?.let { json ->
            val draft = com.google.gson.Gson().fromJson(json, Medication::class.java)
            viewModel.saveMedication(draft.copy(reminderTime = draft.reminderTime.takeIf { granted }, reminderTimes = draft.reminderTimes.takeIf { granted }.orEmpty()))
            notificationsBlocked = !granted
            if (!granted) showBlockedMessage("Notifications are blocked; the medication will be saved without a reminder.")
        }
        medicationPendingNotificationPermission = null
    }

    fun openPrivacyPolicy() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            privacyPolicyUrl.toUri()
        )
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            showMessage(privacyPolicyFailedMessage)
        }
    }

    if (showReminderTimePicker) {
        val current = periodReminderTimeMinutes.coerceIn(0L, MINUTES_PER_DAY - 1L)
        LunarLogTimePickerDialog(
            title = stringResource(id = R.string.settings_reminder_time),
            initialHour = (current / 60L).toInt(),
            initialMinute = (current % 60L).toInt(),
            onDismiss = { showReminderTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.setPeriodReminderTimeMinutes(hour * 60L + minute)
                showReminderTimePicker = false
            }
        )
    }

    if (showNukeDialog) {
        AlertDialog(
            onDismissRequest = { showNukeDialog = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_delete_all_data_ec409c)) },
            text = { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_this_cannot_be_undone_all_logs_and_cycles_will_be_erase_519023)) },
            confirmButton = {
                // Matches the other three destructive confirms in the app: a TextButton in `error`.
                TextButton(
                    onClick = {
                        viewModel.nukeData()
                        showNukeDialog = false
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_delete_forever_704373), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNukeDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_cancel_19766e))
                }
            }
        )
    }

    restorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { if (!isRestoring) viewModel.cancelRestore() },
            title = { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_restore_this_backup_d3357d)) },
            text = { Text("$preview\n\nRestoring replaces the current records. A recovery backup will be saved privately on this device until your next restore or reset. Backups contain readable health data; store exported files securely.") },
            confirmButton = {
                TextButton(
                    enabled = !isRestoring,
                    onClick = {
                        viewModel.confirmRestore()
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_restore_a76e13), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(enabled = !isRestoring, onClick = { viewModel.cancelRestore() }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_cancel_19766e)) }
            }
        )
    }

    if (showAddMedicationDialog && (editingMedicationId == null || medications.any { it.id == editingMedicationId })) {
        MedicationEditor(initial = medications.firstOrNull { it.id == editingMedicationId }, isSaving = medicationSaving, error = medicationError,
            onDismiss = { showAddMedicationDialog = false; editingMedicationId = null; viewModel.resetMedicationSave() },
            onSave = { medication ->
                if (medication.reminderTime != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    medicationPendingNotificationPermission = com.google.gson.Gson().toJson(medication)
                    medicationNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else viewModel.saveMedication(medication)
            })
    }

    medicationPendingDelete?.let { medication ->
        AlertDialog(
            onDismissRequest = { medicationPendingDelete = null },
            title = { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_delete_medication_249f69)) },
            text = { Text("This also removes the dose history for ${medication.name}.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMedication(medication.id)
                        medicationPendingDelete = null
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_delete_e2d0a5), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { medicationPendingDelete = null }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_cancel_19766e)) }
            }
        )
    }

    Scaffold(
        topBar = {
            LunarLogTopAppBar(
                title = "Settings",
                onNavigateBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.screenVertical
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)
        ) {
            // An early `return@Column` here used to skip the composer's endGroup calls for
            // this lambda, which corrupts the group stack and throws deep inside Compose's
            // own runtime (ArrayIndexOutOfBoundsException in IntStack.peek2) the moment the
            // screen is composed. A real branch is the only safe shape.
            if (!isLoaded) {
                SettingsSkeleton()
            } else {

                SettingsSection(title = "Security") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_app_lock_5eb400))
                            Text(
                                androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_require_authentication_when_returning_to_the_app_600021),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            modifier = Modifier.semantics { contentDescription = "App Lock" },
                            checked = isAppLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    checkBiometric { viewModel.toggleAppLock(true) }
                                } else {
                                    viewModel.toggleAppLock(false)
                                }
                            }
                        )
                    }
                    if (isAppLockEnabled) {
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_lock_timeout_e38013), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatLockTimeout(appLockTimeoutSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // No Spacer: the chips' 48dp touch target already contributes 8dp of clear
                            // space above the 32dp pill.
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                listOf(0L to "Now", 30L to "30s", 120L to "2m").forEach { (seconds, label) ->
                                    FilterChip(
                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                        selected = appLockTimeoutSeconds == seconds,
                                        onClick = { viewModel.setAppLockTimeoutSeconds(seconds) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }

                SettingsSection(title = stringResource(id = R.string.settings_notifications)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_cycle_prediction_alerts_96dc07))
                            Text(
                                androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_notify_about_upcoming_period_and_estimated_fertile_day__ff901b),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = cycleNotificationEnabled,
                            onCheckedChange = { enabled ->
                                if (!enabled) {
                                    viewModel.setCycleNotificationEnabled(false)
                                    return@Switch
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        notificationsBlocked = false
                                        viewModel.setCycleNotificationEnabled(true)
                                    } else {
                                        cycleNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    viewModel.setCycleNotificationEnabled(true)
                                }
                            }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.settings_daily_period_log_reminder))
                            Text(
                                stringResource(id = R.string.settings_daily_period_log_reminder_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            modifier = Modifier.semantics {
                                contentDescription = "Daily period log reminder"
                            },
                            checked = periodReminderEnabled,
                            onCheckedChange = { enabled ->
                                if (!enabled) {
                                    viewModel.setPeriodReminderEnabled(false)
                                    return@Switch
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        notificationsBlocked = false
                                        viewModel.setPeriodReminderEnabled(true)
                                    } else {
                                        reminderNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    viewModel.setPeriodReminderEnabled(true)
                                }
                            }
                        )
                    }

                    // A blocked permission is a state, not an event: it outlives any transient message,
                    // so it stays on screen until the user actually changes it.
                    if (notificationsBlocked) {
                        Text(
                            androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_notifications_are_blocked_enable_them_in_system_setting_c7cb64),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { showReminderTimePicker = true }
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.settings_reminder_time))
                            Text(
                                formatMinutes(periodReminderTimeMinutes, is24Hour),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            stringResource(id = R.string.settings_change),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                SettingsSection(title = "Medications") {
                    if (medications.isEmpty()) {
                        Text(
                            androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_no_medications_added_a5d701),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        medications.forEach { medication ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(medication.name, style = MaterialTheme.typography.titleMedium)
                                    if (medication.isArchived) Text("Archived", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val schedule = medication.frequency
                                        .replace('_', ' ')
                                        .replaceFirstChar { it.uppercase() }
                                    val times = com.lunarlog.logic.MedicationScheduler.reminderTimes(medication)
                                    val reminder = if (times.isEmpty()) "" else " at " + times.joinToString(", ") { formatMinutes(it, is24Hour) }
                                    Text(
                                        listOfNotNull(
                                            medication.dosage.takeIf { it.isNotBlank() },
                                            if (medication.frequency == "as_needed") schedule else "$schedule • ${medication.dosesPerDay} planned doses$reminder"
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { editingMedicationId = medication.id; viewModel.resetMedicationSave(); showAddMedicationDialog = true }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_edit_464c4f)) }
                                        TextButton(onClick = { viewModel.setMedicationArchived(medication, !medication.isArchived) }) { Text(if (medication.isArchived) "Resume" else "Archive") }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                                IconButton(onClick = { medicationPendingDelete = medication }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete ${medication.name}",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { editingMedicationId = null; viewModel.resetMedicationSave(); showAddMedicationDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_add_medication_65629f))
                    }
                }

                SettingsSection(title = "Appearance") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ColorLens, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_theme_color_d543a8))
                    }

                    // SpaceBetween, not spacedBy: six 48dp swatches with 12dp gaps come to 348dp,
                    // one dp wider than the card's interior on a 411dp phone, so the last swatch
                    // used to drop onto a line of its own. Distributing the slack instead lands the
                    // same ~12dp gaps and can't orphan one.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        ThemeSwatches.forEach { (colorName, colorLong) ->
                            ThemeSwatch(
                                name = colorName,
                                colorLong = colorLong,
                                isSelected = themeSeedColor == colorLong ||
                                    (themeSeedColor == null && colorLong == DefaultThemeSeedColor),
                                onClick = { viewModel.setThemeSeedColor(colorLong) }
                            )
                        }
                    }
                }

                if (isUpdateAvailable) {
                    SettingsSection(title = "Update Available") {
                        Text(
                            androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_a_new_version_of_lunarlog_is_available_2c1f05),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onInstallUpdate,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_install_update_7e08c5))
                        }
                    }
                }

                SettingsSection(title = "Custom symptoms and moods") {
                    TextButton(onClick = { manageSymptoms = true }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_manage_custom_choices_52cfcc)) }
                }
                SettingsSection(title = "Widget privacy") {
                    Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_widgets_can_display_health_information_and_allow_quick__824be5))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_hide_widget_details_c1783e), Modifier.weight(1f))
                        Switch(checked = redactWidgets, onCheckedChange = viewModel::setRedactWidgets)
                    }
                }
                SettingsSection(title = "Data Management") {
                    if (hasRecovery) TextButton(onClick = { recoveryLauncher.launch("lunarlog_before_restore.json") }, enabled = !isRestoring) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_save_previous_data_recovery_backup_04cd66)) }
                    SettingsActionRow(
                        icon = Icons.Filled.Download,
                        title = "Back up data",
                        enabled = !isRestoring,
                        subtitle = "Save all records to a readable JSON file. Keep it somewhere private.",
                        onClick = {
                            exportLauncher.launch("lunarlog_backup_${System.currentTimeMillis()}.json")
                        }
                    )
                    SettingsActionRow(
                        icon = Icons.Filled.Upload,
                        title = "Restore backup",
                        subtitle = "Replaces everything currently in the app.",
                        enabled = !isRestoring,
                        onClick = { importLauncher.launch(arrayOf("application/json")) }
                    )
                    // Rewriting the database from a backup takes seconds on a large file, with nothing
                    // else on screen to say the app is busy.
                    if (isRestoring) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    // Outlined rather than filled: it already has a confirmation dialog, so it does not
                    // need to be the loudest thing on the screen as well.
                    OutlinedButton(
                        onClick = { showNukeDialog = true },
                        enabled = !isRestoring,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_delete_all_data_a867ee))
                    }
                }

                SettingsSection(title = "About") {
                    TextButton(onClick = onCheckUpdates) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_check_for_updates_f26f32)) }
                    Column {
                        Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_lunarlog_4b65cc), style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_a_privacy_first_period_tracker_43bae8),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_lunarlog_is_not_a_medical_device_and_does_not_diagnose__0f0567),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { openPrivacyPolicy() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(id = R.string.settings_privacy_policy))
                    }
                }
            }
        }
    }
}

/**
 * Header plus card, the shape every settings group takes.
 *
 * The rhythm — 8dp under the header (owned by [SectionHeader]), 16dp between rows inside the card,
 * 32dp between sections (owned by the caller's `Arrangement`) — is decided here once instead of being
 * re-typed at seven call sites.
 */
@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title)
        LunarLogCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                content = content
            )
        }
    }
}

/** A tappable icon + label + supporting-text row, for actions that live inside a settings card. */
@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .alpha(contentAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(Spacing.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * One theme colour circle.
 *
 * Every swatch carries a border so the pale options have an edge against the card; selection
 * thickens and darkens it rather than adding one from nothing, and the check mark scales in — picking
 * a colour is the one purely aesthetic action in the app and its feedback should not be the least
 * animated thing on screen.
 */
@Composable
private fun ThemeSwatch(
    name: String,
    colorLong: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color(colorLong)
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = tween(durationMillis = 180),
        label = "swatchBorderWidth"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(durationMillis = 180),
        label = "swatchBorderColor"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(borderWidth, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "$name theme color"
                role = Role.Button
                selected = isSelected
            }
    ) {
        AnimatedVisibility(
            visible = isSelected,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                // Same contrast helper the calendar uses, rather than a second, cruder luminance cut.
                tint = bestContentColor(color)
            )
        }
    }
}

/**
 * Placeholder in the shape of the real content, shown for the frame or two before the stored
 * settings arrive. Same section rhythm as the live screen, so nothing shifts when it swaps.
 */
@Composable
private fun SettingsSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)
    ) {
        repeat(3) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .padding(bottom = Spacing.sm)
                        .size(width = 120.dp, height = 20.dp)
                        .clip(MaterialTheme.shapes.small)
                        .shimmerEffect()
                )
                LunarLogCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .shimmerEffect()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The app's time picker: M3's [TimePicker] in a dialog container.
 *
 * Replaces the platform `TimePickerDialog`, which was a second, differently-themed window stacked on
 * top of the Compose UI, and which hardcoded a 12-hour clock. `is24HourFormat` means the dial now
 * matches the device setting — and matches the localised label the picked time is rendered into.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LunarLogTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = DateFormat.is24HourFormat(context)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_ok_565339)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_cancel_19766e)) }
        }
    )
}
