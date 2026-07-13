package com.lunarlog.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LunarLogDeepLinkTest {
    @Test
    fun `known app links map to routes`() {
        assertEquals(Screen.Calendar.route, lunarLogRouteForDeepLink("lunarlog://calendar"))
        assertEquals(Screen.Analysis.route, lunarLogRouteForDeepLink("lunarlog://analysis"))
        assertEquals(Screen.Logging.route, lunarLogRouteForDeepLink("lunarlog://logging"))
        assertEquals(Screen.Details.createRoute(20_647), lunarLogRouteForDeepLink("lunarlog://details/20647"))
        assertEquals(Screen.Details.createRoute(-1), lunarLogRouteForDeepLink("lunarlog://details/-1"))
    }

    @Test
    fun `malformed or unsupported links are rejected`() {
        listOf(
            "https://details/20647",
            "lunarlog://unknown",
            "lunarlog://calendar/extra",
            "lunarlog://analysis?source=test",
            "lunarlog://details",
            "lunarlog://details/not-a-number",
            "lunarlog://details/20647/extra",
            "lunarlog://details/${Long.MAX_VALUE}"
        ).forEach { link ->
            assertNull(link, lunarLogRouteForDeepLink(link))
        }
    }
}
