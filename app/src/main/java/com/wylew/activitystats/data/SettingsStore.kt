package com.wylew.activitystats.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.theme.ColorSource
import com.wylew.activitystats.theme.ThemeMode
import com.wylew.activitystats.theme.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** App settings: units, theme, last sync time. */
class SettingsStore(private val context: Context) {

    private val data: Flow<Preferences> get() = context.dataStore.data

    val lastSyncMs: Flow<Long?> = data.map { it[LAST_SYNC] }.distinctUntilChanged()

    val units: Flow<Units> = data.map { p ->
        p[UNITS]?.let { runCatching { Units.valueOf(it) }.getOrNull() } ?: Units.IMPERIAL
    }.distinctUntilChanged()

    val theme: Flow<ThemeSettings> = data.map { p ->
        val d = ThemeSettings()
        ThemeSettings(
            mode = p[THEME_MODE]?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: d.mode,
            source = p[COLOR_SOURCE]?.let { v -> ColorSource.entries.firstOrNull { it.name == v } } ?: d.source,
            primarySeed = p[SEED_PRIMARY] ?: d.primarySeed,
            accentSeed = p[SEED_ACCENT] ?: d.accentSeed,
        )
    }.distinctUntilChanged()

    suspend fun setTheme(t: ThemeSettings) {
        context.dataStore.edit {
            it[THEME_MODE] = t.mode.name
            it[COLOR_SOURCE] = t.source.name
            it[SEED_PRIMARY] = t.primarySeed
            it[SEED_ACCENT] = t.accentSeed
        }
    }

    suspend fun setLastSync(ms: Long) {
        context.dataStore.edit { it[LAST_SYNC] = ms }
    }

    suspend fun setUnits(units: Units) {
        context.dataStore.edit { it[UNITS] = units.name }
    }

    private companion object {
        val LAST_SYNC = longPreferencesKey("last_sync_ms")
        val UNITS = stringPreferencesKey("units")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_SOURCE = stringPreferencesKey("color_source")
        val SEED_PRIMARY = intPreferencesKey("seed_primary")
        val SEED_ACCENT = intPreferencesKey("seed_accent")
    }
}
