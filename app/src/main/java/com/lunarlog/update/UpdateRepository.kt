package com.lunarlog.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor() {
    private val api = GitHubReleaseApi()

    suspend fun checkForUpdate(
        owner: String,
        repo: String,
        currentVersionName: String
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        val latest = api.fetchLatestRelease(owner, repo)
        val latestTag = latest.tag_name?.trim().orEmpty()
        if (latestTag.isBlank()) return@withContext null

        val latestVer = SemVer.parseOrNull(latestTag)
        val currentVer = SemVer.parseOrNull(currentVersionName)
        val isNewer = latestVer != null && currentVer != null && latestVer > currentVer

        if (!isNewer) return@withContext null

        val assets = latest.assets.orEmpty()
        val apkAssets = assets
            .mapNotNull { a ->
                val name = a.name?.trim()
                val url = a.browser_download_url?.trim()
                if (name.isNullOrBlank() || url.isNullOrBlank()) null else name to url
            }
            .filter { (name, _) -> name.endsWith(".apk", ignoreCase = true) }

        if (apkAssets.isEmpty()) return@withContext null

        // Prefer non-debug apks if multiple exist.
        val (apkName, apkUrl) = apkAssets.firstOrNull { (name, _) ->
            !name.contains("debug", ignoreCase = true)
        } ?: apkAssets.first()

        val apkSizeBytes = assets
            .firstOrNull { a ->
                val n = a.name?.trim().orEmpty()
                n.equals(apkName, ignoreCase = true)
            }
            ?.size

        UpdateInfo(
            latestVersionName = latestTag.removePrefix("v"),
            apkName = apkName,
            apkUrl = apkUrl,
            releaseNotes = sanitizeReleaseNotes(latest.body.orEmpty()),
            releaseUrl = latest.html_url?.trim().orEmpty(),
            apkSizeBytes = apkSizeBytes,
            publishedAt = latest.published_at?.trim()
        )
    }

    internal fun sanitizeReleaseNotes(raw: String): String {
        return raw
            .lineSequence()
            .map { line ->
                line
                    .replace(Regex("<[^>]+>"), "")
                    .replace(Regex("""!\[[^]]*]\([^)]*\)"""), "")
                    .replace(Regex("""\[[^]]+]\([^)]*\)""")) { match ->
                        match.value.substringAfter("[").substringBefore("]")
                    }
                    .replace(Regex("""^#{1,6}\s*"""), "")
                    .replace(Regex("""^\s*[-*]\s+"""), "- ")
                    .replace(Regex("""\*\*([^*]+)\*\*"""), "$1")
                    .replace("`", "")
                    .trimEnd()
            }
            .filter { line ->
                line.isNotBlank() &&
                    !line.contains("raw.githubusercontent.com", ignoreCase = true) &&
                    !line.contains("lunarlog-logo", ignoreCase = true)
            }
            .joinToString("\n")
            .trim()
    }
}
