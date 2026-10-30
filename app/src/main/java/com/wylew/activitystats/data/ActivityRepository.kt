package com.wylew.activitystats.data

import com.wylew.activitystats.data.healthconnect.HcStatus
import com.wylew.activitystats.data.healthconnect.HealthConnectSource
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.Dedupe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface SyncResult {
    data class Success(val fetched: Int) : SyncResult
    data object NotConnected : SyncResult
    data class Failed(val message: String) : SyncResult
}

/**
 * Reads workouts from Health Connect (where Garmin Connect and Strava both write) and serves
 * one list with each workout once: when the same run arrives from both apps, the Garmin
 * copy is kept and the Strava title carried over.
 */
class ActivityRepository(
    private val healthConnect: HealthConnectSource,
    private val store: ActivityStore,
    private val settings: SettingsStore,
) {
    private val syncLock = Mutex()

    /** Everything cached, before duplicates are merged. */
    val raw: Flow<List<Activity>> = store.activities

    /** Each workout once, newest first. */
    val activities: Flow<List<Activity>> = store.activities.map { Dedupe.apply(it) }

    suspend fun all(): List<Activity> = Dedupe.apply(store.all())

    /**
     * First run reads from January 1 of last year (older data needs the history permission).
     * After that it re-reads the last three days, so late writes and edits are picked up.
     *
     * @param background true from WorkManager; Health Connect only allows background reads
     *   when "Access data in the background" is granted, so we skip otherwise.
     */
    suspend fun sync(background: Boolean = false): SyncResult = syncLock.withLock {
        if (healthConnect.status() != HcStatus.READY) return@withLock SyncResult.NotConnected
        if (background && !healthConnect.canReadInBackground()) return@withLock SyncResult.NotConnected
        try {
            val newest = store.all().maxOfOrNull { it.startEpochSec }
            val since = newest?.minus(3 * 86_400) ?: backfillStartSec()
            val fetched = healthConnect.readActivities(Instant.ofEpochSecond(since))
            store.upsert(fetched)
            settings.setLastSync(System.currentTimeMillis())
            SyncResult.Success(fetched.size)
        } catch (e: SecurityException) {
            SyncResult.Failed("Health Connect access was removed. Allow it again in Settings.")
        } catch (e: Exception) {
            SyncResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun backfillStartSec(): Long =
        LocalDate.now().minusYears(1).withDayOfYear(1)
            .atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
}
