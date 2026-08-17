package com.lunarlog.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.lunarlog.di.WidgetEntryPoint
import com.lunarlog.logic.MedicationWidgetState
import com.lunarlog.logic.MedicationWidgetStateCalculator
import com.lunarlog.logic.WidgetMedication
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Today's scheduled medications, each tickable straight from the home screen.
 *
 * **This widget puts medication names on the lock/home screen**, which is a deliberate exception to
 * the posture the rest of the app takes — notifications are `VISIBILITY_PRIVATE` and screens carry
 * `FLAG_SECURE` precisely so this information does not appear where a shoulder can see it. A widget
 * has no equivalent of either flag: the launcher renders it, and nothing the app does can redact it
 * over someone's shoulder or in a screenshot. Adding the widget is the consent, so it is opt-in by
 * construction, but it is worth knowing that it is the one surface where LunarLog shows medication
 * names without an unlock.
 */
class MedicationWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        val today = LocalDate.now()
        val state = withContext(Dispatchers.IO) {
            runCatching {
                val repository = entryPoint.medicationRepository()
                MedicationWidgetStateCalculator.calculate(
                    medications = repository.getAllMedicationsSync(),
                    logs = repository.getLogsForDateSync(today.toEpochDay()),
                    today = today
                )
            }.getOrDefault(MedicationWidgetState(items = emptyList(), takenCount = 0))
        }

        provideContent { MedicationContent(state) }
    }

    @Composable
    private fun MedicationContent(state: MedicationWidgetState) {
        val context = LocalContext.current

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .widgetSurface()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Header(state, openApp = deepLinkAction(context, "logging"))

            if (state.items.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(deepLinkAction(context, "logging")),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nothing scheduled today",
                        maxLines = 2,
                        style = TextStyle(
                            color = WidgetColors.muted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                return@Column
            }

            Spacer(GlanceModifier.height(6.dp))

            // LazyColumn rather than a plain Column: the list length is the user's business, and a
            // widget that clips its last row silently is worse than one that scrolls.
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(state.items, itemId = { it.id.toLong() }) { medication ->
                    MedicationRow(medication)
                }
            }
        }
    }

    @Composable
    private fun Header(state: MedicationWidgetState, openApp: Action) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(openApp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Medications",
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = WidgetColors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            if (state.items.isNotEmpty()) {
                Text(
                    text = "${state.takenCount}/${state.totalCount}",
                    maxLines = 1,
                    style = TextStyle(
                        color = if (state.allTaken) WidgetColors.fertileAccent else WidgetColors.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }

    @Composable
    private fun MedicationRow(medication: WidgetMedication) {
        CheckBox(
            checked = medication.taken,
            onCheckedChange = actionRunCallback<ToggleMedicationAction>(
                actionParametersOf(
                    ToggleMedicationAction.MedicationIdKey to medication.id,
                    ToggleMedicationAction.TakenKey to !medication.taken
                )
            ),
            text = medication.label(),
            maxLines = 2,
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            style = TextStyle(
                color = if (medication.taken) WidgetColors.muted else WidgetColors.onSurface,
                fontSize = 13.sp
            ),
            colors = CheckboxDefaults.colors(
                checkedColor = WidgetColors.primary,
                uncheckedColor = WidgetColors.outline
            )
        )
    }

    private companion object {
        val COMPACT = DpSize(250.dp, 110.dp)
        val LARGE = DpSize(250.dp, 250.dp)

        /** "Vitamin D · 1000 IU" — dosage only when there is one to show. */
        fun WidgetMedication.label(): String =
            if (dosage.isBlank()) name else "$name · $dosage"
    }
}

class MedicationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MedicationWidget()
}

class ToggleMedicationAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val medicationId = parameters[MedicationIdKey] ?: return
        val taken = parameters[TakenKey] ?: return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )

        withContext(Dispatchers.IO) {
            runCatching {
                entryPoint.medicationRepository().setMedicationTaken(
                    date = LocalDate.now().toEpochDay(),
                    medicationId = medicationId,
                    taken = taken
                )
            }
        }

        MedicationWidget().update(context, glanceId)
    }

    companion object {
        val MedicationIdKey = ActionParameters.Key<Int>("medication_id")
        val TakenKey = ActionParameters.Key<Boolean>("medication_taken")
    }
}
