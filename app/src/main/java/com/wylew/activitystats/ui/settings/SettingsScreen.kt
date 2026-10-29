package com.wylew.activitystats.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wylew.activitystats.data.Connection
import com.wylew.activitystats.data.healthconnect.HcStatus
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.theme.ThemeSettings
import com.wylew.activitystats.graph
import com.wylew.activitystats.sync.SyncScheduler
import com.wylew.activitystats.widget.Widgets
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val scope = rememberCoroutineScope()
    val connection by graph.settings.connection.collectAsState(Connection(false, null, null))
    val units by graph.settings.units.collectAsState(Units.IMPERIAL)
    val theme by graph.settings.theme.collectAsState(ThemeSettings())
    var hc by remember { mutableStateOf<HcStatus?>(null) }
    var hcBackground by remember { mutableStateOf(false) }

    suspend fun refreshHc() {
        hc = graph.healthConnect.status()
        hcBackground = graph.healthConnect.canReadInBackground()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        scope.launch {
            refreshHc()
            if (hc == HcStatus.READY) SyncScheduler.syncNow(context)
        }
    }

    LifecycleResumeEffect(Unit) {
        scope.launch { refreshHc() }
        onPauseOrDispose { }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Settings", style = MaterialTheme.typography.titleLarge)
            }

            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Health Connect", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    when (hc) {
                        null -> Unit
                        HcStatus.UNAVAILABLE -> Text("Not available on this phone.", style = MaterialTheme.typography.bodyLarge)
                        HcStatus.NEEDS_INSTALL -> {
                            Text("Install Health Connect to read workouts from Garmin Connect and Strava.", style = MaterialTheme.typography.bodyLarge)
                            Button(onClick = { context.startActivity(graph.healthConnect.installIntent()) }) { Text("Install Health Connect") }
                        }
                        HcStatus.NO_PERMISSION -> {
                            Text("Not connected", style = MaterialTheme.typography.bodyLarge)
                            Button(onClick = { permissionLauncher.launch(graph.healthConnect.permissions) }) { Text("Allow access") }
                        }
                        HcStatus.READY -> {
                            Text(
                                if (hcBackground) "Connected. Widgets update in the background." else "Connected. Widgets update when you open the app.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { SyncScheduler.syncNow(context) }) { Text("Sync now") }
                                if (!hcBackground) {
                                    OutlinedButton(onClick = { permissionLauncher.launch(graph.healthConnect.permissions) }) {
                                        Text("Allow background")
                                    }
                                }
                            }
                        }
                    }
                    if (hc != null && hc != HcStatus.UNAVAILABLE && hc != HcStatus.NEEDS_INSTALL) {
                        OutlinedButton(onClick = { context.startActivity(graph.healthConnect.settingsIntent()) }) {
                            Text("Open Health Connect")
                        }
                        Text(
                            "Garmin Connect: More → Settings → Health Connect. Strava: You → Settings → Manage Apps and Devices → Health Connect. " +
                                "If a run is in both, Activity Stats keeps the Garmin copy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Strava API (optional, needs a Strava subscription)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (connection.connected) {
                        Text(
                            connection.athleteName?.let { "Connected as $it" } ?: "Connected",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { SyncScheduler.syncNow(context) }) { Text("Sync now") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    graph.repository.disconnectStrava()
                                    Widgets.updateAll(context)
                                }
                            }) { Text("Disconnect") }
                        }
                        Text(
                            "Disconnecting revokes access on Strava and deletes the activities cached on this phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Button(onClick = { graph.auth.launchAuthorize(context) }, enabled = graph.auth.isConfigured) {
                            Text("Connect Strava account")
                        }
                        if (!graph.auth.isConfigured) {
                            Text(
                                "Add strava.clientId and strava.clientSecret to local.properties, then rebuild.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                ThemeSection(theme) { t ->
                    scope.launch {
                        graph.settings.setTheme(t)
                        Widgets.updateAll(context)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Units", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val options = listOf(Units.IMPERIAL to "Miles", Units.METRIC to "Kilometers")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { i, (u, label) ->
                            SegmentedButton(
                                selected = units == u,
                                onClick = {
                                    scope.launch {
                                        graph.settings.setUnits(u)
                                        Widgets.updateAll(context)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            ) { Text(label) }
                        }
                    }
                }

            }
        }
    }
}
