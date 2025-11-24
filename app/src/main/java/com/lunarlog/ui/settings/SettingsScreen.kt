package com.lunarlog.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val themeSeedColor by viewModel.themeSeedColor.collectAsState()
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
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
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
                .setNegativeButtonText("Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
             Toast.makeText(context, "Biometric hardware not available or set up.", Toast.LENGTH_LONG).show()
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
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = "App Lock Icon")
                    Spacer(modifier = Modifier.padding(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Lock")
                        Text(
                            "Require authentication on startup",
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
        }
    }
}
