package com.wylew.activitystats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wylew.activitystats.ui.theme.ActivityStatsTheme

/** Required by Health Connect: explains what we read and why. */
class PrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ActivityStatsTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .safeDrawingPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("How Activity Stats uses your data", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Activity Stats reads your workouts (type, time, distance, elevation and heart rate) " +
                                "from Health Connect to show totals in the app and on your home screen widgets.",
                        )
                        Text(
                            "It only reads. It never writes to Health Connect, and your data stays on this phone. " +
                                "Nothing is sent to a server or shared with anyone.",
                        )
                        Text(
                            "Background access lets widgets update while the app is closed. " +
                                "History access lets the app show totals from before you installed it.",
                        )
                        Text("You can remove access at any time in Health Connect settings.")
                        Button(onClick = { finish() }) { Text("Done") }
                    }
                }
            }
        }
    }
}
