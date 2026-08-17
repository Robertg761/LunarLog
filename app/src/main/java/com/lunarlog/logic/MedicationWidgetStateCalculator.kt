package com.lunarlog.logic

import com.lunarlog.data.Medication
import com.lunarlog.data.MedicationLog
import java.time.LocalDate

/** One row in the medication widget. */
data class WidgetMedication(
    val id: Int,
    val name: String,
    val dosage: String,
    val taken: Boolean,
    /** Minutes from midnight, or null when the medication has no reminder set. */
    val reminderMinutes: Long?
)

data class MedicationWidgetState(
    val items: List<WidgetMedication>,
    val takenCount: Int
) {
    val totalCount: Int get() = items.size
    val allTaken: Boolean get() = items.isNotEmpty() && takenCount == totalCount
}

/**
 * Picks today's scheduled medications and folds in whether each has been marked taken.
 *
 * `as_needed` medications are excluded by [MedicationScheduler.isMedicationDueToday] and so never
 * appear here — a widget row for something with no schedule would have nothing to check off
 * against. Ordering is by reminder time so the list reads as the day runs, with unscheduled
 * medications after the timed ones and ties broken by name for a stable layout across refreshes.
 */
object MedicationWidgetStateCalculator {

    fun calculate(
        medications: List<Medication>,
        logs: List<MedicationLog>,
        today: LocalDate = LocalDate.now()
    ): MedicationWidgetState {
        val takenIds = logs.filter { it.taken }.map { it.medicationId }.toSet()

        val items = medications
            .filter { MedicationScheduler.isMedicationDueToday(it, today) }
            .sortedWith(
                compareBy(
                    { it.reminderMinutes ?: Long.MAX_VALUE },
                    { it.name.lowercase() },
                    { it.id }
                )
            )
            .map { medication ->
                WidgetMedication(
                    id = medication.id,
                    name = medication.name,
                    dosage = medication.dosage,
                    taken = medication.id in takenIds,
                    reminderMinutes = medication.reminderMinutes
                )
            }

        return MedicationWidgetState(
            items = items,
            takenCount = items.count { it.taken }
        )
    }

    /** `reminderTime` is documented as minutes from midnight; anything outside a day is not a time. */
    private val Medication.reminderMinutes: Long?
        get() = reminderTime?.takeIf { it in 0L..1439L }
}
