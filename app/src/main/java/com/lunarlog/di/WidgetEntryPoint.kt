package com.lunarlog.di

import com.lunarlog.data.AppDatabase
import com.lunarlog.data.CycleRepository
import com.lunarlog.data.DailyLogRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun appDatabase(): AppDatabase
    fun cycleRepository(): CycleRepository
    fun dailyLogRepository(): DailyLogRepository
}

