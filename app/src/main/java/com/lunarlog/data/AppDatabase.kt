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
        LogEntry::class,
        Medication::class,
        MedicationLog::class,
        SymptomDefinition::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun medicationDao(): MedicationDao
    abstract fun symptomDefinitionDao(): SymptomDefinitionDao

    companion object {
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop manual triggers
                db.execSQL("DROP TRIGGER IF EXISTS daily_logs_ai")
                db.execSQL("DROP TRIGGER IF EXISTS daily_logs_ad")
                db.execSQL("DROP TRIGGER IF EXISTS daily_logs_au")

                // Drop Room-generated triggers (in case they exist and are broken)
                db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_daily_logs_fts_BEFORE_UPDATE")
                db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_daily_logs_fts_BEFORE_DELETE")
                db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_daily_logs_fts_AFTER_UPDATE")
                db.execSQL("DROP TRIGGER IF EXISTS room_fts_content_sync_daily_logs_fts_AFTER_INSERT")

                // Drop and recreate FTS table
                db.execSQL("DROP TABLE IF EXISTS daily_logs_fts")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `daily_logs_fts` USING FTS4(`date` INTEGER NOT NULL, `notes` TEXT NOT NULL, content=`daily_logs`)")
                
                // Rebuild index
                db.execSQL("INSERT INTO daily_logs_fts(daily_logs_fts) VALUES ('rebuild')")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
               // 1. Drop the incorrect triggers from MIGRATION_4_5 
               db.execSQL("DROP TRIGGER IF EXISTS daily_logs_ai")
               db.execSQL("DROP TRIGGER IF EXISTS daily_logs_ad")
               db.execSQL("DROP TRIGGER IF EXISTS daily_logs_au")
               
               // 2. Drop the FTS table
               db.execSQL("DROP TABLE IF EXISTS daily_logs_fts")

               // 3. Recreate FTS table
               db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `daily_logs_fts` USING FTS4(`date` INTEGER NOT NULL, `notes` TEXT NOT NULL, content=`daily_logs`)")
               
               // 4. Rebuild index
               db.execSQL("INSERT INTO daily_logs_fts(daily_logs_fts) VALUES ('rebuild')")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `log_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `date` INTEGER NOT NULL, 
                        `time` INTEGER NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `value` TEXT NOT NULL, 
                        `details` TEXT
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_log_entries_date` ON `log_entries` (`date`)")
            }
        }

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
                        INSERT INTO daily_logs_fts(docid, date, notes) VALUES(new.date, new.date, new.notes);
                    END
                """)
                
                // Delete
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS daily_logs_ad AFTER DELETE ON daily_logs BEGIN
                        INSERT INTO daily_logs_fts(daily_logs_fts, docid, date, notes) VALUES('delete', old.date, old.date, old.notes);
                    END
                """)
                
                // Update
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS daily_logs_au AFTER UPDATE ON daily_logs BEGIN
                        INSERT INTO daily_logs_fts(daily_logs_fts, docid, date, notes) VALUES('delete', old.date, old.date, old.notes);
                        INSERT INTO daily_logs_fts(docid, date, notes) VALUES(new.date, new.date, new.notes);
                    END
                """)
            }
        }
    }
}
