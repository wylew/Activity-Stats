package com.wylew.activitystats.data

import com.wylew.activitystats.data.healthconnect.HcStatus
import com.wylew.activitystats.data.healthconnect.HealthConnectSource
import com.wylew.activitystats.data.strava.NotConnectedException
import com.wylew.activitystats.data.strava.StravaApi
import com.wylew.activitystats.data.strava.StravaAuth
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityView
import com.wylew.activitystats.domain.SourceChoice
import com.wylew.activitystats.domain.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface SyncResult {
    data class Success(val fetched: Int) : SyncResult
    data object NotConnected : SyncResult
    data class RateLimited(val retryAfterSec: Long) : SyncResult
    data class Failed(val message: String) : SyncResult
}

/**
 * Pulls from every connected source and serves one de-duplicated list.
 *
 * Health Connect is the main source (Garmin Connect and Strava both write to it, for free).
 * The Strava API is optional and only works with a Strava subscription.
 */
class ActivityRepository(
    private val healthConnect: HealthConnectSource,
    private val stravaApi: StravaApi,
    private val stravaAuth: StravaAuth,
    private val store: ActivityStore,
    private val settings: SettingsStore,
) {
    private val syncLock = Mutex()

    /** Everything cached, before source filtering and de-duplication. */
    val raw: Flow<List<Activity>> = store.activities

    /** Activities from the chosen apps, each workout once, newest first. */
    val activities: Flow<List<Activity>> = combine(store.activities, settings.sources) { all, prefs ->
        ActivityView.apply(all, prefs)
    }

    /** Same, with a per-widget source override. */
    fun activities(override: SourceChoice?): Flow<List<Activity>> =
        combine(store.activities, settings.sources) { all, prefs -> ActivityView.apply(all, prefs, override) }

    suspend fun all(override: SourceChoice? = null): List<Activity> =
        ActivityView.apply(store.all(), settings.sources.first(), override)

    /**
     * @param background true from WorkManager. Health Connect only allows background reads
     *   when the user granted "Access data in the background", so we skip it otherwise.
     */
    suspend fun sync(background: Boolean = false): SyncResult = syncLock.withLock {
        val results = mutableListOf<SyncResult>()

        if (healthConnect.status() == HcStatus.READY && (!background || healthConnect.canReadInBackground())) {
            results += syncHealthConnect()
        }
        if (settings.tokens.first() != null) {
            results += syncStrava()
        }

        val result = when {
            results.isEmpty() -> SyncResult.NotConnected
            results.all { it is SyncResult.Success } ->
                SyncResult.Success(results.sumOf { (it as SyncResult.Success).fetched })
            else -> results.first { it !is SyncResult.Success }
        }
        if (results.any { it is SyncResult.Success }) settings.setLastSync(System.currentTimeMillis())
        result
    }

    /**
     * First run reads from January 1 of last year (Health Connect returns older data only with
     * the history permission). After that it re-reads the last three days so late writes and
     * edits from Garmin or Strava are picked up.
     */
    private suspend fun syncHealthConnect(): SyncResult = try {
        val since = sinceFor(Source.HEALTH_CONNECT)
        val fetched = healthConnect.readActivities(Instant.ofEpochSecond(since))
        store.upsert(fetched)
        SyncResult.Success(fetched.size)
    } catch (e: SecurityException) {
        SyncResult.Failed("Health Connect access was removed. Allow it again in Settings.")
    } catch (e: Exception) {
        SyncResult.Failed("Health Connect: ${e.message ?: e.javaClass.simpleName}")
    }

    private suspend fun syncStrava(): SyncResult = try {
        if (stravaAuth.validAccessToken() == null) throw NotConnectedException()
        val after = sinceFor(Source.STRAVA)
        val fetched = mutableListOf<Activity>()
        var page = 1
        while (page <= MAX_STRAVA_PAGES) {
            val batch = stravaApi.athleteActivities(after, page)
            fetched += batch.map { it.toDomain() }
            if (batch.size < StravaApi.PER_PAGE) break
            page++
        }
        store.upsert(fetched)
        SyncResult.Success(fetched.size)
    } catch (e: NotConnectedException) {
        SyncResult.NotConnected
    } catch (e: RateLimitedException) {
        SyncResult.RateLimited(e.retryAfterSec)
    } catch (e: Exception) {
        SyncResult.Failed("Strava: ${e.message ?: e.javaClass.simpleName}")
    }

    private suspend fun sinceFor(source: Source): Long {
        val newest = store.all().filter { it.source == source }.maxOfOrNull { it.startEpochSec }
        return newest?.minus(3 * 86_400) ?: backfillStartSec()
    }

    suspend fun disconnectStrava() {
        stravaAuth.disconnect()
        store.clear(Source.STRAVA)
    }

    private fun backfillStartSec(): Long =
        LocalDate.now().minusYears(1).withDayOfYear(1)
            .atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

    private companion object {
        const val MAX_STRAVA_PAGES = 10
    }
}
