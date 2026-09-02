package com.lunarlog.update

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {

    private val api = mockk<GitHubReleaseApi>()
    private val repository = UpdateRepository(api)

    private val owner = "Robertg761"
    private val repo = "LunarLog"
    private val trustedUrl = "https://github.com/$owner/$repo/releases/download/v1.11.0/LunarLog-1.11.0.apk"
    private val sha256 = "a".repeat(64)

    private fun asset(
        name: String,
        url: String = trustedUrl,
        size: Long? = 12_345L,
        digest: String? = "sha256:$sha256"
    ) = GitHubAssetDto(name = name, browser_download_url = url, size = size, digest = digest)

    private fun release(tag: String, assets: List<GitHubAssetDto>) = GitHubLatestReleaseDto(
        tag_name = tag,
        html_url = "https://github.com/$owner/$repo/releases/tag/$tag",
        body = "- Fixed a thing",
        published_at = "2026-09-02T00:00:00Z",
        assets = assets
    )

    private fun stubRelease(tag: String, vararg assets: GitHubAssetDto) {
        every { api.fetchLatestRelease(owner, repo) } returns release(tag, assets.toList())
    }

    @Test
    fun `newer release with a trusted apk is offered with its digest`() = runTest {
        stubRelease("v1.11.0", asset("LunarLog-1.11.0.apk"))

        val info = repository.checkForUpdate(owner, repo, "1.10.0")

        assertEquals("1.11.0", info?.latestVersionName)
        assertEquals("LunarLog-1.11.0.apk", info?.apkName)
        assertEquals(trustedUrl, info?.apkUrl)
        assertEquals(12_345L, info?.apkSizeBytes)
        assertEquals(sha256, info?.apkSha256)
        assertEquals("- Fixed a thing", info?.releaseNotes)
    }

    @Test
    fun `release that is not newer is not offered`() = runTest {
        stubRelease("v1.10.0", asset("LunarLog-1.10.0.apk"))
        assertNull(repository.checkForUpdate(owner, repo, "1.10.0"))

        stubRelease("v1.9.0", asset("LunarLog-1.9.0.apk"))
        assertNull(repository.checkForUpdate(owner, repo, "1.10.0"))
    }

    @Test
    fun `non-semver tag is not offered`() = runTest {
        stubRelease("nightly", asset("LunarLog-nightly.apk"))
        assertNull(repository.checkForUpdate(owner, repo, "1.10.0"))
    }

    @Test
    fun `apk hosted outside GitHub releases is never offered`() = runTest {
        stubRelease("v1.11.0", asset("LunarLog-1.11.0.apk", url = "https://example.com/LunarLog-1.11.0.apk"))
        assertNull(repository.checkForUpdate(owner, repo, "1.10.0"))
    }

    @Test
    fun `apk from another repository's releases is never offered`() = runTest {
        stubRelease(
            "v1.11.0",
            asset("LunarLog-1.11.0.apk", url = "https://github.com/someone-else/LunarLog/releases/download/v1.11.0/LunarLog.apk")
        )
        assertNull(repository.checkForUpdate(owner, repo, "1.10.0"))
    }

    @Test
    fun `non-debug apk is preferred and untrusted assets are skipped over`() = runTest {
        stubRelease(
            "v1.11.0",
            asset("LunarLog-1.11.0-debug.apk"),
            asset("LunarLog-1.11.0.apk", url = "http://github.com/$owner/$repo/releases/download/v1.11.0/x.apk"),
            asset("LunarLog-1.11.0-release.apk")
        )

        val info = repository.checkForUpdate(owner, repo, "1.10.0")

        assertEquals("LunarLog-1.11.0-release.apk", info?.apkName)
    }

    @Test
    fun `malformed or absent digest is reported as unknown rather than failing the check`() = runTest {
        stubRelease("v1.11.0", asset("LunarLog-1.11.0.apk", digest = "md5:abc"))
        assertNull(repository.checkForUpdate(owner, repo, "1.10.0")?.apkSha256)

        stubRelease("v1.11.0", asset("LunarLog-1.11.0.apk", digest = null))
        assertNull(repository.checkForUpdate(owner, repo, "1.10.0")?.apkSha256)
    }

    @Test
    fun `isTrustedApkUrl accepts only https GitHub release locations`() {
        assertTrue(UpdateRepository.isTrustedApkUrl(trustedUrl, owner, repo))
        assertTrue(UpdateRepository.isTrustedApkUrl(trustedUrl.replace("Robertg761/LunarLog", "robertg761/lunarlog"), owner, repo))
        assertTrue(
            UpdateRepository.isTrustedApkUrl(
                "https://objects.githubusercontent.com/github-production-release-asset/1/2?X-Amz-Signature=abc",
                owner,
                repo
            )
        )
        assertTrue(UpdateRepository.isTrustedApkUrl("https://release-assets.githubusercontent.com/x/y.apk", owner, repo))

        assertFalse(UpdateRepository.isTrustedApkUrl("http://github.com/$owner/$repo/releases/download/v1/x.apk", owner, repo))
        assertFalse(UpdateRepository.isTrustedApkUrl("https://github.com/$owner/$repo/archive/main.zip", owner, repo))
        assertFalse(UpdateRepository.isTrustedApkUrl("https://github.com/other/$repo/releases/download/v1/x.apk", owner, repo))
        assertFalse(UpdateRepository.isTrustedApkUrl("https://github.com.evil.example/$owner/$repo/releases/download/v1/x.apk", owner, repo))
        assertFalse(UpdateRepository.isTrustedApkUrl("https://user@github.com/$owner/$repo/releases/download/v1/x.apk", owner, repo))
        assertFalse(UpdateRepository.isTrustedApkUrl("https://github.com:8443/$owner/$repo/releases/download/v1/x.apk", owner, repo))
        assertFalse(UpdateRepository.isTrustedApkUrl("", owner, repo))
        assertFalse(UpdateRepository.isTrustedApkUrl("not a url", owner, repo))
    }

    @Test
    fun `parseSha256Digest accepts only a well-formed sha256 prefix`() {
        assertEquals(sha256, UpdateRepository.parseSha256Digest("sha256:$sha256"))
        assertEquals(sha256, UpdateRepository.parseSha256Digest("  SHA256:${sha256.uppercase()}  "))
        assertNull(UpdateRepository.parseSha256Digest(null))
        assertNull(UpdateRepository.parseSha256Digest(sha256))
        assertNull(UpdateRepository.parseSha256Digest("sha256:${sha256.dropLast(1)}"))
        assertNull(UpdateRepository.parseSha256Digest("sha256:${"g".repeat(64)}"))
        assertNull(UpdateRepository.parseSha256Digest("sha1:${"a".repeat(40)}"))
    }

    @Test
    fun `sanitizeReleaseNotes removes release logo html and markdown chrome`() {
        val raw = """
            <p align="center"><img src="https://raw.githubusercontent.com/Robertg761/LunarLog/main/docs/assets/lunarlog-logo-512.png" alt="LunarLog logo" width="96"></p>

            ## [1.7.6] - 2026-04-26

            ### Fixed
            - **App Icon**: Updated the installed launcher icon.
            - **Update Notes**: Prevented HTML from showing in the updater.
        """.trimIndent()

        assertEquals(
            """
            [1.7.6] - 2026-04-26
            Fixed
            - App Icon: Updated the installed launcher icon.
            - Update Notes: Prevented HTML from showing in the updater.
            """.trimIndent(),
            repository.sanitizeReleaseNotes(raw)
        )
    }
}
