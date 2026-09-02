package com.lunarlog

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.lunarlog.ui.navigation.LunarLogNavGraph
import com.lunarlog.ui.theme.LunarLogTheme
import dagger.hilt.android.AndroidEntryPoint
import com.lunarlog.workers.NotificationWorkScheduler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.remember
import com.lunarlog.update.ApkUpdateManager
import com.lunarlog.ui.update.UpdateBottomSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val apkUpdateManager = ApkUpdateManager()
    private val pendingDeepLink = MutableStateFlow<String?>(null)

    /**
     * Set when [authenticateUser] cannot start a prompt. Surfaced through the shared
     * [SnackbarHostState] rather than a Toast so the lock screen offers a way out.
     */
    private val authUnavailableMessage = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        pendingDeepLink.value = intent.lunarLogDeepLinkOrNull()

        if (BuildConfig.ENABLE_GITHUB_UPDATES) {
            // Silent update check for sideloaded GitHub builds.
            viewModel.checkForUpdates()
        }

        // Keep splash screen until data is loaded
        splashScreen.setKeepOnScreenCondition {
            viewModel.uiState.value.isLoading || !viewModel.isLockStateReady.value
        }

        scheduleNotificationWorker()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val isLocked by viewModel.isLocked.collectAsState()
            val isLockStateReady by viewModel.isLockStateReady.collectAsState()
            val deepLink by pendingDeepLink.collectAsState()
            val authError by authUnavailableMessage.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val updateSheetInfo = remember { androidx.compose.runtime.mutableStateOf<com.lunarlog.update.UpdateInfo?>(null) }
            val promptedDownloaded = remember { androidx.compose.runtime.mutableStateOf(false) }

            // Handle Install Trigger (opens the guided in-app updater UI).
            LaunchedEffect(Unit) {
                viewModel.installUpdateTrigger.collect { info ->
                    updateSheetInfo.value = info
                }
            }

            // If an update was downloaded previously, offer install without surprise navigation.
            LaunchedEffect(Unit) {
                if (!BuildConfig.ENABLE_GITHUB_UPDATES) return@LaunchedEffect
                if (promptedDownloaded.value) return@LaunchedEffect
                if (!apkUpdateManager.hasPendingDownloadedUpdate(this@MainActivity, BuildConfig.VERSION_NAME)) {
                    return@LaunchedEffect
                }
                // A file that no longer matches the release is discarded silently; the next update
                // check offers a fresh download instead of prompting to install it.
                if (!apkUpdateManager.verifyDownloadedApk(this@MainActivity)) return@LaunchedEffect
                promptedDownloaded.value = true

                val needsPerm = apkUpdateManager.needsUnknownSourcesPermission(this@MainActivity)
                val result = snackbarHostState.showSnackbar(
                    message = if (needsPerm) "Update downloaded. Enable installs to finish." else "Update downloaded. Ready to install.",
                    actionLabel = if (needsPerm) "Enable" else "Install",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    if (needsPerm) {
                        startActivity(apkUpdateManager.buildUnknownSourcesSettingsIntent(this@MainActivity))
                    } else {
                        apkUpdateManager.buildInstallIntentFromDownloadedApk(this@MainActivity)
                            ?.let { startActivity(it) }
                    }
                }
            }
            
            // Show Snackbar on Update
            LaunchedEffect(uiState.isUpdateAvailable) {
                if (!BuildConfig.ENABLE_GITHUB_UPDATES) return@LaunchedEffect
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

            // App lock with no usable authenticator used to be an unrecoverable dead end behind a
            // Toast. The snackbar hands the user a route to device security settings.
            LaunchedEffect(authError) {
                val message = authError ?: return@LaunchedEffect
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Settings",
                    duration = SnackbarDuration.Long
                )
                authUnavailableMessage.value = null
                if (result == SnackbarResult.ActionPerformed) {
                    runCatching { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
                }
            }

            LunarLogTheme(
                seedColor = uiState.themeSeedColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!uiState.isLoading && isLockStateReady) {
                        val showLockScreen = uiState.isAppLockEnabled && isLocked
                        Crossfade(
                            targetState = showLockScreen,
                            modifier = Modifier.fillMaxSize(),
                            // Fades on unlock; snaps on lock. A crossfade composes and draws BOTH
                            // branches at once, and the re-lock decision is made in onResume() —
                            // after the window is already showing content — so fading into the lock
                            // screen would leave the user's cycle data legible underneath it for
                            // the length of the animation. FLAG_SECURE does not help here: it stops
                            // screenshots and recents thumbnails, not what is on the display.
                            // Locking is the direction where the guarantee matters, so it is instant.
                            animationSpec = if (showLockScreen) snap() else tween(280),
                            label = "lockState"
                        ) { locked ->
                            if (locked) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LockScreenContent(
                                        onUnlock = { authenticateUser() }
                                    )
                                    // The nav graph's Scaffold is not composed while locked, so the
                                    // lock screen hosts the shared snackbar itself — inset-aware
                                    // rather than offset by a hardcoded guess.
                                    SnackbarHost(
                                        hostState = snackbarHostState,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .windowInsetsPadding(WindowInsets.safeDrawing)
                                    )
                                }
                                // Auto-trigger auth on first show
                                LaunchedEffect(Unit) {
                                    authenticateUser()
                                }
                            } else {
                                LunarLogNavGraph(
                                    startDestination = uiState.startDestination,
                                    isUpdateAvailable = uiState.isUpdateAvailable,
                                    pendingDeepLink = deepLink,
                                    onDeepLinkHandled = { handled ->
                                        pendingDeepLink.value = null
                                        if (!handled) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Unable to open that LunarLog link.",
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                        }
                                    },
                                    onInstallUpdate = {
                                        if (BuildConfig.ENABLE_GITHUB_UPDATES) {
                                            viewModel.triggerInstallUpdate()
                                        }
                                    },
                                    // The graph's Scaffold positions the snackbar above the bottom
                                    // nav bar and the system inset automatically.
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        }
                    } else {
                         // Fallback, though splash screen should cover this
                         Box(Modifier.fillMaxSize())
                    }

                    val info = updateSheetInfo.value
                    if (BuildConfig.ENABLE_GITHUB_UPDATES && info != null) {
                        UpdateBottomSheet(
                            info = info,
                            apkUpdateManager = apkUpdateManager,
                            onDismiss = { updateSheetInfo.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink.value = intent.lunarLogDeepLinkOrNull()
    }

    override fun onPause() {
        viewModel.onAppBackgrounded()
        super.onPause()
    }

    private fun authenticateUser() {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
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
                .setAllowedAuthenticators(authenticators)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            authUnavailableMessage.value =
                "App Lock is enabled, but no device authentication is available."
        }
    }

    private fun scheduleNotificationWorker() {
        NotificationWorkScheduler.enqueueCycleNotificationReschedule(applicationContext)
        NotificationWorkScheduler.enqueuePeriodLogReminderReschedule(applicationContext)
    }
}

private fun Intent.lunarLogDeepLinkOrNull(): String? =
    dataString?.takeIf {
        action == Intent.ACTION_VIEW && data?.scheme.equals("lunarlog", ignoreCase = true)
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
