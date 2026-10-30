package com.wylew.activitystats.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wylew.activitystats.domain.Goals
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.graph
import com.wylew.activitystats.ui.widgetconfig.WallpaperBackdrop
import com.wylew.activitystats.ui.widgetconfig.WidgetPreview
import com.wylew.activitystats.widget.GoalRingWidgetReceiver
import com.wylew.activitystats.widget.LatestWidgetReceiver
import com.wylew.activitystats.widget.RollupWidgetReceiver
import com.wylew.activitystats.widget.StreakWidgetReceiver
import com.wylew.activitystats.widget.WidgetKind
import com.wylew.activitystats.widget.WidgetSettings

/** Gallery of the four widgets with live previews and a one-tap "Add to home screen". */
@Composable
fun WidgetsScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val graph = context.graph
    val activities by graph.repository.activities.collectAsState(emptyList())
    val units by graph.settings.units.collectAsState(Units.IMPERIAL)
    val goals by graph.settings.goals.collectAsState(Goals())
    val canPin = remember { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Widgets", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Add one to your home screen, then long-press it and tap the pencil to pick Run, Ride or All, daily or weekly, and its color.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(WidgetKind.entries.toList()) { kind ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WallpaperBackdrop {
                        WidgetPreview(kind, WidgetSettings.defaultsFor(kind), activities, units, goals)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${kind.title} · ${kind.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(kind.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (canPin) {
                            FilledTonalButton(onClick = { pin(context, kind) }) { Text("Add") }
                        }
                    }
                }
            }
        }
        if (!canPin) {
            item {
                Text(
                    "Your launcher doesn't support adding widgets from apps. Long-press the home screen → Widgets → Activity Stats.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun pin(context: Context, kind: WidgetKind) {
    val receiver = when (kind) {
        WidgetKind.ROLLUP -> RollupWidgetReceiver::class.java
        WidgetKind.GOAL -> GoalRingWidgetReceiver::class.java
        WidgetKind.STREAK -> StreakWidgetReceiver::class.java
        WidgetKind.LATEST -> LatestWidgetReceiver::class.java
    }
    AppWidgetManager.getInstance(context).requestPinAppWidget(ComponentName(context, receiver), null, null)
}
