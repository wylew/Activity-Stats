package com.wylew.activitystats

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.wylew.activitystats.ui.dashboard.DashboardScreen
import com.wylew.activitystats.ui.settings.SettingsScreen
import com.wylew.activitystats.ui.theme.ActivityStatsTheme
import com.wylew.activitystats.widget.Widgets
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthRedirect(intent)
        setContent {
            ActivityStatsTheme {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = showSettings) { showSettings = false }
                if (showSettings) {
                    SettingsScreen(onBack = { showSettings = false })
                } else {
                    DashboardScreen(onOpenSettings = { showSettings = true })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent)
    }

    private fun handleOAuthRedirect(intent: Intent?) {
        val uri = intent?.data
        val graph = applicationContext.graph
        if (!graph.auth.isRedirect(uri)) return
        intent?.data = null // don't re-handle on rotation
        lifecycleScope.launch {
            graph.auth.handleRedirect(uri!!)
                .onSuccess {
                    Toast.makeText(this@MainActivity, "Strava connected. Syncing…", Toast.LENGTH_SHORT).show()
                    graph.repository.sync()
                    Widgets.updateAll(this@MainActivity)
                }
                .onFailure {
                    Toast.makeText(this@MainActivity, it.message ?: "Couldn't connect Strava", Toast.LENGTH_LONG).show()
                }
        }
    }
}
