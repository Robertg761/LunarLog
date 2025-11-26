package com.lunarlog.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey val date: LocalDate,
    val flowLevel: Int = 0, // 0=None, 1=Spotting, 2=Light, 3=Medium, 4=Heavy
    val mood: List<String> = emptyList(),
    val symptoms: List<String> = emptyList(),
    val waterIntake: Int = 0,
    val sleepHours: Float = 0f,
    val sleepQuality: Int = 0, // 1-5
    val sexDrive: Int = 0, // 0=None, 1=Low, 2=Medium, 3=High
    val notes: String = "",
    val temperature: Float? = null, // Basal Body Temperature (Celsius or Fahrenheit, handled by UI preference)
    val cervicalMucus: Int = 0 // 0=None/Dry, 1=Sticky, 2=Creamy, 3=Watery, 4=Egg White (Peak Fertility)
)
