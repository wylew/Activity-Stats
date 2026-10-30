package com.wylew.activitystats.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.graph
import com.wylew.activitystats.ui.widgetconfig.WallpaperBackdrop
import com.wylew.activitystats.ui.widgetconfig.WidgetPreview
import com.wylew.activitystats.widget.WidgetKind
import com.wylew.activitystats.widget.WidgetPinnedReceiver
import com.wylew.activitystats.widget.WidgetSettings
import com.wylew.activitystats.widget.WidgetStyle
import com.wylew.activitystats.widget.Widgets

/** Each widget with its three styles previewed live, and "Add" to pin the one shown. */
@Composable
fun WidgetsScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val graph = context.graph
    val activities by graph.repository.activities.collectAsState(emptyList())
    val units by graph.settings.units.collectAsState(Units.IMPERIAL)
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
                    "Pick a style and add it. Long-press a placed widget and tap the pencil to choose Run, Ride or All, the time period and color.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(WidgetKind.entries.toList()) { kind ->
            var style by rememberSaveable(kind) { mutableStateOf(kind.styles.first()) }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${kind.title} · ${kind.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(kind.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (canPin) {
                            FilledTonalButton(onClick = { pin(context, kind, style) }) { Text("Add") }
                        }
                    }
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        kind.styles.forEachIndexed { i, st ->
                            SegmentedButton(
                                selected = style == st,
                                onClick = { style = st },
                                shape = SegmentedButtonDefaults.itemShape(i, kind.styles.size),
                            ) { Text(st.label, maxLines = 1) }
                        }
                    }
                    WallpaperBackdrop {
                        WidgetPreview(kind, WidgetSettings.defaultsFor(kind, style), activities, units)
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

/** Asks the launcher to place the widget; [WidgetPinnedReceiver] then applies [style]. */
private fun pin(context: Context, kind: WidgetKind, style: WidgetStyle) {
    val callback = PendingIntent.getBroadcast(
        context,
        kind.ordinal * 10 + style.ordinal,
        Intent(context, WidgetPinnedReceiver::class.java)
            .putExtra(WidgetPinnedReceiver.EXTRA_KIND, kind.name)
            .putExtra(WidgetPinnedReceiver.EXTRA_STYLE, style.name),
        // Mutable so the system can add the new widget's id.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
    AppWidgetManager.getInstance(context)
        .requestPinAppWidget(ComponentName(context, Widgets.receiverFor(kind)), null, callback)
}
