package com.wylew.activitystats.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.domain.localDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

class LatestWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LatestWidget()
}

/**
 * Most recent workout. Three looks:
 * - Pill: one row (4×1).
 * - Card: name, when, big distance, time and pace (2×2 or larger).
 * - Compact: icon, distance and when.
 */
internal class LatestWidget : ActivityStatsWidget(WidgetKind.LATEST) {

    @Composable
    override fun Content(context: Context, state: WidgetState) {
        val s = state.settings
        val latest = state.activities.firstOrNull { s.filter.matches(it.kind) }
        when (s.style) {
            WidgetStyle.CARD -> CardLatest(latest, s, state.inputs.units, state.palette)
            WidgetStyle.COMPACT -> CompactLatest(latest, s, state.inputs.units, state.palette)
            else -> PillLatest(latest, s, state.inputs.units, state.palette)
        }
    }
}

@Composable
private fun IconTile(s: WidgetSettings, p: GlancePalette, size: Int = 52) {
    Box(GlanceModifier.size(size.dp).cornerRadius(18.dp).background(p.chipBg), contentAlignment = Alignment.Center) {
        Image(ImageProvider(iconFor(s.filter)), null, GlanceModifier.size((size / 2).dp), colorFilter = ColorFilter.tint(p.chipFg))
    }
}

@Composable
private fun PillLatest(latest: Activity?, s: WidgetSettings, units: Units, p: GlancePalette) {
    WidgetFrame(p, padding = 0.dp, radius = 40.dp) {
        Row(
            GlanceModifier.fillMaxSize().padding(start = 12.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(s, p)
            Spacer(GlanceModifier.width(14.dp))
            Column(GlanceModifier.defaultWeight()) {
                if (latest == null) {
                    Text("No ${s.filter.plural} yet", style = TextStyle(color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                } else {
                    Text("${whenLabel(latest)} · ${latest.name}", style = TextStyle(color = p.sub, fontSize = 12.sp), maxLines = 1)
                    Text(statsLine(latest, units), style = TextStyle(color = p.fg, fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                }
            }
            latest?.origin?.let { origin ->
                Spacer(GlanceModifier.width(8.dp))
                Box(GlanceModifier.background(p.row).cornerRadius(8.dp).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(origin, style = TextStyle(color = p.sub, fontSize = 11.sp, fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}

@Composable
private fun CardLatest(latest: Activity?, s: WidgetSettings, units: Units, p: GlancePalette) {
    WidgetFrame(p) {
        Column(GlanceModifier.fillMaxSize()) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconTile(s, p, size = 40)
                Spacer(GlanceModifier.width(10.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(latest?.name ?: "No ${s.filter.plural} yet", style = TextStyle(color = p.fg, fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                    if (latest != null) Text(whenLabel(latest), style = TextStyle(color = p.sub, fontSize = 11.sp), maxLines = 1)
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            if (latest != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (latest.distanceM > 0) Format.distance(latest.distanceM, units) else Format.duration(latest.movingSec.toLong()),
                        style = TextStyle(color = p.accent, fontSize = 44.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        if (latest.distanceM > 0) Format.distanceUnit(units) else "",
                        style = TextStyle(color = p.fg, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        modifier = GlanceModifier.padding(bottom = 8.dp),
                    )
                }
                Row(GlanceModifier.fillMaxWidth()) {
                    Stat("Time", Format.duration(latest.movingSec.toLong()), p, GlanceModifier.defaultWeight())
                    if (latest.distanceM > 0) {
                        Stat("Pace", "${Format.pace(latest.distanceM, latest.movingSec.toLong(), units)}${Format.paceUnit(units)}", p, GlanceModifier.defaultWeight())
                    }
                    latest.avgHr?.let { Stat("HR", "%.0f".format(it), p, GlanceModifier.defaultWeight()) }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, p: GlancePalette, modifier: GlanceModifier) {
    Column(modifier) {
        Text(label, style = TextStyle(color = p.sub, fontSize = 11.sp))
        Text(value, style = TextStyle(color = p.fg, fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun CompactLatest(latest: Activity?, s: WidgetSettings, units: Units, p: GlancePalette) {
    WidgetFrame(p, padding = 12.dp) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            IconTile(s, p, size = 44)
            Spacer(GlanceModifier.width(10.dp))
            Column(GlanceModifier.defaultWeight()) {
                if (latest == null) {
                    Text("No ${s.filter.plural}", style = TextStyle(color = p.fg, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                } else {
                    Text(
                        if (latest.distanceM > 0) "${Format.distance(latest.distanceM, units)} ${Format.distanceUnit(units)}" else Format.duration(latest.movingSec.toLong()),
                        style = TextStyle(color = p.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Text(whenLabel(latest).substringBefore(","), style = TextStyle(color = p.sub, fontSize = 12.sp), maxLines = 1)
                }
            }
        }
    }
}

internal fun whenLabel(a: Activity, zone: ZoneId = ZoneId.systemDefault()): String {
    val d = a.localDate(zone)
    val today = LocalDate.now(zone)
    val time = Instant.ofEpochSecond(a.startEpochSec).atZone(zone).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    return when {
        d == today -> "Today, $time"
        d == today.minusDays(1) -> "Yesterday, $time"
        d.isAfter(today.minusDays(7)) -> d.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()) + ", $time"
        else -> d.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}

internal fun statsLine(a: Activity, units: Units): String {
    val parts = mutableListOf<String>()
    if (a.distanceM > 0) parts += "${Format.distance(a.distanceM, units)} ${Format.distanceUnit(units)}"
    parts += Format.duration(a.movingSec.toLong())
    if (a.distanceM > 0) parts += Format.pace(a.distanceM, a.movingSec.toLong(), units)
    return parts.joinToString(" · ")
}
