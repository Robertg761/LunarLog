package com.lunarlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomDefinitionDao {
    @Query("SELECT * FROM symptom_definitions ORDER BY category, displayName")
    fun getAllSymptoms(): Flow<List<SymptomDefinition>>

    @Query("SELECT * FROM symptom_definitions WHERE isArchived = 0 ORDER BY category, displayName")
    fun getActiveSymptoms(): Flow<List<SymptomDefinition>>

    @Query("UPDATE symptom_definitions SET displayName = :label WHERE id = :id AND isCustom = 1")
    suspend fun renameCustom(id: Long, label: String)

    @Query("UPDATE symptom_definitions SET isArchived = :archived WHERE id = :id AND isCustom = 1")
    suspend fun archiveCustom(id: Long, archived: Boolean)


    @Query("SELECT * FROM symptom_definitions ORDER BY category, displayName")
    suspend fun getAllSymptomsSync(): List<SymptomDefinition>

    @Query("SELECT * FROM symptom_definitions WHERE category = :category ORDER BY displayName")
    fun getSymptomsByCategory(category: SymptomCategory): Flow<List<SymptomDefinition>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(symptom: SymptomDefinition)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(symptoms: List<SymptomDefinition>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(symptoms: List<SymptomDefinition>)

    @Query("SELECT * FROM symptom_definitions WHERE name = :name LIMIT 1")
    suspend fun getSymptomByName(name: String): SymptomDefinition?

    @Query("DELETE FROM symptom_definitions WHERE name IN (:names) AND isCustom = 0")
    suspend fun deleteNonCustomByNames(names: Set<String>)
}
