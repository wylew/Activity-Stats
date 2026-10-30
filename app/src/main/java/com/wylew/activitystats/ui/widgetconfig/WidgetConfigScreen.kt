package com.wylew.activitystats.ui.widgetconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.widget.WidgetColor
import com.wylew.activitystats.widget.WidgetKind
import com.wylew.activitystats.widget.WidgetMetric
import com.wylew.activitystats.widget.WidgetPeriod
import com.wylew.activitystats.widget.WidgetSettings
import com.wylew.activitystats.widget.iconFor
import com.wylew.activitystats.widget.widgetPalette

@Composable
fun WidgetConfigScreen(
    kind: WidgetKind,
    initial: WidgetSettings,
    activities: List<Activity>,
    units: Units,
    connected: Boolean,
    onConnect: () -> Unit,
    onClose: () -> Unit,
    onSave: (WidgetSettings) -> Unit,
) {
    var s by remember { mutableStateOf(initial) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                Column {
                    Text("Set up widget", style = MaterialTheme.typography.titleLarge)
                    Text("${kind.title} · ${kind.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            WallpaperBackdrop(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                WidgetPreview(kind, s, activities, units)
            }

            if (!connected) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Connect Health Connect in Activity Stats to fill this widget. You can save it now; it fills in after the first sync.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(onClick = onConnect) { Text("Open Activity Stats") }
                }
            }

            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Section("Style") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        kind.styles.forEachIndexed { i, style ->
                            SegmentedButton(
                                selected = s.style == style,
                                onClick = {
                                    // Switching style picks up that style's color default unless the user already chose one.
                                    val d = WidgetSettings.defaultsFor(kind, style)
                                    s = s.copy(style = style, color = if (s.color == WidgetSettings.defaultsFor(kind, s.style).color) d.color else s.color)
                                },
                                shape = SegmentedButtonDefaults.itemShape(i, kind.styles.size),
                            ) { Text(style.label, maxLines = 1) }
                        }
                    }
                }

                Section("Activity") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActivityFilter.entries.forEach { f ->
                            FilterChip(
                                selected = s.filter == f,
                                onClick = { s = s.copy(filter = f) },
                                label = { Text(f.label) },
                                leadingIcon = {
                                    Icon(painterResource(iconFor(f)), null, Modifier.size(FilterChipDefaults.IconSize))
                                },
                                modifier = Modifier.height(44.dp),
                            )
                        }
                    }
                }

                if (kind.usesPeriod) {
                    Section("Roll-up") {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            WidgetPeriod.entries.forEachIndexed { i, p ->
                                SegmentedButton(
                                    selected = s.period == p,
                                    onClick = { s = s.copy(period = p) },
                                    shape = SegmentedButtonDefaults.itemShape(i, WidgetPeriod.entries.size),
                                ) { Text(p.label) }
                            }
                        }
                        Text(
                            when (s.period) {
                                WidgetPeriod.DAILY -> "Today only. Resets at midnight."
                                WidgetPeriod.WEEKLY -> "Monday to Sunday. Resets every Monday."
                                WidgetPeriod.MONTHLY -> "This calendar month. Resets on the 1st."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }


                if (kind.usesMetric) {
                    Section("Headline number") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WidgetMetric.entries.forEach { m ->
                                FilterChip(
                                    selected = s.metric == m,
                                    onClick = { s = s.copy(metric = m) },
                                    label = { Text(m.label) },
                                    modifier = Modifier.height(44.dp),
                                )
                            }
                        }
                    }
                }


                Section("Color") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WidgetColor.entries.forEach { c ->
                            val selected = s.color == c
                            val shape = if (selected) RoundedCornerShape(16.dp) else CircleShape
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.widgetPalette(c).bg, shape)
                                    .border(
                                        2.dp,
                                        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                        shape,
                                    )
                                    .selectable(selected = selected, role = Role.RadioButton, onClick = { s = s.copy(color = c) })
                            )
                        }
                    }
                }

                Button(onClick = { onSave(s) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Add to home screen", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
