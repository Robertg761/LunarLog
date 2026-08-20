package com.lunarlog.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "cycles")
data class Cycle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    /**
     * True when [endDate] was filled in by the app rather than the user — a new period was
     * started while this one was still open, so the real end was never recorded. Estimated
     * ends are shown on the calendar but excluded from period-length averages; any user edit
     * that sets an end date clears the flag.
     */
    @ColumnInfo(defaultValue = "0") val endEstimated: Boolean = false
)
