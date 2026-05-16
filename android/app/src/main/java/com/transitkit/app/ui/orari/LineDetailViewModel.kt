package com.transitkit.app.ui.orari

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.data.gtfsrt.VehiclePosition
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.RouteDirection
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.model.ServiceAlert
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.AlertStore
import com.transitkit.app.data.store.FavoritesStore
import com.transitkit.app.data.store.VehicleStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Veicolo live per la card "1 IN SERVIZIO" del LineDetail. iOS parity con
 * Movete "Vettura B19 / Prossima fermata: Oak Grove Road".
 */
data class LiveVehicleCard(
    val vehicleId: String,
    val label: String,            // es. "Vettura B19" o fallback vehicleId
    val nextStopName: String?,    // resolved, può essere null se currentStopId non matcha
    val tripId: String?,          // trip in corso — null se il feed non lo espone
    val firstStopId: String?,     // prima fermata del trip — per centrare TripDetail
    val headsign: String,         // direzione corrente — TripDetail la usa come titolo
    val routeName: String,        // short name della linea — passato a TripDetail
    val routeColor: String,       // hex senza '#'
)

@HiltViewModel
class LineDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository,
    private val vehicleStore: VehicleStore,
    private val alertStore: AlertStore,
    private val favoritesStore: FavoritesStore,
) : ViewModel() {

    private val routeId: String = checkNotNull(savedStateHandle["routeId"])

    /** Active service alerts that include this route among their affected entities. */
    val routeAlerts: StateFlow<List<ServiceAlert>> = alertStore.byRouteId
        .map { it[routeId].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val route: StateFlow<ScheduleRoute?> = scheduleRepository.routes
        .map { routes -> routes.firstOrNull { it.id == routeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Schedule route index — used by `AlertCard` to render affected-line badges. */
    val routesById: StateFlow<Map<String, ScheduleRoute>> = scheduleRepository.routes
        .map { it.associateBy { r -> r.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val selectedDirectionIndex: StateFlow<Int> = savedStateHandle.getStateFlow("directionIndex", 0)

    val directions: StateFlow<List<RouteDirection>> = route
        .map { it?.directions ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDirection(index: Int) {
        savedStateHandle["directionIndex"] = index
    }

    /** Lista veicoli live per la rotta (full data, non solo count). */
    val liveVehicles: StateFlow<List<VehiclePosition>> = vehicleStore.vehiclesByRouteId
        .map { byRoute -> byRoute[routeId].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val liveVehicleCount: StateFlow<Int> = liveVehicles
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Cards "1 IN SERVIZIO" — combina veicoli + lookup nome prossima fermata + i
     *  campi necessari per spingere TripDetail al tap (route id/name/color,
     *  direzione corrente come headsign, fermata di partenza per centrare la
     *  timeline). Quando il feed non espone `tripId`, la card resta visibile ma
     *  non navigabile a TripDetail. */
    val liveVehicleCards: StateFlow<List<LiveVehicleCard>> = combine(
        liveVehicles, scheduleRepository.stops, route, selectedDirectionIndex
    ) { vehicles, stops, currentRoute, dirIdx ->
        val stopsById = stops.associateBy { it.id }
        val direction = currentRoute?.directions?.getOrNull(dirIdx)
        val firstStopId = direction?.stopIds?.firstOrNull()
        val headsign = direction?.headsign ?: ""
        val routeName = currentRoute?.name ?: ""
        val routeColor = currentRoute?.color ?: ""
        vehicles.map { v ->
            val label = v.label.takeIf { it.isNotBlank() } ?: v.vehicleId
            LiveVehicleCard(
                vehicleId = v.vehicleId,
                label = label,
                nextStopName = v.currentStopId?.let { stopsById[it]?.name },
                tripId = v.tripId?.takeIf { it.isNotBlank() },
                firstStopId = firstStopId,
                headsign = headsign,
                routeName = routeName,
                routeColor = routeColor,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Stops ordered by the selected direction's stopIds sequence
    val stops: StateFlow<List<ResolvedStop>> = combine(route, scheduleRepository.stops, selectedDirectionIndex) { r, allStops, dirIdx ->
        val stopsById = allStops.associateBy { it.id }
        r?.directions?.getOrNull(dirIdx)?.stopIds?.mapNotNull { stopsById[it] } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Maps route short name → GTFS color — used to color coincidence badges per iOS. */
    val routeColorByName: StateFlow<Map<String, String>> = scheduleRepository.routes
        .map { routes -> routes.associate { it.name to it.color } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // -------------------------------------------------------------------------
    // Route favorites — controlla iscrizione agli alert per linea.
    // -------------------------------------------------------------------------

    val isFavorite: StateFlow<Boolean> = favoritesStore.favoriteRouteIds
        .map { it.contains(routeId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentlyFav = favoritesStore.favoriteRouteIds.let { flow ->
                // grab last value via stateIn snapshot
                isFavorite.value
            }
            if (currentlyFav) favoritesStore.removeFavoriteRoute(routeId)
            else favoritesStore.addFavoriteRoute(routeId)
        }
    }
}
