package com.lunarlog.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {

    private fun parse(raw: String): SemVer = requireNotNull(SemVer.parseOrNull(raw)) { "expected $raw to parse" }

    @Test
    fun `parses core version with optional v prefix`() {
        assertEquals(SemVer(1, 10, 0), parse("1.10.0"))
        assertEquals(SemVer(1, 10, 0), parse("v1.10.0"))
        assertEquals(SemVer(1, 10, 0), parse("  V1.10.0  "))
    }

    @Test
    fun `parses prerelease identifiers and ignores build metadata`() {
        assertEquals(SemVer(2, 0, 0, listOf("rc", "1")), parse("2.0.0-rc.1"))
        assertEquals(SemVer(2, 0, 0, listOf("rc", "1")), parse("2.0.0-rc.1+build.7"))
        assertEquals(SemVer(2, 0, 0), parse("2.0.0+20260902"))
    }

    @Test
    fun `rejects malformed versions`() {
        listOf(
            "",
            "1",
            "1.2",
            "1.2.3.4",
            "01.2.3",
            "1.02.3",
            "1.2.3-",
            "1.2.3-01",
            "1.2.3-rc..1",
            "1.2.3-rc.1+",
            "1.2.3-rc_1",
            "nightly-20260902",
            "1.2.3 beta"
        ).forEach { raw ->
            assertNull("expected $raw to be rejected", SemVer.parseOrNull(raw))
        }
    }

    @Test
    fun `rejects components that overflow an int`() {
        assertNull(SemVer.parseOrNull("99999999999.0.0"))
    }

    @Test
    fun `orders core versions numerically not lexically`() {
        assertTrue(parse("1.10.0") > parse("1.9.9"))
        assertTrue(parse("2.0.0") > parse("1.99.99"))
        assertTrue(parse("1.0.10") > parse("1.0.9"))
    }

    @Test
    fun `prerelease sorts below the release it precedes`() {
        assertTrue(parse("1.8.0-rc.1") < parse("1.8.0"))
        assertTrue(parse("1.8.0-rc.1") > parse("1.7.9"))
    }

    @Test
    fun `follows the precedence example from the SemVer specification`() {
        val ordered = listOf(
            "1.0.0-alpha",
            "1.0.0-alpha.1",
            "1.0.0-alpha.beta",
            "1.0.0-beta",
            "1.0.0-beta.2",
            "1.0.0-beta.11",
            "1.0.0-rc.1",
            "1.0.0"
        ).map(::parse)

        ordered.zipWithNext().forEach { (lower, higher) ->
            assertTrue("$lower should sort below $higher", lower < higher)
        }
    }

    @Test
    fun `numeric identifiers larger than a long still compare by magnitude`() {
        assertTrue(parse("1.0.0-rc.100000000000000000000") > parse("1.0.0-rc.99999999999999999999"))
    }

    @Test
    fun `build metadata does not affect equality or ordering`() {
        assertEquals(0, parse("1.8.0+build.2").compareTo(parse("1.8.0+build.1")))
        assertEquals(parse("1.8.0+build.2"), parse("1.8.0"))
    }
}
