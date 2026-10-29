package com.wylew.activitystats.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
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
import androidx.glance.unit.ColorProvider
import com.wylew.activitystats.MainActivity
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupCalculator
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.graph
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class RollupWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RollupWidget()
}

object Widgets {
    suspend fun updateAll(context: Context) {
        RollupWidget().updateAll(context)
    }
}

/**
 * One configurable widget: Run / Ride / All, Daily or Weekly.
 * 2x2 shows the headline; 4x2 and wider adds day bars (weekly) or today's list (daily).
 */
class RollupWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = context.graph
        val initialActivities = graph.repository.all()
        val initialUnits = graph.settings.units.first()

        provideContent {
            val activities by graph.repository.activities.collectAsState(initialActivities)
            val units by graph.settings.units.collectAsState(initialUnits)
            val settings = WidgetPrefs.read(currentState<Preferences>())
            val rollup = RollupCalculator.compute(
                activities, settings.filter, settings.period.rollup, LocalDate.now(), ZoneId.systemDefault()
            )
            GlanceTheme {
                WidgetContent(rollup, settings, units)
            }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
    }
}

private data class Palette(
    val bg: ColorProvider,
    val fg: ColorProvider,
    val sub: ColorProvider,
    val accent: ColorProvider,
    val bar: ColorProvider,
    val current: ColorProvider,
    val future: ColorProvider,
    val chipBg: ColorProvider,
    val chipFg: ColorProvider,
    val row: ColorProvider,
)

@Composable
private fun palette(color: WidgetColor): Palette {
    val c = GlanceTheme.colors
    return when (color) {
        WidgetColor.NEUTRAL -> Palette(
            bg = c.widgetBackground, fg = c.onSurface, sub = c.onSurfaceVariant, accent = c.primary,
            bar = c.primary, current = c.tertiary, future = c.surfaceVariant,
            chipBg = c.primaryContainer, chipFg = c.onPrimaryContainer, row = c.surfaceVariant,
        )
        WidgetColor.PRIMARY -> Palette(
            bg = c.primary, fg = c.onPrimary, sub = c.onPrimary, accent = c.onPrimary,
            bar = c.primaryContainer, current = c.tertiaryContainer, future = c.inversePrimary,
            chipBg = c.primaryContainer, chipFg = c.onPrimaryContainer, row = c.inversePrimary,
        )
        WidgetColor.TERTIARY -> Palette(
            bg = c.tertiaryContainer, fg = c.onTertiaryContainer, sub = c.onTertiaryContainer, accent = c.tertiary,
            bar = c.tertiary, current = c.primary, future = c.surface,
            chipBg = c.tertiary, chipFg = c.onTertiary, row = c.surface,
        )
    }
}

@Composable
private fun WidgetContent(r: Rollup, s: WidgetSettings, units: Units) {
    val p = palette(s.color)
    val wide = LocalSize.current.width >= RollupWidget.WIDE.width
    val h = headline(r, s, units)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(p.bg)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
    ) {
        if (wide) {
            Row(GlanceModifier.fillMaxSize()) {
                Summary(h, p, big = 52, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                Spacer(GlanceModifier.width(12.dp))
                Box(GlanceModifier.defaultWeight().fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    if (s.period == WidgetPeriod.WEEKLY) Bars(r, p) else TodayList(r, s, units, p)
                }
            }
        } else {
            Summary(h, p, big = 44, modifier = GlanceModifier.fillMaxSize())
        }
    }
}

@Composable
private fun Summary(h: Headline, p: Palette, big: Int, modifier: GlanceModifier) {
    Column(modifier) {
        Box(GlanceModifier.background(p.chipBg).cornerRadius(14.dp).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(h.chip, style = TextStyle(color = p.chipFg, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(verticalAlignment = Alignment.Bottom) {
            Text(h.big, style = TextStyle(color = p.accent, fontSize = big.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Spacer(GlanceModifier.width(4.dp))
            Text(
                h.bigUnit,
                style = TextStyle(color = p.fg, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.padding(bottom = 8.dp),
                maxLines = 1,
            )
        }
        Text(h.second, style = TextStyle(color = p.fg, fontSize = 17.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun Bars(r: Rollup, p: Palette) {
    val max = r.buckets.maxOfOrNull { it.distanceM }?.takeIf { it > 0 } ?: 1.0
    Row(GlanceModifier.fillMaxWidth().height(108.dp), verticalAlignment = Alignment.Bottom) {
        r.buckets.forEach { b ->
            Column(
                GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val (height, color) = when {
                    b.isFuture -> 14 to p.future
                    b.distanceM <= 0.0 -> 6 to p.future
                    else -> maxOf(10, (b.distanceM / max * 80).toInt()) to (if (b.isCurrent) p.current else p.bar)
                }
                val width = if (b.isFuture || b.distanceM > 0.0) 14 else 6
                Box(GlanceModifier.width(width.dp).height(height.dp).cornerRadius(7.dp).background(color)) {}
                Spacer(GlanceModifier.height(4.dp))
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
private fun TodayList(r: Rollup, s: WidgetSettings, units: Units, p: Palette) {
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
