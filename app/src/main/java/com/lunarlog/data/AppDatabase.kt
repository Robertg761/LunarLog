package com.lunarlog.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lunarlog.logic.Converters

@Database(
    entities = [
        Cycle::class,
        DailyLog::class,
        Medication::class,
        MedicationLog::class,
        SymptomDefinition::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun medicationDao(): MedicationDao
    abstract fun symptomDefinitionDao(): SymptomDefinitionDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `symptom_definitions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `displayName` TEXT NOT NULL, 
                        `category` TEXT NOT NULL, 
                        `isCustom` INTEGER NOT NULL
                    )
                    """
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_symptom_definitions_name` ON `symptom_definitions` (`name`)")
            }
        }
    }
}
