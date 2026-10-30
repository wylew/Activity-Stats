package com.wylew.activitystats.ui.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.Format
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.domain.localDate
import com.wylew.activitystats.graph
import com.wylew.activitystats.ui.components.ActivityRow
import com.wylew.activitystats.ui.components.groupShape
import com.wylew.activitystats.widget.iconFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/** Every workout, newest first, grouped by week with a weekly total. */
@Composable
fun ActivitiesScreen(padding: PaddingValues) {
    val graph = LocalContext.current.graph
    val activities by graph.repository.activities.collectAsState(emptyList())
    val units by graph.settings.units.collectAsState(Units.IMPERIAL)
    var filter by rememberSaveable { mutableStateOf(ActivityFilter.ALL) }

    val weeks = remember(activities, filter) { groupByWeek(activities.filter { filter.matches(it.kind) }) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Activities", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 4.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.label) },
                        leadingIcon = { Icon(painterResource(iconFor(f)), null, Modifier.size(FilterChipDefaults.IconSize)) },
                    )
                }
            }
        }
        if (weeks.isEmpty()) {
            item {
                Text(
                    "Nothing here yet. Workouts appear after Garmin Connect or Strava share them with Health Connect.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
        weeks.forEach { (weekStart, items) ->
            item(key = weekStart.toString()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
                        Text(weekTitle(weekStart), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            "${Format.distance(items.sumOf { it.distanceM }, units)} ${Format.distanceUnit(units)} · ${items.size} ${filter.noun(items.size)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items.forEachIndexed { i, a -> ActivityRow(a, units, groupShape(i, items.size)) }
                }
            }
        }
    }
}

private fun groupByWeek(list: List<Activity>): List<Pair<LocalDate, List<Activity>>> {
    val zone = ZoneId.systemDefault()
    val monday = TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
    return list.groupBy { it.localDate(zone).with(monday) }
        .toList()
        .sortedByDescending { it.first }
}

private fun weekTitle(start: LocalDate): String {
    val thisWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return when (start) {
        thisWeek -> "This week"
        thisWeek.minusWeeks(1) -> "Last week"
        else -> "Week of " + start.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}
