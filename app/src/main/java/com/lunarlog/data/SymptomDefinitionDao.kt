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

    @Query("SELECT * FROM symptom_definitions WHERE category = :category ORDER BY displayName")
    fun getSymptomsByCategory(category: SymptomCategory): Flow<List<SymptomDefinition>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(symptom: SymptomDefinition)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(symptoms: List<SymptomDefinition>)

    @Query("SELECT * FROM symptom_definitions WHERE name = :name LIMIT 1")
    suspend fun getSymptomByName(name: String): SymptomDefinition?
}
