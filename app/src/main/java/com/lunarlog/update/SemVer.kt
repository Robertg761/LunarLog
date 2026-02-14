package com.lunarlog.update

/**
 * Minimal semver-ish implementation (major.minor.patch). Missing parts default to 0.
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    companion object {
        fun parseOrNull(raw: String): SemVer? {
            val normalized = raw.trim().removePrefix("v").removePrefix("V")
            val parts = normalized.split(".")
            if (parts.isEmpty()) return null

            fun part(i: Int): Int? = parts.getOrNull(i)?.takeIf { it.isNotBlank() }?.toIntOrNull()

            val major = part(0) ?: return null
            val minor = part(1) ?: 0
            val patch = part(2) ?: 0
            return SemVer(major, minor, patch)
        }
    }
}

