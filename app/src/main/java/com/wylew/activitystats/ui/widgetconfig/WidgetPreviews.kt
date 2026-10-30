package com.wylew.activitystats.ui.widgetconfig

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wylew.activitystats.R
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Goals
import com.wylew.activitystats.domain.RollupCalculator
import com.wylew.activitystats.domain.Streaks
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.ui.components.CookieShape
import com.wylew.activitystats.ui.components.RollupBars
import com.wylew.activitystats.widget.WidgetKind
import com.wylew.activitystats.widget.WidgetPalette
import com.wylew.activitystats.widget.WidgetPeriod
import com.wylew.activitystats.widget.WidgetSettings
import com.wylew.activitystats.widget.chipName
import com.wylew.activitystats.widget.headline
import com.wylew.activitystats.widget.iconFor
import com.wylew.activitystats.widget.statsLine
import com.wylew.activitystats.widget.whenLabel
import com.wylew.activitystats.widget.widgetPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Compose mirror of a home-screen widget, at its default size. */
@Composable
fun WidgetPreview(kind: WidgetKind, s: WidgetSettings, activities: List<Activity>, units: Units, goals: Goals) {
    val p = MaterialTheme.colorScheme.widgetPalette(s.color)
    when (kind) {
        WidgetKind.ROLLUP -> RollupPreview(s, activities, units, p)
        WidgetKind.GOAL -> GoalPreview(s, activities, units, goals, p)
        WidgetKind.STREAK -> StreakPreview(s, activities, p)
        WidgetKind.LATEST -> LatestPreview(s, activities, units, p)
    }
}

@Composable
private fun RollupPreview(s: WidgetSettings, activities: List<Activity>, units: Units, p: WidgetPalette) {
    val r = RollupCalculator.compute(activities, s.filter, s.period.rollup, LocalDate.now(), ZoneId.systemDefault())
    val h = headline(r, s, units)
    Row(
        Modifier
            .fillMaxWidth()
            .height(184.dp)
            .background(p.bg, RoundedCornerShape(28.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                Modifier
                    .background(p.chipBg, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(iconFor(s.filter)), null, tint = p.chipFg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(h.chip, color = p.chipFg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        h.big, color = p.accent, fontSize = if (h.big.length > 3) 44.sp else 60.sp,
                        lineHeight = 60.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(h.bigUnit, color = p.fg, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
                }
                Text(h.second, color = p.fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
            if (s.period == WidgetPeriod.WEEKLY) {
                RollupBars(
                    r.buckets, barColor = p.bar, currentColor = p.current,
                    futureColor = p.future, labelColor = p.sub, maxBarHeight = 108.dp,
                )
            } else {
                val fmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    if (r.items.isEmpty()) Text("No ${s.filter.plural} yet today", color = p.sub, fontSize = 13.sp)
                    r.items.take(3).forEach { a ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(p.row, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("${Format.distance(a.distanceM, units)} ${Format.distanceUnit(units)}", color = p.fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(
                                Instant.ofEpochSecond(a.startEpochSec).atZone(ZoneId.systemDefault()).toLocalTime().format(fmt),
                                color = p.sub, fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalPreview(s: WidgetSettings, activities: List<Activity>, units: Units, goals: Goals, p: WidgetPalette) {
    val r = RollupCalculator.compute(activities, s.filter, s.period.rollup, LocalDate.now(), ZoneId.systemDefault())
    val goal = goals.forPeriod(s.filter, s.period.rollup)
    val progress = if (goal > 0) (r.distanceM / goal).toFloat().coerceIn(0f, 1f) else 0f
    Box(
        Modifier
            .size(176.dp)
            .background(p.bg, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        WavyRing(progress, p, Modifier.size(150.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Format.distance(r.distanceM, units), color = p.fg, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp)
            Text("of ${Format.distance(goal, units)} ${Format.distanceUnit(units)}", color = p.fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${chipName(s.filter)} · ${s.period.chipSuffix}", color = p.sub, fontSize = 11.sp)
        }
    }
}

/** Compose version of the widget's wavy ring: wave up to [progress], flat track for the rest. */
@Composable
fun WavyRing(progress: Float, p: WidgetPalette, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.055f
        val amp = stroke * 0.45f
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2 - stroke / 2 - amp
        val gap = if (progress > 0f) 12f else 0f
        val sweep = 360f * (1 - progress) - 2 * gap
        if (sweep > 0) {
            drawArc(
                color = p.future, startAngle = -90f + 360f * progress + gap, sweepAngle = sweep, useCenter = false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        if (progress > 0f) {
            val path = Path()
            val steps = (360 * progress).toInt().coerceAtLeast(2)
            for (i in 0..steps) {
                val t = i / 360.0 * 2 * PI
                val rr = r + amp * sin(14 * t).toFloat()
                val a = t - PI / 2
                val x = c.x + rr * cos(a).toFloat()
                val y = c.y + rr * sin(a).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, p.accent, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun StreakPreview(s: WidgetSettings, activities: List<Activity>, p: WidgetPalette) {
    val streak = Streaks.weekly(activities, s.filter, LocalDate.now(), ZoneId.systemDefault())
    Column(
        Modifier
            .size(176.dp)
            .background(p.bg, RoundedCornerShape(28.dp))
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.size(64.dp).background(p.accent, CookieShape()), contentAlignment = Alignment.Center) {
                Text(streak.weeks.toString(), color = p.bg, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.weight(1f))
            Icon(
                painterResource(if (streak.thisWeekActive) R.drawable.ic_flame else iconFor(s.filter)),
                contentDescription = null, tint = p.fg, modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(if (streak.weeks == 0) "Start a streak" else "week streak", color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            "${chipName(s.filter)} · ${if (streak.thisWeekActive) "this week done" else "this week open"}",
            color = p.sub, fontSize = 11.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            streak.recent.forEach { active ->
                Box(Modifier.weight(1f).height(10.dp).background(if (active) p.fg else p.future, RoundedCornerShape(5.dp)))
            }
        }
    }
}

@Composable
private fun LatestPreview(s: WidgetSettings, activities: List<Activity>, units: Units, p: WidgetPalette) {
    val latest = activities.firstOrNull { s.filter.matches(it.kind) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(p.bg, RoundedCornerShape(42.dp))
            .padding(start = 14.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).background(p.chipBg, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(iconFor(s.filter)), null, tint = p.chipFg, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (latest == null) {
                Text("No ${s.filter.plural} yet", color = p.fg, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Sync from Garmin or Strava", color = p.sub, fontSize = 12.sp)
            } else {
                Text("${whenLabel(latest)} · ${latest.name}", color = p.sub, fontSize = 12.sp, maxLines = 1)
                Text(statsLine(latest, units), color = p.fg, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        latest?.origin?.let {
            Text(
                it, color = p.sub, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(p.row, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Fills a preview area like a wallpaper, so widgets read as they will on the home screen. */
@Composable
fun WallpaperBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), RoundedCornerShape(32.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}
