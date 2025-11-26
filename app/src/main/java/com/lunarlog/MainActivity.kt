package com.lunarlog

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.lunarlog.ui.navigation.LunarLogNavGraph
import com.lunarlog.ui.theme.LunarLogTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lunarlog.workers.CycleNotificationWorker
import java.util.concurrent.TimeUnit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

import androidx.activity.enableEdgeToEdge

import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.objects.Update
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.remember

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // App Updater (Silent Check)
        val appUpdaterUtils = AppUpdaterUtils(this)
            .setUpdateFrom(UpdateFrom.GITHUB)
            .setGitHubUserAndRepo("Robertg761", "Period-Tracker")
            .withListener(object : AppUpdaterUtils.UpdateListener {
                override fun onSuccess(update: Update?, isUpdateAvailable: Boolean?) {
                    if (isUpdateAvailable == true) {
                        viewModel.setUpdateAvailable(true)
                    }
                }
                override fun onFailed(error: AppUpdaterError?) {
                    // Log error if needed
                }
            })
        appUpdaterUtils.start()
        
        // Keep splash screen until data is loaded
        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value.isLoading
        }

        scheduleNotificationWorker()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val isLocked by viewModel.isLocked.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            // Handle Install Trigger
            LaunchedEffect(Unit) {
                viewModel.installUpdateTrigger.collect {
                    AppUpdater(this@MainActivity)
                        .setUpdateFrom(UpdateFrom.GITHUB)
                        .setGitHubUserAndRepo("Robertg761", "Period-Tracker")
                        .setDisplay(Display.DIALOG)
                        .showAppUpdated(false)
                        .start()
                }
            }
            
            // Show Snackbar on Update
            LaunchedEffect(uiState.isUpdateAvailable) {
                if (uiState.isUpdateAvailable) {
                    val result = snackbarHostState.showSnackbar(
                        message = "New update available",
                        actionLabel = "Install",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.triggerInstallUpdate()
                    }
                }
            }

            LunarLogTheme(
                seedColor = uiState.themeSeedColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!uiState.isLoading) {
                        if (uiState.isAppLockEnabled && isLocked) {
                            // Show Lock Screen / Prompt
                            LockScreenContent(
                                onUnlock = { authenticateUser() }
                            )
                            // Auto-trigger auth on first show
                            LaunchedEffect(Unit) {
                                authenticateUser()
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                LunarLogNavGraph(
                                    startDestination = uiState.startDestination,
                                    isUpdateAvailable = uiState.isUpdateAvailable,
                                    onInstallUpdate = { viewModel.triggerInstallUpdate() }
                                )
                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp) // Avoid covering nav bar
                                )
                            }
                        }
                    } else {
                         // Fallback, though splash screen should cover this
                         Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    private fun authenticateUser() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(this)
            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        viewModel.unlock()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                         // Allow retry via button if cancelled or error
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock LunarLog")
                .setSubtitle("Confirm your identity to access your health data")
                .setNegativeButtonText("Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            // Fallback if hardware not available, just unlock (or ask for PIN if we implemented that)
            // For now, if no bio hardware, we unlock to avoid lockout
             viewModel.unlock()
        }
    }

    private fun scheduleNotificationWorker() {
        val workRequest = PeriodicWorkRequestBuilder<CycleNotificationWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CycleNotificationWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}

@androidx.compose.runtime.Composable
fun LockScreenContent(onUnlock: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Icon(
                 imageVector = Icons.Filled.Lock,
                 contentDescription = "Locked",
                 modifier = Modifier.size(64.dp),
                 tint = MaterialTheme.colorScheme.primary
             )
             Spacer(modifier = Modifier.height(16.dp))
             Text("LunarLog is Locked", style = MaterialTheme.typography.headlineMedium)
             Spacer(modifier = Modifier.height(32.dp))
             Button(onClick = onUnlock) {
                 Text("Unlock")
             }
        }
    }
}
