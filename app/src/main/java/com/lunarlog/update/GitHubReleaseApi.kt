package com.lunarlog.update

import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

internal data class GitHubLatestReleaseDto(
    val tag_name: String? = null,
    val html_url: String? = null,
    val body: String? = null,
    val published_at: String? = null,
    val assets: List<GitHubAssetDto>? = null
)

internal data class GitHubAssetDto(
    val name: String? = null,
    val browser_download_url: String? = null,
    val size: Long? = null
)

class GitHubReleaseApi(
    private val gson: Gson = Gson()
) {
    internal fun fetchLatestRelease(owner: String, repo: String): GitHubLatestReleaseDto {
        val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            // GitHub API requires a User-Agent.
            setRequestProperty("User-Agent", "LunarLog")
            setRequestProperty("Accept", "application/vnd.github+json")
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = if (stream != null) {
            BufferedReader(InputStreamReader(stream)).use { it.readText() }
        } else {
            ""
        }
        if (code !in 200..299) {
            throw IllegalStateException("GitHub API error $code: $body")
        }
        return gson.fromJson(body, GitHubLatestReleaseDto::class.java)
    }
}
