package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "simple_player_settings")

/**
 * Repository to persist and observe user playback preferences using Jetpack Preferences DataStore.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_DOUBLE_TAP_SEEK_SECONDS = intPreferencesKey("double_tap_seek_seconds")
        val KEY_LONG_PRESS_SPEED_MULTIPLIER = floatPreferencesKey("long_press_speed_multiplier")

        const val DEFAULT_SEEK_SECONDS = 10
        const val DEFAULT_SPEED_MULTIPLIER = 2.0f

        val AVAILABLE_LONG_PRESS_SPEEDS = listOf(1.5f, 2.0f, 2.5f, 3.0f)
    }

    /**
     * Flow of double-tap seek duration in seconds (range 5 to 60, default 10).
     */
    val seekDurationSecondsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_DOUBLE_TAP_SEEK_SECONDS] ?: DEFAULT_SEEK_SECONDS
    }

    /**
     * Flow of long-press playback speed multiplier (1.5x, 2.0x, 2.5x, 3.0x, default 2.0x).
     */
    val longPressSpeedMultiplierFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_LONG_PRESS_SPEED_MULTIPLIER] ?: DEFAULT_SPEED_MULTIPLIER
    }

    /**
     * Updates the double-tap seek duration in seconds (clamped to 5..60).
     */
    suspend fun setSeekDurationSeconds(seconds: Int) {
        val coerced = seconds.coerceIn(5, 60)
        context.dataStore.edit { preferences ->
            preferences[KEY_DOUBLE_TAP_SEEK_SECONDS] = coerced
        }
    }

    /**
     * Updates the long-press speed multiplier.
     */
    suspend fun setLongPressSpeedMultiplier(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LONG_PRESS_SPEED_MULTIPLIER] = speed
        }
    }

    /**
     * Resets settings to default values.
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences[KEY_DOUBLE_TAP_SEEK_SECONDS] = DEFAULT_SEEK_SECONDS
            preferences[KEY_LONG_PRESS_SPEED_MULTIPLIER] = DEFAULT_SPEED_MULTIPLIER
        }
    }
}
