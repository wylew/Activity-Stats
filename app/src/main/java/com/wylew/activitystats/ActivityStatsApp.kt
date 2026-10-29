package com.wylew.activitystats

import android.app.Application
import android.content.Context
import com.wylew.activitystats.data.ActivityRepository
import com.wylew.activitystats.data.ActivityStore
import com.wylew.activitystats.data.SettingsStore
import com.wylew.activitystats.data.strava.StravaApi
import com.wylew.activitystats.data.strava.StravaAuth
import com.wylew.activitystats.sync.SyncScheduler
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

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
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    val settings = SettingsStore(context)
    val auth = StravaAuth(settings, http, json)
    val api = StravaApi(http, json, auth)
    val store = ActivityStore(File(context.filesDir, "activities.json"), json)
    val repository = ActivityRepository(api, auth, store, settings)
}

val Context.graph: AppGraph get() = (applicationContext as ActivityStatsApp).graph
