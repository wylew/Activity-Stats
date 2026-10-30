package com.wylew.activitystats.ui.dashboard

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wylew.activitystats.data.healthconnect.HcStatus
import com.wylew.activitystats.graph
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupPeriod
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.ui.components.ActivityRow
import com.wylew.activitystats.ui.components.ConnectedButtonGroup
import com.wylew.activitystats.ui.components.RollupBars
import com.wylew.activitystats.ui.components.groupShape
import com.wylew.activitystats.widget.iconFor
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(
    padding: PaddingValues,
    snackbar: SnackbarHostState,
    onSeeAll: () -> Unit,
    vm: DashboardViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header(state, syncing, vm::selectFilter, vm::sync) }
        item {
            ConnectedButtonGroup(
                options = listOf(RollupPeriod.WEEKLY to "Week", RollupPeriod.MONTHLY to "Month", RollupPeriod.YEARLY to "Year"),
                selected = state.period,
                onSelect = vm::selectPeriod,
            )
        }

        if (!state.loading && !state.hasSource) {
            item {
                SourcesSetupCard(
                    status = state.healthConnect,
                    onAllow = { hcPermissionLauncher.launch(vm.healthConnectPermissions) },
                    onInstall = vm::installHealthConnect,
                )
            }
        }

        val r = state.rollup ?: return@LazyColumn
        item { HeroCard(r, state.previous, state.units) }
        item { StatGrid(r, state.units) }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent ${r.filter.plural}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onSeeAll) { Text("See all") }
            }
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
        val recent = r.items.take(5)
        if (recent.isNotEmpty()) {
            item {
                // One item so the rows sit 2dp apart as a connected group.
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    recent.forEachIndexed { i, a -> ActivityRow(a, state.units, groupShape(i, recent.size)) }
                }
            }
        }
        if (state.hasSource) {
            item { SourcesCard(state) }
        }
    }
}

@Composable
private fun Header(state: DashboardState, syncing: Boolean, onFilter: (ActivityFilter) -> Unit, onSync: () -> Unit) {
    val r = state.rollup
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.padding(start = 4.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                FilledTonalButton(
                    onClick = { menu = true },
                    contentPadding = PaddingValues(start = 16.dp, end = 10.dp),
                ) {
                    Text(filterTitle(state.filter), fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose activity")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    ActivityFilter.entries.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(filterTitle(f)) },
                            leadingIcon = { Icon(painterResource(iconFor(f)), null, Modifier.size(20.dp)) },
                            trailingIcon = { if (f == state.filter) Icon(Icons.Filled.Check, null) },
                            onClick = { onFilter(f); menu = false },
                        )
                    }
                }
            }
            FilledTonalIconButton(
                onClick = onSync,
                enabled = state.hasSource && !syncing,
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                if (syncing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Sync, contentDescription = "Sync now")
            }
        }
        Text(
            title(state.period, r),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (r != null) {
            Text(rangeText(r), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SourcesSetupCard(status: HcStatus, onAllow: () -> Unit, onInstall: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Bring in your workouts", style = MaterialTheme.typography.titleLarge)
            when (status) {
                HcStatus.UNAVAILABLE -> {
                    Text("Health Connect isn't available on this phone, so Activity Stats can't read workouts here.", style = MaterialTheme.typography.bodyMedium)
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
                        "Your data stays on this phone. Allow background access so widgets update while the app is closed.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(r: Rollup, previous: Rollup?, units: Units) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.primary, contentColor = cs.onPrimary, shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Distance", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(Format.distance(r.distanceM, units), fontSize = 72.sp, lineHeight = 72.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)
                        Spacer(Modifier.width(6.dp))
                        Text(Format.distanceUnit(units), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    }
                }
                Box(Modifier.size(76.dp).background(cs.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(r.count.toString(), color = cs.onPrimaryContainer, fontSize = 30.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)
                        Text(r.filter.noun(r.count), color = cs.onPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (previous != null) {
                Text(
                    deltaText(r, previous, units),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(cs.onPrimary.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            if (r.buckets.isNotEmpty()) {
                RollupBars(
                    r.buckets,
                    barColor = cs.primaryContainer,
                    currentColor = cs.tertiaryContainer,
                    futureColor = cs.onPrimary.copy(alpha = 0.45f),
                    labelColor = cs.primaryContainer,
                    maxBarHeight = 100.dp,
                    modifier = Modifier.height(128.dp),
                )
            }
        }
    }
}

private data class Stat(val label: String, val value: String, val unit: String, val icon: ImageVector)

@Composable
private fun StatGrid(r: Rollup, units: Units) {
    val cs = MaterialTheme.colorScheme
    val stats = listOf(
        Stat("Moving time", Format.duration(r.movingSec), if (r.movingSec >= 3600) "h" else "min", Icons.Outlined.Timer),
        Stat("Avg pace", Format.pace(r.distanceM, r.movingSec, units), Format.paceUnit(units), Icons.Outlined.Bolt),
        Stat("Elevation", Format.elevation(r.elevationM, units), Format.elevationUnit(units), Icons.Outlined.Terrain),
        Stat("Avg heart rate", r.avgHr?.let { "%.0f".format(it) } ?: "–", "bpm", Icons.Outlined.FavoriteBorder),
    )
    // Each tile's icon sits in a different shape, as in the design.
    val tiles = listOf(
        Triple(cs.primaryContainer, cs.onPrimaryContainer, CircleShape as Shape),
        Triple(cs.tertiaryContainer, cs.onTertiaryContainer, RoundedCornerShape(14.dp)),
        Triple(cs.secondaryContainer, cs.onSecondaryContainer, RoundedCornerShape(50, 50, 14, 50)),
        Triple(cs.tertiaryContainer, cs.onTertiaryContainer, RoundedCornerShape(14, 50, 50, 50)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEachIndexed { col, stat ->
                    val (bg, fg, shape) = tiles[row * 2 + col]
                    Surface(color = cs.surfaceContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.weight(1f).heightIn(min = 132.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(44.dp).background(bg, shape), contentAlignment = Alignment.Center) {
                                Icon(stat.icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text(stat.label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(stat.value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stat.unit, color = cs.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
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
private fun SourcesCard(state: DashboardState) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val synced = state.lastSyncMs?.let {
        "Synced " + DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString().lowercase()
    } ?: "Not synced yet"
    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, cs.outlineVariant),
        color = cs.surface,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MonitorHeart, contentDescription = null, tint = cs.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Health Connect", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(synced, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                TextButton(onClick = { context.startActivity(context.graph.healthConnect.settingsIntent()) }) { Text("Open") }
            }
            if (state.origins.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.origins.forEach { name ->
                        Row(
                            Modifier
                                .background(cs.secondaryContainer, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = cs.onSecondaryContainer, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(name, style = MaterialTheme.typography.labelLarge, color = cs.onSecondaryContainer)
                        }
                    }
                }
            }
            if (state.merged > 0) {
                Text(
                    "${state.merged} duplicate ${if (state.merged == 1) state.filter.singular else state.filter.plural} merged ${periodPhrase(state.period)} (Garmin + Strava copies).",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }
}

private fun filterTitle(f: ActivityFilter) = when (f) {
    ActivityFilter.RUN -> "Running"
    ActivityFilter.RIDE -> "Riding"
    ActivityFilter.ALL -> "All activities"
}

private fun periodPhrase(p: RollupPeriod) = when (p) {
    RollupPeriod.DAILY -> "today"
    RollupPeriod.WEEKLY -> "this week"
    RollupPeriod.MONTHLY -> "this month"
    RollupPeriod.YEARLY -> "this year"
}

private fun title(period: RollupPeriod, r: Rollup?): String = when (period) {
    RollupPeriod.DAILY -> "Today"
    RollupPeriod.WEEKLY -> "This week"
    RollupPeriod.MONTHLY -> r?.start?.month?.getDisplayName(TextStyle.FULL, Locale.getDefault()) ?: "This month"
    RollupPeriod.YEARLY -> r?.start?.year?.toString() ?: "This year"
}

private fun rangeText(r: Rollup): String {
    val fmt = DateTimeFormatter.ofPattern("MMM d")
    return "${r.start.format(fmt)} – ${r.end.minusDays(1).format(fmt)}"
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
