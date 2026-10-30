package com.wylew.activitystats.ui.sources

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wylew.activitystats.R
import com.wylew.activitystats.data.healthconnect.HcStatus
import com.wylew.activitystats.domain.PreferOrigin
import com.wylew.activitystats.domain.SourceChoice
import com.wylew.activitystats.domain.SourcePrefs
import com.wylew.activitystats.graph
import com.wylew.activitystats.sync.SyncScheduler
import com.wylew.activitystats.ui.components.ConnectedButtonGroup
import com.wylew.activitystats.widget.Widgets
import kotlinx.coroutines.launch

/**
 * "Where do your activities come from?" — first-run onboarding, and reachable from Settings.
 * Garmin, Strava or Both; setup steps for each; which copy wins when a run is in both.
 */
@Composable
fun SourcesScreen(padding: PaddingValues, onboarding: Boolean, onDone: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val scope = rememberCoroutineScope()
    val saved by graph.settings.sources.collectAsState(SourcePrefs())
    var draft by remember { mutableStateOf<SourcePrefs?>(null) }
    LaunchedEffect(saved) { if (draft == null) draft = saved }
    val prefs = draft ?: saved

    var hc by remember { mutableStateOf<HcStatus?>(null) }
    var granted by remember { mutableStateOf(emptySet<String>()) }
    suspend fun refresh() {
        hc = graph.healthConnect.status()
        granted = graph.healthConnect.granted()
    }
    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        scope.launch { refresh() }
    }
    LifecycleResumeEffect(Unit) {
        scope.launch { refresh() }
        onPauseOrDispose { }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (!onboarding) {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        } else {
            Spacer(Modifier.height(24.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Sync, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
            }
            Text(
                "Where do your activities come from?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Pick the app you already use. You can change this later, and each widget can override it.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SourceOption(
                selected = prefs.choice == SourceChoice.GARMIN,
                title = "Garmin Connect",
                body = "Your watch syncs to Garmin Connect, which shares activities with Activity Stats on this phone.",
                icon = { Icon(Icons.Outlined.Watch, null) },
                tint = MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { draft = prefs.copy(choice = SourceChoice.GARMIN) },
            )
            SourceOption(
                selected = prefs.choice == SourceChoice.STRAVA,
                title = "Strava",
                body = "Uses the activities the Strava app shares with this phone. No Strava subscription needed.",
                icon = { Icon(painterResource(R.drawable.ic_run), null) },
                tint = MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = { draft = prefs.copy(choice = SourceChoice.STRAVA) },
            )
            SourceOption(
                selected = prefs.choice == SourceChoice.BOTH,
                title = "Both",
                body = "Uses Garmin and Strava together and keeps one copy of each activity.",
                icon = { Icon(Icons.Outlined.GridView, null) },
                tint = MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { draft = prefs.copy(choice = SourceChoice.BOTH) },
            )
        }

        if (prefs.choice != SourceChoice.STRAVA) {
            SetupCard("Garmin, via Health Connect") {
                Text("In Garmin Connect: More → Settings → Health Connect, and allow sharing. Then let Activity Stats read:", style = MaterialTheme.typography.bodyMedium)
                PermissionChips(granted, graph.healthConnect.permissions)
                HealthConnectButton(hc, onAllow = { launcher.launch(graph.healthConnect.permissions) }, onInstall = { context.startActivity(graph.healthConnect.installIntent()) })
            }
        }
        if (prefs.choice != SourceChoice.GARMIN) {
            SetupCard("Strava, via Health Connect") {
                Text("In Strava: You → Settings → Manage Apps and Devices → Health Connect, and allow sharing.", style = MaterialTheme.typography.bodyMedium)
                if (prefs.choice == SourceChoice.STRAVA) {
                    HealthConnectButton(hc, onAllow = { launcher.launch(graph.healthConnect.permissions) }, onInstall = { context.startActivity(graph.healthConnect.installIntent()) })
                }
                Text(
                    "Have a Strava subscription? You can also connect the Strava API in Settings for titles and gear.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (prefs.choice == SourceChoice.BOTH) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("When a run shows up in both", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ConnectedButtonGroup(
                    options = PreferOrigin.entries.map { it to it.label },
                    selected = prefs.prefer,
                    onSelect = { draft = prefs.copy(prefer = it) },
                )
                Text(
                    "Matched by start time (within 2 min), sport and distance (within 3%). A Strava title is kept either way.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    graph.settings.setSources(prefs)
                    if (onboarding) graph.settings.setOnboarded()
                    Widgets.updateAll(context)
                    if (hc == HcStatus.READY) SyncScheduler.syncNow(context)
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(if (onboarding) "Continue" else "Done", fontWeight = FontWeight.Bold) }
        if (onboarding) {
            TextButton(
                onClick = { scope.launch { graph.settings.setOnboarded(); onDone() } },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Skip for now") }
        }
    }
}

@Composable
private fun SourceOption(
    selected: Boolean,
    title: String,
    body: String,
    icon: @Composable () -> Unit,
    tint: Pair<Color, Color>,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val corner by animateDpAsState(if (selected) 28.dp else 16.dp, label = "corner")
    val iconCorner by animateDpAsState(if (selected) 24.dp else 16.dp, label = "iconCorner")
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) cs.surfaceContainer else Color.Transparent, RoundedCornerShape(corner))
            .border(if (selected) 2.dp else 0.dp, if (selected) cs.primary else Color.Transparent, RoundedCornerShape(corner))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = tint.first, contentColor = tint.second, shape = RoundedCornerShape(iconCorner), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { icon() }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun SetupCard(title: String, content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun PermissionChips(granted: Set<String>, requested: Set<String>) {
    val labels = listOf(
        "Exercise" to "READ_EXERCISE",
        "Distance" to "READ_DISTANCE",
        "Heart rate" to "READ_HEART_RATE",
        "Elevation" to "READ_ELEVATION_GAINED",
        "Past data" to "READ_HEALTH_DATA_HISTORY",
        "Background" to "READ_HEALTH_DATA_IN_BACKGROUND",
    )
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, suffix) ->
                    val on = granted.any { it.endsWith(suffix) } && requested.any { it.endsWith(suffix) }
                    Row(
                        Modifier
                            .background(if (on) cs.secondaryContainer else cs.surfaceContainerHigh, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (on) {
                            Icon(Icons.Filled.Check, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(label, style = MaterialTheme.typography.labelMedium, color = if (on) cs.onSecondaryContainer else cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthConnectButton(hc: HcStatus?, onAllow: () -> Unit, onInstall: () -> Unit) {
    when (hc) {
        null -> Unit
        HcStatus.UNAVAILABLE -> Text("Health Connect isn't available on this phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        HcStatus.NEEDS_INSTALL -> OutlinedButton(onClick = onInstall, modifier = Modifier.fillMaxWidth()) { Text("Install Health Connect") }
        HcStatus.NO_PERMISSION -> OutlinedButton(onClick = onAllow, modifier = Modifier.fillMaxWidth()) { Text("Allow Health Connect access") }
        HcStatus.READY -> OutlinedButton(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Connected · review access")
        }
    }
}
