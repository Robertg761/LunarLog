package com.lunarlog.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class DatabaseInitializer @Inject constructor(
    private val symptomDao: SymptomDefinitionDao
) {

    suspend fun initialize() = withContext(Dispatchers.IO) {
        // Check if any symptoms exist using a lightweight query
        // We can't use 'count(*)' easily with Dao unless we add a method, 
        // but 'getAllSymptoms' returns a Flow. 
        // We need a suspend function to check count or existence.
        // Let's rely on insertAll with ON CONFLICT IGNORE which handles it efficiently.
        // Or checking one by one. But inserting all with ignore is safest for "defaults".
        
        symptomDao.insertAll(SymptomData.defaultSymptoms)
    }
}
