package com.lunarlog.data

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lunarlog.BuildConfig
import com.lunarlog.core.model.Cycle
import com.lunarlog.core.model.DailyLog
import com.lunarlog.data.backup.BackupDataV2
import com.lunarlog.data.backup.BackupPayloadV2
import com.lunarlog.data.backup.CycleDto
import com.lunarlog.data.backup.DailyLogDto
import com.lunarlog.data.backup.LogEntryDto
import com.lunarlog.data.backup.MedicationDto
import com.lunarlog.data.backup.MedicationLogDto
import com.lunarlog.data.backup.SymptomDefinitionDto
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManagementRepository @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val appDatabase: AppDatabase
) {
    private val gson = Gson()

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val cycleDao = appDatabase.cycleDao()
        val dailyLogDao = appDatabase.dailyLogDao()
        val logEntryDao = appDatabase.logEntryDao()
        val medicationDao = appDatabase.medicationDao()
        val symptomDao = appDatabase.symptomDefinitionDao()

        val cycles = cycleDao.getAllCyclesSync().map {
            CycleDto(
                id = it.id,
                startEpochDay = it.startDate.toEpochDay(),
                endEpochDay = it.endDate?.toEpochDay()
            )
        }

        val dailyLogs = dailyLogDao.getAllLogsSync().map {
            DailyLogDto(
                dateEpochDay = it.date.toEpochDay(),
                flowLevel = it.flowLevel,
                mood = it.mood,
                symptoms = it.symptoms,
                waterIntake = it.waterIntake,
                sleepHours = it.sleepHours,
                sleepQuality = it.sleepQuality,
                sexDrive = it.sexDrive,
                notes = it.notes,
                temperature = it.temperature,
                cervicalMucus = it.cervicalMucus
            )
        }

        val logEntries = logEntryDao.getAllEntriesSync().map {
            LogEntryDto(
                id = it.id,
                dateEpochDay = it.date,
                timeEpochMillis = it.time,
                type = it.type.name,
                value = it.value,
                details = it.details
            )
        }

        val medications = medicationDao.getAllMedicationsSync().map {
            MedicationDto(
                id = it.id,
                name = it.name,
                dosage = it.dosage,
                frequency = it.frequency,
                startDateEpochDay = it.startDate,
                endDateEpochDay = it.endDate,
                reminderTimeMinutes = it.reminderTime
            )
        }

        val medicationLogs = medicationDao.getAllMedicationLogsSync().map {
            MedicationLogDto(
                id = it.id,
                dateEpochDay = it.date,
                medicationId = it.medicationId,
                taken = it.taken,
                timestampMillis = it.timestamp
            )
        }

        val symptomDefinitions = symptomDao.getAllSymptomsSync().map {
            SymptomDefinitionDto(
                id = it.id,
                name = it.name,
                displayName = it.displayName,
                category = it.category.name,
                isCustom = it.isCustom
            )
        }

        val payload = BackupPayloadV2(
            exportedAtMillis = System.currentTimeMillis(),
            appVersionName = BuildConfig.VERSION_NAME,
            data = BackupDataV2(
                cycles = cycles,
                dailyLogs = dailyLogs,
                logEntries = logEntries,
                medications = medications,
                medicationLogs = medicationLogs,
                symptomDefinitions = symptomDefinitions
            )
        )

        gson.toJson(payload)
    }

    suspend fun restoreFromJson(json: String) = withContext(Dispatchers.IO) {
        val payload = parseBackupPayload(json)
            ?: throw IllegalArgumentException("Unsupported or invalid backup format")

        val cycleDao = appDatabase.cycleDao()
        val medicationDao = appDatabase.medicationDao()
        val symptomDao = appDatabase.symptomDefinitionDao()
        val logEntryDao = appDatabase.logEntryDao()

        // Validate cross-references before wiping.
        val medicationIds = payload.data.medications.map { it.id }.toSet()
        val badMedLogs = payload.data.medicationLogs.firstOrNull { it.medicationId !in medicationIds }
        if (badMedLogs != null) {
            throw IllegalArgumentException("Backup contains medication logs referencing missing medications")
        }

        appDatabase.withTransaction {
            appDatabase.clearAllTables()

            // Symptom definitions
            symptomDao.insertAllReplace(payload.data.symptomDefinitions.map {
                SymptomDefinition(
                    id = it.id,
                    name = it.name,
                    displayName = it.displayName,
                    category = try {
                        SymptomCategory.valueOf(it.category)
                    } catch (_: IllegalArgumentException) {
                        SymptomCategory.OTHER
                    },
                    isCustom = it.isCustom
                )
            })

            // Medications + logs (medicationId must remain stable)
            payload.data.medications.forEach {
                medicationDao.insertMedication(
                    Medication(
                        id = it.id,
                        name = it.name,
                        dosage = it.dosage,
                        frequency = it.frequency,
                        startDate = it.startDateEpochDay,
                        endDate = it.endDateEpochDay,
                        reminderTime = it.reminderTimeMinutes
                    )
                )
            }
            payload.data.medicationLogs.forEach {
                medicationDao.logMedication(
                    MedicationLog(
                        id = it.id,
                        date = it.dateEpochDay,
                        medicationId = it.medicationId,
                        taken = it.taken,
                        timestamp = it.timestampMillis
                    )
                )
            }

            // Cycles
            payload.data.cycles.forEach {
                cycleDao.insertCycle(
                    Cycle(
                        id = it.id,
                        startDate = LocalDate.ofEpochDay(it.startEpochDay),
                        endDate = it.endEpochDay?.let(LocalDate::ofEpochDay)
                    )
                )
            }

            // Log entries (granular)
            val datesWithEntries = payload.data.logEntries.map { it.dateEpochDay }.toMutableSet()
            payload.data.logEntries.forEach {
                logEntryDao.insertEntry(
                    LogEntry(
                        id = it.id,
                        date = it.dateEpochDay,
                        time = it.timeEpochMillis,
                        type = try {
                            LogEntryType.valueOf(it.type)
                        } catch (_: IllegalArgumentException) {
                            LogEntryType.NOTE
                        },
                        value = it.value,
                        details = it.details
                    )
                )
            }

            // If backup contains legacy dailyLogs without granular entries, convert them to entries so we don't lose data.
            payload.data.dailyLogs.forEach { dl ->
                if (datesWithEntries.contains(dl.dateEpochDay)) return@forEach

                val localDate = LocalDate.ofEpochDay(dl.dateEpochDay)
                val defaultTime = localDate
                    .atTime(12, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                suspend fun add(type: LogEntryType, value: String) {
                    logEntryDao.insertEntry(
                        LogEntry(
                            date = dl.dateEpochDay,
                            time = defaultTime,
                            type = type,
                            value = value
                        )
                    )
                }

                if (dl.flowLevel > 0) add(LogEntryType.FLOW, dl.flowLevel.toString())
                dl.symptoms.orEmpty().forEach { add(LogEntryType.SYMPTOM, it) }
                dl.mood.orEmpty().forEach { add(LogEntryType.MOOD, it) }
                if (dl.waterIntake > 0) add(LogEntryType.WATER, dl.waterIntake.toString())
                if (dl.sleepHours > 0f) add(LogEntryType.SLEEP, dl.sleepHours.toString())
                if (dl.sleepQuality > 0) add(LogEntryType.SLEEP_QUALITY, dl.sleepQuality.toString())
                if (dl.sexDrive > 0) add(LogEntryType.SEX, dl.sexDrive.toString())
                val notes = dl.notes.orEmpty()
                if (notes.isNotEmpty()) add(LogEntryType.NOTE, notes)
                if (dl.temperature != null) add(LogEntryType.TEMPERATURE, dl.temperature.toString())
                if (dl.cervicalMucus > 0) add(LogEntryType.MUCUS, dl.cervicalMucus.toString())

                // If the daily log was completely empty, we still want a row to exist.
                if (
                    dl.flowLevel == 0 &&
                    dl.symptoms.isNullOrEmpty() &&
                    dl.mood.isNullOrEmpty() &&
                    dl.waterIntake == 0 &&
                    dl.sleepHours == 0f &&
                    dl.sleepQuality == 0 &&
                    dl.sexDrive == 0 &&
                    notes.isEmpty() &&
                    dl.temperature == null &&
                    dl.cervicalMucus == 0
                ) {
                    // Force aggregate row creation below.
                }

                datesWithEntries.add(dl.dateEpochDay)
            }

            // Rebuild aggregates from entries for every date represented in the backup.
            val allDates = (payload.data.dailyLogs.map { it.dateEpochDay } + datesWithEntries).toSet()
            allDates.forEach { dateEpochDay ->
                dailyLogRepository.rebuildDailyLogAggregateInTransaction(dateEpochDay)
            }
        }
    }

    suspend fun nukeData() = withContext(Dispatchers.IO) {
        appDatabase.clearAllTables()
    }

    private fun parseBackupPayload(json: String): BackupPayloadV2? {
        val root = try {
            JsonParser.parseString(json)
        } catch (_: Exception) {
            return null
        }
        if (!root.isJsonObject) return null
        val obj = root.asJsonObject
        val version = obj.get("version")?.takeIf { it.isJsonPrimitive }?.asInt ?: 1
        return if (version == 2 && obj.has("data")) {
            gson.fromJson(obj, BackupPayloadV2::class.java)
        } else {
            parseLegacyPayload(obj)
        }
    }

    /**
     * Best-effort support for older backups that were created by serializing Room entities directly.
     * We only require cycles + dailyLogs to be present.
     */
    private fun parseLegacyPayload(obj: JsonObject): BackupPayloadV2? {
        val cyclesEl = obj.get("cycles") ?: return null
        val dailyLogsEl = obj.get("dailyLogs") ?: return null
        if (!cyclesEl.isJsonArray || !dailyLogsEl.isJsonArray) return null

        val cycles = cyclesEl.asJsonArray.mapNotNull { el ->
            val o = el.asJsonObject
            val id = o.get("id")?.asInt ?: 0
            val start = parseEpochDay(o.get("startDate")) ?: return@mapNotNull null
            val end = parseEpochDay(o.get("endDate"))
            CycleDto(id = id, startEpochDay = start, endEpochDay = end)
        }

        val dailyLogs = dailyLogsEl.asJsonArray.mapNotNull { el ->
            val o = el.asJsonObject
            val date = parseEpochDay(o.get("date")) ?: return@mapNotNull null
            DailyLogDto(
                dateEpochDay = date,
                flowLevel = o.get("flowLevel")?.asInt ?: 0,
                mood = o.get("mood")?.asJsonArray?.mapNotNull { it.asString } ?: emptyList(),
                symptoms = o.get("symptoms")?.asJsonArray?.mapNotNull { it.asString } ?: emptyList(),
                waterIntake = o.get("waterIntake")?.asInt ?: 0,
                sleepHours = o.get("sleepHours")?.asFloat ?: 0f,
                sleepQuality = o.get("sleepQuality")?.asInt ?: 0,
                sexDrive = o.get("sexDrive")?.asInt ?: 0,
                notes = o.get("notes")?.asString ?: "",
                temperature = if (o.get("temperature")?.isJsonNull == true) null else o.get("temperature")?.asFloat,
                cervicalMucus = o.get("cervicalMucus")?.asInt ?: 0
            )
        }

        return BackupPayloadV2(
            exportedAtMillis = System.currentTimeMillis(),
            appVersionName = null,
            data = BackupDataV2(
                cycles = cycles,
                dailyLogs = dailyLogs,
                logEntries = emptyList(),
                medications = emptyList(),
                medicationLogs = emptyList(),
                symptomDefinitions = emptyList()
            )
        )
    }

    private fun parseEpochDay(el: JsonElement?): Long? {
        if (el == null || el.isJsonNull) return null
        if (el.isJsonPrimitive) {
            val prim = el.asJsonPrimitive
            if (prim.isNumber) return prim.asLong
            if (prim.isString) {
                return try {
                    LocalDate.parse(prim.asString).toEpochDay()
                } catch (_: Exception) {
                    null
                }
            }
        }
        if (!el.isJsonObject) return null
        val o = el.asJsonObject
        val year = o.get("year")?.asInt
        val month = (o.get("monthValue") ?: o.get("month"))?.asInt
        val day = (o.get("dayOfMonth") ?: o.get("day"))?.asInt
        if (year != null && month != null && day != null) {
            return try {
                LocalDate.of(year, month, day).toEpochDay()
            } catch (_: Exception) {
                null
            }
        }
        return null
    }
}

