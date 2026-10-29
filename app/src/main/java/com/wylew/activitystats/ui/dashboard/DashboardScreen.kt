package com.wylew.activitystats.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wylew.activitystats.data.healthconnect.HcStatus
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupPeriod
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.ui.components.CookieShape
import com.wylew.activitystats.ui.components.RollupBars
import com.wylew.activitystats.ui.components.WavyProgress
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(onOpenSettings: () -> Unit, vm: DashboardViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val hcPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted -> vm.onHealthConnectResult(granted) }

    LifecycleResumeEffect(Unit) {
        vm.refreshSources()
        onPauseOrDispose { }
    }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.messageShown() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(state, syncing, onSync = vm::sync, onOpenSettings = onOpenSettings) }
            item { PeriodPicker(state.period, vm::selectPeriod) }
            item { FilterRow(state.filter, vm::selectFilter) }

            if (!state.loading && !state.hasSource) {
                item {
                    SourcesCard(
                        status = state.healthConnect,
                        onAllow = { hcPermissionLauncher.launch(vm.healthConnectPermissions) },
                        onInstall = vm::installHealthConnect,
                        onStrava = vm::connectStrava,
                    )
                }
            }

            val r = state.rollup
            if (r != null) {
                item { HeroCard(r, state.previous, state.units) }
                item { StatGrid(r, state.units) }
                item { GoalCard(r, state.goalM, state.units) }
                item {
                    Text(
                        "Recent",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                    )
                }
                if (r.items.isEmpty()) {
                    item {
                        Text(
                            "No ${r.filter.plural} in this period yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                val recent = r.items.take(8)
                items(recent.size, key = { recent[it].id }) { i ->
                    ActivityRow(recent[i], state.units, groupShape(i, recent.size))
                }
                item { SyncFooter(state) }
            }
        }
    }
}

@Composable
private fun Header(state: DashboardState, syncing: Boolean, onSync: () -> Unit, onOpenSettings: () -> Unit) {
    val r = state.rollup
    Column(Modifier.padding(start = 4.dp, top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Activity Stats",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(onClick = onSync, enabled = state.hasSource && !syncing, modifier = Modifier.size(48.dp)) {
                if (syncing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Refresh, contentDescription = "Sync now")
            }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
        }
        Text(title(state.period, r?.start), style = MaterialTheme.typography.displaySmall)
        if (r != null) {
            Text(rangeText(r), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PeriodPicker(selected: RollupPeriod, onSelect: (RollupPeriod) -> Unit) {
    val options = listOf(RollupPeriod.WEEKLY to "Week", RollupPeriod.MONTHLY to "Month", RollupPeriod.YEARLY to "Year")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (p, label) ->
            SegmentedButton(
                selected = selected == p,
                onClick = { onSelect(p) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label) }
        }
    }
}

@Composable
private fun FilterRow(selected: ActivityFilter, onSelect: (ActivityFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ActivityFilter.entries.toList()) { f ->
            FilterChip(selected = selected == f, onClick = { onSelect(f) }, label = { Text(f.label) })
        }
    }
}

@Composable
private fun SourcesCard(status: HcStatus, onAllow: () -> Unit, onInstall: () -> Unit, onStrava: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Bring in your workouts", style = MaterialTheme.typography.titleLarge)
            when (status) {
                HcStatus.UNAVAILABLE -> {
                    Text(
                        "Health Connect isn't available on this phone. You can connect the Strava API instead (needs a Strava subscription).",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = onStrava) { Text("Connect Strava API") }
                }
                HcStatus.NEEDS_INSTALL -> {
                    Text(
                        "Activity Stats reads from Health Connect, Google's free on-phone hub for fitness data. Install it first.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = onInstall) { Text("Install Health Connect") }
                }
                HcStatus.NO_PERMISSION, HcStatus.READY -> {
                    Text("1. In Garmin Connect: More → Settings → Health Connect, and allow sharing.", style = MaterialTheme.typography.bodyMedium)
                    Text("2. In Strava: You → Settings → Manage Apps and Devices → Health Connect.", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Let Activity Stats read your workouts:", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onAllow) { Text("Allow Health Connect access") }
                    Text(
                        "Your data stays on this phone. Turn on background access so widgets update while the app is closed.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (status != HcStatus.UNAVAILABLE) {
                TextButton(onClick = onStrava) { Text("Or use the Strava API (subscription required)") }
            }
        }
    }
}

@Composable
private fun HeroCard(r: Rollup, previous: Rollup?, units: Units) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.primary, contentColor = cs.onPrimary, shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Distance", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(Format.distance(r.distanceM, units), fontSize = 64.sp, lineHeight = 64.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)
                        Spacer(Modifier.width(6.dp))
                        Text(Format.distanceUnit(units), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
                Box(
                    Modifier.size(76.dp).background(cs.primaryContainer, CookieShape()),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(r.count.toString(), color = cs.onPrimaryContainer, fontSize = 28.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text(r.filter.noun(r.count), color = cs.onPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (previous != null) {
                Text(
                    deltaText(r, previous, units),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(cs.onPrimary.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            if (r.buckets.isNotEmpty()) {
                RollupBars(
                    r.buckets,
                    barColor = cs.primaryContainer,
                    currentColor = cs.tertiaryContainer,
                    futureColor = cs.onPrimary.copy(alpha = 0.4f),
                    labelColor = cs.primaryContainer,
                    maxBarHeight = 100.dp,
                    modifier = Modifier.height(128.dp),
                )
            }
        }
    }
}

@Composable
private fun StatGrid(r: Rollup, units: Units) {
    val cs = MaterialTheme.colorScheme
    val stats = listOf(
        Triple("Moving time", Format.duration(r.movingSec), if (r.movingSec >= 3600) "h" else "min"),
        Triple("Avg pace", Format.pace(r.distanceM, r.movingSec, units), Format.paceUnit(units)),
        Triple("Elevation", Format.elevation(r.elevationM, units), Format.elevationUnit(units)),
        Triple("Avg heart rate", r.avgHr?.let { "%.0f".format(it) } ?: "–", "bpm"),
    )
    val tiles = listOf(
        cs.primaryContainer to cs.onPrimaryContainer,
        cs.tertiaryContainer to cs.onTertiaryContainer,
        cs.secondaryContainer to cs.onSecondaryContainer,
        cs.tertiaryContainer to cs.onTertiaryContainer,
    )
    val shapes: List<Shape> = listOf(
        CookieShape(lobes = 8, depth = 0.08f),
        RoundedCornerShape(14.dp),
        RoundedCornerShape(50, 50, 14, 50),
        RoundedCornerShape(14, 50, 50, 50),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEachIndexed { col, (label, value, unit) ->
                    val i = row * 2 + col
                    Surface(
                        color = cs.surfaceContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f).heightIn(min = 120.dp),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(40.dp).background(tiles[i].first, shapes[i]), contentAlignment = Alignment.Center) {
                                Text(label.first().toString(), color = tiles[i].second, fontWeight = FontWeight.ExtraBold)
                            }
                            Column {
                                Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(unit, color = cs.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(r: Rollup, goalM: Double, units: Units) {
    if (goalM <= 0) return
    val cs = MaterialTheme.colorScheme
    val progress = (r.distanceM / goalM).toFloat()
    Surface(color = cs.tertiaryContainer, contentColor = cs.onTertiaryContainer, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                Text("Goal", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "${Format.distance(r.distanceM, units)} of ${Format.distance(goalM, units)} ${Format.distanceUnit(units)} · ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            WavyProgress(progress, color = cs.tertiary, trackColor = cs.tertiary.copy(alpha = 0.25f))
        }
    }
}

@Composable
private fun ActivityRow(a: Activity, units: Units, shape: Shape) {
    val cs = MaterialTheme.colorScheme
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochSecond(a.startEpochSec).atZone(zone)
    val day = start.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    Surface(color = cs.surfaceContainerLow, shape = shape) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.name.ifBlank { a.sportType }, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    "$day · ${Format.duration(a.movingSec.toLong())} · ${Format.pace(a.distanceM, a.movingSec.toLong(), units)} ${Format.paceUnit(units)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${Format.distance(a.distanceM, units)} ${Format.distanceUnit(units)}", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(listOfNotNull(a.sportType, a.origin).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SyncFooter(state: DashboardState) {
    val last = state.connection.lastSyncMs ?: return
    val t = Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    Text(
        "Last synced $t",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

/** Connected list shape: big outer corners, small inner ones. */
private fun groupShape(i: Int, n: Int): Shape = when {
    n == 1 -> RoundedCornerShape(24.dp)
    i == 0 -> RoundedCornerShape(24.dp, 24.dp, 6.dp, 6.dp)
    i == n - 1 -> RoundedCornerShape(6.dp, 6.dp, 24.dp, 24.dp)
    else -> RoundedCornerShape(6.dp)
}

private fun title(period: RollupPeriod, start: LocalDate?): String = when (period) {
    RollupPeriod.DAILY -> "Today"
    RollupPeriod.WEEKLY -> "This week"
    RollupPeriod.MONTHLY -> start?.month?.getDisplayName(TextStyle.FULL, Locale.getDefault()) ?: "This month"
    RollupPeriod.YEARLY -> start?.year?.toString() ?: "This year"
}

private fun rangeText(r: Rollup): String {
    val fmt = DateTimeFormatter.ofPattern("MMM d")
    val last = r.end.minusDays(1)
    return "${r.start.format(fmt)} – ${last.format(fmt)}"
}

private fun deltaText(r: Rollup, prev: Rollup, units: Units): String {
    val diff = Format.distanceValue(r.distanceM - prev.distanceM, units)
    val sign = if (diff >= 0) "+" else "−"
    val label = when (r.period) {
        RollupPeriod.DAILY -> "yesterday"
        RollupPeriod.WEEKLY -> "this point last week"
        RollupPeriod.MONTHLY -> "this point last month"
        RollupPeriod.YEARLY -> "this point last year"
    }
    return "$sign${"%.1f".format(abs(diff))} ${Format.distanceUnit(units)} vs $label"
}
