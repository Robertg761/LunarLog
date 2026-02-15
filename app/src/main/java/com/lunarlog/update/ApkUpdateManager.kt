package com.lunarlog.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
    }

    fun buildUnknownSourcesSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun startDownload(context: Context, info: UpdateInfo): Long {
        val fileName = "LunarLog-${info.latestVersionName}.apk"
        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
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
        prefs(context).edit()
            .putLong(KEY_DOWNLOAD_ID, downloadId)
            .putString(KEY_APK_PATH, file.absolutePath)
            .apply()

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

    fun getDownloadedApkFile(context: Context): File? {
        val apkPath = prefs(context).getString(KEY_APK_PATH, null) ?: return null
        return File(apkPath)
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
    }
}
