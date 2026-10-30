package com.wylew.activitystats.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupPeriod
import com.wylew.activitystats.domain.SourceChoice
import com.wylew.activitystats.domain.Units

/** The four home-screen widgets from the Rollup design. */
enum class WidgetKind(val title: String, val description: String, val size: String) {
    ROLLUP("Roll-up", "Daily or weekly totals with day bars", "4×2"),
    GOAL("Goal ring", "Distance toward your goal", "2×2"),
    STREAK("Streak", "Weeks in a row with an activity", "2×2"),
    LATEST("Latest activity", "Your most recent workout", "4×1");

    /** Which options the setup screen shows for this widget. */
    val usesPeriod: Boolean get() = this == ROLLUP || this == GOAL
    val usesMetric: Boolean get() = this == ROLLUP
}

enum class WidgetPeriod(val label: String, val rollup: RollupPeriod, val chipSuffix: String) {
    DAILY("Daily", RollupPeriod.DAILY, "today"),
    WEEKLY("Weekly", RollupPeriod.WEEKLY, "week"),
}

enum class WidgetMetric(val label: String) { COUNT("Count"), DISTANCE("Distance"), TIME("Time") }

enum class WidgetColor(val label: String) { NEUTRAL("Neutral"), PRIMARY("Primary"), TERTIARY("Accent") }

/** Per-widget data source; APP_DEFAULT follows Settings → Data sources. */
enum class WidgetSource(val label: String, val sub: String, val choice: SourceChoice?) {
    APP_DEFAULT("App default", "Follows your data source setting", null),
    GARMIN("Garmin only", "Only workouts from Garmin Connect", SourceChoice.GARMIN),
    STRAVA("Strava only", "Only workouts from Strava", SourceChoice.STRAVA),
}

/** Per-widget choices, stored in each widget instance's Glance state. */
data class WidgetSettings(
    val filter: ActivityFilter = ActivityFilter.RUN,
    val period: WidgetPeriod = WidgetPeriod.WEEKLY,
    val metric: WidgetMetric = WidgetMetric.COUNT,
    val color: WidgetColor = WidgetColor.NEUTRAL,
    val source: WidgetSource = WidgetSource.APP_DEFAULT,
) {
    companion object {
        /** Matches the home-screen mockup: clay goal ring, blue streak tile. */
        fun defaultsFor(kind: WidgetKind) = when (kind) {
            WidgetKind.GOAL -> WidgetSettings(color = WidgetColor.TERTIARY)
            WidgetKind.STREAK -> WidgetSettings(color = WidgetColor.PRIMARY)
            else -> WidgetSettings()
        }
    }
}

object WidgetPrefs {
    private val FILTER = stringPreferencesKey("filter")
    private val PERIOD = stringPreferencesKey("period")
    private val METRIC = stringPreferencesKey("metric")
    private val COLOR = stringPreferencesKey("color")
    private val SOURCE = stringPreferencesKey("source")

    fun read(p: Preferences, kind: WidgetKind): WidgetSettings {
        val d = WidgetSettings.defaultsFor(kind)
        return WidgetSettings(
            filter = p[FILTER].toEnum(d.filter),
            period = p[PERIOD].toEnum(d.period),
            metric = p[METRIC].toEnum(d.metric),
            color = p[COLOR].toEnum(d.color),
            source = p[SOURCE].toEnum(d.source),
        )
    }

    fun write(p: MutablePreferences, s: WidgetSettings) {
        p[FILTER] = s.filter.name
        p[PERIOD] = s.period.name
        p[METRIC] = s.metric.name
        p[COLOR] = s.color.name
        p[SOURCE] = s.source.name
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        this?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: default
}

/** The text a roll-up widget shows, shared by the real widget and the in-app preview. */
data class Headline(val chip: String, val big: String, val bigUnit: String, val second: String)

fun chipName(filter: ActivityFilter) = when (filter) {
    ActivityFilter.RUN -> "Runs"
    ActivityFilter.RIDE -> "Rides"
    ActivityFilter.ALL -> "All"
}

fun headline(r: Rollup, s: WidgetSettings, units: Units): Headline {
    val dist = Format.distance(r.distanceM, units)
    val distUnit = Format.distanceUnit(units)
    val noun = s.filter.noun(r.count)
    val chip = "${chipName(s.filter)} · ${s.period.chipSuffix}"
    return when (s.metric) {
        WidgetMetric.COUNT -> Headline(chip, r.count.toString(), noun, "$dist $distUnit")
        WidgetMetric.DISTANCE -> Headline(chip, dist, distUnit, "${r.count} $noun")
        WidgetMetric.TIME -> Headline(chip, Format.duration(r.movingSec), if (r.movingSec >= 3600) "h" else "min", "$dist $distUnit")
    }
}
