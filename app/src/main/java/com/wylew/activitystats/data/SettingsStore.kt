package com.wylew.activitystats.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.Goals
import com.wylew.activitystats.domain.PreferOrigin
import com.wylew.activitystats.domain.SourceChoice
import com.wylew.activitystats.domain.SourcePrefs
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.theme.ColorSource
import com.wylew.activitystats.theme.ThemeMode
import com.wylew.activitystats.theme.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class StravaTokens(val access: String, val refresh: String, val expiresAtSec: Long)

data class Connection(val connected: Boolean, val athleteName: String?, val lastSyncMs: Long?)

/**
 * App settings and Strava tokens.
 *
 * Tokens sit in app-private DataStore, which is fine for a personal app. Move them to
 * an encrypted store before distributing builds to anyone else.
 */
class SettingsStore(private val context: Context) {

    private val data: Flow<Preferences> get() = context.dataStore.data

    val tokens: Flow<StravaTokens?> = data.map { p ->
        val a = p[ACCESS]
        val r = p[REFRESH]
        val e = p[EXPIRES]
        if (a != null && r != null && e != null) StravaTokens(a, r, e) else null
    }.distinctUntilChanged()

    val connection: Flow<Connection> = data.map { p ->
        Connection(
            connected = p[REFRESH] != null,
            athleteName = p[ATHLETE],
            lastSyncMs = p[LAST_SYNC],
        )
    }.distinctUntilChanged()

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

    val sources: Flow<SourcePrefs> = data.map { p ->
        val d = SourcePrefs()
        SourcePrefs(
            choice = p[SOURCE_CHOICE]?.let { v -> SourceChoice.entries.firstOrNull { it.name == v } } ?: d.choice,
            prefer = p[PREFER_ORIGIN]?.let { v -> PreferOrigin.entries.firstOrNull { it.name == v } } ?: d.prefer,
        )
    }.distinctUntilChanged()

    suspend fun setSources(s: SourcePrefs) {
        context.dataStore.edit {
            it[SOURCE_CHOICE] = s.choice.name
            it[PREFER_ORIGIN] = s.prefer.name
        }
    }

    /** False until the first-run "Where do your activities come from?" screen is done. */
    val onboarded: Flow<Boolean> = data.map { it[ONBOARDED] ?: false }.distinctUntilChanged()

    suspend fun setOnboarded() {
        context.dataStore.edit { it[ONBOARDED] = true }
    }

    val goals: Flow<Goals> = data.map { p ->
        val d = Goals()
        Goals(
            runWeeklyM = p[GOAL_RUN] ?: d.runWeeklyM,
            rideWeeklyM = p[GOAL_RIDE] ?: d.rideWeeklyM,
            allWeeklyM = p[GOAL_ALL] ?: d.allWeeklyM,
        )
    }.distinctUntilChanged()

    suspend fun setWeeklyGoal(filter: ActivityFilter, meters: Double) {
        val key = when (filter) {
            ActivityFilter.RUN -> GOAL_RUN
            ActivityFilter.RIDE -> GOAL_RIDE
            ActivityFilter.ALL -> GOAL_ALL
        }
        context.dataStore.edit { it[key] = meters }
    }

    suspend fun setTheme(t: ThemeSettings) {
        context.dataStore.edit {
            it[THEME_MODE] = t.mode.name
            it[COLOR_SOURCE] = t.source.name
            it[SEED_PRIMARY] = t.primarySeed
            it[SEED_ACCENT] = t.accentSeed
        }
    }

    suspend fun saveTokens(t: StravaTokens) {
        context.dataStore.edit {
            it[ACCESS] = t.access
            it[REFRESH] = t.refresh
            it[EXPIRES] = t.expiresAtSec
        }
    }

    suspend fun saveAthlete(name: String?) {
        context.dataStore.edit { if (name == null) it.remove(ATHLETE) else it[ATHLETE] = name }
    }

    suspend fun clearAuth() {
        context.dataStore.edit {
            it.remove(ACCESS); it.remove(REFRESH); it.remove(EXPIRES); it.remove(ATHLETE); it.remove(LAST_SYNC)
        }
    }

    suspend fun setLastSync(ms: Long) {
        context.dataStore.edit { it[LAST_SYNC] = ms }
    }

    suspend fun setUnits(units: Units) {
        context.dataStore.edit { it[UNITS] = units.name }
    }

    private companion object {
        val ACCESS = stringPreferencesKey("strava_access")
        val REFRESH = stringPreferencesKey("strava_refresh")
        val EXPIRES = longPreferencesKey("strava_expires_at")
        val ATHLETE = stringPreferencesKey("strava_athlete")
        val LAST_SYNC = longPreferencesKey("last_sync_ms")
        val UNITS = stringPreferencesKey("units")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SOURCE_CHOICE = stringPreferencesKey("source_choice")
        val PREFER_ORIGIN = stringPreferencesKey("prefer_origin")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val GOAL_RUN = doublePreferencesKey("goal_run_weekly_m")
        val GOAL_RIDE = doublePreferencesKey("goal_ride_weekly_m")
        val GOAL_ALL = doublePreferencesKey("goal_all_weekly_m")
        val COLOR_SOURCE = stringPreferencesKey("color_source")
        val SEED_PRIMARY = intPreferencesKey("seed_primary")
        val SEED_ACCENT = intPreferencesKey("seed_accent")
    }
}
