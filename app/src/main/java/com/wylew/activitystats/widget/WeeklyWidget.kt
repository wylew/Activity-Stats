package com.wylew.activitystats.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.wylew.activitystats.domain.Bucket
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupCalculator
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class WeeklyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeeklyWidget()
}

/**
 * This week (Monday to Sunday), the total big. Three looks:
 * - Bars: a bar per day, M to S.
 * - Badge: the number in the scalloped badge, a dot per day.
 * - Big number: just the number.
 * Two rows or taller it's the square layout, centered; one row high it's a single row
 * with the day markers after the text once there's room; tall and narrow (2×4) the days
 * run down the left, spelled out.
 */
internal class WeeklyWidget : ActivityStatsWidget(WidgetKind.WEEKLY) {

    override val sizeMode = SizeMode.Exact

    @Composable
    override fun Content(context: Context, state: WidgetState) {
        val s = state.settings
        val r = RollupCalculator.compute(state.activities, s.filter, s.period.rollup, LocalDate.now(), ZoneId.systemDefault())
        val h = headline(r, s, state.inputs.units)
        val size = LocalSize.current
        if (size.height < ROW_MAX) {
            WeeklyRowContent(context, r, s, h, state.palette)
        } else if (size.height >= TALL_MIN && size.height >= size.width * 1.4f) {
            WeeklyTallContent(context, r, s, h, state.palette)
        } else {
            WeeklyContent(context, r, s, h, state.palette)
        }
    }

    companion object {
        /** Anything shorter is a one-row widget. */
        val ROW_MAX = 150.dp
        /** At least this tall, and clearly taller than wide, gets the day list. */
        val TALL_MIN = 300.dp
    }
}

@Composable
private fun WeeklyRowContent(context: Context, r: Rollup, s: WidgetSettings, h: Headline, p: GlancePalette) {
    val height = LocalSize.current.height.value
    val wide = LocalSize.current.width.value >= 250f
    WidgetFrame(p, padding = 0.dp) {
        Row(
            GlanceModifier.fillMaxSize().padding(horizontal = if (s.style == WidgetStyle.BADGE) 10.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (s.style == WidgetStyle.BADGE) {
                // The badge fills the row's height; the number scales with it.
                val badge = minOf(136f, height - 16f)
                val badgeText = if (h.big.length > 4) h.big.substringBefore('.') else h.big
                Badge(context, badgeText, p, size = badge.dp, fontSize = (badge * 0.35f).sp)
                Spacer(GlanceModifier.width(10.dp))
                Text(h.caption, style = TextStyle(color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 2)
            } else {
                // As big as the row allows, capped so it matches the square layout on a normal row.
                Total(h, p, minOf(48f, height * 0.5f).sp)
            }
            if (wide && s.style != WidgetStyle.NUMBER) {
                Spacer(GlanceModifier.width(16.dp))
                Box(GlanceModifier.defaultWeight().padding(end = 8.dp)) {
                    if (s.style == WidgetStyle.BADGE) DayDots(r, p) else DayBars(r, p)
                }
            }
        }
    }
}

/** The square weekly layout; also used by a weekly Totals widget sized down to 2×2. */
@Composable
internal fun WeeklyContent(context: Context, r: Rollup, s: WidgetSettings, h: Headline, p: GlancePalette) {
    WidgetFrame(p, padding = 14.dp) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            // The badge is tall, so it sits at the top rather than floating in the middle.
            if (s.style != WidgetStyle.BADGE) Spacer(GlanceModifier.defaultWeight())
            Header(context, h, s, p)
            Spacer(GlanceModifier.defaultWeight())
            when (s.style) {
                WidgetStyle.BADGE -> DayDots(r, p)
                WidgetStyle.NUMBER -> {}
                else -> DayBars(r, p)
            }
        }
    }
}

/** The total, as a badge or a number, with its caption. Shared by the square and tall layouts. */
@Composable
private fun Header(context: Context, h: Headline, s: WidgetSettings, p: GlancePalette) {
    when (s.style) {
        WidgetStyle.BADGE -> {
            val badgeText = if (h.big.length > 4) h.big.substringBefore('.') else h.big
            Badge(context, badgeText, p, size = 136.dp, fontSize = 48.sp)
            Spacer(GlanceModifier.height(6.dp))
            Text(h.caption, style = TextStyle(color = p.fg, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), maxLines = 1)
        }
        WidgetStyle.NUMBER -> {
            Total(h, p, 64.sp)
            SubLine(h, s, p)
        }
        else -> {
            Total(h, p, 48.sp)
            SubLine(h, s, p)
        }
    }
}

/** 2×4 and similar: the total on top, then Monday to Sunday down the left with names spelled out. */
@Composable
private fun WeeklyTallContent(context: Context, r: Rollup, s: WidgetSettings, h: Headline, p: GlancePalette) {
    val pad = 16
    WidgetFrame(p, padding = pad.dp) {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (s.style == WidgetStyle.NUMBER) Spacer(GlanceModifier.defaultWeight())
            Header(context, h, s, p)
            if (s.style == WidgetStyle.NUMBER) {
                Spacer(GlanceModifier.defaultWeight())
            } else {
                Spacer(GlanceModifier.height(12.dp))
                val trackWidth = LocalSize.current.width.value - 2 * pad - NAME_WIDTH - 8
                val max = r.buckets.maxOfOrNull { it.distanceM }?.takeIf { it > 0 } ?: 1.0
                Column(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    r.buckets.forEachIndexed { i, b ->
                        Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                            val name = r.start.plusDays(i.toLong()).dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                            val active = if (b.isCurrent) p.current else p.fg
                            if (s.style == WidgetStyle.BADGE) {
                                Box(GlanceModifier.size(12.dp).cornerRadius(6.dp).background(if (b.count > 0) active else p.future)) {}
                                Spacer(GlanceModifier.width(10.dp))
                                DayName(name, b, p)
                            } else {
                                Box(GlanceModifier.width(NAME_WIDTH.dp)) { DayName(name, b, p) }
                                Spacer(GlanceModifier.width(8.dp))
                                Box(GlanceModifier.width(trackWidth.dp).height(12.dp).cornerRadius(6.dp).background(p.future)) {
                                    if (b.distanceM > 0.0) {
                                        val fill = maxOf(12f, (b.distanceM / max * trackWidth).toFloat())
                                        Box(GlanceModifier.width(fill.dp).height(12.dp).cornerRadius(6.dp).background(if (b.isCurrent) p.current else p.bar)) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayName(name: String, b: Bucket, p: GlancePalette) {
    Text(
        name,
        style = TextStyle(
            color = if (b.isCurrent) p.fg else p.sub,
            fontSize = 14.sp,
            fontWeight = if (b.isCurrent) FontWeight.Bold else FontWeight.Normal,
        ),
        maxLines = 1,
    )
}

private const val NAME_WIDTH = 84

/** The headline number with its unit, centered. Long numbers shrink to fit. */
@Composable
private fun Total(h: Headline, p: GlancePalette, size: TextUnit) {
    val fontSize = if (h.big.length > 4) (size.value * 0.75f).sp else size
    val unitSize = minOf(15f, fontSize.value * 0.45f).sp
    Row(verticalAlignment = Alignment.Bottom) {
        Text(h.big, style = TextStyle(color = p.accent, fontSize = fontSize, fontWeight = FontWeight.Bold), maxLines = 1)
        Spacer(GlanceModifier.width(4.dp))
        Text(
            h.bigUnit,
            style = TextStyle(color = p.fg, fontSize = unitSize, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.padding(bottom = (fontSize.value / 6).dp),
            maxLines = 1,
        )
    }
}

/** "3 runs this week" with the activity icon. */
@Composable
private fun SubLine(h: Headline, s: WidgetSettings, p: GlancePalette) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(ImageProvider(iconFor(s.filter)), null, GlanceModifier.size(14.dp), colorFilter = ColorFilter.tint(p.sub))
        Spacer(GlanceModifier.width(4.dp))
        Text("${h.second} ${s.period.phrase}", style = TextStyle(color = p.sub, fontSize = 12.sp), maxLines = 1)
    }
}

/** Seven tracks, Monday to Sunday, filled by that day's distance, each with its day letter. */
@Composable
private fun DayBars(r: Rollup, p: GlancePalette) {
    val max = r.buckets.maxOfOrNull { it.distanceM }?.takeIf { it > 0 } ?: 1.0
    DayRow(r.buckets, p) { b ->
        Box(GlanceModifier.width(12.dp).height(TRACK.dp).cornerRadius(6.dp).background(p.future), contentAlignment = Alignment.BottomCenter) {
            if (b.distanceM > 0.0) {
                val fill = maxOf(8, (b.distanceM / max * TRACK).toInt())
                Box(GlanceModifier.width(12.dp).height(fill.dp).cornerRadius(6.dp).background(if (b.isCurrent) p.current else p.bar)) {}
            }
        }
    }
}

/** A dot per day, Monday to Sunday: filled on days with an activity. */
@Composable
private fun DayDots(r: Rollup, p: GlancePalette) {
    DayRow(r.buckets, p) { b ->
        val color = when {
            b.count > 0 -> if (b.isCurrent) p.current else p.fg
            else -> p.future
        }
        Box(GlanceModifier.size(12.dp).cornerRadius(6.dp).background(color)) {}
    }
}

@Composable
private fun DayRow(buckets: List<Bucket>, p: GlancePalette, marker: @Composable (Bucket) -> Unit) {
    Row(GlanceModifier.fillMaxWidth()) {
        buckets.forEach { b ->
            Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                marker(b)
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    b.label,
                    style = TextStyle(
                        color = if (b.isCurrent) p.fg else p.sub,
                        fontSize = 10.sp,
                        fontWeight = if (b.isCurrent) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}

private const val TRACK = 30
