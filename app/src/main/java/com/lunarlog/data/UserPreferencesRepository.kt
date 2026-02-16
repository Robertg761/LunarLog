package com.lunarlog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
    private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    private val THEME_SEED_COLOR = longPreferencesKey("theme_seed_color") // Store ARGB
    private val PERIOD_LOG_REMINDER_ENABLED = booleanPreferencesKey("period_log_reminder_enabled")
    private val PERIOD_LOG_REMINDER_TIME_MINUTES = longPreferencesKey("period_log_reminder_time_minutes")

    val isFirstRun: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_FIRST_RUN] ?: true
        }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[APP_LOCK_ENABLED] ?: false
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
    
    suspend fun setFirstRunComplete() {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_RUN] = false
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED] = enabled
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

    suspend fun getPeriodLogReminderEnabledSync(): Boolean {
        return context.dataStore.data.first()[PERIOD_LOG_REMINDER_ENABLED] ?: false
    }

    suspend fun getPeriodLogReminderTimeMinutesSync(): Long {
        return context.dataStore.data.first()[PERIOD_LOG_REMINDER_TIME_MINUTES] ?: (20L * 60L)
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
