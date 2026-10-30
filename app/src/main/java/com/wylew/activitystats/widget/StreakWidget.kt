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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wylew.activitystats.R
import com.wylew.activitystats.domain.Streak
import com.wylew.activitystats.domain.Streaks
import java.time.LocalDate
import java.time.ZoneId

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

/**
 * Weeks in a row with an activity. Three looks:
 * - Badge: count in a scalloped badge, last 12 weeks as a dot row.
 * - Calendar: last 12 weeks as a 3×4 grid, count in the header.
 * - Big number: the count, large.
 */
internal class StreakWidget : ActivityStatsWidget(WidgetKind.STREAK) {

    @Composable
    override fun Content(context: Context, state: WidgetState) {
        val s = state.settings
        val p = state.palette
        val streak = Streaks.weekly(state.activities, s.filter, LocalDate.now(), ZoneId.systemDefault())
        when (s.style) {
            WidgetStyle.CALENDAR -> CalendarStreak(streak, s, p)
            WidgetStyle.NUMBER -> NumberStreak(streak, s, p)
            else -> BadgeStreak(context, streak, s, p)
        }
    }
}

internal fun streakCaption(streak: Streak) = if (streak.weeks == 0) "Start a streak" else "week streak"

internal fun streakSub(streak: Streak, s: WidgetSettings) =
    "${chipName(s.filter)} · ${if (streak.thisWeekActive) "this week done" else "this week open"}"

@Composable
private fun BadgeStreak(context: Context, streak: Streak, s: WidgetSettings, p: GlancePalette) {
    WidgetFrame(p) {
        Column(GlanceModifier.fillMaxSize()) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Badge(context, streak.weeks.toString(), p)
                Spacer(GlanceModifier.defaultWeight())
                Image(
                    ImageProvider(if (streak.thisWeekActive) R.drawable.ic_flame else iconFor(s.filter)),
                    null, GlanceModifier.size(22.dp), colorFilter = ColorFilter.tint(p.fg),
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(streakCaption(streak), style = TextStyle(color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.Bold))
            Text(streakSub(streak, s), style = TextStyle(color = p.sub, fontSize = 11.sp), maxLines = 1)
            Spacer(GlanceModifier.height(8.dp))
            Dots(streak.recent.map { if (it) DotState.ON else DotState.OFF }, p)
        }
    }
}

@Composable
private fun CalendarStreak(streak: Streak, s: WidgetSettings, p: GlancePalette) {
    WidgetFrame(p, padding = 14.dp) {
        Column(GlanceModifier.fillMaxSize()) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(streak.weeks.toString(), style = TextStyle(color = p.accent, fontSize = 30.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.width(6.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text("weeks", style = TextStyle(color = p.fg, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    Text(chipName(s.filter), style = TextStyle(color = p.sub, fontSize = 11.sp))
                }
                Image(ImageProvider(iconFor(s.filter)), null, GlanceModifier.size(20.dp), colorFilter = ColorFilter.tint(p.fg))
            }
            Spacer(GlanceModifier.defaultWeight())
            // 12 weeks, oldest top-left, this week bottom-right.
            streak.recent.chunked(4).forEachIndexed { row, weeks ->
                if (row > 0) Spacer(GlanceModifier.height(5.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    weeks.forEachIndexed { i, active ->
                        if (i > 0) Spacer(GlanceModifier.width(5.dp))
                        val isThisWeek = row == 2 && i == 3
                        Box(
                            GlanceModifier.defaultWeight().height(18.dp).cornerRadius(if (isThisWeek) 9.dp else 6.dp)
                                .background(if (active) (if (isThisWeek) p.current else p.bar) else p.future)
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberStreak(streak: Streak, s: WidgetSettings, p: GlancePalette) {
    WidgetFrame(p, padding = 18.dp) {
        Column(GlanceModifier.fillMaxSize()) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Chip(chipName(s.filter), p, icon = iconFor(s.filter))
                Spacer(GlanceModifier.defaultWeight())
                if (streak.thisWeekActive) {
                    Image(ImageProvider(R.drawable.ic_flame), null, GlanceModifier.size(22.dp), colorFilter = ColorFilter.tint(p.current))
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(streak.weeks.toString(), style = TextStyle(color = p.accent, fontSize = 64.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text(streakCaption(streak), style = TextStyle(color = p.fg, fontSize = 15.sp, fontWeight = FontWeight.Bold))
        }
    }
}
