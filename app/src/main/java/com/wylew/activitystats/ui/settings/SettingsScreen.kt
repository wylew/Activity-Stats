package com.wylew.activitystats.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wylew.activitystats.data.healthconnect.HcStatus
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.graph
import com.wylew.activitystats.sync.SyncScheduler
import com.wylew.activitystats.theme.ThemeSettings
import com.wylew.activitystats.widget.Widgets
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val graph = context.graph
    val scope = rememberCoroutineScope()
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
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp),
            )

            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label("Health Connect")
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
                    if (hc == HcStatus.READY || hc == HcStatus.NO_PERMISSION) {
                        OutlinedButton(onClick = { context.startActivity(graph.healthConnect.settingsIntent()) }) {
                            Text("Open Health Connect")
                        }
                        Text(
                            "Turn on sharing in Garmin Connect (More → Settings → Health Connect) and Strava " +
                                "(You → Settings → Manage Apps and Devices → Health Connect). If a workout comes from both, it's counted once.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                ThemeSection(theme) { t ->
                    scope.launch {
                        graph.settings.setTheme(t)
                        Widgets.updateAll(context)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label("Units")
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

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
