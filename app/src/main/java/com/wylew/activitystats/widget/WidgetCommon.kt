package com.wylew.activitystats.widget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wylew.activitystats.MainActivity
import com.wylew.activitystats.R
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.ActivityView
import com.wylew.activitystats.domain.Goals
import com.wylew.activitystats.domain.SourcePrefs
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.graph
import com.wylew.activitystats.theme.ThemeSettings
import kotlinx.coroutines.flow.first

object Widgets {
    /** Redraw every widget of every kind, e.g. after a sync or a settings change. */
    suspend fun updateAll(context: Context) {
        RollupWidget().updateAll(context)
        GoalRingWidget().updateAll(context)
        StreakWidget().updateAll(context)
        LatestWidget().updateAll(context)
    }
}

/** Everything a widget draws from, loaded once and then kept live while the widget session runs. */
data class WidgetInputs(
    val raw: List<Activity>,
    val sources: SourcePrefs,
    val units: Units,
    val theme: ThemeSettings,
    val goals: Goals,
)

internal suspend fun loadInputs(context: Context): WidgetInputs {
    val g = context.graph
    return WidgetInputs(
        raw = g.store.all(),
        sources = g.settings.sources.first(),
        units = g.settings.units.first(),
        theme = g.settings.theme.first(),
        goals = g.settings.goals.first(),
    )
}

/** Live inputs plus this widget's own settings and filtered activities. */
internal class WidgetState(
    val inputs: WidgetInputs,
    val settings: WidgetSettings,
    val activities: List<Activity>,
    val palette: GlancePalette,
)

@Composable
internal fun rememberWidgetState(context: Context, kind: WidgetKind, initial: WidgetInputs): WidgetState {
    val g = context.graph
    val raw by g.store.activities.collectAsState(initial.raw)
    val sources by g.settings.sources.collectAsState(initial.sources)
    val units by g.settings.units.collectAsState(initial.units)
    val theme by g.settings.theme.collectAsState(initial.theme)
    val goals by g.settings.goals.collectAsState(initial.goals)
    val settings = WidgetPrefs.read(currentState<Preferences>(), kind)
    val activities = remember(raw, sources, settings.source) { ActivityView.apply(raw, sources, settings.source.choice) }
    val palette = remember(theme, settings.color) { glancePalette(context, theme, settings.color) }
    return WidgetState(WidgetInputs(raw, sources, units, theme, goals), settings, activities, palette)
}

/** Rounded widget surface that opens the app. */
@Composable
internal fun WidgetFrame(p: GlancePalette, padding: Int = 16, content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(p.bg)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .clickable(actionStartActivity<MainActivity>())
            .padding(padding.dp),
    ) { content() }
}

@Composable
internal fun Chip(text: String, p: GlancePalette) {
    Box(GlanceModifier.background(p.chipBg).cornerRadius(14.dp).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, style = TextStyle(color = p.chipFg, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

@DrawableRes
internal fun iconFor(filter: ActivityFilter): Int = when (filter) {
    ActivityFilter.RUN -> R.drawable.ic_run
    ActivityFilter.RIDE -> R.drawable.ic_ride
    ActivityFilter.ALL -> R.drawable.ic_all
}

/** Shared base: load inputs, then compose. */
internal abstract class ActivityStatsWidget(private val kind: WidgetKind) : GlanceAppWidget() {
    @Composable
    abstract fun Content(context: Context, state: WidgetState)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = loadInputs(context)
        provideContent {
            val state = rememberWidgetState(context, kind, initial)
            Content(context, state)
        }
    }
}
