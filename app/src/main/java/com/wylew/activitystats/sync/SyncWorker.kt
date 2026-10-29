package com.wylew.activitystats.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wylew.activitystats.data.SyncResult
import com.wylew.activitystats.graph
import com.wylew.activitystats.widget.Widgets
import java.util.concurrent.TimeUnit

/**
 * Pulls from Strava, then redraws every widget.
 *
 * Runs hourly. That also rolls widgets over after midnight (new day) and on Monday (new week).
 * Strava webhooks would make this instant but need a public server, so polling it is for now.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val result = applicationContext.graph.repository.sync()
        Widgets.updateAll(applicationContext)
        return when (result) {
            is SyncResult.Success, SyncResult.NotConnected -> Result.success()
            is SyncResult.RateLimited, is SyncResult.Failed -> if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

object SyncScheduler {
    private const val PERIODIC = "strava-sync-periodic"
    private const val NOW = "strava-sync-now"

    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(network)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(network).build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
    }
}
