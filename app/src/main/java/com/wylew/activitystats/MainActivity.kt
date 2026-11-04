package com.wylew.activitystats

import android.os.Bundle
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
        setContent {
            ActivityStatsTheme {
                AppRoot()
            }
        }
    }

    /** Every time the app comes to the front, pull anything new so the widgets catch up too. */
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            if (graph.repository.syncIfStale() != null) Widgets.updateAll(this@MainActivity)
        }
    }
}
