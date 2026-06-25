package com.transitkit.app.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.PlannerLocation
import com.transitkit.app.data.model.PlannerStop
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.ConnectionsStore
import com.transitkit.app.data.store.SearchHistoryStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val connectionsStore: ConnectionsStore,
    private val scheduleRepository: ScheduleRepository,
    private val config: OperatorConfig,
    private val searchHistoryStore: SearchHistoryStore,
) : ViewModel() {

    data class WhenSelection(
        val mode: Int = 0,  // 0 = now, 1 = departAt, 2 = arriveBy
        val date: java.util.Date = java.util.Date(),
    )

    private val _origin = MutableStateFlow<PlannerLocation?>(null)
    val origin = _origin.asStateFlow()

    private val _destination = MutableStateFlow<PlannerLocation?>(null)
    val destination = _destination.asStateFlow()

    private val _whenSelection = MutableStateFlow(WhenSelection())
    val whenSelection = _whenSelection.asStateFlow()

    private val _journeys = MutableStateFlow<List<Journey>>(emptyList())
    val journeys = _journeys.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    /**
     * Esito fallito dell'ultima ricerca. `Unreachable` (backend giù / rete
     * assente / 5xx / decode — transitorio, con retry) è distinto dal genuino
     * "nessun viaggio" (200 OK, zero itinerari → `journeys` vuoti, niente
     * errore) e dall'input invalido. Prima un timeout del routing finiva
     * mascherato da "nessun viaggio", una bugia che dava la colpa al viaggio
     * invece che all'outage. Parità con iOS `PlannerScreen.isUnreachable`.
     */
    enum class SearchError { SameStop, OutOfServiceArea, Unreachable }

    private val _searchError = MutableStateFlow<SearchError?>(null)
    val searchError = _searchError.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched = _hasSearched.asStateFlow()

    val connectionsState = connectionsStore.state

    val allStops = scheduleRepository.stops

    // ── Home-box state (separate from planner tab state) ──────────────────────

    private val _homeOrigin = MutableStateFlow<PlannerLocation?>(null)
    val homeOrigin = _homeOrigin.asStateFlow()

    private val _homeDestination = MutableStateFlow<PlannerLocation?>(null)
    val homeDestination = _homeDestination.asStateFlow()

    fun setHomeOrigin(loc: PlannerLocation?) {
        _homeOrigin.value = loc
        recordLocationUsed(loc)
    }
    fun setHomeDestination(loc: PlannerLocation?) {
        _homeDestination.value = loc
        recordLocationUsed(loc)
    }
    fun swapHomeStops() {
        val tmp = _homeOrigin.value
        _homeOrigin.value = _homeDestination.value
        _homeDestination.value = tmp
    }
    fun clearHomeState() {
        _homeOrigin.value = null
        _homeDestination.value = null
    }

    // ── Current GPS location (fed by HomeScreen, used by LocationPickerScreen) ─

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    fun updateGpsLocation(lat: Double, lon: Double) {
        _currentLocation.value = Pair(lat, lon)
    }

    /** Fallback map center from operator config (lat, lon) + default zoom. */
    val mapFallbackCenter: Pair<Double, Double> = Pair(config.map.centerLat, config.map.centerLng)
    val mapDefaultZoom: Double = config.map.defaultZoom

    /** Operator timezone ID (e.g. "America/New_York") — drives clock display + picker. */
    val operatorTimezone: String get() = config.timezone

    /** Recently used stops (most recent first), resolved against the loaded stop list. */
    val recentStops: StateFlow<List<ResolvedStop>> = combine(
        searchHistoryStore.recentStopIds,
        scheduleRepository.stops,
    ) { ids, stops ->
        val byId = stops.associateBy { it.id }
        ids.mapNotNull { byId[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Nearby stops sorted by haversine distance from current GPS location. */
    fun nearbyStops(limit: Int = 6): List<Pair<ResolvedStop, Double>> {
        val loc = _currentLocation.value ?: return emptyList()
        return scheduleRepository.stops.value
            .map { stop -> stop to haversineMeters(loc.first, loc.second, stop.lat, stop.lon) }
            .sortedBy { it.second }
            .take(limit)
    }

    private fun recordLocationUsed(loc: PlannerLocation?) {
        val s = loc?.stopId ?: return
        viewModelScope.launch { searchHistoryStore.recordStop(s) }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2).let { it * it }
        return 2 * r * kotlin.math.asin(kotlin.math.sqrt(a))
    }

    init {
        // MOTIS is remote — initialize the routing provider immediately, no GTFS
        // bundle to fetch first.
        connectionsStore.load(config)
        viewModelScope.launch {
            connectionsStore.state.collect { state ->
                if (state == ConnectionsStore.LoadState.READY) onConnectionsReady()
            }
        }
        applyPendingPrefill()
    }

    /**
     * Consumes a one-shot prefill set by the deep-link handler in MainActivity.
     * `whenStr` ("HH:MM") is interpreted in the operator timezone — not the
     * device TZ — so `transitkit://planner?when=14:30` for AppalCART means
     * 14:30 New York local, not 14:30 Rome local.
     */
    private fun applyPendingPrefill() {
        val prefill = PendingPlannerPrefillStore.consume() ?: return
        viewModelScope.launch {
            // Wait for stops to be ready so we can match names → ResolvedStop.
            val stops = scheduleRepository.stops
            val current = stops.value
            val available = if (current.isNotEmpty()) current else run {
                scheduleRepository.load()
                stops.value
            }
            fun matchByName(name: String?): PlannerLocation? {
                if (name.isNullOrBlank()) return null
                val lower = name.trim().lowercase()
                val match = available.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    ?: available.firstOrNull { it.name.lowercase().contains(lower) }
                return match?.let { PlannerLocation.fromStop(it) }
            }
            prefill.from?.let { matchByName(it) }?.also { _origin.value = it }
            prefill.to?.let { matchByName(it) }?.also { _destination.value = it }
            prefill.whenStr?.let { whenStr ->
                parseHhMmInOperatorTz(whenStr, config.timezone)?.let { date ->
                    _whenSelection.value = WhenSelection(mode = 1, date = date)
                }
            }
            if (_origin.value != null && _destination.value != null) triggerSearch()
        }
    }

    private fun parseHhMmInOperatorTz(hhmm: String, tz: String): java.util.Date? {
        val parts = hhmm.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(tz))
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
        cal.set(java.util.Calendar.MINUTE, minute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        // If the requested time is already in the past today, roll to tomorrow.
        if (cal.timeInMillis < System.currentTimeMillis()) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.time
    }

    private val _selectedJourney = MutableStateFlow<Journey?>(null)
    val selectedJourney = _selectedJourney.asStateFlow()

    fun selectJourney(journey: Journey) { _selectedJourney.value = journey }

    private var searchJob: Job? = null

    fun setOrigin(loc: PlannerLocation?) {
        _origin.value = loc
        recordLocationUsed(loc)
        triggerSearch()
    }

    fun setDestination(loc: PlannerLocation?) {
        _destination.value = loc
        recordLocationUsed(loc)
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
        if (o.lat == d.lat && o.lon == d.lon) {
            _searchError.value = SearchError.SameStop
            _hasSearched.value = true
            _journeys.value = emptyList()
            return
        }
        if (connectionsStore.loadState != ConnectionsStore.LoadState.READY) return

        // Service-area guard: una coordinata libera (Place / CurrentLocation) lontana
        // oltre `MAX_DISTANCE_FROM_SERVICE_AREA_METERS` dalla fermata più vicima non
        // può produrre routing sensato — la intercettiamo prima di chiamare MOTIS.
        if (isOutsideServiceArea(o) || isOutsideServiceArea(d)) {
            searchJob?.cancel()
            _searchError.value = SearchError.OutOfServiceArea
            _hasSearched.value = true
            _journeys.value = emptyList()
            return
        }

        // MOTIS routes natively from lat/lon. Stop id is passed through when known
        // for label/ICS resolution downstream, but lat/lon drives the routing query.
        val op = PlannerStop(o.stopId ?: "${o.lat},${o.lon}", o.name, o.lat, o.lon)
        val dp = PlannerStop(d.stopId ?: "${d.lat},${d.lon}", d.name, d.lat, d.lon)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _hasSearched.value = true
            _searchError.value = null
            try {
                val results = withContext(Dispatchers.IO) {
                    when (_whenSelection.value.mode) {
                        1 -> connectionsStore.query(op, dp, _whenSelection.value.date.time)
                        2 -> connectionsStore.queryArriveBy(op, dp, _whenSelection.value.date.time)
                        else -> connectionsStore.query(op, dp, System.currentTimeMillis())
                    }
                }
                _journeys.value = results
                _isSearching.value = false
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Timeout / connessione / 5xx / decode: il servizio percorsi non
                // risponde. NON è "nessun viaggio" — è un outage, con retry.
                android.util.Log.w("Planner", "routing error: ${e.message}")
                _journeys.value = emptyList()
                _searchError.value = SearchError.Unreachable
                _isSearching.value = false
            }
        }
    }

    /** Retry dell'ultima ricerca dopo un outage (bottone "Riprova"). */
    fun retry() = triggerSearch()

    private fun reset() {
        searchJob?.cancel()
        _journeys.value = emptyList()
        _searchError.value = null
        _hasSearched.value = false
    }

    fun nearestStopWithDistance(lat: Double, lon: Double): Pair<ResolvedStop, Double>? {
        val stops = scheduleRepository.stops.value
        if (stops.isEmpty()) return null
        return stops
            .map { stop -> stop to haversineMeters(lat, lon, stop.lat, stop.lon) }
            .minByOrNull { it.second }
    }

    /**
     * `true` se la location è una coordinata libera (Place / CurrentLocation) e la
     * fermata più vicina dista oltre [MAX_DISTANCE_FROM_SERVICE_AREA_METERS]. Le
     * Stop sono per costruzione dentro l'area di servizio. Soglia generosa: cattura
     * "altro continente" senza bloccare casi legittimi al limite dell'area.
     */
    private fun isOutsideServiceArea(loc: PlannerLocation): Boolean {
        if (loc.kind == PlannerLocation.Kind.Stop) return false
        val nearest = nearestStopWithDistance(loc.lat, loc.lon) ?: return false
        return nearest.second > MAX_DISTANCE_FROM_SERVICE_AREA_METERS
    }

    private companion object {
        const val MAX_DISTANCE_FROM_SERVICE_AREA_METERS = 50_000.0
    }

    fun onConnectionsReady() {
        if (_origin.value != null && _destination.value != null) triggerSearch()
    }
}
