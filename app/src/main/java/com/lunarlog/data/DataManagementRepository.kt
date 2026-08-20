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
    private val appDatabase: AppDatabase,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val gson = Gson()

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val cycleDao = appDatabase.cycleDao()
        val dailyLogDao = appDatabase.dailyLogDao()
        val logEntryDao = appDatabase.logEntryDao()
        val medicationDao = appDatabase.medicationDao()
        val symptomDao = appDatabase.symptomDefinitionDao()

        val data = appDatabase.withTransaction {
            val cycles = cycleDao.getAllCyclesSync().map {
                CycleDto(
                    id = it.id,
                    startEpochDay = it.startDate.toEpochDay(),
                    endEpochDay = it.endDate?.toEpochDay(),
                    endEstimated = it.endEstimated
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

            BackupDataV2(
                cycles = cycles,
                dailyLogs = dailyLogs,
                logEntries = logEntries,
                medications = medications,
                medicationLogs = medicationLogs,
                symptomDefinitions = symptomDefinitions
            )
        }

        val payload = BackupPayloadV2(
            exportedAtMillis = System.currentTimeMillis(),
            appVersionName = BuildConfig.VERSION_NAME,
            data = data.copy(preferences = userPreferencesRepository.createBackupPreferences())
        )

        gson.toJson(payload)
    }

    suspend fun restoreFromJson(json: String) = withContext(Dispatchers.IO) {
        val payload = parseBackupPayload(json)
            ?: throw IllegalArgumentException("Unsupported or invalid backup format")

        validatePayload(payload)

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
            val restoredSymptoms = payload.data.symptomDefinitions.map {
                SymptomDefinition(
                    id = it.id,
                    name = it.name,
                    displayName = it.displayName,
                    category = SymptomCategory.valueOf(it.category),
                    isCustom = it.isCustom
                )
            }.filterNot { !it.isCustom && it.name in SymptomData.retiredDefaultNames }
            symptomDao.insertAllReplace(restoredSymptoms.ifEmpty { SymptomData.defaultSymptoms })

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
                        endDate = it.endEpochDay?.let(LocalDate::ofEpochDay),
                        endEstimated = it.endEstimated && it.endEpochDay != null
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
                        type = LogEntryType.valueOf(it.type),
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

                datesWithEntries.add(dl.dateEpochDay)
            }

            // Rebuild aggregates from entries for every date represented in the backup.
            val allDates = (payload.data.dailyLogs.map { it.dateEpochDay } + datesWithEntries).toSet()
            allDates.forEach { dateEpochDay ->
                dailyLogRepository.rebuildDailyLogAggregateInTransaction(dateEpochDay)
            }
        }

        payload.data.preferences?.let { userPreferencesRepository.restoreBackupPreferences(it) }
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
        return try {
            val obj = root.asJsonObject
            val version = obj.get("version")?.takeIf { it.isJsonPrimitive }?.asInt ?: 1
            if (version == 2 && obj.has("data")) {
                gson.fromJson(obj, BackupPayloadV2::class.java)
            } else {
                parseLegacyPayload(obj)
            }
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (error: Exception) {
            throw IllegalArgumentException("Backup contains malformed fields", error)
        }
    }

    /**
     * Strict support for older backups that were created by serializing Room entities directly.
     * Every record must parse successfully; a lossy legacy restore is never accepted.
     */
    private fun parseLegacyPayload(obj: JsonObject): BackupPayloadV2? {
        val cyclesEl = obj.get("cycles") ?: return null
        val dailyLogsEl = obj.get("dailyLogs") ?: return null
        if (!cyclesEl.isJsonArray || !dailyLogsEl.isJsonArray) return null

        val cycles = cyclesEl.asJsonArray.mapIndexed { index, el ->
            if (!el.isJsonObject) throw IllegalArgumentException("Legacy cycle $index is not an object")
            val o = el.asJsonObject
            val id = o.get("id")?.asInt ?: 0
            val start = parseEpochDay(o.get("startDate"))
                ?: throw IllegalArgumentException("Legacy cycle $index has an invalid start date")
            val endElement = o.get("endDate")
            val end = if (endElement == null || endElement.isJsonNull) {
                null
            } else {
                parseEpochDay(endElement)
                    ?: throw IllegalArgumentException("Legacy cycle $index has an invalid end date")
            }
            CycleDto(id = id, startEpochDay = start, endEpochDay = end)
        }

        val dailyLogs = dailyLogsEl.asJsonArray.mapIndexed { index, el ->
            if (!el.isJsonObject) throw IllegalArgumentException("Legacy daily log $index is not an object")
            val o = el.asJsonObject
            val date = parseEpochDay(o.get("date"))
                ?: throw IllegalArgumentException("Legacy daily log $index has an invalid date")
            DailyLogDto(
                dateEpochDay = date,
                flowLevel = o.get("flowLevel")?.asInt ?: 0,
                mood = parseLegacyStringList(o.get("mood"), "daily log $index mood"),
                symptoms = parseLegacyStringList(o.get("symptoms"), "daily log $index symptoms"),
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

    private fun parseLegacyStringList(element: JsonElement?, label: String): List<String> {
        if (element == null || element.isJsonNull) return emptyList()
        if (!element.isJsonArray) throw IllegalArgumentException("Legacy $label is not an array")
        return element.asJsonArray.mapIndexed { index, item ->
            if (!item.isJsonPrimitive || !item.asJsonPrimitive.isString) {
                throw IllegalArgumentException("Legacy $label item $index is not text")
            }
            item.asString
        }
    }

    private fun validatePayload(payload: BackupPayloadV2) {
        val data = payload.data

        fun requireDate(epochDay: Long, label: String): LocalDate = try {
            LocalDate.ofEpochDay(epochDay)
        } catch (error: Exception) {
            throw IllegalArgumentException("$label is outside the supported date range", error)
        }

        fun requireUniquePositiveIds(ids: List<Long>, label: String) {
            val positiveIds = ids.filter { it > 0 }
            if (positiveIds.size != positiveIds.toSet().size) {
                throw IllegalArgumentException("Backup contains duplicate $label IDs")
            }
        }

        requireUniquePositiveIds(data.cycles.map { it.id.toLong() }, "period")
        val periodRanges = data.cycles.mapIndexed { index, cycle ->
            val start = requireDate(cycle.startEpochDay, "Period $index start date")
            val end = cycle.endEpochDay?.let { requireDate(it, "Period $index end date") }
            if (end != null && end.isBefore(start)) {
                throw IllegalArgumentException("Period $index ends before it starts")
            }
            start to end
        }.sortedBy { it.first }
        periodRanges.zipWithNext().forEachIndexed { index, (current, next) ->
            val currentEnd = current.second ?: LocalDate.MAX
            if (!currentEnd.isBefore(next.first)) {
                throw IllegalArgumentException("Periods $index and ${index + 1} overlap")
            }
        }

        val dailyLogDates = mutableSetOf<Long>()
        data.dailyLogs.forEachIndexed { index, log ->
            requireDate(log.dateEpochDay, "Daily log $index date")
            if (!dailyLogDates.add(log.dateEpochDay)) {
                throw IllegalArgumentException("Backup contains duplicate daily log dates")
            }
            require(log.flowLevel in 0..4) { "Daily log $index has an invalid flow level" }
            require(log.waterIntake >= 0) { "Daily log $index has invalid water intake" }
            require(log.sleepHours in 0f..24f) { "Daily log $index has invalid sleep hours" }
            require(log.sleepQuality in 0..5) { "Daily log $index has invalid sleep quality" }
            require(log.sexDrive in 0..5) { "Daily log $index has invalid sex drive" }
            require(log.cervicalMucus in 0..4) { "Daily log $index has invalid cervical mucus" }
            val temperature = log.temperature
            require(
                temperature == null || temperature in 34f..43f || temperature in 90f..110f
            ) { "Daily log $index has an invalid temperature" }
        }

        requireUniquePositiveIds(data.logEntries.map { it.id }, "log entry")
        data.logEntries.forEachIndexed { index, entry ->
            requireDate(entry.dateEpochDay, "Log entry $index date")
            try {
                LogEntryType.valueOf(entry.type)
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("Log entry $index has an unknown type", error)
            }
            require(entry.value.isNotBlank()) { "Log entry $index has an empty value" }
        }

        requireUniquePositiveIds(data.medications.map { it.id.toLong() }, "medication")
        data.medications.forEachIndexed { index, medication ->
            val start = requireDate(medication.startDateEpochDay, "Medication $index start date")
            val end = medication.endDateEpochDay?.let { requireDate(it, "Medication $index end date") }
            require(medication.name.isNotBlank()) { "Medication $index has no name" }
            require(medication.id > 0) { "Medication $index has an invalid ID" }
            require(medication.frequency in setOf("daily", "weekly", "as_needed")) {
                "Medication $index has an invalid frequency"
            }
            require(end == null || !end.isBefore(start)) { "Medication $index ends before it starts" }
            require(medication.reminderTimeMinutes == null || medication.reminderTimeMinutes in 0L..1439L) {
                "Medication $index has an invalid reminder time"
            }
        }

        val medicationIds = data.medications.map { it.id }.toSet()
        requireUniquePositiveIds(data.medicationLogs.map { it.id }, "medication log")
        val medicationLogKeys = mutableSetOf<Pair<Long, Int>>()
        data.medicationLogs.forEachIndexed { index, log ->
            requireDate(log.dateEpochDay, "Medication log $index date")
            require(log.medicationId in medicationIds) {
                "Medication log $index references a missing medication"
            }
            require(medicationLogKeys.add(log.dateEpochDay to log.medicationId)) {
                "Backup contains duplicate medication doses for one day"
            }
        }

        requireUniquePositiveIds(data.symptomDefinitions.map { it.id }, "symptom definition")
        val symptomNames = mutableSetOf<String>()
        data.symptomDefinitions.forEachIndexed { index, symptom ->
            require(symptom.name.isNotBlank() && symptom.displayName.isNotBlank()) {
                "Symptom definition $index has an empty name"
            }
            require(symptomNames.add(symptom.name)) {
                "Backup contains duplicate symptom definition names"
            }
            try {
                SymptomCategory.valueOf(symptom.category)
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("Symptom definition $index has an unknown category", error)
            }
        }

        data.preferences?.let { preferences ->
            require(preferences.periodLogReminderTimeMinutes in 0L..1439L) {
                "Backup has an invalid period reminder time"
            }
        }
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
