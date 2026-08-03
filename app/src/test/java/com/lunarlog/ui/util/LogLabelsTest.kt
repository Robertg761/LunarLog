package com.lunarlog.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The integer levels on `DailyLog` are only meaningful because of the words these helpers put on
 * them, and nothing checked that the two agreed.
 *
 * They did not. The Add Entry sheet carried its own list of mucus words, one rung short of the
 * model's, so picking "Sticky" stored a 2 and the day summary read it back as "Creamy"; its sex
 * drive slider ran to 5 where the model defines 3, and everything above 3 came back as "None". Both
 * failures were silent — a wrong word is still a word — and both are the shape this file guards:
 * a level inside the documented domain must have its own name, and only values outside it may fall
 * back to the not-recorded word.
 */
class LogLabelsTest {

    private fun assertDomainIsNamed(domain: IntRange, absent: String, label: (Int) -> String) {
        val named = domain.filter { it != 0 }.map(label)
        named.forEach { assertTrue("a level inside the domain reads as \"$absent\"", it != absent) }
        assertEquals("two levels share a label: $named", named.size, named.toSet().size)
        assertEquals(absent, label(0))
        assertEquals(absent, label(domain.last + 1))
        assertEquals(absent, label(-1))
    }

    /** `DailyLog.flowLevel` — 0=None, 1=Spotting, 2=Light, 3=Medium, 4=Heavy. */
    @Test
    fun `flow names every level the model defines`() {
        assertDomainIsNamed(0..4, "None", ::flowLabel)
        assertEquals("Spotting", flowLabel(1))
        assertEquals("Heavy", flowLabel(4))
    }

    /** `DailyLog.sexDrive` — 0=None, 1=Low, 2=Medium, 3=High. */
    @Test
    fun `sex drive names every level the model defines`() {
        assertDomainIsNamed(0..3, "None", ::sexDriveLabel)
        assertEquals("Low", sexDriveLabel(1))
        assertEquals("High", sexDriveLabel(3))
    }

    /** `DailyLog.cervicalMucus` — 0=None/Dry, 1=Sticky, 2=Creamy, 3=Watery, 4=Egg White. */
    @Test
    fun `cervical mucus names every level the model defines`() {
        assertDomainIsNamed(0..4, "None/Dry", ::mucusLabel)
        assertEquals("Sticky", mucusLabel(1))
        assertEquals("Creamy", mucusLabel(2))
        assertEquals("Watery", mucusLabel(3))
        // The fertile signal AdvancedCycleIntelligence reads as `cervicalMucus >= 3`.
        assertEquals("Egg White", mucusLabel(4))
    }
}
