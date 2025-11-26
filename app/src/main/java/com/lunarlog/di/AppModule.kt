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
        .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8)
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
    fun provideLogEntryDao(database: AppDatabase): com.lunarlog.data.LogEntryDao {
        return database.logEntryDao()
    }

    @Provides
    @Singleton
    fun provideSymptomRepository(symptomDao: SymptomDefinitionDao): SymptomRepository {
        return SymptomRepositoryImpl(symptomDao)
    }
}
