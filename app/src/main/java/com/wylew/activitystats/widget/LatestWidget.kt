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
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wylew.activitystats.MainActivity
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.domain.localDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

class LatestWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LatestWidget()
}

/** 4×1 pill: icon tile, when + name, distance · time · pace, and which app recorded it. */
internal class LatestWidget : ActivityStatsWidget(WidgetKind.LATEST) {

    @Composable
    override fun Content(context: Context, state: WidgetState) {
        val s = state.settings
        val p = state.palette
        val units = state.inputs.units
        val latest = state.activities.firstOrNull { s.filter.matches(it.kind) }

        Row(
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(p.bg)
                .cornerRadius(40.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(start = 12.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                GlanceModifier.size(52.dp).cornerRadius(18.dp).background(p.chipBg),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    ImageProvider(iconFor(s.filter)), contentDescription = null,
                    modifier = GlanceModifier.size(26.dp), colorFilter = ColorFilter.tint(p.chipFg),
                )
            }
            Spacer(GlanceModifier.width(14.dp))
            Column(GlanceModifier.defaultWeight()) {
                if (latest == null) {
                    Text("No ${s.filter.plural} yet", style = TextStyle(color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    Text("Sync from Garmin or Strava", style = TextStyle(color = p.sub, fontSize = 12.sp))
                } else {
                    Text(whenLabel(latest) + " · " + latest.name, style = TextStyle(color = p.sub, fontSize = 12.sp), maxLines = 1)
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
