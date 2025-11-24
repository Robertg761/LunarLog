package com.lunarlog.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lunarlog.data.AppDatabase
import com.lunarlog.data.CycleDao
import com.lunarlog.data.DailyLogDao
import com.lunarlog.data.SymptomData
import com.lunarlog.data.SymptomDefinitionDao
import com.lunarlog.data.SymptomRepository
import com.lunarlog.data.SymptomRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lunar_log_database"
        )
        .addMigrations(AppDatabase.MIGRATION_3_4)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                insertDefaultSymptoms(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Check if table is empty (e.g. after migration)
                val cursor = db.query("SELECT count(*) FROM symptom_definitions")
                if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                    insertDefaultSymptoms(db)
                }
                cursor.close()
            }

            private fun insertDefaultSymptoms(db: SupportSQLiteDatabase) {
                db.beginTransaction()
                try {
                    val sql = "INSERT OR IGNORE INTO symptom_definitions (name, displayName, category, isCustom) VALUES (?, ?, ?, 0)"
                    SymptomData.defaultSymptoms.forEach { symptom ->
                        db.execSQL(sql, arrayOf(symptom.name, symptom.displayName, symptom.category.name))
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
        })
        .fallbackToDestructiveMigration() // Keep this for safety if manual migration fails or for dev
        .build()
    }

    @Provides
    fun provideCycleDao(database: AppDatabase): CycleDao {
        return database.cycleDao()
    }

    @Provides
    fun provideDailyLogDao(database: AppDatabase): DailyLogDao {
        return database.dailyLogDao()
    }

    @Provides
    fun provideMedicationDao(database: AppDatabase): com.lunarlog.data.MedicationDao {
        return database.medicationDao()
    }

    @Provides
    fun provideSymptomDao(database: AppDatabase): SymptomDefinitionDao {
        return database.symptomDefinitionDao()
    }

    @Provides
    @Singleton
    fun provideSymptomRepository(symptomDao: SymptomDefinitionDao): SymptomRepository {
        return SymptomRepositoryImpl(symptomDao)
    }
}
