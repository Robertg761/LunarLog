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
    fun `isNewerVersion fallback treats different non-semver tags as newer`() {
        assertTrue(ApkUpdateManager.isNewerVersion("nightly-20260217", "nightly-20260216"))
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
}
