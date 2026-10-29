package com.wylew.activitystats.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wylew.activitystats.data.Connection
import com.wylew.activitystats.data.SyncResult
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupCalculator
import com.wylew.activitystats.domain.RollupPeriod
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.graph
import com.wylew.activitystats.widget.Widgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DashboardState(
    val loading: Boolean = true,
    val connection: Connection = Connection(false, null, null),
    val units: Units = Units.IMPERIAL,
    val period: RollupPeriod = RollupPeriod.WEEKLY,
    val filter: ActivityFilter = ActivityFilter.RUN,
    val rollup: Rollup? = null,
    val previous: Rollup? = null,
    /** Default goal for the period, in meters. TODO: make editable in settings. */
    val goalM: Double = 0.0,
)

private data class Selection(val period: RollupPeriod, val filter: ActivityFilter)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = app.graph
    private val selection = MutableStateFlow(Selection(RollupPeriod.WEEKLY, ActivityFilter.RUN))

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val state: StateFlow<DashboardState> = combine(
        graph.store.activities,
        graph.settings.connection,
        graph.settings.units,
        selection,
    ) { activities, connection, units, sel ->
        build(activities, connection, units, sel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    init {
        viewModelScope.launch {
            if (graph.settings.connection.first().connected) sync()
        }
    }

    fun selectPeriod(p: RollupPeriod) { selection.value = selection.value.copy(period = p) }
    fun selectFilter(f: ActivityFilter) { selection.value = selection.value.copy(filter = f) }
    fun messageShown() { _message.value = null }

    fun connect() {
        if (!graph.auth.isConfigured) {
            _message.value = "Add strava.clientId and strava.clientSecret to local.properties, then rebuild."
            return
        }
        graph.auth.launchAuthorize(getApplication())
    }

    fun sync() {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            when (val r = graph.repository.sync()) {
                is SyncResult.Success -> if (r.fetched > 0) _message.value = "Synced ${r.fetched} from Strava"
                SyncResult.NotConnected -> Unit
                is SyncResult.RateLimited -> _message.value = "Strava rate limit hit. Try again in ~15 minutes."
                is SyncResult.Failed -> _message.value = "Sync failed: ${r.message}"
            }
            Widgets.updateAll(getApplication())
            _syncing.value = false
        }
    }

    private fun build(activities: List<Activity>, connection: Connection, units: Units, sel: Selection): DashboardState {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        return DashboardState(
            loading = false,
            connection = connection,
            units = units,
            period = sel.period,
            filter = sel.filter,
            rollup = RollupCalculator.compute(activities, sel.filter, sel.period, today, zone),
            previous = RollupCalculator.previousToDate(activities, sel.filter, sel.period, today, zone),
            goalM = defaultGoal(sel.period, sel.filter),
        )
    }

    private fun defaultGoal(period: RollupPeriod, filter: ActivityFilter): Double {
        val weeklyKm = when (filter) {
            ActivityFilter.RUN -> 40.0
            ActivityFilter.RIDE -> 120.0
            ActivityFilter.ALL -> 150.0
        }
        val km = when (period) {
            RollupPeriod.DAILY -> weeklyKm / 7
            RollupPeriod.WEEKLY -> weeklyKm
            RollupPeriod.MONTHLY -> weeklyKm * 4.3
            RollupPeriod.YEARLY -> weeklyKm * 52
        }
        return km * 1000
    }
}
