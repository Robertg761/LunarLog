package com.lunarlog.ui.settings

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.lunarlog.BuildConfig
import com.lunarlog.R
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    isUpdateAvailable: Boolean = false,
    onInstallUpdate: () -> Unit = {}
) {
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val themeSeedColor by viewModel.themeSeedColor.collectAsState()
    val periodReminderEnabled by viewModel.periodReminderEnabled.collectAsState()
    val periodReminderTimeMinutes by viewModel.periodReminderTimeMinutes.collectAsState()
    val cycleNotificationEnabled by viewModel.cycleNotificationEnabled.collectAsState()
    val appLockTimeoutSeconds by viewModel.appLockTimeoutSeconds.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current
    var showNukeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
    }

    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportData(it) }
    }

    // Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importData(it) }
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
                         Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                     }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm Identity")
                .setSubtitle("Authenticate to enable App Lock")
                .setAllowedAuthenticators(authenticators)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
             Toast.makeText(context, "Biometric hardware not available or set up.", Toast.LENGTH_LONG).show()
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

    // Notification permission (Android 13+)
    val reminderNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setPeriodReminderEnabled(true)
            Toast.makeText(context, "Daily reminder enabled.", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setPeriodReminderEnabled(false)
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    val cycleNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setCycleNotificationEnabled(true)
            Toast.makeText(context, "Cycle alerts enabled.", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setCycleNotificationEnabled(false)
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    fun formatMinutes(minutes: Long): String {
        val m = minutes.coerceIn(0L, (24L * 60L) - 1L)
        val hour24 = (m / 60L).toInt()
        val min = (m % 60L).toInt()
        val hour12 = ((hour24 + 11) % 12) + 1
        val ampm = if (hour24 < 12) "AM" else "PM"
        return String.format(Locale.US, "%d:%02d %s", hour12, min, ampm)
    }

    fun showTimePicker() {
        val currentMinutes = periodReminderTimeMinutes.coerceIn(0L, (24L * 60L) - 1L)
        val currentHour = (currentMinutes / 60L).toInt()
        val currentMinute = (currentMinutes % 60L).toInt()

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newMinutes = (hourOfDay * 60L) + minute.toLong()
                viewModel.setPeriodReminderTimeMinutes(newMinutes)
                Toast.makeText(context, "Reminder time set to ${formatMinutes(newMinutes)}", Toast.LENGTH_SHORT).show()
            },
            currentHour,
            currentMinute,
            false
        ).show()
    }

    fun openPrivacyPolicy() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(context.getString(R.string.privacy_policy_url))
        )
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, R.string.privacy_policy_open_failed, Toast.LENGTH_LONG).show()
        }
    }

    if (showNukeDialog) {
        AlertDialog(
            onDismissRequest = { showNukeDialog = false },
            title = { Text("Delete All Data?") },
            text = { Text("This cannot be undone. All logs and cycles will be erased forever.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.nukeData()
                        showNukeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNukeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Security Section
            Text(
                "Security",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "App Lock Icon")
                        Spacer(modifier = Modifier.padding(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Lock")
                            Text(
                                "Require authentication when returning to the app",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Lock timeout",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            formatLockTimeout(appLockTimeoutSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0L to "Now", 30L to "30s", 120L to "2m").forEach { (seconds, label) ->
                                FilterChip(
                                    selected = appLockTimeoutSeconds == seconds,
                                    onClick = { viewModel.setAppLockTimeoutSeconds(seconds) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Notifications
            Text(
                stringResource(id = R.string.settings_notifications),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cycle prediction alerts")
                            Text(
                                "Notify about upcoming period and fertile window updates",
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
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.settings_daily_period_log_reminder))
                            Text(
                                stringResource(id = R.string.settings_daily_period_log_reminder_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
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
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.settings_reminder_time))
                            Text(
                                formatMinutes(periodReminderTimeMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(stringResource(id = R.string.settings_change), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Appearance
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ColorLens, contentDescription = "Theme Color Icon")
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text("Theme Color")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val colors = listOf(
                        0xFFFFB2DD, // Blossom (Default)
                        0xFFE1BEE7, // Lavender
                        0xFFFFCCBC, // Peach
                        0xFFB2DFDB, // Teal
                        0xFFBBDEFB, // Blue
                        0xFFC5E1A5  // Green
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { colorLong ->
                            val color = Color(colorLong)
                            val isSelected = themeSeedColor == colorLong || (themeSeedColor == null && colorLong == 0xFFFFB2DD.toLong())
                            
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { viewModel.setThemeSeedColor(colorLong) }
                                    .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isUpdateAvailable) {
                Text(
                    "Update Available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "A new version of LunarLog is available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onInstallUpdate,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text("Install Update")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Data Management
            Text(
                "Data Management",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { exportLauncher.launch("lunarlog_backup_${System.currentTimeMillis()}.json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Download, "Download Icon")
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Backup Data")
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Upload, "Upload Icon")
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Restore Backup")
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { showNukeDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.DeleteForever, "Delete Icon")
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Nuke Data (Factory Reset)")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // About Section
            Text(
                "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LunarLog", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "A privacy-first period tracker.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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
