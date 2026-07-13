package com.lunarlog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.lunarlog.data.backup.BackupPreferencesDto

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
    private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    private val APP_LOCK_MODE = stringPreferencesKey("app_lock_mode")
    private val APP_LOCK_TIMEOUT_SECONDS = longPreferencesKey("app_lock_timeout_seconds")
    private val THEME_SEED_COLOR = longPreferencesKey("theme_seed_color") // Store ARGB
    private val PERIOD_LOG_REMINDER_ENABLED = booleanPreferencesKey("period_log_reminder_enabled")
    private val PERIOD_LOG_REMINDER_TIME_MINUTES = longPreferencesKey("period_log_reminder_time_minutes")
    private val CYCLE_NOTIFICATION_ENABLED = booleanPreferencesKey("cycle_notification_enabled")

    val isFirstRun: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_FIRST_RUN] ?: true
        }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[APP_LOCK_ENABLED] ?: false
        }

    val appLockMode: Flow<AppLockMode> = context.dataStore.data
        .map { preferences ->
            val stored = preferences[APP_LOCK_MODE]
            if (stored != null) {
                try {
                    AppLockMode.valueOf(stored)
                } catch (_: IllegalArgumentException) {
                    AppLockMode.NONE
                }
            } else {
                // Backward compatibility with old boolean switch.
                if (preferences[APP_LOCK_ENABLED] == true) {
                    AppLockMode.BIOMETRIC_REQUIRED
                } else {
                    AppLockMode.NONE
                }
            }
        }

    val appLockTimeoutSeconds: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[APP_LOCK_TIMEOUT_SECONDS] ?: 0L
        }
    
    val themeSeedColor: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_SEED_COLOR]
        }

    val periodLogReminderEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PERIOD_LOG_REMINDER_ENABLED] ?: false
        }

    /**
     * Minutes from midnight in the user's local time zone. Default: 8:00 PM.
     */
    val periodLogReminderTimeMinutes: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[PERIOD_LOG_REMINDER_TIME_MINUTES] ?: (20L * 60L)
        }

    val cycleNotificationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CYCLE_NOTIFICATION_ENABLED] ?: false
        }
    
    suspend fun setFirstRunComplete() {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_RUN] = false
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED] = enabled
            preferences[APP_LOCK_MODE] = if (enabled) AppLockMode.BIOMETRIC_REQUIRED.name else AppLockMode.NONE.name
        }
    }

    suspend fun setAppLockMode(mode: AppLockMode) {
        context.dataStore.edit { preferences ->
            preferences[APP_LOCK_MODE] = mode.name
            preferences[APP_LOCK_ENABLED] = mode != AppLockMode.NONE
        }
    }

    suspend fun setAppLockTimeoutSeconds(seconds: Long) {
        context.dataStore.edit { preferences ->
            preferences[APP_LOCK_TIMEOUT_SECONDS] = seconds.coerceAtLeast(0L)
        }
    }

    suspend fun setThemeSeedColor(color: Long) {
        context.dataStore.edit { preferences ->
            preferences[THEME_SEED_COLOR] = color
        }
    }

    suspend fun setPeriodLogReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PERIOD_LOG_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setPeriodLogReminderTimeMinutes(minutes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PERIOD_LOG_REMINDER_TIME_MINUTES] = minutes
        }
    }

    suspend fun setCycleNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CYCLE_NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun getPeriodLogReminderEnabledSync(): Boolean {
        return context.dataStore.data.first()[PERIOD_LOG_REMINDER_ENABLED] ?: false
    }

    suspend fun getPeriodLogReminderTimeMinutesSync(): Long {
        return context.dataStore.data.first()[PERIOD_LOG_REMINDER_TIME_MINUTES] ?: (20L * 60L)
    }

    suspend fun getCycleNotificationEnabledSync(): Boolean {
        return context.dataStore.data.first()[CYCLE_NOTIFICATION_ENABLED] ?: false
    }

    suspend fun createBackupPreferences(): BackupPreferencesDto {
        val preferences = context.dataStore.data.first()
        return BackupPreferencesDto(
            themeSeedColor = preferences[THEME_SEED_COLOR],
            periodLogReminderEnabled = preferences[PERIOD_LOG_REMINDER_ENABLED] ?: false,
            periodLogReminderTimeMinutes = preferences[PERIOD_LOG_REMINDER_TIME_MINUTES] ?: (20L * 60L),
            cycleNotificationEnabled = preferences[CYCLE_NOTIFICATION_ENABLED] ?: false
        )
    }

    suspend fun restoreBackupPreferences(backup: BackupPreferencesDto) {
        context.dataStore.edit { preferences ->
            val theme = backup.themeSeedColor
            if (theme == null) preferences.remove(THEME_SEED_COLOR) else preferences[THEME_SEED_COLOR] = theme
            preferences[PERIOD_LOG_REMINDER_ENABLED] = backup.periodLogReminderEnabled
            preferences[PERIOD_LOG_REMINDER_TIME_MINUTES] =
                backup.periodLogReminderTimeMinutes.coerceIn(0L, (24L * 60L) - 1L)
            preferences[CYCLE_NOTIFICATION_ENABLED] = backup.cycleNotificationEnabled
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
