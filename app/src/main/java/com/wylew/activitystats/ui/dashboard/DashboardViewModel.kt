package com.wylew.activitystats.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wylew.activitystats.data.Connection
import com.wylew.activitystats.data.SyncResult
import com.wylew.activitystats.data.healthconnect.HcStatus
import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.ActivityFilter
import com.wylew.activitystats.domain.ActivityView
import com.wylew.activitystats.domain.Goals
import com.wylew.activitystats.domain.Rollup
import com.wylew.activitystats.domain.RollupCalculator
import com.wylew.activitystats.domain.RollupPeriod
import com.wylew.activitystats.domain.SourcePrefs
import com.wylew.activitystats.domain.Units
import com.wylew.activitystats.domain.localDate
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
    val healthConnect: HcStatus = HcStatus.NO_PERMISSION,
    val sources: SourcePrefs = SourcePrefs(),
    val units: Units = Units.IMPERIAL,
    val period: RollupPeriod = RollupPeriod.WEEKLY,
    val filter: ActivityFilter = ActivityFilter.RUN,
    val rollup: Rollup? = null,
    val previous: Rollup? = null,
    val goalM: Double = 0.0,
    /** Copies collapsed in this period, e.g. a run that came from both Garmin and Strava. */
    val merged: Int = 0,
    /** Apps that actually supplied data, e.g. ["Garmin", "Strava"]. */
    val origins: List<String> = emptyList(),
) {
    val hasSource: Boolean get() = healthConnect == HcStatus.READY || connection.connected
}

private data class Selection(val period: RollupPeriod, val filter: ActivityFilter)

private data class Inputs(
    val raw: List<Activity>,
    val sources: SourcePrefs,
    val goals: Goals,
    val units: Units,
    val connection: Connection,
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = app.graph
    private val selection = MutableStateFlow(Selection(RollupPeriod.WEEKLY, ActivityFilter.RUN))
    private val hcStatus = MutableStateFlow(HcStatus.NO_PERMISSION)

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val healthConnectPermissions: Set<String> get() = graph.healthConnect.permissions

    private val inputs = combine(
        graph.repository.raw,
        graph.settings.sources,
        graph.settings.goals,
        graph.settings.units,
        graph.settings.connection,
    ) { raw, sources, goals, units, connection -> Inputs(raw, sources, goals, units, connection) }

    val state: StateFlow<DashboardState> = combine(inputs, selection, hcStatus) { i, sel, hc ->
        build(i, sel, hc)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    init {
        viewModelScope.launch {
            refreshSourcesNow()
            if (hcStatus.value == HcStatus.READY || graph.settings.connection.first().connected) sync()
        }
    }

    fun refreshSources() {
        viewModelScope.launch { refreshSourcesNow() }
    }

    private suspend fun refreshSourcesNow() {
        hcStatus.value = graph.healthConnect.status()
    }

    fun onHealthConnectResult(granted: Set<String>) {
        viewModelScope.launch {
            refreshSourcesNow()
            when {
                hcStatus.value == HcStatus.READY -> sync()
                granted.isEmpty() -> _message.value = "Activity Stats needs exercise access to show your workouts."
            }
        }
    }

    fun selectPeriod(p: RollupPeriod) { selection.value = selection.value.copy(period = p) }
    fun selectFilter(f: ActivityFilter) { selection.value = selection.value.copy(filter = f) }
    fun messageShown() { _message.value = null }

    fun installHealthConnect() {
        getApplication<Application>().startActivity(graph.healthConnect.installIntent())
    }

    fun connectStrava() {
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
                is SyncResult.Success, SyncResult.NotConnected -> Unit
                is SyncResult.RateLimited -> _message.value = "Strava rate limit hit. Try again in ~15 minutes."
                is SyncResult.Failed -> _message.value = "Sync failed: ${r.message}"
            }
            Widgets.updateAll(getApplication())
            _syncing.value = false
        }
    }

    private fun build(i: Inputs, sel: Selection, hc: HcStatus): DashboardState {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val activities = ActivityView.apply(i.raw, i.sources)
        val rollup = RollupCalculator.compute(activities, sel.filter, sel.period, today, zone)

        val rawInPeriod = i.raw.filter {
            val d = it.localDate(zone)
            sel.filter.matches(it.kind) && !d.isBefore(rollup.start) && d.isBefore(rollup.end)
        }
        return DashboardState(
            loading = false,
            connection = i.connection,
            healthConnect = hc,
            sources = i.sources,
            units = i.units,
            period = sel.period,
            filter = sel.filter,
            rollup = rollup,
            previous = RollupCalculator.previousToDate(activities, sel.filter, sel.period, today, zone),
            goalM = i.goals.forPeriod(sel.filter, sel.period),
            merged = ActivityView.mergedCount(rawInPeriod, i.sources),
            origins = i.raw.mapNotNull { it.origin }.distinct().sorted(),
        )
    }
}
