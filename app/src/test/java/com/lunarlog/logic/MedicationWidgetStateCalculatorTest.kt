package com.lunarlog.logic

import com.lunarlog.data.Medication
import com.lunarlog.data.MedicationLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MedicationWidgetStateCalculatorTest {

    /** A Tuesday, which the weekly cases below lean on. */
    private val today: LocalDate = LocalDate.of(2026, 2, 17)

    private fun medication(
        id: Int,
        name: String,
        frequency: String = "daily",
        reminderTime: Long? = null,
        startDate: LocalDate = today.minusDays(30),
        endDate: LocalDate? = null
    ) = Medication(
        id = id,
        name = name,
        dosage = "1 tablet",
        frequency = frequency,
        startDate = startDate.toEpochDay(),
        endDate = endDate?.toEpochDay(),
        reminderTime = reminderTime
    )

    private fun log(medicationId: Int, taken: Boolean = true) = MedicationLog(
        date = today.toEpochDay(),
        medicationId = medicationId,
        taken = taken,
        timestamp = 0L
    )

    @Test
    fun `orders timed medications by reminder time`() {
        val medications = listOf(
            medication(id = 1, name = "Evening", reminderTime = 20L * 60),
            medication(id = 2, name = "Morning", reminderTime = 8L * 60),
            medication(id = 3, name = "Midday", reminderTime = 13L * 60)
        )

        val state = MedicationWidgetStateCalculator.calculate(medications, emptyList(), today)

        assertEquals(listOf("Morning", "Midday", "Evening"), state.items.map { it.name })
    }

    @Test
    fun `puts medications with no reminder after the timed ones`() {
        val medications = listOf(
            medication(id = 1, name = "Anytime", reminderTime = null),
            medication(id = 2, name = "Bedtime", reminderTime = 23L * 60)
        )

        val state = MedicationWidgetStateCalculator.calculate(medications, emptyList(), today)

        assertEquals(listOf("Bedtime", "Anytime"), state.items.map { it.name })
    }

    @Test
    fun `breaks ties by name regardless of case so the layout is stable`() {
        val medications = listOf(
            medication(id = 1, name = "zinc", reminderTime = 8L * 60),
            medication(id = 2, name = "Aspirin", reminderTime = 8L * 60),
            medication(id = 3, name = "iron", reminderTime = 8L * 60)
        )

        val state = MedicationWidgetStateCalculator.calculate(medications, emptyList(), today)

        assertEquals(listOf("Aspirin", "iron", "zinc"), state.items.map { it.name })
    }

    @Test
    fun `breaks a full tie by id`() {
        val medications = listOf(
            medication(id = 7, name = "Iron", reminderTime = 8L * 60),
            medication(id = 3, name = "Iron", reminderTime = 8L * 60)
        )

        val state = MedicationWidgetStateCalculator.calculate(medications, emptyList(), today)

        assertEquals(listOf(3, 7), state.items.map { it.id })
    }

    @Test
    fun `ignores a reminder time that is not a time of day`() {
        val medications = listOf(
            medication(id = 1, name = "Bad data", reminderTime = 5000L),
            medication(id = 2, name = "Bedtime", reminderTime = 23L * 60)
        )

        val state = MedicationWidgetStateCalculator.calculate(medications, emptyList(), today)

        // Sorted as unscheduled, and nothing downstream is handed a time it could not render.
        assertEquals(listOf("Bedtime", "Bad data"), state.items.map { it.name })
        assertNull(state.items.last().reminderMinutes)
    }

    @Test
    fun `excludes as-needed medications, which have nothing to check off against`() {
        val medications = listOf(
            medication(id = 1, name = "Painkiller", frequency = "as_needed"),
            medication(id = 2, name = "Daily", frequency = "daily")
        )

        val state = MedicationWidgetStateCalculator.calculate(medications, emptyList(), today)

        assertEquals(listOf("Daily"), state.items.map { it.name })
    }

    @Test
    fun `includes a weekly medication only on its own weekday`() {
        val onTuesday = medication(
            id = 1,
            name = "Weekly",
            frequency = "weekly",
            startDate = today.minusWeeks(2)
        )
        val onWednesday = medication(
            id = 2,
            name = "Other week",
            frequency = "weekly",
            startDate = today.minusWeeks(2).plusDays(1)
        )

        val state = MedicationWidgetStateCalculator.calculate(
            listOf(onTuesday, onWednesday),
            emptyList(),
            today
        )

        assertEquals(listOf("Weekly"), state.items.map { it.name })
    }

    @Test
    fun `excludes medications outside their date range`() {
        val medications = listOf(
            medication(id = 1, name = "Not started", startDate = today.plusDays(1)),
            medication(
                id = 2,
                name = "Finished",
                startDate = today.minusDays(30),
                endDate = today.minusDays(1)
            ),
            medication(id = 3, name = "Ends today", startDate = today.minusDays(30), endDate = today),
            medication(id = 4, name = "Starts today", startDate = today)
        )

        val state = MedicationWidgetStateCalculator.calculate(medications, emptyList(), today)

        assertEquals(listOf("Ends today", "Starts today"), state.items.map { it.name })
    }

    @Test
    fun `folds today's logs into each row`() {
        val medications = listOf(
            medication(id = 1, name = "Taken", reminderTime = 8L * 60),
            medication(id = 2, name = "Untaken", reminderTime = 9L * 60)
        )

        val state = MedicationWidgetStateCalculator.calculate(medications, listOf(log(1)), today)

        assertTrue(state.items.first { it.id == 1 }.taken)
        assertFalse(state.items.first { it.id == 2 }.taken)
        assertEquals(1, state.takenCount)
        assertEquals(2, state.totalCount)
        assertFalse(state.allTaken)
    }

    @Test
    fun `treats an explicitly untaken log as not taken`() {
        val medications = listOf(medication(id = 1, name = "Skipped"))

        val state = MedicationWidgetStateCalculator.calculate(
            medications,
            listOf(log(medicationId = 1, taken = false)),
            today
        )

        assertFalse(state.items.single().taken)
        assertEquals(0, state.takenCount)
    }

    @Test
    fun `reports all taken only once every row is checked`() {
        val medications = listOf(
            medication(id = 1, name = "First", reminderTime = 8L * 60),
            medication(id = 2, name = "Second", reminderTime = 9L * 60)
        )

        val state = MedicationWidgetStateCalculator.calculate(
            medications,
            listOf(log(1), log(2)),
            today
        )

        assertEquals(2, state.takenCount)
        assertTrue(state.allTaken)
    }

    @Test
    fun `is not all taken when there is nothing scheduled`() {
        val state = MedicationWidgetStateCalculator.calculate(emptyList(), emptyList(), today)

        assertEquals(0, state.totalCount)
        assertEquals(0, state.takenCount)
        // Otherwise an empty day would draw the widget's "all done" state.
        assertFalse(state.allTaken)
    }

    @Test
    fun `ignores logs for medications that are not scheduled today`() {
        val medications = listOf(medication(id = 1, name = "Daily"))

        val state = MedicationWidgetStateCalculator.calculate(
            medications,
            listOf(log(medicationId = 1), log(medicationId = 99)),
            today
        )

        assertEquals(1, state.totalCount)
        assertEquals(1, state.takenCount)
    }

    @Test
    fun `carries name, dosage and reminder through to the row`() {
        val medications = listOf(
            Medication(
                id = 4,
                name = "Iron",
                dosage = "65 mg",
                frequency = "daily",
                startDate = today.minusDays(1).toEpochDay(),
                reminderTime = 7L * 60 + 30
            )
        )

        val state = MedicationWidgetStateCalculator.calculate(medications, emptyList(), today)

        val row = state.items.single()
        assertEquals(4, row.id)
        assertEquals("Iron", row.name)
        assertEquals("65 mg", row.dosage)
        assertEquals(450L, row.reminderMinutes)
    }
}
