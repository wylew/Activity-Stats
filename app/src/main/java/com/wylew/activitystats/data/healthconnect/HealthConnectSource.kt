package com.wylew.activitystats.data.healthconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityKind
import com.wylew.activitystats.domain.Source
import java.time.Duration
import java.time.Instant

enum class HcStatus {
    /** Not supported on this device or profile. */
    UNAVAILABLE,
    /** Android 12–13: the Health Connect app needs installing or updating. */
    NEEDS_INSTALL,
    /** Installed, but we can't read exercise yet. */
    NO_PERMISSION,
    READY,
}

/**
 * Reads workouts that Garmin Connect, Strava (and anything else) write to Health Connect.
 *
 * No accounts or API keys: everything stays on the phone. Each exercise session is read,
 * then distance, elevation, heart rate and active duration are aggregated over the
 * session's time window from the same app that recorded it.
 */
class HealthConnectSource(private val context: Context) {

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    private val exercise = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val distance = HealthPermission.getReadPermission(DistanceRecord::class)
    private val heartRate = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val elevation = HealthPermission.getReadPermission(ElevationGainedRecord::class)

    /** Everything we ask for. Only [exercise] is required; the rest add detail. */
    val permissions: Set<String> = setOf(
        exercise, distance, heartRate, elevation, PERMISSION_HISTORY, PERMISSION_BACKGROUND,
    )

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context, PROVIDER)

    suspend fun status(): HcStatus = when (sdkStatus()) {
        HealthConnectClient.SDK_UNAVAILABLE -> HcStatus.UNAVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HcStatus.NEEDS_INSTALL
        else -> if (exercise in granted()) HcStatus.READY else HcStatus.NO_PERMISSION
    }

    suspend fun granted(): Set<String> =
        runCatching { client.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())

    suspend fun canReadInBackground(): Boolean = PERMISSION_BACKGROUND in granted()

    /** Play Store page for Health Connect (Android 12–13). */
    fun installIntent(): Intent = Intent(Intent.ACTION_VIEW).apply {
        setPackage("com.android.vending")
        data = Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding")
        putExtra("overlay", true)
        putExtra("callerId", context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Health Connect's own screen, where people turn on sharing from Garmin and Strava. */
    fun settingsIntent(): Intent =
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** All exercise sessions that started after [since], with per-session totals. */
    suspend fun readActivities(since: Instant): List<Activity> {
        val granted = granted()
        if (exercise !in granted) throw SecurityException("Health Connect exercise access not granted")

        val sessions = mutableListOf<ExerciseSessionRecord>()
        var token: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(since),
                    pageToken = token,
                )
            )
            sessions += response.records
            token = response.pageToken
        } while (token != null)

        val metrics = buildSet<AggregateMetric<*>> {
            add(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)
            if (distance in granted) add(DistanceRecord.DISTANCE_TOTAL)
            if (elevation in granted) add(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)
            if (heartRate in granted) add(HeartRateRecord.BPM_AVG)
        }

        return sessions.map { s ->
            val totals = client.aggregate(
                AggregateRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(s.startTime, s.endTime),
                    dataOriginFilter = setOf(s.metadata.dataOrigin),
                )
            )
            val active = totals[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]
                ?: Duration.between(s.startTime, s.endTime)
            val (kind, label) = kindOf(s.exerciseType)
            Activity(
                id = "hc:${s.metadata.id}",
                source = Source.HEALTH_CONNECT,
                name = s.title?.takeIf { it.isNotBlank() } ?: label,
                kind = kind,
                sportType = label,
                startEpochSec = s.startTime.epochSecond,
                distanceM = totals[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0,
                movingSec = active.seconds.toInt(),
                elevationM = totals[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters ?: 0.0,
                avgHr = totals[HeartRateRecord.BPM_AVG]?.toDouble(),
                origin = originLabel(s.metadata.dataOrigin.packageName),
            )
        }
    }

    private fun kindOf(type: Int): Pair<ActivityKind, String> = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> ActivityKind.RUN to "Run"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> ActivityKind.RUN to "Treadmill run"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> ActivityKind.RIDE to "Ride"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> ActivityKind.RIDE to "Indoor ride"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> ActivityKind.WALK to "Walk"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> ActivityKind.HIKE to "Hike"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> ActivityKind.SWIM to "Pool swim"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> ActivityKind.SWIM to "Open water swim"
        else -> ActivityKind.OTHER to "Workout"
    }

    companion object {
        const val PROVIDER = "com.google.android.apps.healthdata"
        // String forms so we don't depend on when these constants landed in the client library.
        const val PERMISSION_HISTORY = "android.permission.health.READ_HEALTH_DATA_HISTORY"
        const val PERMISSION_BACKGROUND = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

        const val GARMIN_PACKAGE = "com.garmin.android.apps.connectmobile"
        const val STRAVA_PACKAGE = "com.strava"

        fun originLabel(pkg: String): String = when (pkg) {
            GARMIN_PACKAGE -> "Garmin"
            STRAVA_PACKAGE -> "Strava"
            "com.google.android.apps.fitness" -> "Fit"
            "com.sec.android.app.shealth" -> "Samsung Health"
            else -> pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }
}
