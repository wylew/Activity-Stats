package com.wylew.activitystats.domain

/** Which recording apps to count. Mirrors the "Where do your activities come from?" screen. */
enum class SourceChoice(val label: String) {
    GARMIN("Garmin Connect"),
    STRAVA("Strava"),
    BOTH("Both"),
}

/** Which copy wins when the same workout is in both apps. */
enum class PreferOrigin(val label: String, val origin: String) {
    GARMIN("Prefer Garmin", "Garmin"),
    STRAVA("Prefer Strava", "Strava"),
}

data class SourcePrefs(
    val choice: SourceChoice = SourceChoice.BOTH,
    val prefer: PreferOrigin = PreferOrigin.GARMIN,
)

object ActivityView {
    /** Filters to the chosen apps and collapses duplicates. */
    fun apply(all: List<Activity>, prefs: SourcePrefs, override: SourceChoice? = null): List<Activity> {
        val choice = override ?: prefs.choice
        val filtered = when (choice) {
            SourceChoice.GARMIN -> all.filter { it.origin != "Strava" }
            SourceChoice.STRAVA -> all.filter { it.origin == "Strava" }
            SourceChoice.BOTH -> all
        }
        return Dedupe.apply(filtered, prefs.prefer.origin)
    }

    /** How many copies were merged away, for "2 duplicates merged". */
    fun mergedCount(all: List<Activity>, prefs: SourcePrefs): Int {
        if (prefs.choice != SourceChoice.BOTH) return 0
        return all.size - Dedupe.apply(all, prefs.prefer.origin).size
    }
}
