package com.wylew.activitystats.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wylew.activitystats.domain.Bucket
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupCalculator
import com.wylew.activitystats.domain.Units
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class TotalsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TotalsWidget()
}

/**
 * Daily, weekly or monthly totals for runs, rides or everything. Three looks:
 * - Bars: headline beside day (or week) bars; today's list when daily.
 * - Badge: the number in a scalloped badge with a dot per day (or week), like the streak.
 * - Big number: just the number.
 */
internal class TotalsWidget : ActivityStatsWidget(WidgetKind.TOTALS) {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    @Composable
    override fun Content(context: Context, state: WidgetState) {
        val s = state.settings
        val r = RollupCalculator.compute(state.activities, s.filter, s.period.rollup, LocalDate.now(), ZoneId.systemDefault())
        val h = headline(r, s, state.inputs.units)
        val p = state.palette
        val wide = LocalSize.current.width >= WIDE.width
        when (s.style) {
            WidgetStyle.BADGE -> BadgeTotals(context, r, s, h, p, wide)
            WidgetStyle.NUMBER -> NumberTotals(s, h, p, wide)
            else -> BarsTotals(r, s, h, p, wide, state.inputs.units)
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
    }
}

@Composable
private fun BarsTotals(r: Rollup, s: WidgetSettings, h: Headline, p: GlancePalette, wide: Boolean, units: Units) {
    WidgetFrame(p) {
        if (wide) {
            Row(GlanceModifier.fillMaxSize()) {
                Summary(h, s, p, big = 56, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                Spacer(GlanceModifier.width(12.dp))
                Box(GlanceModifier.defaultWeight().fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    if (s.period == WidgetPeriod.DAILY) TodayList(r, s, units, p) else Bars(r.buckets, p)
                }
            }
        } else {
            Summary(h, s, p, big = 44, modifier = GlanceModifier.fillMaxSize())
        }
    }
}

@Composable
private fun BadgeTotals(context: Context, r: Rollup, s: WidgetSettings, h: Headline, p: GlancePalette, wide: Boolean) {
    val badgeText = if (h.big.length > 4) h.big.substringBefore('.') else h.big
    WidgetFrame(p) {
        Column(GlanceModifier.fillMaxSize()) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Badge(context, badgeText, p, size = if (wide) 72.dp else 64.dp, fontSize = if (badgeText.length > 3) 20.sp else 26.sp)
                Spacer(GlanceModifier.defaultWeight())
                Image(ImageProvider(iconFor(s.filter)), null, GlanceModifier.size(22.dp), colorFilter = ColorFilter.tint(p.fg))
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(h.caption, style = TextStyle(color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text(h.second, style = TextStyle(color = p.sub, fontSize = 12.sp), maxLines = 1)
            val dots = dotsFor(r)
            if (dots.isNotEmpty()) {
                Spacer(GlanceModifier.height(8.dp))
                Dots(dots, p)
            }
        }
    }
}

@Composable
private fun NumberTotals(s: WidgetSettings, h: Headline, p: GlancePalette, wide: Boolean) {
    WidgetFrame(p, padding = 18.dp) {
        Column(GlanceModifier.fillMaxSize()) {
            Chip(h.chip, p, icon = iconFor(s.filter))
            Spacer(GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    h.big,
                    style = TextStyle(color = p.accent, fontSize = (if (wide) 72 else 52).sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    h.bigUnit,
                    style = TextStyle(color = p.fg, fontSize = 18.sp, fontWeight = FontWeight.Medium),
                    modifier = GlanceModifier.padding(bottom = 12.dp),
                    maxLines = 1,
                )
            }
            Text(h.second, style = TextStyle(color = p.sub, fontSize = 14.sp), maxLines = 1)
        }
    }
}

/** One dot per day (weekly) or per week (monthly); nothing for daily. */
internal fun dotsFor(r: Rollup): List<DotState> = r.buckets.map {
    when {
        it.isFuture -> DotState.FUTURE
        it.count > 0 -> DotState.ON
        else -> DotState.OFF
    }
}

@Composable
private fun Summary(h: Headline, s: WidgetSettings, p: GlancePalette, big: Int, modifier: GlanceModifier) {
    Column(modifier) {
        Chip(h.chip, p, icon = iconFor(s.filter))
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
private fun Bars(buckets: List<Bucket>, p: GlancePalette) {
    val max = buckets.maxOfOrNull { it.distanceM }?.takeIf { it > 0 } ?: 1.0
    Row(GlanceModifier.fillMaxWidth().height(116.dp), verticalAlignment = Alignment.Bottom) {
        buckets.forEach { b ->
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
