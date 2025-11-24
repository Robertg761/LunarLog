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
        DailyLogFts::class,
        Medication::class,
        MedicationLog::class,
        SymptomDefinition::class
    ],
    version = 5,
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create FTS table
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `daily_logs_fts` USING FTS4(content=`daily_logs`, date, notes)")
                
                // Populate with existing data
                db.execSQL("INSERT INTO daily_logs_fts(daily_logs_fts) VALUES ('rebuild')")

                // Triggers to keep FTS in sync
                // Insert
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS daily_logs_ai AFTER INSERT ON daily_logs BEGIN
                        INSERT INTO daily_logs_fts(rowid, date, notes) VALUES(new.date, new.date, new.notes);
                    END
                """)
                
                // Delete
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS daily_logs_ad AFTER DELETE ON daily_logs BEGIN
                        INSERT INTO daily_logs_fts(daily_logs_fts, rowid, date, notes) VALUES('delete', old.date, old.date, old.notes);
                    END
                """)
                
                // Update
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS daily_logs_au AFTER UPDATE ON daily_logs BEGIN
                        INSERT INTO daily_logs_fts(daily_logs_fts, rowid, date, notes) VALUES('delete', old.date, old.date, old.notes);
                        INSERT INTO daily_logs_fts(rowid, date, notes) VALUES(new.date, new.date, new.notes);
                    END
                """)
            }
        }
    }
}
