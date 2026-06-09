package com.transitkit.app.ui.mappa

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.gtfsrt.VehiclePosition
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.VehicleStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "Vista linea" riusabile per mappe secondarie (es. mappa espansa del
 * dettaglio fermata): incapsula selezione di una linea + derivazione
 * reattiva di polilinea, fermate della linea e mezzi live filtrati —
 * stessa semantica della mappa principale ([MappaViewModel]) ma con
 * stato locale non persistito e liste VUOTE quando nessuna linea è
 * selezionata (i layer restano composti e si svuotano da soli).
 */
@HiltViewModel
class MapLineFocusViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val vehicleStore: VehicleStore,
    private val config: OperatorConfig,
) : ViewModel() {

    private val _selectedRouteId = MutableStateFlow<String?>(null)
    val selectedRouteId: StateFlow<String?> = _selectedRouteId.asStateFlow()

    val routes: StateFlow<List<ScheduleRoute>> = scheduleRepository.routes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedRoute: StateFlow<ScheduleRoute?> =
        combine(scheduleRepository.routes, _selectedRouteId) { routeList, routeId ->
            routeId?.let { id -> routeList.firstOrNull { it.id == id } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Una lista di Point per direzione — stessa derivazione di [MappaViewModel.routePolylines]. */
    val routePolylines: StateFlow<List<List<Point>>> =
        combine(scheduleRepository.routes, _selectedRouteId, scheduleRepository.stops) { routeList, routeId, stopList ->
            if (routeId == null) return@combine emptyList()
            val route = routeList.firstOrNull { it.id == routeId } ?: return@combine emptyList()
            val stopsById = stopList.associateBy { it.id }
            route.directions.mapNotNull { dir ->
                val fromShape = dir.shape.mapNotNull { pair ->
                    if (pair.size >= 2) Point.fromLngLat(pair[1], pair[0]) else null
                }
                val coords = if (fromShape.size >= 2) fromShape
                else dir.stopIds.mapNotNull { sid ->
                    stopsById[sid]?.let { Point.fromLngLat(it.lon, it.lat) }
                }
                coords.takeIf { it.size >= 2 }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Fermate della linea selezionata; vuota senza selezione. */
    val lineStops: StateFlow<List<ResolvedStop>> =
        combine(scheduleRepository.stops, _selectedRouteId) { stopList, routeId ->
            if (routeId == null) emptyList()
            else stopList.filter { it.routeIds.contains(routeId) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Mezzi live della sola linea selezionata, col colore route (filtro per routeId — main map parity). */
    val vehiclesWithColor: StateFlow<List<Pair<VehiclePosition, Color>>> =
        combine(vehicleStore.vehicles, scheduleRepository.routes, _selectedRouteId) { vehicleList, routeList, routeId ->
            if (routeId == null) return@combine emptyList()
            val accentFallback = com.transitkit.app.ui.components.parseHexColor(
                config.theme.accentColor,
                fallback = Color(0xFF06845C),
            )
            val routeColorMap = routeList.associate { route ->
                route.id to if (route.color.isNotBlank())
                    com.transitkit.app.ui.components.parseHexColor(route.color, fallback = accentFallback)
                else null
            }
            vehicleList
                .filter { it.routeId == routeId }
                .map { vehicle -> vehicle to (routeColorMap[vehicle.routeId] ?: accentFallback) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            scheduleRepository.load()
            // Idempotente: il polling app-wide è gestito da Home/Mappa, qui
            // solo garanzia difensiva per deep-link diretti al dettaglio.
            vehicleStore.startPolling()
        }
    }

    fun toggleRoute(routeId: String) {
        _selectedRouteId.value = if (_selectedRouteId.value == routeId) null else routeId
    }

    fun clear() {
        _selectedRouteId.value = null
    }
}
