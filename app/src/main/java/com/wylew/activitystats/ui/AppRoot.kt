package com.wylew.activitystats.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.wylew.activitystats.ui.activities.ActivitiesScreen
import com.wylew.activitystats.ui.dashboard.DashboardScreen
import com.wylew.activitystats.ui.settings.SettingsScreen
import com.wylew.activitystats.ui.widgets.WidgetsScreen

private enum class Tab(val label: String, val selected: ImageVector, val unselected: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    ACTIVITIES("Activities", Icons.AutoMirrored.Filled.FormatListBulleted, Icons.AutoMirrored.Outlined.FormatListBulleted),
    WIDGETS("Widgets", Icons.Filled.Widgets, Icons.Outlined.Widgets),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

/** The four tabs from the design. First-run setup lives on the dashboard. */
@Composable
fun AppRoot() {
    var tab by rememberSaveable { mutableStateOf(Tab.DASHBOARD) }
    val snackbar = remember { SnackbarHostState() }

    BackHandler(enabled = tab != Tab.DASHBOARD) { tab = Tab.DASHBOARD }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    val on = t == tab
                    NavigationBarItem(
                        selected = on,
                        onClick = { tab = t },
                        icon = { Icon(if (on) t.selected else t.unselected, contentDescription = null) },
                        label = { Text(t.label, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium) },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.DASHBOARD -> DashboardScreen(padding = padding, snackbar = snackbar, onSeeAll = { tab = Tab.ACTIVITIES })
            Tab.ACTIVITIES -> ActivitiesScreen(padding)
            Tab.WIDGETS -> WidgetsScreen(padding)
            Tab.SETTINGS -> SettingsScreen(padding)
        }
    }
}
