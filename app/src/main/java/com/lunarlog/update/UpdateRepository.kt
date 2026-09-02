package com.lunarlog.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository internal constructor(
    private val api: GitHubReleaseApi
) {
    @Inject
    constructor() : this(GitHubReleaseApi())

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

        val asset = selectApkAsset(latest.assets.orEmpty(), owner, repo) ?: return@withContext null

        UpdateInfo(
            latestVersionName = latestTag.removePrefix("v"),
            apkName = asset.name,
            apkUrl = asset.url,
            releaseNotes = sanitizeReleaseNotes(latest.body.orEmpty()),
            releaseUrl = latest.html_url?.trim().orEmpty(),
            apkSizeBytes = asset.sizeBytes,
            publishedAt = latest.published_at?.trim(),
            apkSha256 = asset.sha256
        )
    }

    internal data class ApkAsset(
        val name: String,
        val url: String,
        val sizeBytes: Long?,
        val sha256: String?
    )

    /**
     * The release's APK, preferring a non-debug build when several are attached. Assets whose
     * download URL is not a GitHub release location are ignored outright rather than offered.
     */
    internal fun selectApkAsset(assets: List<GitHubAssetDto>, owner: String, repo: String): ApkAsset? {
        val candidates = assets.mapNotNull { asset ->
            val name = asset.name?.trim().orEmpty()
            val url = asset.browser_download_url?.trim().orEmpty()
            if (name.isBlank() || !name.endsWith(".apk", ignoreCase = true)) return@mapNotNull null
            if (!isTrustedApkUrl(url, owner, repo)) return@mapNotNull null
            ApkAsset(
                name = name,
                url = url,
                sizeBytes = asset.size,
                sha256 = parseSha256Digest(asset.digest)
            )
        }
        return candidates.firstOrNull { !it.name.contains("debug", ignoreCase = true) }
            ?: candidates.firstOrNull()
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

    companion object {
        /**
         * Hosts GitHub serves release assets from besides `github.com` itself, which redirects
         * downloads here.
         */
        private val assetCdnHosts = setOf(
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com"
        )

        private val sha256HexPattern = Regex("^[0-9a-f]{64}$")

        /**
         * The release JSON arrives over TLS from api.github.com, but its `browser_download_url` is
         * still data from a network response that is handed straight to DownloadManager, which
         * fetches whatever it is given. Only HTTPS locations under this repository's releases, or
         * GitHub's asset CDN, are accepted, so a bad response can at worst point at the wrong
         * GitHub file, which the digest check then rejects, rather than at an arbitrary server.
         */
        internal fun isTrustedApkUrl(raw: String, owner: String, repo: String): Boolean {
            val uri = try {
                URI(raw)
            } catch (_: Exception) {
                return false
            }
            if (!uri.scheme.equals("https", ignoreCase = true)) return false
            if (uri.userInfo != null || uri.port != -1) return false
            val host = uri.host?.lowercase() ?: return false
            val path = uri.path.orEmpty()
            return when (host) {
                "github.com" -> path.startsWith("/$owner/$repo/releases/download/", ignoreCase = true)
                in assetCdnHosts -> true
                else -> false
            }
        }

        /** GitHub reports asset digests as `sha256:<hex>`; anything else is treated as absent. */
        internal fun parseSha256Digest(raw: String?): String? {
            val value = raw?.trim()?.lowercase() ?: return null
            if (!value.startsWith("sha256:")) return null
            return value.removePrefix("sha256:").takeIf(sha256HexPattern::matches)
        }
    }
}
