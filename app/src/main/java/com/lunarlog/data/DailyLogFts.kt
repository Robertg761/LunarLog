package com.lunarlog.data

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.ColumnInfo

@Entity(tableName = "daily_logs_fts")
@Fts4(contentEntity = DailyLog::class)
data class DailyLogFts(
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "notes") val notes: String
)
