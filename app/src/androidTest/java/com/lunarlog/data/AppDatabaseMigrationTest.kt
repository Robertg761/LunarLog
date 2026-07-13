package com.lunarlog.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @After
    fun cleanUp() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrate8To9_deduplicatesLogs_dropsOrphans_andEnforcesConstraints() {
        migrationHelper.createDatabase(TEST_DATABASE, 8).apply {
            execSQL(
                """
                INSERT INTO medications
                    (id, name, dosage, frequency, startDate, endDate, reminderTime)
                VALUES
                    (1, 'Medication', '10 mg', 'daily', 20000, NULL, 480)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO medication_logs (id, date, medicationId, taken, timestamp)
                VALUES
                    (1, 20001, 1, 0, 100),
                    (2, 20001, 1, 1, 200),
                    (3, 20002, 999, 1, 300)
                """.trimIndent()
            )
            close()
        }

        val migrated = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            9,
            true,
            AppDatabase.MIGRATION_8_9
        )

        assertEquals(
            1,
            migrated.longQuery(
                "SELECT COUNT(*) FROM medication_logs WHERE date = 20001 AND medicationId = 1"
            )
        )
        assertEquals(
            200,
            migrated.longQuery(
                "SELECT timestamp FROM medication_logs WHERE date = 20001 AND medicationId = 1"
            )
        )
        assertEquals(
            1,
            migrated.longQuery(
                "SELECT taken FROM medication_logs WHERE date = 20001 AND medicationId = 1"
            )
        )
        assertEquals(
            0,
            migrated.longQuery("SELECT COUNT(*) FROM medication_logs WHERE medicationId = 999")
        )

        assertThrows(SQLiteConstraintException::class.java) {
            migrated.execSQL(
                """
                INSERT INTO medication_logs (date, medicationId, taken, timestamp)
                VALUES (20001, 1, 1, 400)
                """.trimIndent()
            )
        }

        // MigrationTestHelper opens a raw SupportSQLiteDatabase; mirror Room's normal
        // connection setup before exercising the declared ON DELETE CASCADE behavior.
        migrated.execSQL("PRAGMA foreign_keys = ON")
        assertEquals(1, migrated.longQuery("PRAGMA foreign_keys"))
        migrated.execSQL("DELETE FROM medications WHERE id = 1")
        assertEquals(
            0,
            migrated.longQuery("SELECT COUNT(*) FROM medication_logs WHERE medicationId = 1")
        )
        migrated.close()
    }

    private fun SupportSQLiteDatabase.longQuery(sql: String): Long =
        query(sql).use { cursor ->
            check(cursor.moveToFirst()) { "Query returned no rows: $sql" }
            cursor.getLong(0)
        }

    private companion object {
        const val TEST_DATABASE = "lunar-log-migration-test"
    }
}
