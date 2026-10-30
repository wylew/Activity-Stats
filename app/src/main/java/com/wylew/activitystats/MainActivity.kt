package com.wylew.activitystats

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.wylew.activitystats.ui.AppRoot
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
                AppRoot()
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
