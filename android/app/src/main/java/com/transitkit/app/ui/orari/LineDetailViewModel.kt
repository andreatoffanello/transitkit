package com.transitkit.app.ui.orari

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.RouteDirection
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.model.ServiceAlert
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.AlertStore
import com.transitkit.app.data.store.VehicleStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LineDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository,
    private val vehicleStore: VehicleStore,
    private val alertStore: AlertStore,
) : ViewModel() {

    private val routeId: String = checkNotNull(savedStateHandle["routeId"])

    /** Active service alerts that include this route among their affected entities. */
    val routeAlerts: StateFlow<List<ServiceAlert>> = alertStore.byRouteId
        .map { it[routeId].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val route: StateFlow<ScheduleRoute?> = scheduleRepository.routes
        .map { routes -> routes.firstOrNull { it.id == routeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedDirectionIndex: StateFlow<Int> = savedStateHandle.getStateFlow("directionIndex", 0)

    val directions: StateFlow<List<RouteDirection>> = route
        .map { it?.directions ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDirection(index: Int) {
        savedStateHandle["directionIndex"] = index
    }

    val liveVehicleCount: StateFlow<Int> = vehicleStore.vehiclesByRouteId
        .map { byRoute -> byRoute[routeId]?.size ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // Stops ordered by the selected direction's stopIds sequence
    val stops: StateFlow<List<ResolvedStop>> = combine(route, scheduleRepository.stops, selectedDirectionIndex) { r, allStops, dirIdx ->
        val stopsById = allStops.associateBy { it.id }
        r?.directions?.getOrNull(dirIdx)?.stopIds?.mapNotNull { stopsById[it] } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Maps route short name → GTFS color — used to color coincidence badges per iOS. */
    val routeColorByName: StateFlow<Map<String, String>> = scheduleRepository.routes
        .map { routes -> routes.associate { it.name to it.color } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
