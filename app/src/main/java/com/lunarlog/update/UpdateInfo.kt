package com.lunarlog.update

data class UpdateInfo(
    val latestVersionName: String,
    val apkName: String,
    val apkUrl: String,
    val releaseNotes: String = "",
    val releaseUrl: String = "",
    val apkSizeBytes: Long? = null,
    val publishedAt: String? = null,
    /** Lower-case hex SHA-256 of the APK as published by GitHub, when the release carries one. */
    val apkSha256: String? = null
)

