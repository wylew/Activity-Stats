package com.wylew.activitystats.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupPeriod
import com.wylew.activitystats.domain.Units

/** The home-screen widgets. Each has three looks, picked in its setup screen. */
enum class WidgetKind(val title: String, val description: String, val size: String, val styles: List<WidgetStyle>) {
    TOTALS(
        "Totals", "Daily, weekly or monthly runs, rides or everything", "4×2",
        listOf(WidgetStyle.BARS, WidgetStyle.BADGE, WidgetStyle.NUMBER),
    ),
    STREAK(
        "Streak", "Weeks in a row with an activity", "2×2",
        listOf(WidgetStyle.BADGE, WidgetStyle.CALENDAR, WidgetStyle.NUMBER),
    ),
    LATEST(
        "Latest activity", "Your most recent workout", "4×1",
        listOf(WidgetStyle.PILL, WidgetStyle.CARD, WidgetStyle.COMPACT),
    );

    val usesPeriod: Boolean get() = this == TOTALS
    val usesMetric: Boolean get() = this == TOTALS
}

enum class WidgetStyle(val label: String) {
    /** Totals: headline plus day (or week) bars. */
    BARS("Bars"),
    /** Scalloped badge holding the number, with a row of dots. Shared by Totals and Streak. */
    BADGE("Badge"),
    /** Just the number, as big as it fits. Shared by Totals and Streak. */
    NUMBER("Big number"),
    /** Streak: the last 12 weeks as a grid. */
    CALENDAR("Calendar"),
    /** Latest: one-row pill. */
    PILL("Pill"),
    /** Latest: a card with the stats stacked. */
    CARD("Card"),
    /** Latest: distance and when, nothing else. */
    COMPACT("Compact"),
}

enum class WidgetPeriod(val label: String, val rollup: RollupPeriod, val chipSuffix: String, val phrase: String) {
    DAILY("Daily", RollupPeriod.DAILY, "today", "today"),
    WEEKLY("Weekly", RollupPeriod.WEEKLY, "week", "this week"),
    MONTHLY("Monthly", RollupPeriod.MONTHLY, "month", "this month"),
}

enum class WidgetMetric(val label: String) { COUNT("Count"), DISTANCE("Distance"), TIME("Time") }

enum class WidgetColor(val label: String) { NEUTRAL("Neutral"), PRIMARY("Primary"), TERTIARY("Accent") }

/** Per-widget choices, stored in each widget instance's Glance state. */
data class WidgetSettings(
    val style: WidgetStyle,
    val filter: ActivityFilter = ActivityFilter.RUN,
    val period: WidgetPeriod = WidgetPeriod.WEEKLY,
    val metric: WidgetMetric = WidgetMetric.DISTANCE,
    val color: WidgetColor = WidgetColor.NEUTRAL,
) {
    companion object {
        fun defaultsFor(kind: WidgetKind, style: WidgetStyle = kind.styles.first()) = WidgetSettings(
            style = style,
            metric = if (kind == WidgetKind.TOTALS && style == WidgetStyle.BARS) WidgetMetric.COUNT else WidgetMetric.DISTANCE,
            // The badge styles read best on a solid color, as in the design.
            color = if (style == WidgetStyle.BADGE) WidgetColor.PRIMARY else WidgetColor.NEUTRAL,
        )
    }
}

object WidgetPrefs {
    private val STYLE = stringPreferencesKey("style")
    private val FILTER = stringPreferencesKey("filter")
    private val PERIOD = stringPreferencesKey("period")
    private val METRIC = stringPreferencesKey("metric")
    private val COLOR = stringPreferencesKey("color")

    fun read(p: Preferences, kind: WidgetKind): WidgetSettings {
        val style = p[STYLE]?.let { v -> kind.styles.firstOrNull { it.name == v } } ?: kind.styles.first()
        val d = WidgetSettings.defaultsFor(kind, style)
        return WidgetSettings(
            style = style,
            filter = p[FILTER].toEnum(d.filter),
            period = p[PERIOD].toEnum(d.period),
            metric = p[METRIC].toEnum(d.metric),
            color = p[COLOR].toEnum(d.color),
        )
    }

    fun write(p: MutablePreferences, s: WidgetSettings) {
        p[STYLE] = s.style.name
        p[FILTER] = s.filter.name
        p[PERIOD] = s.period.name
        p[METRIC] = s.metric.name
        p[COLOR] = s.color.name
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        this?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: default
}

/** The text a totals widget shows, shared by the real widget and the in-app preview. */
data class Headline(
    /** "Runs · week" */
    val chip: String,
    /** "13.2" */
    val big: String,
    /** "mi" */
    val bigUnit: String,
    /** "3 runs" */
    val second: String,
    /** "mi this week", used under the badge. */
    val caption: String,
)

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
    val phrase = s.period.phrase
    return when (s.metric) {
        WidgetMetric.COUNT -> Headline(chip, r.count.toString(), noun, "$dist $distUnit", "$noun $phrase")
        WidgetMetric.DISTANCE -> Headline(chip, dist, distUnit, "${r.count} $noun", "$distUnit $phrase")
        WidgetMetric.TIME -> {
            val unit = if (r.movingSec >= 3600) "h" else "min"
            Headline(chip, Format.duration(r.movingSec), unit, "$dist $distUnit", "$unit $phrase")
        }
    }
}
