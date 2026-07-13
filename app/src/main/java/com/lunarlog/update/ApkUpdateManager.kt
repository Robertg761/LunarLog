package com.lunarlog.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.core.content.edit
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

class ApkUpdateManager(
    private val prefsName: String = "lunarlog_updater"
) {
    private fun prefs(context: Context) =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    data class DownloadQueryResult(
        val status: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val reason: Int?
    )

    fun needsUnknownSourcesPermission(context: Context): Boolean {
        return !context.packageManager.canRequestPackageInstalls()
    }

    fun buildUnknownSourcesSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun startDownload(context: Context, info: UpdateInfo): Long {
        val fileName = "LunarLog-${info.latestVersionName}.apk"
        val request = DownloadManager.Request(info.apkUrl.toUri())
            .setTitle("LunarLog update")
            .setDescription("Downloading LunarLog ${info.latestVersionName}")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        prefs(context).edit {
            putLong(KEY_DOWNLOAD_ID, downloadId)
            putString(KEY_APK_PATH, file.absolutePath)
            putString(KEY_DOWNLOADED_VERSION_NAME, normalizeVersionName(info.latestVersionName))
        }

        return downloadId
    }

    fun queryDownload(context: Context): DownloadQueryResult? {
        val downloadId = prefs(context).getLong(KEY_DOWNLOAD_ID, -1L)
        if (downloadId <= 0) return null

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
        cursor.use {
            if (!it.moveToFirst()) return null
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val bytesDownloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val totalBytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val reason = try {
                it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            } catch (_: Exception) {
                null
            }
            return DownloadQueryResult(
                status = status,
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                reason = reason
            )
        }
    }

    fun hasDownloadedApk(context: Context): Boolean {
        val apkFile = getDownloadedApkFile(context) ?: return false
        val q = queryDownload(context) ?: return false
        return q.status == DownloadManager.STATUS_SUCCESSFUL && apkFile.exists()
    }

    fun hasDownloadedApkForVersion(context: Context, versionName: String): Boolean {
        if (!hasDownloadedApk(context)) return false
        val downloaded = getDownloadedVersionName(context) ?: return false
        return normalizeVersionName(downloaded) == normalizeVersionName(versionName)
    }

    fun hasPendingDownloadedUpdate(context: Context, currentVersionName: String): Boolean {
        if (!hasDownloadedApk(context)) return false

        val downloadedVersion = getDownloadedVersionName(context)
        if (downloadedVersion == null) {
            // Can't verify version; preserve existing behavior and allow install prompt.
            return true
        }

        val isPending = isNewerVersion(downloadedVersion, currentVersionName)
        if (!isPending) {
            clearDownloadedState(context, deleteApk = true)
        }
        return isPending
    }

    fun getDownloadedApkFile(context: Context): File? {
        val apkPath = prefs(context).getString(KEY_APK_PATH, null) ?: return null
        return File(apkPath)
    }

    fun getDownloadedVersionName(context: Context): String? {
        val stored = prefs(context).getString(KEY_DOWNLOADED_VERSION_NAME, null)?.trim()
        if (!stored.isNullOrEmpty()) return stored

        val fileName = getDownloadedApkFile(context)?.name ?: return null
        return extractVersionFromApkFileName(fileName)
    }

    fun clearDownloadedState(context: Context, deleteApk: Boolean = false) {
        val existingFile = getDownloadedApkFile(context)
        val existingDownloadId = prefs(context).getLong(KEY_DOWNLOAD_ID, -1L)

        if (existingDownloadId > 0) {
            runCatching {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.remove(existingDownloadId)
            }
        }

        if (deleteApk) {
            runCatching {
                if (existingFile?.exists() == true) {
                    existingFile.delete()
                }
            }
        }

        prefs(context).edit {
            remove(KEY_DOWNLOAD_ID)
            remove(KEY_APK_PATH)
            remove(KEY_DOWNLOADED_VERSION_NAME)
        }
    }

    fun buildInstallIntentFromDownloadedApk(context: Context): Intent? {
        val apkFile = getDownloadedApkFile(context) ?: return null
        if (!apkFile.exists()) return null
        if (needsUnknownSourcesPermission(context)) return null

        val authority = "${context.packageName}.fileprovider"
        val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    companion object {
        private const val KEY_DOWNLOAD_ID = "download_id"
        private const val KEY_APK_PATH = "apk_path"
        private const val KEY_DOWNLOADED_VERSION_NAME = "downloaded_version_name"

        internal fun isNewerVersion(downloadedVersionName: String, currentVersionName: String): Boolean {
            val downloaded = normalizeVersionName(downloadedVersionName)
            val current = normalizeVersionName(currentVersionName)

            val downloadedSemVer = SemVer.parseOrNull(downloaded)
            val currentSemVer = SemVer.parseOrNull(current)
            return downloadedSemVer != null && currentSemVer != null && downloadedSemVer > currentSemVer
        }

        internal fun normalizeVersionName(raw: String): String {
            return raw.trim().removePrefix("v").removePrefix("V")
        }

        internal fun extractVersionFromApkFileName(fileName: String): String? {
            val prefix = "LunarLog-"
            val suffix = ".apk"
            if (!fileName.startsWith(prefix) || !fileName.endsWith(suffix)) return null
            val rawVersion = fileName.substring(prefix.length, fileName.length - suffix.length)
            return rawVersion.takeIf { it.isNotBlank() }?.let { normalizeVersionName(it) }
        }
    }
}
