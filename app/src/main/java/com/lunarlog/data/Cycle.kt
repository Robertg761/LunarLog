package com.lunarlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "cycles")
data class Cycle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: Long, // Stored as Epoch Day
    val endDate: Long? = null
)
