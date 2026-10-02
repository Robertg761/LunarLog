package com.lunarlog.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import android.content.Intent
import org.junit.Before
import org.junit.After
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lunarlog.MainActivity
import com.lunarlog.workers.notificationDestination
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AppNavigationDeviceTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    // ActivityScenario filters lifecycle events by the original launch intent.
    // MainActivity legitimately replaces that intent when receiving a notification,
    // so observe the resumed activity directly for this end-to-end test.
    private fun resumedActivity(): MainActivity? {
        var activity: MainActivity? = null
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).filterIsInstance<MainActivity>().singleOrNull()
        }
        return activity
    }

    private fun currentActivity(): MainActivity = checkNotNull(resumedActivity())

    @Before fun launchApp() {
        val context = instrumentation.targetContext
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        rule.waitUntil(60_000) { resumedActivity() != null }
    }

    @After fun closeApp() {
        instrumentation.runOnMainSync {
            Stage.values().flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
                .filterIsInstance<MainActivity>().distinct().forEach { it.finish() }
        }
        instrumentation.waitForIdleSync()
    }

    private fun recreateActivity() {
        val previous = currentActivity()
        instrumentation.runOnMainSync { previous.recreate() }
        rule.waitUntil(120_000) { resumedActivity()?.let { it !== previous } == true }
    }

    private fun rotateTo(orientation: Int, configuration: Int) {
        val activity = currentActivity()
        instrumentation.runOnMainSync { activity.requestedOrientation = orientation }
        rule.waitUntil(120_000) { resumedActivity()?.resources?.configuration?.orientation == configuration }
    }

    @Test fun notificationDestinationsAndDraftSurviveActivityRecreation() {
        try {
            val skipLabel = currentActivity().getString(com.lunarlog.R.string.ui_i_don_t_remember_skip_for_now_247c78)
            rule.waitUntil(120_000) {
                rule.onAllNodesWithText(skipLabel).fetchSemanticsNodes().isNotEmpty() ||
                    rule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
            }
            if (rule.onAllNodesWithText(skipLabel).fetchSemanticsNodes().isNotEmpty()) {
                rule.onNodeWithText(skipLabel).performScrollTo().performClick()
            }
            rule.waitUntil(120_000) { rule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty() }
            notificationDestination(currentActivity(), "calendar").send()
            rule.waitUntil(60_000) { rule.onAllNodesWithContentDescription("Previous Month").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithContentDescription("Previous Month").assertIsDisplayed()
            // After consuming a notification link, moving elsewhere and recreating must
            // restore the user's current destination instead of replaying that old link.
            rule.onNodeWithText("Home").performClick()
            rule.waitUntil(60_000) { rule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty() }
            recreateActivity()
            rule.waitUntil(60_000) { rule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty() }
            notificationDestination(currentActivity(), "details/${LocalDate.now().toEpochDay()}").send()
            rule.waitUntil(60_000) { rule.onAllNodesWithContentDescription("Add Log").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithContentDescription("Add Log").assertIsDisplayed().performClick()
            rule.onNodeWithText("Note").performScrollTo().performClick()
            rule.onNode(hasSetTextAction() and hasText("Note")).performScrollTo().performTextReplacement("Draft survives activity recreation")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            recreateActivity()
            rule.waitUntil(120_000) { rule.onAllNodesWithText("Draft survives activity recreation").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Draft survives activity recreation").assertExists()
            rotateTo(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, android.content.res.Configuration.ORIENTATION_LANDSCAPE)
            rule.waitUntil(120_000) { rule.onAllNodesWithText("Draft survives activity recreation").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Save 1 item").assertIsDisplayed()
            rotateTo(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, android.content.res.Configuration.ORIENTATION_PORTRAIT)
            rule.onNodeWithText("Draft survives activity recreation").assertExists()
            rule.onNodeWithText("Save 1 item").assertIsDisplayed().performClick()
            rule.waitUntil(60_000) {
                rule.onAllNodes(hasSetTextAction() and hasText("Note")).fetchSemanticsNodes().isEmpty()
            }
            rule.onNodeWithText("Draft survives activity recreation").assertExists()
        } catch (failure: Throwable) {
            // Preserve the body failure before activity cleanup runs.
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            java.io.File(context.getExternalFilesDir(null), "navigation-failure.txt")
                .writeText(failure.stackTraceToString())
            throw failure
        }
    }
}
