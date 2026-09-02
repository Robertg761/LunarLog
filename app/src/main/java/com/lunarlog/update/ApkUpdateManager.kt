package com.lunarlog.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.core.content.edit
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

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
            // VISIBLE, not VISIBLE_NOTIFY_COMPLETED: the "download complete" notification hands the
            // file straight to the package installer with the APK MIME type, which would skip
            // verifyDownloadedApk. With VISIBLE the progress notification simply goes away when
            // the transfer finishes and the app's own Install prompt is the only route left.
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            // DownloadManager allows roaming by default. An unannounced APK download is not worth
            // a roaming bill; the transfer waits until the device is back on a non-roaming network.
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        prefs(context).edit {
            putLong(KEY_DOWNLOAD_ID, downloadId)
            putString(KEY_APK_PATH, file.absolutePath)
            putString(KEY_DOWNLOADED_VERSION_NAME, normalizeVersionName(info.latestVersionName))
            putString(KEY_APK_SHA256, info.apkSha256)
            putLong(KEY_APK_SIZE_BYTES, info.apkSizeBytes ?: -1L)
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
            remove(KEY_APK_SHA256)
            remove(KEY_APK_SIZE_BYTES)
        }
    }

    /**
     * Whether the downloaded file is the one the release advertised.
     *
     * DownloadManager only reports that a transfer completed; it has no opinion on whether the
     * bytes are the release asset. The digest GitHub publishes for the asset is compared against
     * the file before it is offered to the installer, so a truncated, substituted, or tampered
     * download is discarded here rather than surfacing as an installer failure. Android verifies
     * the signing certificate at install time regardless; this check sits in front of it.
     *
     * Releases predating GitHub's asset digests carry no hash and fall back to the advertised
     * size; when even that is unknown the file is accepted as-is.
     *
     * Reads the whole APK, so it runs on the IO dispatcher. On a mismatch the download and its
     * bookkeeping are cleared, so the next update check offers a fresh download.
     */
    suspend fun verifyDownloadedApk(context: Context): Boolean = withContext(Dispatchers.IO) {
        val apkFile = getDownloadedApkFile(context) ?: return@withContext false
        if (!apkFile.exists()) return@withContext false
        val prefs = prefs(context)
        val expectedSha256 = prefs.getString(KEY_APK_SHA256, null)
        val expectedSize = prefs.getLong(KEY_APK_SIZE_BYTES, -1L)
        val matches = try {
            downloadMatchesRelease(
                expectedSha256 = expectedSha256,
                expectedSizeBytes = expectedSize,
                actualSizeBytes = apkFile.length(),
                actualSha256 = { sha256Hex(apkFile) }
            )
        } catch (_: IOException) {
            false
        }
        if (!matches) clearDownloadedState(context, deleteApk = true)
        matches
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
        private const val KEY_APK_SHA256 = "apk_sha256"
        private const val KEY_APK_SIZE_BYTES = "apk_size_bytes"

        /**
         * The decision behind [verifyDownloadedApk], kept free of Android so it can be tested:
         * a published digest is authoritative; without one the advertised size is the best
         * available check; with neither there is nothing to compare against and the file is
         * accepted. [actualSha256] is only invoked when a digest is there to compare it with.
         */
        internal fun downloadMatchesRelease(
            expectedSha256: String?,
            expectedSizeBytes: Long,
            actualSizeBytes: Long,
            actualSha256: () -> String
        ): Boolean = when {
            expectedSha256 != null -> actualSha256() == expectedSha256
            expectedSizeBytes > 0L -> actualSizeBytes == expectedSizeBytes
            else -> true
        }

        internal fun sha256Hex(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

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
