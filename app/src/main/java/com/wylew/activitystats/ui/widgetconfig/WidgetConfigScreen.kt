package com.wylew.activitystats.ui.widgetconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupCalculator
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.ui.components.RollupBars
import com.wylew.activitystats.widget.WidgetColor
import com.wylew.activitystats.widget.WidgetMetric
import com.wylew.activitystats.widget.WidgetPeriod
import com.wylew.activitystats.widget.WidgetSettings
import com.wylew.activitystats.widget.headline
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun WidgetConfigScreen(
    initial: WidgetSettings,
    activities: List<Activity>,
    units: Units,
    connected: Boolean,
    onConnect: () -> Unit,
    onClose: () -> Unit,
    onSave: (WidgetSettings) -> Unit,
) {
    var s by remember { mutableStateOf(initial) }
    val rollup = remember(s, activities) {
        RollupCalculator.compute(activities, s.filter, s.period.rollup, LocalDate.now(), ZoneId.systemDefault())
    }

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
                Text("Set up widget", style = MaterialTheme.typography.titleLarge)
            }

            // Live preview on a wallpaper-ish backdrop.
            Box(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                WidgetPreview(rollup, s, units)
            }

            if (!connected) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Connect Strava to fill this widget with your activities.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(onClick = onConnect) { Text("Connect Strava") }
                }
            }

            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Section("Activity") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActivityFilter.entries.forEach { f ->
                            FilterChip(
                                selected = s.filter == f,
                                onClick = { s = s.copy(filter = f) },
                                label = { Text(f.label) },
                            )
                        }
                    }
                }

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
                        if (s.period == WidgetPeriod.WEEKLY) "Monday to Sunday. Resets every Monday." else "Today only. Resets at midnight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Section("Headline number") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetMetric.entries.forEach { m ->
                            FilterChip(selected = s.metric == m, onClick = { s = s.copy(metric = m) }, label = { Text(m.label) })
                        }
                    }
                }

                Section("Color") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WidgetColor.entries.forEach { c ->
                            val (bg, _) = previewColors(c)
                            val selected = s.color == c
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .background(bg, if (selected) RoundedCornerShape(16.dp) else CircleShape)
                                    .border(
                                        2.dp,
                                        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                        if (selected) RoundedCornerShape(16.dp) else CircleShape,
                                    )
                                    .selectable(selected = selected, role = Role.RadioButton, onClick = { s = s.copy(color = c) })
                            )
                        }
                    }
                }

                Button(onClick = { onSave(s) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Save widget", fontWeight = FontWeight.Bold)
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

@Composable
private fun previewColors(c: WidgetColor): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    return when (c) {
        WidgetColor.NEUTRAL -> cs.surfaceContainerLow to cs.onSurface
        WidgetColor.PRIMARY -> cs.primary to cs.onPrimary
        WidgetColor.TERTIARY -> cs.tertiaryContainer to cs.onTertiaryContainer
    }
}

/** Compose mirror of the Glance widget at 4x2, so the choice is visible before saving. */
@Composable
fun WidgetPreview(r: Rollup, s: WidgetSettings, units: Units) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg) = previewColors(s.color)
    val accent = when (s.color) {
        WidgetColor.NEUTRAL -> cs.primary
        WidgetColor.PRIMARY -> cs.onPrimary
        WidgetColor.TERTIARY -> cs.tertiary
    }
    val bar = when (s.color) {
        WidgetColor.NEUTRAL -> cs.primary
        WidgetColor.PRIMARY -> cs.primaryContainer
        WidgetColor.TERTIARY -> cs.tertiary
    }
    val current = if (s.color == WidgetColor.TERTIARY) cs.primary else cs.tertiary
    val h = headline(r, s, units)

    Row(
        Modifier
            .fillMaxWidth()
            .height(184.dp)
            .background(bg, RoundedCornerShape(28.dp))
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                h.chip,
                color = if (s.color == WidgetColor.TERTIARY) cs.onTertiary else cs.onPrimaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(if (s.color == WidgetColor.TERTIARY) cs.tertiary else cs.primaryContainer, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(h.big, color = accent, fontSize = if (h.big.length > 3) 44.sp else 56.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp)
                    Spacer(Modifier.width(6.dp))
                    Text(h.bigUnit, color = fg, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
                }
                Text(h.second, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
            if (s.period == WidgetPeriod.WEEKLY) {
                RollupBars(
                    r.buckets,
                    barColor = bar,
                    currentColor = current,
                    futureColor = fg.copy(alpha = 0.3f),
                    labelColor = fg.copy(alpha = 0.8f),
                    maxBarHeight = 100.dp,
                )
            } else {
                val fmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    if (r.items.isEmpty()) Text("No ${s.filter.plural} yet today", color = fg.copy(alpha = 0.8f), fontSize = 13.sp)
                    r.items.take(3).forEach { a ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(fg.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("${Format.distance(a.distanceM, units)} ${Format.distanceUnit(units)}", color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(
                                Instant.ofEpochSecond(a.startEpochSec).atZone(ZoneId.systemDefault()).toLocalTime().format(fmt),
                                color = fg.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
