package com.wylew.activitystats.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupCalculator
import com.wylew.activitystats.domain.Units
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class RollupWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RollupWidget()
}

/**
 * 4×2 roll-up: Run / Ride / All, Daily or Weekly.
 * Narrow sizes show the headline; wide ones add day bars (weekly) or today's list (daily).
 */
internal class RollupWidget : ActivityStatsWidget(WidgetKind.ROLLUP) {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    @Composable
    override fun Content(context: Context, state: WidgetState) {
        val s = state.settings
        val r = RollupCalculator.compute(state.activities, s.filter, s.period.rollup, LocalDate.now(), ZoneId.systemDefault())
        val p = state.palette
        val wide = LocalSize.current.width >= WIDE.width
        val h = headline(r, s, state.inputs.units)

        WidgetFrame(p) {
            if (wide) {
                Row(GlanceModifier.fillMaxSize()) {
                    Summary(h, p, big = 56, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                    Spacer(GlanceModifier.width(12.dp))
                    Box(GlanceModifier.defaultWeight().fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        if (s.period == WidgetPeriod.WEEKLY) Bars(r, p) else TodayList(r, s, state.inputs.units, p)
                    }
                }
            } else {
                Summary(h, p, big = 44, modifier = GlanceModifier.fillMaxSize())
            }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
    }
}

@Composable
private fun Summary(h: Headline, p: GlancePalette, big: Int, modifier: GlanceModifier) {
    Column(modifier) {
        Chip(h.chip, p)
        Spacer(GlanceModifier.defaultWeight())
        Row(verticalAlignment = Alignment.Bottom) {
            Text(h.big, style = TextStyle(color = p.accent, fontSize = big.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Spacer(GlanceModifier.width(4.dp))
            Text(
                h.bigUnit,
                style = TextStyle(color = p.fg, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.padding(bottom = 9.dp),
                maxLines = 1,
            )
        }
        Text(h.second, style = TextStyle(color = p.fg, fontSize = 20.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun Bars(r: Rollup, p: GlancePalette) {
    val max = r.buckets.maxOfOrNull { it.distanceM }?.takeIf { it > 0 } ?: 1.0
    Row(GlanceModifier.fillMaxWidth().height(116.dp), verticalAlignment = Alignment.Bottom) {
        r.buckets.forEach { b ->
            Column(
                GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val (height, color) = when {
                    b.isFuture -> 18 to p.future
                    b.distanceM <= 0.0 -> 8 to p.future
                    else -> maxOf(12, (b.distanceM / max * 88).toInt()) to (if (b.isCurrent) p.current else p.bar)
                }
                val width = if (b.distanceM > 0.0 || b.isFuture) 18 else 8
                Box(GlanceModifier.width(width.dp).height(height.dp).cornerRadius(9.dp).background(color)) {}
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    b.label,
                    style = TextStyle(
                        color = if (b.isCurrent) p.fg else p.sub,
                        fontSize = 11.sp,
                        fontWeight = if (b.isCurrent) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TodayList(r: Rollup, s: WidgetSettings, units: Units, p: GlancePalette) {
    val zone = ZoneId.systemDefault()
    val timeFmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    Column(GlanceModifier.fillMaxWidth()) {
        if (r.items.isEmpty()) {
            Text("No ${s.filter.plural} yet today", style = TextStyle(color = p.sub, fontSize = 13.sp))
        }
        r.items.take(3).forEach { a ->
            Row(
                GlanceModifier.fillMaxWidth().background(p.row).cornerRadius(10.dp).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${Format.distance(a.distanceM, units)} ${Format.distanceUnit(units)}",
                    style = TextStyle(color = p.fg, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    Instant.ofEpochSecond(a.startEpochSec).atZone(zone).toLocalTime().format(timeFmt),
                    style = TextStyle(color = p.sub, fontSize = 12.sp),
                )
            }
            Spacer(GlanceModifier.height(6.dp))
        }
    }
}
