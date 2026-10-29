package com.wylew.activitystats.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wylew.activitystats.domain.Units
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
    }
}
