package com.lunarlog.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkUpdateManagerTest {

    @Test
    fun `isNewerVersion returns true when downloaded semver is newer`() {
        assertTrue(ApkUpdateManager.isNewerVersion("1.7.0", "1.6.1"))
    }

    @Test
    fun `isNewerVersion returns false when versions are equal ignoring v prefix`() {
        assertFalse(ApkUpdateManager.isNewerVersion("v1.7.0", "1.7.0"))
    }

    @Test
    fun `isNewerVersion rejects untrusted non-semver labels`() {
        assertFalse(ApkUpdateManager.isNewerVersion("nightly-20260217", "nightly-20260216"))
    }

    @Test
    fun `stable release is newer than prerelease of same version`() {
        assertTrue(ApkUpdateManager.isNewerVersion("1.8.0", "1.8.0-rc.1"))
        assertFalse(ApkUpdateManager.isNewerVersion("1.8.0-rc.1", "1.8.0"))
    }

    @Test
    fun `numeric prerelease identifiers use numeric ordering`() {
        assertTrue(ApkUpdateManager.isNewerVersion("1.8.0-rc.10", "1.8.0-rc.2"))
        assertTrue(
            ApkUpdateManager.isNewerVersion(
                "1.8.0-rc.100000000000000000000",
                "1.8.0-rc.99999999999999999999"
            )
        )
    }

    @Test
    fun `invalid prerelease identifiers are rejected`() {
        assertFalse(ApkUpdateManager.isNewerVersion("1.8.0-01", "1.7.0"))
        assertFalse(ApkUpdateManager.isNewerVersion("1.8.0-rc..1", "1.7.0"))
    }

    @Test
    fun `build metadata does not affect precedence`() {
        assertFalse(ApkUpdateManager.isNewerVersion("1.8.0+build.2", "1.8.0+build.1"))
    }

    @Test
    fun `normalizeVersionName removes v prefix and trims`() {
        assertEquals("1.7.0", ApkUpdateManager.normalizeVersionName("  v1.7.0  "))
    }

    @Test
    fun `extractVersionFromApkFileName returns normalized version`() {
        assertEquals("1.7.0", ApkUpdateManager.extractVersionFromApkFileName("LunarLog-v1.7.0.apk"))
    }

    @Test
    fun `extractVersionFromApkFileName returns null for unrelated filename`() {
        assertNull(ApkUpdateManager.extractVersionFromApkFileName("SomeOtherApp-1.7.0.apk"))
    }

    @Test
    fun `sha256Hex matches the published digest of a known file`() {
        val file = java.io.File.createTempFile("lunarlog-apk-", ".apk")
        try {
            file.writeText("abc")
            assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                ApkUpdateManager.sha256Hex(file)
            )
        } finally {
            file.delete()
        }
    }
}
