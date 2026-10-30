package com.wylew.activitystats.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import com.wylew.activitystats.MainActivity
import com.wylew.activitystats.R
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.Dedupe
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.graph
import com.wylew.activitystats.theme.ThemeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object Widgets {
    /** Redraw every widget, e.g. after a sync or a settings change. */
    suspend fun updateAll(context: Context) {
        TotalsWidget().updateAll(context)
        StreakWidget().updateAll(context)
        LatestWidget().updateAll(context)
    }

    fun widgetFor(kind: WidgetKind): GlanceAppWidget = when (kind) {
        WidgetKind.TOTALS -> TotalsWidget()
        WidgetKind.STREAK -> StreakWidget()
        WidgetKind.LATEST -> LatestWidget()
    }

    fun receiverFor(kind: WidgetKind): Class<*> = when (kind) {
        WidgetKind.TOTALS -> TotalsWidgetReceiver::class.java
        WidgetKind.STREAK -> StreakWidgetReceiver::class.java
        WidgetKind.LATEST -> LatestWidgetReceiver::class.java
    }

    fun kindOfReceiver(className: String?): WidgetKind = when (className) {
        StreakWidgetReceiver::class.java.name -> WidgetKind.STREAK
        LatestWidgetReceiver::class.java.name -> WidgetKind.LATEST
        else -> WidgetKind.TOTALS
    }
}

/**
 * Gets the system's "widget pinned" callback from the in-app gallery and applies the style
 * the user was previewing, so "Add" places exactly what they saw.
 */
class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { k -> WidgetKind.entries.firstOrNull { it.name == k } } ?: return
        val style = intent.getStringExtra(EXTRA_STYLE)?.let { s -> kind.styles.firstOrNull { it.name == s } } ?: return
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(id)
                updateAppWidgetState(context, glanceId) { WidgetPrefs.write(it, WidgetSettings.defaultsFor(kind, style)) }
                Widgets.widgetFor(kind).update(context, glanceId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_STYLE = "style"
    }
}

/** Everything a widget draws from, loaded once and then kept live while the widget session runs. */
data class WidgetInputs(val activities: List<Activity>, val units: Units, val theme: ThemeSettings)

internal suspend fun loadInputs(context: Context): WidgetInputs {
    val g = context.graph
    return WidgetInputs(g.repository.all(), g.settings.units.first(), g.settings.theme.first())
}

internal class WidgetState(
    val inputs: WidgetInputs,
    val settings: WidgetSettings,
    val palette: GlancePalette,
) {
    val activities: List<Activity> get() = inputs.activities
}

@Composable
internal fun rememberWidgetState(context: Context, kind: WidgetKind, initial: WidgetInputs): WidgetState {
    val g = context.graph
    val raw by g.store.activities.collectAsState(null)
    val units by g.settings.units.collectAsState(initial.units)
    val theme by g.settings.theme.collectAsState(initial.theme)
    val activities = remember(raw) { raw?.let { Dedupe.apply(it) } ?: initial.activities }
    val settings = WidgetPrefs.read(currentState<Preferences>(), kind)
    val palette = remember(theme, settings.color) { glancePalette(context, theme, settings.color) }
    return WidgetState(WidgetInputs(activities, units, theme), settings, palette)
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

/** Rounded widget surface that opens the app. */
@Composable
internal fun WidgetFrame(p: GlancePalette, padding: Dp = 16.dp, radius: Dp? = null, content: @Composable () -> Unit) {
    val base = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .background(p.bg)
    val rounded = if (radius != null) base.cornerRadius(radius) else base.cornerRadius(android.R.dimen.system_app_widget_background_radius)
    Box(
        modifier = rounded.clickable(actionStartActivity<MainActivity>()).padding(padding),
    ) { content() }
}

@Composable
internal fun Chip(text: String, p: GlancePalette, @DrawableRes icon: Int? = null) {
    Row(
        GlanceModifier.background(p.chipBg).cornerRadius(14.dp).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(ImageProvider(icon), null, GlanceModifier.size(14.dp), colorFilter = ColorFilter.tint(p.chipFg))
            Spacer(GlanceModifier.width(5.dp))
        }
        Text(text, style = TextStyle(color = p.chipFg, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

/** Scalloped badge with text inside, tinted to the widget's accent. */
@Composable
internal fun Badge(context: Context, text: String, p: GlancePalette, size: Dp = 64.dp, fontSize: TextUnit = 28.sp) {
    val px = (size.value * context.resources.displayMetrics.density).toInt()
    val cookie = remember(px) { WidgetBitmaps.cookie(px) }
    Box(GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        Image(ImageProvider(cookie), null, GlanceModifier.size(size), colorFilter = ColorFilter.tint(p.accent))
        Text(text, style = TextStyle(color = p.bg, fontSize = fontSize, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

/** A row of pill dots: on, off, or not yet (future). */
@Composable
internal fun Dots(states: List<DotState>, p: GlancePalette, height: Dp = 10.dp) {
    Row(GlanceModifier.fillMaxWidth()) {
        states.forEachIndexed { i, state ->
            if (i > 0) Spacer(GlanceModifier.width(3.dp))
            Box(
                GlanceModifier.defaultWeight().height(height).cornerRadius(height / 2)
                    .background(
                        when (state) {
                            DotState.ON -> p.fg
                            DotState.OFF -> p.future
                            DotState.FUTURE -> p.row
                        }
                    )
            ) {}
        }
    }
}

enum class DotState { ON, OFF, FUTURE }

@DrawableRes
internal fun iconFor(filter: ActivityFilter): Int = when (filter) {
    ActivityFilter.RUN -> R.drawable.ic_run
    ActivityFilter.RIDE -> R.drawable.ic_ride
    ActivityFilter.ALL -> R.drawable.ic_all
}
