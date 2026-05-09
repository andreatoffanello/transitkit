package com.transitkit.app.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.ConnectionsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val connectionsStore: ConnectionsStore,
    private val scheduleRepository: ScheduleRepository,
    private val config: OperatorConfig,
) : ViewModel() {

    data class WhenSelection(
        val mode: Int = 0,  // 0 = now, 1 = departAt, 2 = arriveBy
        val date: java.util.Date = java.util.Date(),
    )

    private val _origin = MutableStateFlow<ResolvedStop?>(null)
    val origin = _origin.asStateFlow()

    private val _destination = MutableStateFlow<ResolvedStop?>(null)
    val destination = _destination.asStateFlow()

    private val _whenSelection = MutableStateFlow(WhenSelection())
    val whenSelection = _whenSelection.asStateFlow()

    private val _journeys = MutableStateFlow<List<Journey>>(emptyList())
    val journeys = _journeys.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched = _hasSearched.asStateFlow()

    val connectionsState = connectionsStore.state

    val allStops = scheduleRepository.stops

    init {
        // Trigger connections download as soon as routes are available.
        viewModelScope.launch {
            scheduleRepository.routes.collect { routes ->
                if (routes.isNotEmpty() &&
                    connectionsStore.loadState == ConnectionsStore.LoadState.IDLE
                ) {
                    connectionsStore.load(config, routes)
                }
            }
        }
        // Re-trigger search after connections become ready.
        viewModelScope.launch {
            connectionsStore.state.collect { state ->
                if (state == ConnectionsStore.LoadState.READY) {
                    onConnectionsReady()
                }
            }
        }
    }

    private val _selectedJourney = MutableStateFlow<Journey?>(null)
    val selectedJourney = _selectedJourney.asStateFlow()

    fun selectJourney(journey: Journey) { _selectedJourney.value = journey }

    private var searchJob: Job? = null

    fun setOrigin(stop: ResolvedStop?) {
        _origin.value = stop
        triggerSearch()
    }

    fun setDestination(stop: ResolvedStop?) {
        _destination.value = stop
        triggerSearch()
    }

    fun setWhenSelection(sel: WhenSelection) {
        _whenSelection.value = sel
        triggerSearch()
    }

    fun swapStops() {
        val tmp = _origin.value
        _origin.value = _destination.value
        _destination.value = tmp
        triggerSearch()
    }

    fun triggerSearch() {
        val o = _origin.value ?: run { reset(); return }
        val d = _destination.value ?: run { reset(); return }
        if (o.id == d.id) {
            _searchError.value = "Origin and destination are the same"
            return
        }
        if (connectionsStore.loadState != ConnectionsStore.LoadState.READY) return

        val op = com.transitkit.app.data.model.PlannerStop(o.id, o.name, o.lat, o.lon)
        val dp = com.transitkit.app.data.model.PlannerStop(d.id, d.name, d.lat, d.lon)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _hasSearched.value = true
            _searchError.value = null
            val tz = config.timezone.ifBlank { "America/New_York" }
            val results = withContext(Dispatchers.IO) {
                when (_whenSelection.value.mode) {
                    1 -> connectionsStore.query(op, dp, _whenSelection.value.date.time, tz)
                    2 -> connectionsStore.queryArriveBy(op, dp, _whenSelection.value.date.time, tz)
                    else -> connectionsStore.query(op, dp, System.currentTimeMillis(), tz)
                }
            }
            _journeys.value = results
            _isSearching.value = false
        }
    }

    private fun reset() {
        searchJob?.cancel()
        _journeys.value = emptyList()
        _searchError.value = null
        _hasSearched.value = false
    }

    fun nearestStop(lat: Double, lon: Double): ResolvedStop? =
        scheduleRepository.stops.value.minByOrNull {
            val dLat = it.lat - lat
            val dLon = it.lon - lon
            dLat * dLat + dLon * dLon
        }

    fun onConnectionsReady() {
        if (_origin.value != null && _destination.value != null) triggerSearch()
    }
}
