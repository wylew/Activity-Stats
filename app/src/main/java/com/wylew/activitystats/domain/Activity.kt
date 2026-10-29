package com.wylew.activitystats.domain

import kotlinx.serialization.Serializable

/** Where an activity came from. Health Connect (Garmin) is planned; Strava is live. */
@Serializable
enum class Source { STRAVA, HEALTH_CONNECT }

@Serializable
enum class ActivityKind {
    RUN, RIDE, WALK, HIKE, SWIM, OTHER;

    companion object {
        /** Maps Strava's `sport_type` (falls back to the legacy `type`). */
        fun fromStrava(sportType: String?): ActivityKind = when (sportType) {
            "Run", "TrailRun", "VirtualRun" -> RUN
            "Ride", "MountainBikeRide", "GravelRide", "EBikeRide", "EMountainBikeRide",
            "VirtualRide", "Velomobile", "Handcycle" -> RIDE
            "Walk" -> WALK
            "Hike" -> HIKE
            "Swim" -> SWIM
            else -> OTHER
        }
    }
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
    /** Unique across sources, e.g. "strava:1234567". */
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
)
