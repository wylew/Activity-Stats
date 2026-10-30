package com.wylew.activitystats.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wylew.activitystats.R
import com.wylew.activitystats.domain.Streaks
import java.time.LocalDate
import java.time.ZoneId

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

/** 2×2: weeks in a row with an activity, in a cookie badge, plus the last 12 weeks as dots. */
internal class StreakWidget : ActivityStatsWidget(WidgetKind.STREAK) {

    @Composable
    override fun Content(context: Context, state: WidgetState) {
        val s = state.settings
        val p = state.palette
        val streak = Streaks.weekly(state.activities, s.filter, LocalDate.now(), ZoneId.systemDefault())
        val badgePx = (64 * context.resources.displayMetrics.density).toInt()
        val cookie = remember(badgePx) { WidgetBitmaps.cookie(badgePx) }

        WidgetFrame(p) {
            Column(GlanceModifier.fillMaxSize()) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(GlanceModifier.size(64.dp), contentAlignment = Alignment.Center) {
                        Image(
                            ImageProvider(cookie), contentDescription = null,
                            modifier = GlanceModifier.size(64.dp), colorFilter = ColorFilter.tint(p.accent),
                        )
                        Text(
                            streak.weeks.toString(),
                            style = TextStyle(color = p.bg, fontSize = 28.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    Image(
                        ImageProvider(if (streak.thisWeekActive) R.drawable.ic_flame else iconFor(s.filter)),
                        contentDescription = null,
                        modifier = GlanceModifier.size(22.dp),
                        colorFilter = ColorFilter.tint(p.fg),
                    )
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    if (streak.weeks == 0) "Start a streak" else "week streak",
                    style = TextStyle(color = p.fg, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    "${chipName(s.filter)} · ${if (streak.thisWeekActive) "this week done" else "this week open"}",
                    style = TextStyle(color = p.sub, fontSize = 11.sp),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    streak.recent.forEachIndexed { i, active ->
                        if (i > 0) Spacer(GlanceModifier.width(3.dp))
                        Box(
                            GlanceModifier.defaultWeight().height(10.dp).cornerRadius(5.dp)
                                .background(if (active) p.fg else p.future)
                        ) {}
                    }
                }
            }
        }
    }
}
