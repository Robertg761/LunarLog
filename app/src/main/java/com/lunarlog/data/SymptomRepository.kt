package com.lunarlog.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SymptomRepository {
    fun getAllSymptoms(): Flow<List<SymptomDefinition>>
    fun getSymptomsByCategory(category: SymptomCategory): Flow<List<SymptomDefinition>>
    suspend fun addCustomSymptom(name: String, category: SymptomCategory)
}

@Singleton
class SymptomRepositoryImpl @Inject constructor(
    private val symptomDao: SymptomDefinitionDao
) : SymptomRepository {

    override fun getAllSymptoms(): Flow<List<SymptomDefinition>> {
        return symptomDao.getAllSymptoms()
    }

    override fun getSymptomsByCategory(category: SymptomCategory): Flow<List<SymptomDefinition>> {
        return symptomDao.getSymptomsByCategory(category)
    }

    override suspend fun addCustomSymptom(name: String, category: SymptomCategory) {
        // Check if exists to avoid error, though IGNORE strategy handles it, we might want to return something
        val existing = symptomDao.getSymptomByName(name)
        if (existing == null) {
            val newSymptom = SymptomDefinition(
                name = name,
                displayName = name, // Use name as display name for custom
                category = category,
                isCustom = true
            )
            symptomDao.insert(newSymptom)
        }
    }
}
