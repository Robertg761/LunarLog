package com.lunarlog.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SymptomRepository {
    fun getAllSymptoms(): Flow<List<SymptomDefinition>>
    fun getManagedSymptoms(): Flow<List<SymptomDefinition>>
    suspend fun renameCustom(id: Long, label: String)
    suspend fun archiveCustom(id: Long, archived: Boolean)
    fun getSymptomsByCategory(category: SymptomCategory): Flow<List<SymptomDefinition>>
    suspend fun addCustomSymptom(name: String, category: SymptomCategory)
}

@Singleton
class SymptomRepositoryImpl @Inject constructor(
    private val symptomDao: SymptomDefinitionDao
) : SymptomRepository {

    override fun getAllSymptoms(): Flow<List<SymptomDefinition>> {
        return symptomDao.getActiveSymptoms()
    }

    override fun getManagedSymptoms() = symptomDao.getAllSymptoms()

    override suspend fun renameCustom(id: Long, label: String) {
        val normalized = label.trim().replace(Regex("\\s+"), " ").take(50)
        require(normalized.isNotBlank()) { "Name is required" }
        symptomDao.renameCustom(id, normalized)
    }

    override suspend fun archiveCustom(id: Long, archived: Boolean) = symptomDao.archiveCustom(id, archived)

    override fun getSymptomsByCategory(category: SymptomCategory): Flow<List<SymptomDefinition>> {
        return symptomDao.getSymptomsByCategory(category)
    }

    override suspend fun addCustomSymptom(name: String, category: SymptomCategory) {
        // Check if exists to avoid error, though IGNORE strategy handles it, we might want to return something
        val existing = symptomDao.getSymptomByName(name)
        if (existing?.isArchived == true && existing.isCustom) symptomDao.archiveCustom(existing.id, false)
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
