package com.wylew.activitystats.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupPeriod
import com.wylew.activitystats.domain.Units

enum class WidgetPeriod(val label: String, val rollup: RollupPeriod, val chipSuffix: String) {
    DAILY("Daily", RollupPeriod.DAILY, "today"),
    WEEKLY("Weekly", RollupPeriod.WEEKLY, "week"),
}

enum class WidgetMetric(val label: String) { COUNT("Count"), DISTANCE("Distance"), TIME("Time") }

enum class WidgetColor(val label: String) { NEUTRAL("Neutral"), PRIMARY("Primary"), TERTIARY("Accent") }

/** Per-widget choices, stored in each widget instance's Glance state. */
data class WidgetSettings(
    val filter: ActivityFilter = ActivityFilter.RUN,
    val period: WidgetPeriod = WidgetPeriod.WEEKLY,
    val metric: WidgetMetric = WidgetMetric.COUNT,
    val color: WidgetColor = WidgetColor.NEUTRAL,
)

object WidgetPrefs {
    private val FILTER = stringPreferencesKey("filter")
    private val PERIOD = stringPreferencesKey("period")
    private val METRIC = stringPreferencesKey("metric")
    private val COLOR = stringPreferencesKey("color")

    fun read(p: Preferences): WidgetSettings {
        val d = WidgetSettings()
        return WidgetSettings(
            filter = p[FILTER].toEnum(d.filter),
            period = p[PERIOD].toEnum(d.period),
            metric = p[METRIC].toEnum(d.metric),
            color = p[COLOR].toEnum(d.color),
        )
    }

    fun write(p: MutablePreferences, s: WidgetSettings) {
        p[FILTER] = s.filter.name
        p[PERIOD] = s.period.name
        p[METRIC] = s.metric.name
        p[COLOR] = s.color.name
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        this?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: default
}

/** The text a widget shows, shared by the real widget and the in-app preview. */
data class Headline(val chip: String, val big: String, val bigUnit: String, val second: String)

fun headline(r: Rollup, s: WidgetSettings, units: Units): Headline {
    val chipName = when (s.filter) {
        ActivityFilter.RUN -> "Runs"
        ActivityFilter.RIDE -> "Rides"
        ActivityFilter.ALL -> "All"
    }
    val dist = Format.distance(r.distanceM, units)
    val distUnit = Format.distanceUnit(units)
    val noun = s.filter.noun(r.count)
    val chip = "$chipName · ${s.period.chipSuffix}"
    return when (s.metric) {
        WidgetMetric.COUNT -> Headline(chip, r.count.toString(), noun, "$dist $distUnit")
        WidgetMetric.DISTANCE -> Headline(chip, dist, distUnit, "${r.count} $noun")
        WidgetMetric.TIME -> Headline(chip, Format.duration(r.movingSec), if (r.movingSec >= 3600) "h" else "min", "$dist $distUnit")
    }
}
