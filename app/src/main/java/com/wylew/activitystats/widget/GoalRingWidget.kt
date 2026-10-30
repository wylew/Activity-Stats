package com.wylew.activitystats.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.RollupCalculator
import java.time.LocalDate
import java.time.ZoneId

class GoalRingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoalRingWidget()
}

/** 2×2 wavy progress ring toward the daily or weekly distance goal. */
internal class GoalRingWidget : ActivityStatsWidget(WidgetKind.GOAL) {

    override val sizeMode = SizeMode.Exact

    @Composable
    override fun Content(context: Context, state: WidgetState) {
        val s = state.settings
        val units = state.inputs.units
        val r = RollupCalculator.compute(state.activities, s.filter, s.period.rollup, LocalDate.now(), ZoneId.systemDefault())
        val goal = state.inputs.goals.forPeriod(s.filter, s.period.rollup)
        val progress = if (goal > 0) (r.distanceM / goal).toFloat() else 0f
        val p = state.palette

        val size = LocalSize.current
        val ring = (min(size.width, size.height) - 20.dp).coerceAtLeast(80.dp)
        val px = (ring.value * context.resources.displayMetrics.density).toInt()
        val stroke = px * 0.055f
        val wave = remember(px, progress) { WidgetBitmaps.wavyArc(px, stroke, progress) }
        val track = remember(px, progress) { WidgetBitmaps.track(px, stroke, progress) }
        val big = if (ring.value > 130) 34 else 26

        WidgetFrame(p, padding = 10) {
            Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    ImageProvider(track), contentDescription = null,
                    modifier = GlanceModifier.size(ring), colorFilter = ColorFilter.tint(p.future),
                )
                Image(
                    ImageProvider(wave),
                    contentDescription = "${(progress * 100).toInt()} percent of goal",
                    modifier = GlanceModifier.size(ring), colorFilter = ColorFilter.tint(p.accent),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Format.distance(r.distanceM, units),
                        style = TextStyle(color = p.fg, fontSize = big.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Text(
                        "of ${Format.distance(goal, units)} ${Format.distanceUnit(units)}",
                        style = TextStyle(color = p.fg, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                    )
                    Text(
                        "${chipName(s.filter)} · ${s.period.chipSuffix}",
                        style = TextStyle(color = p.sub, fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
