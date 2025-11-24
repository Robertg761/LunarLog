package com.lunarlog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SymptomCategory {
    PHYSICAL,
    EMOTIONAL,
    DISCHARGE,
    OTHER
}

@Entity(
    tableName = "symptom_definitions",
    indices = [Index(value = ["name"], unique = true)]
)
data class SymptomDefinition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // The internal key/name used in DailyLog (e.g., "Headache")
    val displayName: String, // Display name (can be localized later, currently same as name)
    val category: SymptomCategory,
    val isCustom: Boolean = false
)
