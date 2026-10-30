package com.wylew.activitystats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wylew.activitystats.R
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityKind
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.widget.whenLabel

/** One workout in a connected list: icon tile, name, when · time · pace, distance and source. */
@Composable
fun ActivityRow(a: Activity, units: Units, shape: Shape) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.surfaceContainerLow, shape = shape) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).background(cs.secondaryContainer, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(iconFor(a.kind)), contentDescription = null, tint = cs.onSecondaryContainer, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(a.name.ifBlank { a.sportType }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val pace = if (a.distanceM > 0) " · ${Format.pace(a.distanceM, a.movingSec.toLong(), units)} ${Format.paceUnit(units)}" else ""
                Text(
                    "${whenLabel(a).substringBefore(",")} · ${Format.duration(a.movingSec.toLong())}$pace",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (a.distanceM > 0) {
                    Text("${Format.distance(a.distanceM, units)} ${Format.distanceUnit(units)}", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                }
                a.origin?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant) }
            }
        }
    }
}

private fun iconFor(kind: ActivityKind): Int = when (kind) {
    ActivityKind.RUN, ActivityKind.WALK, ActivityKind.HIKE -> R.drawable.ic_run
    ActivityKind.RIDE -> R.drawable.ic_ride
    else -> R.drawable.ic_all
}

/** Connected list shape: big outer corners, small inner ones. */
fun groupShape(i: Int, n: Int): Shape = when {
    n == 1 -> RoundedCornerShape(24.dp)
    i == 0 -> RoundedCornerShape(24.dp, 24.dp, 6.dp, 6.dp)
    i == n - 1 -> RoundedCornerShape(6.dp, 6.dp, 24.dp, 24.dp)
    else -> RoundedCornerShape(6.dp)
}
