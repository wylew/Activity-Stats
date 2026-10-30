package com.wylew.activitystats.domain

import kotlinx.serialization.Serializable

/** Where an activity was read from. Only Health Connect today; STRAVA is kept for old caches. */
@Serializable
enum class Source { STRAVA, HEALTH_CONNECT }

@Serializable
enum class ActivityKind {
    RUN, RIDE, WALK, HIKE, SWIM, OTHER
}

/** What a widget or the dashboard counts. */
enum class ActivityFilter(val label: String, val singular: String, val plural: String) {
    RUN("Run", "run", "runs"),
    RIDE("Ride", "ride", "rides"),
    ALL("All activities", "activity", "activities");

    fun matches(kind: ActivityKind): Boolean = when (this) {
        RUN -> kind == ActivityKind.RUN
        RIDE -> kind == ActivityKind.RIDE
        ALL -> true
    }

    fun noun(count: Int) = if (count == 1) singular else plural
}

@Serializable
data class Activity(
    /** Unique per record, e.g. "hc:<Health Connect id>". */
    val id: String,
    val source: Source,
    val name: String,
    val kind: ActivityKind,
    val sportType: String,
    val startEpochSec: Long,
    val distanceM: Double,
    val movingSec: Int,
    val elevationM: Double,
    val avgHr: Double? = null,
    /** App that recorded it, e.g. "Garmin" or "Strava". */
    val origin: String? = null,
)
