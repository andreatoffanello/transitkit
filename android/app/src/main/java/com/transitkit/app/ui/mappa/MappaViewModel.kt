package com.transitkit.app.ui.mappa

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.model.ResolvedDeparture
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.gtfsrt.VehiclePosition
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.VehicleStore
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.compose.ui.graphics.Color
import com.transitkit.app.data.model.ScheduleRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MappaViewModel @Inject constructor(
    private val vehicleStore: VehicleStore,
    private val config: OperatorConfig,
    private val scheduleRepository: ScheduleRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // ---------------------------------------------------------------------------
    // Public state
    // ---------------------------------------------------------------------------

    // Differential merge: preserves existing VehiclePosition object references for
    // unchanged vehicles so Compose MarkerState objects are not needlessly recreated.
    private val _vehicles = MutableStateFlow<List<VehiclePosition>>(emptyList())
    val vehicles: StateFlow<List<VehiclePosition>> = _vehicles.asStateFlow()

    val mapCenter: StateFlow<LatLng> = MutableStateFlow(
        LatLng(config.map.centerLat, config.map.centerLng)
    ).asStateFlow()

    val defaultZoom: StateFlow<Float> = MutableStateFlow(
        config.map.defaultZoom.toFloat()
    ).asStateFlow()

    private val _selectedStop = MutableStateFlow<ResolvedStop?>(null)
    val selectedStop: StateFlow<ResolvedStop?> = _selectedStop.asStateFlow()

    private val _stopDepartures = MutableStateFlow<List<ResolvedDeparture>>(emptyList())
    val stopDepartures: StateFlow<List<ResolvedDeparture>> = _stopDepartures.asStateFlow()

    private val _isDeparturesLoading = MutableStateFlow(false)
    val isDeparturesLoading: StateFlow<Boolean> = _isDeparturesLoading.asStateFlow()

    private val _isVehiclePollingActive = MutableStateFlow(false)
    val isVehiclePollingActive: StateFlow<Boolean> = _isVehiclePollingActive.asStateFlow()

    val routes: StateFlow<List<ScheduleRoute>> = scheduleRepository.routes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Selected vehicle for bottom sheet
    private val _selectedVehicle = MutableStateFlow<Pair<VehiclePosition, Color>?>(null)
    val selectedVehicle: StateFlow<Pair<VehiclePosition, Color>?> = _selectedVehicle.asStateFlow()

    val tripDelays: StateFlow<Map<String, Int>> = vehicleStore.tripDelays

    /**
     * Live vehicle count per route, keyed by `routeId`. Consumed by the line-picker sheet
     * and by the dismiss chip to show a green dot + "N" when a line has active vehicles.
     * Recomputed reactively whenever the vehicle feed updates.
     */
    val liveCountByRouteId: StateFlow<Map<String, Int>> =
        _vehicles.map { list ->
            list.mapNotNull { it.routeId }.groupingBy { it }.eachCount()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _selectedRouteId: StateFlow<String?> = savedStateHandle.getStateFlow("selected_route_id", null)
    val selectedRouteId: StateFlow<String?> = _selectedRouteId

    /**
     * Polyline coordinates for the currently selected route. Flattened across directions —
     * each direction's shape is emitted as its own list. Null when no route is selected.
     *
     * Mirrors iOS `MappaTab.recomputeRoutePolylines` — prefers the shape encoded on each
     * `RouteDirection`, falls back to the ordered stop coordinates for that direction.
     * Shape tuples in JSON are `[lat, lng]` (see pipeline/build.py::parse_shapes).
     */
    val routePolylines: StateFlow<List<List<LatLng>>> =
        combine(scheduleRepository.routes, _selectedRouteId, scheduleRepository.stops) { routeList, routeId, stopList ->
            if (routeId == null) return@combine emptyList()
            val route = routeList.firstOrNull { it.id == routeId } ?: return@combine emptyList()
            val stopsById = stopList.associateBy { it.id }
            route.directions.mapNotNull { dir ->
                val fromShape = dir.shape.mapNotNull { pair ->
                    if (pair.size >= 2) LatLng(pair[0], pair[1]) else null
                }
                val coords = if (fromShape.size >= 2) fromShape
                else dir.stopIds.mapNotNull { sid ->
                    stopsById[sid]?.let { LatLng(it.lat, it.lon) }
                }
                coords.takeIf { it.size >= 2 }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stops: StateFlow<List<ResolvedStop>> =
        combine(scheduleRepository.stops, _selectedRouteId) { stopList, routeId ->
            if (routeId == null) stopList
            else stopList.filter { it.routeIds.contains(routeId) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Vehicles paired with their GTFS route color (falls back to brand accent). */
    val vehiclesWithColor: StateFlow<List<Pair<VehiclePosition, Color>>> =
        combine(_vehicles, scheduleRepository.routes, _selectedRouteId) { vehicleList, routeList, routeId ->
            val routeColorMap = routeList.associate { route ->
                route.id to if (route.color.isNotBlank())
                    runCatching {
                        Color(android.graphics.Color.parseColor("#${route.color}"))
                    }.getOrNull()
                else null
            }
            val filtered = if (routeId == null) vehicleList
                           else vehicleList.filter { it.routeId == routeId }
            filtered.map { vehicle ->
                vehicle to (routeColorMap[vehicle.routeId]
                    ?: runCatching { Color(android.graphics.Color.parseColor(config.theme.accentColor)) }
                        .getOrElse { Color(0xFF06845C) })
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            vehicleStore.stopPolling()
            _isVehiclePollingActive.value = false
        }
        override fun onStart(owner: LifecycleOwner) {
            vehicleStore.startPolling()
            _isVehiclePollingActive.value = true
        }
    }

    init {
        viewModelScope.launch {
            scheduleRepository.load()
            vehicleStore.startPolling()
            _isVehiclePollingActive.value = true
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)

        vehicleStore.vehicles
            .onEach { newVehicles ->
                val existing = _vehicles.value.associateBy { it.vehicleId }
                _vehicles.value = newVehicles.map { new ->
                    existing[new.vehicleId]?.takeIf { it == new } ?: new
                }
                // Rebind the open vehicle card to the latest snapshot so
                // position, currentStopId, status and timestamp stay current.
                val selection = _selectedVehicle.value
                if (selection != null) {
                    val updated = newVehicles.firstOrNull { it.vehicleId == selection.first.vehicleId }
                    if (updated != null) {
                        if (updated != selection.first) {
                            _selectedVehicle.value = updated to selection.second
                        }
                    } else {
                        _selectedVehicle.value = null
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        vehicleStore.stopPolling()
        _isVehiclePollingActive.value = false
        super.onCleared()
    }

    // ---------------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------------

    fun selectStop(stop: ResolvedStop) {
        _selectedStop.value = stop
        _stopDepartures.value = emptyList()
        viewModelScope.launch {
            _isDeparturesLoading.value = true
            try {
                val departures = scheduleRepository.upcomingDepartures(stop.id)
                _stopDepartures.value = departures
            } catch (_: Exception) {
                _stopDepartures.value = emptyList()
            } finally {
                _isDeparturesLoading.value = false
            }
        }
    }

    fun clearSelectedStop() {
        _selectedStop.value = null
        _stopDepartures.value = emptyList()
    }

    fun selectVehicle(vehicle: VehiclePosition, color: Color) {
        _selectedVehicle.value = vehicle to color
    }

    fun clearSelectedVehicle() {
        _selectedVehicle.value = null
    }

    fun selectRoute(routeId: String?) {
        savedStateHandle["selected_route_id"] = if (_selectedRouteId.value == routeId) null else routeId
    }

    /** Resolves either our synthetic station id or an original GTFS stop_id
     *  (as received from GTFS-RT feeds) back to a human-readable stop name. */
    fun resolveStopName(anyStopId: String): String? =
        scheduleRepository.resolveStop(anyStopId)?.name

    /** Predicted arrival epoch millis: scheduled departure for (trip, stop) +
     *  RT delay. Returns null when we can't resolve both. */
    fun predictedArrivalEpochMs(tripId: String, anyStopId: String): Long? {
        val scheduled = scheduleRepository.scheduledDepartureEpochMs(tripId, anyStopId) ?: return null
        val delaySec = tripDelays.value[tripId] ?: 0
        return scheduled + delaySec * 1000L
    }

    /** Operator timezone ID — used by the vehicle card to render clock times
     *  in the operator's local time rather than the device's. */
    val operatorTimezoneId: String
        get() = config.timezone
}
