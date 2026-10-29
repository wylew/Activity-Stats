package com.wylew.activitystats.domain

import kotlin.math.abs
import kotlin.math.max

/**
 * Collapses the same workout recorded by more than one app.
 *
 * A Garmin run that syncs to Strava shows up twice in Health Connect (once from each app),
 * and a third time if the Strava API is connected. Two activities are the same workout when
 * they start within 2 minutes, are the same sport, and their distances are within 3%.
 *
 * The copy from the watch app wins for numbers; a Strava title is kept if the winner has none.
 */
object Dedupe {
    private const val START_TOLERANCE_SEC = 120
    private const val DISTANCE_TOLERANCE = 0.03
    private const val SHORT_DISTANCE_M = 200.0

    fun apply(all: List<Activity>): List<Activity> {
        if (all.size < 2) return all
        val kept = mutableListOf<Activity>()
        for (a in all.sortedBy { it.startEpochSec }) {
            val i = kept.indexOfLast { isSameWorkout(it, a) }
            if (i == -1) {
                kept += a
            } else {
                kept[i] = merge(kept[i], a)
            }
        }
        return kept.sortedByDescending { it.startEpochSec }
    }

    fun isSameWorkout(a: Activity, b: Activity): Boolean {
        if (abs(a.startEpochSec - b.startEpochSec) > START_TOLERANCE_SEC) return false
        val kindsMatch = a.kind == b.kind || a.kind == ActivityKind.OTHER || b.kind == ActivityKind.OTHER
        if (!kindsMatch) return false
        val longer = max(a.distanceM, b.distanceM)
        if (longer < SHORT_DISTANCE_M) return true
        return abs(a.distanceM - b.distanceM) / longer <= DISTANCE_TOLERANCE
    }

    private fun merge(x: Activity, y: Activity): Activity {
        val (winner, loser) = if (rank(y) > rank(x)) y to x else x to y
        val name = when {
            loser.origin == "Strava" && loser.name.isNotBlank() && winner.name == winner.sportType -> loser.name
            else -> winner.name
        }
        return winner.copy(
            name = name,
            avgHr = winner.avgHr ?: loser.avgHr,
            elevationM = if (winner.elevationM > 0) winner.elevationM else loser.elevationM,
            kind = if (winner.kind == ActivityKind.OTHER) loser.kind else winner.kind,
        )
    }

    /** Higher wins: the watch's own app, then other recorders, then Strava copies. */
    private fun rank(a: Activity): Int = when (a.origin) {
        "Garmin" -> 3
        "Strava" -> 1
        null -> 0
        else -> 2
    }
}
