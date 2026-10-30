package com.wylew.activitystats.domain

/** Weekly distance goals per activity filter; other periods scale from these. */
data class Goals(
    val runWeeklyM: Double = 25 * METERS_PER_MILE,
    val rideWeeklyM: Double = 75 * METERS_PER_MILE,
    val allWeeklyM: Double = 100 * METERS_PER_MILE,
) {
    fun weekly(filter: ActivityFilter): Double = when (filter) {
        ActivityFilter.RUN -> runWeeklyM
        ActivityFilter.RIDE -> rideWeeklyM
        ActivityFilter.ALL -> allWeeklyM
    }

    fun forPeriod(filter: ActivityFilter, period: RollupPeriod): Double = weekly(filter) * when (period) {
        RollupPeriod.DAILY -> 1.0 / 7
        RollupPeriod.WEEKLY -> 1.0
        RollupPeriod.MONTHLY -> 365.25 / 12 / 7
        RollupPeriod.YEARLY -> 365.25 / 7
    }

    fun withWeekly(filter: ActivityFilter, meters: Double): Goals = when (filter) {
        ActivityFilter.RUN -> copy(runWeeklyM = meters)
        ActivityFilter.RIDE -> copy(rideWeeklyM = meters)
        ActivityFilter.ALL -> copy(allWeeklyM = meters)
    }

    companion object {
        const val METERS_PER_MILE = 1609.344
    }
}
