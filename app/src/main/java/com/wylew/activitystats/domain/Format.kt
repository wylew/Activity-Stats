package com.wylew.activitystats.domain

import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class Units { IMPERIAL, METRIC }

object Format {
    private const val METERS_PER_MILE = 1609.344
    private const val FEET_PER_METER = 3.28084

    fun distanceValue(meters: Double, units: Units): Double =
        if (units == Units.IMPERIAL) meters / METERS_PER_MILE else meters / 1000.0

    /** "13.2"; whole numbers past 100 ("622"). */
    fun distance(meters: Double, units: Units): String {
        val v = distanceValue(meters, units)
        return if (v >= 100) String.format(Locale.getDefault(), "%,d", v.roundToInt())
        else String.format(Locale.getDefault(), "%.1f", v)
    }

    fun distanceUnit(units: Units) = if (units == Units.IMPERIAL) "mi" else "km"

    /** "1:58" for hours:minutes, "34:12" under an hour. */
    fun duration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d", h, m)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    /** Pace per mile or km, "8:56". Empty when there's no distance. */
    fun pace(meters: Double, seconds: Long, units: Units): String {
        val d = distanceValue(meters, units)
        if (d <= 0.0 || seconds <= 0) return "–"
        val secPer = (seconds / d).roundToLong()
        return String.format(Locale.US, "%d:%02d", secPer / 60, secPer % 60)
    }

    fun paceUnit(units: Units) = if (units == Units.IMPERIAL) "/mi" else "/km"

    fun elevation(meters: Double, units: Units): String {
        val v = if (units == Units.IMPERIAL) meters * FEET_PER_METER else meters
        return String.format(Locale.getDefault(), "%,d", v.roundToInt())
    }

    fun elevationUnit(units: Units) = if (units == Units.IMPERIAL) "ft" else "m"
}
