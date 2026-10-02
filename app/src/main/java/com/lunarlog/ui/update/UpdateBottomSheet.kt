package com.lunarlog.ui.update

import android.app.DownloadManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lunarlog.ui.components.CardDivider
import com.lunarlog.ui.theme.Spacing
import com.lunarlog.update.ApkUpdateManager
import com.lunarlog.update.UpdateInfo
import kotlinx.coroutines.delay

private enum class UpdateStage {
    Available,
    Downloading,
    PermissionRequired,
    ReadyToInstall,
    Error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBottomSheet(
    info: UpdateInfo,
    apkUpdateManager: ApkUpdateManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scroll = rememberScrollState()

    var stage by remember { mutableStateOf(UpdateStage.Available) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var progressText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var notesExpanded by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun recomputeStage() {
        val q = apkUpdateManager.queryDownload(context)
        stage = when {
            apkUpdateManager.hasDownloadedApkForVersion(context, info.latestVersionName) -> {
                if (apkUpdateManager.needsUnknownSourcesPermission(context)) UpdateStage.PermissionRequired else UpdateStage.ReadyToInstall
            }
            q?.status == DownloadManager.STATUS_RUNNING ||
                q?.status == DownloadManager.STATUS_PENDING ||
                q?.status == DownloadManager.STATUS_PAUSED -> UpdateStage.Downloading
            q?.status == DownloadManager.STATUS_FAILED -> UpdateStage.Error
            else -> UpdateStage.Available
        }
    }

    LaunchedEffect(info.latestVersionName) {
        recomputeStage()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                recomputeStage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(stage) {
        if (stage != UpdateStage.Downloading) return@LaunchedEffect
        while (stage == UpdateStage.Downloading) {
            val q = apkUpdateManager.queryDownload(context)
            if (q == null) {
                progress = null
                progressText = null
                stage = UpdateStage.Available
                break
            }

            when (q.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    progress = 1f
                    progressText = "Download complete"
                    stage = if (apkUpdateManager.needsUnknownSourcesPermission(context)) {
                        UpdateStage.PermissionRequired
                    } else {
                        UpdateStage.ReadyToInstall
                    }
                    break
                }
                DownloadManager.STATUS_FAILED -> {
                    errorText = "Download failed. Please try again."
                    stage = UpdateStage.Error
                    break
                }
                else -> {
                    val total = q.totalBytes
                    val soFar = q.bytesDownloaded
                    progress = if (total > 0) (soFar.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null
                    progressText = if (total > 0) {
                        val pct = ((soFar * 100) / total).coerceIn(0, 100)
                        "Downloading... $pct%"
                    } else {
                        "Downloading..."
                    }
                }
            }
            delay(750)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Sheets take the same warm `surfaceContainer` as LunarLogCard rather than
        // BottomSheetDefaults' `surfaceContainerLow`, so a sheet reads as the same material
        // as the cards it slides over instead of a second, paler one.
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = Spacing.sheetHorizontal)
                .padding(bottom = Spacing.sheetHorizontal)
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_update_lunarlog_cad03b),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "New version: ${info.latestVersionName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (info.releaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                CardDivider()
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_what_s_new_135e91), style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { notesExpanded = !notesExpanded }) {
                        Text(if (notesExpanded) "Hide" else "Show")
                    }
                }
                Text(
                    text = info.releaseNotes.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (notesExpanded) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            when (stage) {
                UpdateStage.Available -> {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_android_will_show_an_install_prompt_this_update_is_down_d6658c),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_not_now_a0e63d)) }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Button(onClick = {
                            errorText = null
                            progress = null
                            progressText = null
                            try { apkUpdateManager.startDownload(context, info); stage = UpdateStage.Downloading }
                            catch (e: Exception) { errorText = "Unable to start download: ${e.message}"; stage = UpdateStage.Error }
                        }) {
                            Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_download_d6eafe))
                        }
                    }
                }

                UpdateStage.Downloading -> {
                    Text(
                        text = progressText ?: "Downloading...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    if (progress != null) {
                        LinearProgressIndicator(progress = { progress!! }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = {
                            apkUpdateManager.clearDownloadedState(context, deleteApk = true)
                            stage = UpdateStage.Available; progress = null; progressText = null
                        }) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_cancel_download_de5b23)) }
                    }
                }

                UpdateStage.PermissionRequired -> {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_to_install_this_update_android_needs_you_to_allow_insta_1887c3),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_tap_open_settings_enable_allow_from_this_source_then_co_605ff5),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_later_73b6e4)) }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Button(onClick = {
                            try { context.startActivity(apkUpdateManager.buildUnknownSourcesSettingsIntent(context)) } catch (e: Exception) { errorText = "Unable to open install settings"; stage = UpdateStage.Error }
                        }) {
                            Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_open_settings_ca381c))
                        }
                    }
                }

                UpdateStage.ReadyToInstall -> {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_ready_to_install_android_will_show_an_install_prompt_444678),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_close_7d9eb7)) }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Button(onClick = {
                            if (apkUpdateManager.needsUnknownSourcesPermission(context)) {
                                stage = UpdateStage.PermissionRequired
                                return@Button
                            }
                            val intent = apkUpdateManager.buildInstallIntentFromDownloadedApk(context)
                            if (intent == null) {
                                errorText = "Couldn't start installer. Please re-download the update."
                                stage = UpdateStage.Error
                                return@Button
                            }
                            try { context.startActivity(intent) } catch (e: Exception) { errorText = "Unable to open installer: ${e.message}"; stage = UpdateStage.Error }
                        }) {
                            Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_install_569ca4))
                        }
                    }
                }

                UpdateStage.Error -> {
                    Text(
                        text = errorText ?: "Something went wrong.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_close_7d9eb7)) }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Button(onClick = {
                            errorText = null
                            progress = null
                            progressText = null
                            try { apkUpdateManager.startDownload(context, info); stage = UpdateStage.Downloading }
                            catch (e: Exception) { errorText = "Unable to start download: ${e.message}"; stage = UpdateStage.Error }
                        }) {
                            Text(androidx.compose.ui.res.stringResource(com.lunarlog.R.string.ui_try_again_d8b839))
                        }
                    }
                }
            }
        }
    }
}
