package com.wylew.activitystats.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive "connected button group": the selected button morphs into a full pill,
 * the others keep small inner corners. Spring-animated.
 * Swap for material3's ButtonGroup once it's public in a stable release.
 */
@Composable
fun <T> ConnectedButtonGroup(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { i, (value, label) ->
            val on = value == selected
            val outer = 24.dp
            val inner by animateDpAsState(if (on) 24.dp else 8.dp, spring(dampingRatio = 0.6f), label = "corner")
            val start = if (i == 0) outer else inner
            val end = if (i == options.lastIndex) outer else inner
            Button(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (on) cs.primary else cs.surfaceContainerHigh,
                    contentColor = if (on) cs.onPrimary else cs.onSurface,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics {
                        role = Role.RadioButton
                        this.selected = on
                    },
            ) {
                Text(label, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}
