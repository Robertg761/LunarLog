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
            .putBoolean(KEY_UNKNOWN_SOURCES_LAUNCHED, false)
            .apply()

        return downloadId
    }

    fun maybePromptInstallDownloadedApk(context: Context): Boolean {
        val p = prefs(context)
        val downloadId = p.getLong(KEY_DOWNLOAD_ID, -1L)
        val apkPath = p.getString(KEY_APK_PATH, null) ?: return false
        if (downloadId <= 0) return false

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
        cursor.use {
            if (!it.moveToFirst()) return false
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) return false
        }

        val apkFile = File(apkPath)
        if (!apkFile.exists()) return false

        // Unknown sources permission gate (API 26+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                // Avoid repeatedly spamming settings while we poll for completion.
                if (!p.getBoolean(KEY_UNKNOWN_SOURCES_LAUNCHED, false)) {
                    p.edit().putBoolean(KEY_UNKNOWN_SOURCES_LAUNCHED, true).apply()
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                return false
            }
        }
        p.edit().putBoolean(KEY_UNKNOWN_SOURCES_LAUNCHED, false).apply()

        val authority = "${context.packageName}.fileprovider"
        val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(installIntent)
        return true
    }

    companion object {
        private const val KEY_DOWNLOAD_ID = "download_id"
        private const val KEY_APK_PATH = "apk_path"
        private const val KEY_UNKNOWN_SOURCES_LAUNCHED = "unknown_sources_launched"
    }
}
