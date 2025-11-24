package com.lunarlog.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.room.Room
import com.lunarlog.R
import com.lunarlog.data.AppDatabase
import com.lunarlog.data.Cycle
import com.lunarlog.data.DailyLog
import com.lunarlog.logic.CyclePredictionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class LogPeriodWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Fetch data manually since Hilt injection is tricky in GlanceAppWidget
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "lunar_log_database").build()
        val cycleDao = db.cycleDao()
        
        // This is a simplified fetch on the main thread (Glance uses suspend but we need to be careful)
        // Ideally we use a repository pattern but for a simple widget this works
        val cycles = withContext(Dispatchers.IO) { cycleDao.getAllCyclesSync() }
        
        val today = LocalDate.now()
        val latestCycle = cycles.maxByOrNull { it.startDate }
        
        val dayOfCycle = if (latestCycle != null) {
            val start = LocalDate.ofEpochDay(latestCycle.startDate)
            ChronoUnit.DAYS.between(start, today).toInt() + 1
        } else {
            1
        }
        
        val daysUntil = if (latestCycle != null) {
            val avg = CyclePredictionUtils.calculateAverageCycleLength(cycles)
            val nextPeriod = CyclePredictionUtils.predictNextPeriod(latestCycle, avg)
            ChronoUnit.DAYS.between(today, nextPeriod).toInt()
        } else {
            28
        }
        
        db.close()

        provideContent {
            WidgetContent(dayOfCycle, daysUntil)
        }
    }

    @Composable
    private fun WidgetContent(dayOfCycle: Int, daysUntil: Int) {
        // Colors
        val primaryColor = ColorProvider(R.color.colorPrimary)
        val onPrimaryColor = ColorProvider(android.R.color.white)
        val surfaceColor = ColorProvider(android.R.color.white)
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surfaceColor)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Day",
                        style = TextStyle(color = primaryColor)
                    )
                    Text(
                        text = "$dayOfCycle",
                        style = TextStyle(
                            fontSize = androidx.compose.ui.unit.TextUnit(36f, androidx.compose.ui.unit.TextUnitType.Sp),
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    )
                }
                
                Spacer(GlanceModifier.width(16.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (daysUntil <= 0) "Due!" else "$daysUntil left",
                        style = TextStyle(color = ColorProvider(android.R.color.darker_gray))
                    )
                }
            }

            Spacer(GlanceModifier.height(12.dp))

            // Action Button
            Row(
                modifier = GlanceModifier
                    .background(primaryColor)
                    .padding(8.dp)
                    .clickable(actionRunCallback<LogPeriodAction>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // We don't have easy vector icon support in glance without resources, text is safer
                Text(
                    text = "+ Log Period",
                    style = TextStyle(color = onPrimaryColor, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

class LogPeriodWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LogPeriodWidget()
}

class LogPeriodAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Log Period Logic
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "lunar_log_database").build()
        val cycleDao = db.cycleDao()
        
        val today = LocalDate.now().toEpochDay()
        
        // Simple logic: Start a new cycle today
        // Check if cycle already exists for today to avoid dupes
        val existing = cycleDao.getCycleForDateSync(today) // We need to add this method or reuse logic
        if (existing == null) {
            // Close previous cycle if needed
            val cycles = cycleDao.getAllCyclesSync()
            val lastCycle = cycles.maxByOrNull { it.startDate }
            
            if (lastCycle != null && lastCycle.endDate == null) {
                cycleDao.updateCycle(lastCycle.copy(endDate = today - 1))
            }
            
            cycleDao.insertCycle(Cycle(startDate = today))
            
            // Log flow for today too
            val dailyLogDao = db.dailyLogDao()
            dailyLogDao.insertLog(DailyLog(date = today, flowLevel = 2)) // Default medium flow
        }
        
        db.close()
        
        // Refresh widget
        LogPeriodWidget().update(context, glanceId)
    }
}
