package com.lunarlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String = "",
    val frequency: String = "daily", // daily, weekly, as_needed
    val startDate: Long, // Epoch Day
    val endDate: Long? = null,
    val reminderTime: Long? = null // Minutes from midnight, or null if no reminder
)

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // Epoch Day
    val medicationId: Int,
    val taken: Boolean = true,
    val timestamp: Long // System.currentTimeMillis() of when it was logged
)
