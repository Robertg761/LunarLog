package com.lunarlog.di

import android.content.Context
import androidx.room.Room
import com.lunarlog.data.AppDatabase
import com.lunarlog.data.CycleDao
import com.lunarlog.data.DailyLogDao
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
        .fallbackToDestructiveMigration()
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
}
