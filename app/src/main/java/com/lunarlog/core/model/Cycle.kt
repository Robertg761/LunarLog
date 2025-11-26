package com.lunarlog.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycles")
data class Cycle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: Long, // Stored as Epoch Day
    val endDate: Long? = null
)
