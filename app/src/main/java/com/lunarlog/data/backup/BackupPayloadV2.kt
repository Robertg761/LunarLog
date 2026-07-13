package com.lunarlog.data.backup

data class BackupPayloadV2(
    val version: Int = 2,
    val exportedAtMillis: Long,
    val appVersionName: String? = null,
    val data: BackupDataV2
)

data class BackupDataV2(
    val cycles: List<CycleDto> = emptyList(),
    val dailyLogs: List<DailyLogDto> = emptyList(),
    val logEntries: List<LogEntryDto> = emptyList(),
    val medications: List<MedicationDto> = emptyList(),
    val medicationLogs: List<MedicationLogDto> = emptyList(),
    val symptomDefinitions: List<SymptomDefinitionDto> = emptyList(),
    val preferences: BackupPreferencesDto? = null
)

data class BackupPreferencesDto(
    val themeSeedColor: Long? = null,
    val periodLogReminderEnabled: Boolean = false,
    val periodLogReminderTimeMinutes: Long = 20L * 60L,
    val cycleNotificationEnabled: Boolean = false
)

data class CycleDto(
    val id: Int = 0,
    val startEpochDay: Long,
    val endEpochDay: Long? = null
)

data class DailyLogDto(
    val dateEpochDay: Long,
    val flowLevel: Int = 0,
    // Gson does not apply Kotlin default values for absent fields, so these may deserialize as null.
    val mood: List<String>? = emptyList(),
    val symptoms: List<String>? = emptyList(),
    val waterIntake: Int = 0,
    val sleepHours: Float = 0f,
    val sleepQuality: Int = 0,
    val sexDrive: Int = 0,
    // See note above about Gson + Kotlin defaults.
    val notes: String? = "",
    val temperature: Float? = null,
    val cervicalMucus: Int = 0
)

data class LogEntryDto(
    val id: Long = 0,
    val dateEpochDay: Long,
    val timeEpochMillis: Long,
    val type: String,
    val value: String,
    val details: String? = null
)

data class MedicationDto(
    val id: Int = 0,
    val name: String,
    val dosage: String = "",
    val frequency: String = "daily",
    val startDateEpochDay: Long,
    val endDateEpochDay: Long? = null,
    val reminderTimeMinutes: Long? = null
)

data class MedicationLogDto(
    val id: Long = 0,
    val dateEpochDay: Long,
    val medicationId: Int,
    val taken: Boolean = true,
    val timestampMillis: Long
)

data class SymptomDefinitionDto(
    val id: Long = 0,
    val name: String,
    val displayName: String,
    val category: String,
    val isCustom: Boolean = false
)
