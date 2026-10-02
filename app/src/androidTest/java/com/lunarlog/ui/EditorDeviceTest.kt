package com.lunarlog.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lunarlog.data.LogEntry
import com.lunarlog.data.LogEntryType
import com.lunarlog.data.Medication
import com.lunarlog.ui.loglist.AddEntrySheet
import com.lunarlog.ui.settings.MedicationEditor
import com.lunarlog.ui.theme.LunarLogTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class EditorDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val today = LocalDate.now()

    @Test fun noteDraftAndFailureSurviveSavedStateRestoration() {
        var saved: Map<LogEntryType, List<String>>? = null
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            LunarLogTheme {
                AddEntrySheet(today, initialEntry = LogEntry(id = 1, date = today.toEpochDay(), time = 1000, type = LogEntryType.NOTE, value = "Original"),
                    saveError = "Test write failure", onDismiss = {}, onSave = { payload, _, _ -> saved = payload })
            }
        }
        rule.onNode(hasSetTextAction() and hasText("Note")).performScrollTo().performTextReplacement("Draft survives recreation")
        closeSoftKeyboard()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Draft survives recreation").assertExists()
        rule.onNodeWithText("Test write failure").assertIsDisplayed()
        rule.onNodeWithText("Save 1 item").performClick()
        rule.runOnIdle { assertEquals(listOf("Draft survives recreation"), saved?.get(LogEntryType.NOTE)) }
    }

    @Test fun explicitZeroRemainsARecordedObservation() {
        var saved: Map<LogEntryType, List<String>>? = null
        rule.setContent { LunarLogTheme {
            AddEntrySheet(today, onDismiss = {}, onSave = { payload, _, _ -> saved = payload })
        } }
        rule.onNodeWithText("Record zero / none").performScrollTo().performClick()
        rule.onNodeWithText("Save 1 item").performClick()
        rule.runOnIdle { assertEquals(listOf("0"), saved?.get(LogEntryType.FLOW)) }
    }

    @Test fun medicationDraftRetainsMultipleRemindersAndConfirmsDiscard() {
        val original = Medication(id = 1, name = "Example", dosage = "10 mg", startDate = today.toEpochDay(), dosesPerDay = 2, reminderTimes = listOf(480, 1200))
        var saved: Medication? = null
        var dismissed = false
        val restoration = StateRestorationTester(rule)
        restoration.setContent { LunarLogTheme {
            MedicationEditor(original, false, null, { dismissed = true }, { saved = it })
        } }
        rule.onNode(hasSetTextAction() and hasText("Name")).performTextReplacement("Edited medication")
        closeSoftKeyboard()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Edited medication").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Discard unsaved medication?").assertIsDisplayed()
        rule.runOnIdle { assertFalse(dismissed) }
        rule.onNodeWithText("Keep editing").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.runOnIdle {
            assertEquals("Edited medication", saved?.name)
            assertEquals(2, saved?.dosesPerDay)
            assertEquals(listOf(480L, 1200L), saved?.reminderTimes)
        }
    }

    @Test fun medicationControlsRemainReachableAtDoubleFontScale() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                LunarLogTheme { MedicationEditor(Medication(name = "Example", startDate = today.toEpochDay()), false, null, {}, {}) }
            }
        }
        rule.onNodeWithText("As needed").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText("Weekly").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText("Save").assertIsDisplayed().assertIsEnabled()
        val file = File(rule.activity.getExternalFilesDir(null), "medication-200-percent.png")
        rule.onNode(isDialog()).captureToImage().asAndroidBitmap().let { bitmap ->
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
