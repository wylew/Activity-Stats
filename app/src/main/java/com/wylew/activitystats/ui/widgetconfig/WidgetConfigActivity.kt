package com.wylew.activitystats.ui.widgetconfig

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.wylew.activitystats.MainActivity
import com.wylew.activitystats.data.Connection
import com.wylew.activitystats.data.healthconnect.HcStatus
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.graph
import com.wylew.activitystats.ui.theme.ActivityStatsTheme
import com.wylew.activitystats.widget.RollupWidget
import com.wylew.activitystats.widget.WidgetPrefs
import com.wylew.activitystats.widget.WidgetSettings
import kotlinx.coroutines.launch

/** Shown when a widget is placed (and on long-press → reconfigure). */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val resultData = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultData)

        val graph = applicationContext.graph
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)

        enableEdgeToEdge()
        setContent {
            ActivityStatsTheme {
                val activities by graph.repository.activities.collectAsState(emptyList())
                val units by graph.settings.units.collectAsState(Units.IMPERIAL)
                val connection by graph.settings.connection.collectAsState(Connection(false, null, null))
                val hcReady by produceState(false) { value = graph.healthConnect.status() == HcStatus.READY }
                var initial by remember { mutableStateOf<WidgetSettings?>(null) }

                LaunchedEffect(glanceId) {
                    val prefs = getAppWidgetState(this@WidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId)
                    initial = WidgetPrefs.read(prefs)
                }

                initial?.let { start ->
                    WidgetConfigScreen(
                        initial = start,
                        activities = activities,
                        units = units,
                        connected = hcReady || connection.connected,
                        onConnect = { startActivity(Intent(this@WidgetConfigActivity, MainActivity::class.java)) },
                        onClose = { finish() },
                        onSave = { chosen ->
                            lifecycleScope.launch {
                                updateAppWidgetState(this@WidgetConfigActivity, glanceId) { WidgetPrefs.write(it, chosen) }
                                RollupWidget().update(this@WidgetConfigActivity, glanceId)
                                setResult(RESULT_OK, resultData)
                                finish()
                            }
                        },
                    )
                }
            }
        }
    }
}
