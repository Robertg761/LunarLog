package com.lunarlog.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateRepositoryTest {

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
            UpdateRepository().sanitizeReleaseNotes(raw)
        )
    }
}
