package com.transitkit.app.ui.orari

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.data.model.Departure
import com.transitkit.app.data.model.ResolvedDeparture
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.toDeparture
import com.transitkit.app.data.model.ServiceAlert
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.AlertStore
import com.transitkit.app.data.store.FavoritesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

sealed class DeparturesState {
    object Loading : DeparturesState()
    data class Success(val departures: List<Departure>) : DeparturesState()
    object Empty : DeparturesState()
    data class Error(val message: String) : DeparturesState()
}

@HiltViewModel
class StopDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository,
    private val favoritesStore: FavoritesStore,
    private val alertStore: AlertStore,
) : ViewModel() {

    val stopId: String = run {
        val raw: String = checkNotNull(savedStateHandle["stopId"])
        try { URLDecoder.decode(raw, StandardCharsets.UTF_8.name()) } catch (_: Exception) { raw }
    }

    /** Active service alerts that affect this stop **or** any of the routes
     *  that serve it. Matches the Movete scope: a stop is "relevant" when
     *  either its station id is listed in the alert's `affectedStopIds`, or
     *  any line passing through this stop appears in `affectedRouteIds`. */
    val stopAlerts: StateFlow<List<ServiceAlert>> = combine(
        alertStore.activeAlerts,
        scheduleRepository.stops.map { stops -> stops.firstOrNull { it.id == stopId } },
    ) { alerts, stop ->
        if (stop == null) return@combine emptyList()
        val stationIds = (stop.gtfsStopIds + stop.id).toSet()
        val routeIdsForStop = stop.routeIds.toSet()
        alerts.filter { a ->
            a.affectedStopIds.any { it in stationIds } || a.isRelevant(routeIdsForStop)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Schedule route index — used by `AlertCard` to render affected-line badges. */
    val routesById: StateFlow<Map<String, com.transitkit.app.data.model.ScheduleRoute>> =
        scheduleRepository.routes
            .map { it.associateBy { r -> r.id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Resolved human-readable stop name from the schedule store. Available
     *  once the schedule loads; null until then. Used by the detail top bar
     *  so deep links that only carry the stopId still render a nice title
     *  instead of the raw "appalcart_asu_college_st_station" slug. */
    val resolvedStopName: StateFlow<String?> = scheduleRepository.stops
        .map { stops -> stops.firstOrNull { it.id == stopId }?.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _loadState = MutableStateFlow<DeparturesState>(DeparturesState.Loading)
    private val _rawDepartures = MutableStateFlow<List<ResolvedDeparture>>(emptyList())
    private val _allDepartures = MutableStateFlow<List<ResolvedDeparture>>(emptyList())
    val selectedRouteFilter: StateFlow<String?> = savedStateHandle.getStateFlow("selectedRouteFilter", null)

    /** All departures for the day (unfiltered by time) — for FullScheduleSheet. */
    val rawDepartures: StateFlow<List<ResolvedDeparture>> = _rawDepartures.asStateFlow()

    /**
     * All departures grouped by service-day signature (sorted weekday keys joined with ",").
     * Key: e.g. "friday,monday,thursday,tuesday,wednesday" → Feriali
     * For FullScheduleSheet day-group selector.
     */
    val departuresByGroup: StateFlow<Map<String, List<ResolvedDeparture>>> = _allDepartures
        .map { all ->
            all.groupBy { dep ->
                dep.serviceDays.map { it.lowercase() }.sorted().joinToString(",")
            }.toSortedMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Distinct routes that serve this stop on **any** day of the week,
     *  sorted alphabetically by route short name so the pill row order
     *  stays stable and matches iOS.
     *  Week-wide list keeps the row stable across days — otherwise a
     *  user opening a stop on Sunday would see a shorter pill row than
     *  on a weekday, which is confusing ("did lines disappear?"). The
     *  departures list underneath is still today-filtered. */
    val availableRoutes: StateFlow<List<ResolvedDeparture>> = _allDepartures
        .map { all ->
            all.distinctBy { it.routeId }
                .sortedBy { it.routeName.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val tickFlow = flow<Unit> {
        while (true) { emit(Unit); delay(15_000L) }
    }

    val departuresState: StateFlow<DeparturesState> = combine(_loadState, _rawDepartures, tickFlow, selectedRouteFilter) { loadState, raw, _, routeFilter ->
        if (loadState is DeparturesState.Loading || loadState is DeparturesState.Error) return@combine loadState
        val tz = TimeZone.getTimeZone(scheduleRepository.operatorTimezone)
        val cal = Calendar.getInstance(tz)
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val upcoming = raw.filter { it.minutesFromMidnight >= nowMinutes }.map { it.toDeparture() }
        val filtered = if (routeFilter != null) upcoming.filter { it.routeId == routeFilter } else upcoming
        if (filtered.isEmpty()) DeparturesState.Empty else DeparturesState.Success(filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeparturesState.Loading)

    /** Operator timezone string (e.g. "Europe/Rome") — used by the UI for countdown display. */
    val operatorTimezone: String get() = scheduleRepository.operatorTimezone

    val isFavorite: StateFlow<Boolean> = favoritesStore.favoriteStopIds
        .map { ids -> ids.contains(stopId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val stopLocation: StateFlow<Pair<Double, Double>?> = scheduleRepository.stops
        .map { stops -> stops.find { it.id == stopId }?.let { it.lat to it.lon } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Stop completo risolto dal schedule store — usato dalla hero map per
     *  passarlo al [com.transitkit.app.ui.mappa.StopSymbolLayer] e mantenere
     *  così la PARITÀ del marker con la mappa principale (single source of
     *  truth: stesso bitmap, stesse soglie zoom, stesso glyph metro). */
    val resolvedStop: StateFlow<ResolvedStop?> = scheduleRepository.stops
        .map { stops -> stops.firstOrNull { it.id == stopId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Maps routeId → stop sequence string ("Fermata A · Fermata B · …") — mirrors iOS scheduleStore.routeStopSequences. */
    val stopSequenceByRouteId: StateFlow<Map<String, String>> = scheduleRepository.scheduleResponse
        .map { schedule ->
            schedule?.routes?.associate { route ->
                val stopNamesById = schedule.stops.associateBy({ it.id }, { it.name })
                val names = route.directions.firstOrNull()?.stopIds
                    ?.mapNotNull { stopNamesById[it] }
                    ?: emptyList()
                route.id to names.joinToString(" · ")
            } ?: emptyMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        loadDepartures()
    }

    fun loadDepartures() {
        viewModelScope.launch {
            _loadState.value = DeparturesState.Loading
            // Ensure schedule is loaded before querying
            scheduleRepository.load()
            val raw = scheduleRepository.upcomingDepartures(stopId)
            _rawDepartures.value = raw
            _allDepartures.value = scheduleRepository.allDepartures(stopId)
            // Transition out of Loading so the tick-combine can take over
            _loadState.value = if (raw.isEmpty()) DeparturesState.Empty else DeparturesState.Success(emptyList())
        }
    }

    fun selectRouteFilter(routeId: String?) {
        val current = savedStateHandle.get<String?>("selectedRouteFilter")
        savedStateHandle["selectedRouteFilter"] = if (current == routeId) null else routeId
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            if (favoritesStore.isFavorite(stopId)) {
                favoritesStore.removeFavorite(stopId)
            } else {
                favoritesStore.addFavorite(stopId)
            }
        }
    }
}
