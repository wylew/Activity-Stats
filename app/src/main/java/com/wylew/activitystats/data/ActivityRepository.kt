package com.wylew.activitystats.data

import com.wylew.activitystats.data.strava.NotConnectedException
import com.wylew.activitystats.data.strava.StravaApi
import com.wylew.activitystats.data.strava.StravaAuth
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

sealed interface SyncResult {
    data class Success(val fetched: Int) : SyncResult
    data object NotConnected : SyncResult
    data class RateLimited(val retryAfterSec: Long) : SyncResult
    data class Failed(val message: String) : SyncResult
}

class ActivityRepository(
    private val api: StravaApi,
    private val auth: StravaAuth,
    private val store: ActivityStore,
    private val settings: SettingsStore,
) {
    private val syncLock = Mutex()

    val activities: Flow<List<Activity>> = store.activities

    /**
     * Pulls new Strava activities.
     *
     * First run backfills from January 1 of last year (enough for year-over-year comparisons).
     * After that it re-reads the last three days, so edits made just after upload (title,
     * sport type, a cropped GPS track) are picked up.
     */
    suspend fun sync(): SyncResult = syncLock.withLock {
        syncInternal()
    }

    private suspend fun syncInternal(): SyncResult {
        return try {
            if (auth.validAccessToken() == null) throw NotConnectedException()
            val newest = store.all().filter { it.source == Source.STRAVA }.maxOfOrNull { it.startEpochSec }
            val after = newest?.minus(3 * 86_400) ?: backfillStartSec()

            val fetched = mutableListOf<Activity>()
            var page = 1
            while (page <= MAX_PAGES) {
                val batch = api.athleteActivities(after, page)
                fetched += batch.map { it.toDomain() }
                if (batch.size < StravaApi.PER_PAGE) break
                page++
            }
            store.upsert(fetched)
            settings.setLastSync(System.currentTimeMillis())
            SyncResult.Success(fetched.size)
        } catch (e: NotConnectedException) {
            SyncResult.NotConnected
        } catch (e: RateLimitedException) {
            SyncResult.RateLimited(e.retryAfterSec)
        } catch (e: Exception) {
            SyncResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun disconnectStrava() {
        auth.disconnect()
        store.clear(Source.STRAVA)
    }

    private fun backfillStartSec(): Long =
        LocalDate.now().minusYears(1).withDayOfYear(1)
            .atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

    private companion object {
        /** 10 pages x 200 = 2,000 activities per sync, well under Strava's read limits. */
        const val MAX_PAGES = 10
    }
}
