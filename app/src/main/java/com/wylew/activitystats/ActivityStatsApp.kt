package com.wylew.activitystats

import android.app.Application
import android.content.Context
import com.wylew.activitystats.data.ActivityRepository
import com.wylew.activitystats.data.ActivityStore
import com.wylew.activitystats.data.SettingsStore
import com.wylew.activitystats.data.healthconnect.HealthConnectSource
import com.wylew.activitystats.sync.SyncScheduler
import kotlinx.serialization.json.Json
import java.io.File

class ActivityStatsApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        SyncScheduler.schedulePeriodic(this)
    }
}

/** Hand-rolled dependency container; small enough not to need Hilt yet. */
class AppGraph(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    val settings = SettingsStore(context)
    val healthConnect = HealthConnectSource(context)
    val store = ActivityStore(File(context.filesDir, "activities.json"), json)
    val repository = ActivityRepository(healthConnect, store, settings)
}

val Context.graph: AppGraph get() = (applicationContext as ActivityStatsApp).graph
