package com.lunarlog.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lunarlog.MainActivity
import com.lunarlog.workers.notificationDestination
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AppNavigationDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun notificationDestinationsAndDraftSurviveActivityRecreation() {
        val skipLabel = rule.activity.getString(com.lunarlog.R.string.ui_i_don_t_remember_skip_for_now_247c78)
        rule.waitUntil(120_000) {
            rule.onAllNodesWithText(skipLabel).fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        if (rule.onAllNodesWithText(skipLabel).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText(skipLabel).performScrollTo().performClick()
        }
        rule.waitUntil(120_000) { rule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty() }
        notificationDestination(rule.activity, "calendar").send()
        rule.waitUntil(60_000) { rule.onAllNodesWithContentDescription("Previous Month").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Previous Month").assertIsDisplayed()
        // After consuming a notification link, moving elsewhere and recreating must
        // restore the user's current destination instead of replaying that old link.
        rule.onNodeWithText("Home").performClick()
        rule.waitUntil(60_000) { rule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty() }
        rule.activityRule.scenario.recreate()
        rule.waitUntil(60_000) { rule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty() }
        notificationDestination(rule.activity, "details/${LocalDate.now().toEpochDay()}").send()
        rule.waitUntil(60_000) { rule.onAllNodesWithContentDescription("Add Log").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithContentDescription("Add Log").assertIsDisplayed().performClick()
        rule.onNodeWithText("Note").performScrollTo().performClick()
        rule.onNode(hasSetTextAction() and hasText("Note")).performScrollTo().performTextReplacement("Draft survives activity recreation")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(120_000) { rule.onAllNodesWithText("Draft survives activity recreation").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Draft survives activity recreation").assertExists()
        rule.activityRule.scenario.onActivity { it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        rule.waitUntil(120_000) { rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
        rule.waitUntil(120_000) { rule.onAllNodesWithText("Draft survives activity recreation").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Save 1 item").assertIsDisplayed()
        rule.activityRule.scenario.onActivity { it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }
}
