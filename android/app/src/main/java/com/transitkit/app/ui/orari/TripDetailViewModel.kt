package com.transitkit.app.ui.orari

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.R
import com.transitkit.app.data.model.StopTime
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.VehicleStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Maps stopId → list of other route short names that also serve that stop
typealias StopCoincidences = Map<String, List<String>>

sealed class TripState {
    object Loading : TripState()
    data class Success(val stops: List<StopTime>, val originIndex: Int) : TripState()
    data class Error(@StringRes val messageRes: Int) : TripState()
}

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleRepository: ScheduleRepository,
    private val vehicleStore: VehicleStore,
) : ViewModel() {

    val tripId: String = java.net.URLDecoder.decode(checkNotNull(savedStateHandle["tripId"]), "UTF-8")
    val fromStopId: String = java.net.URLDecoder.decode(savedStateHandle["fromStopId"] ?: "", "UTF-8")
    val routeColor: String = java.net.URLDecoder.decode(savedStateHandle["routeColor"] ?: "", "UTF-8")
    val headsign: String = java.net.URLDecoder.decode(savedStateHandle["headsign"] ?: "", "UTF-8")
    val routeName: String = java.net.URLDecoder.decode(savedStateHandle["routeName"] ?: "", "UTF-8")

    private val _tripState = MutableStateFlow<TripState>(TripState.Loading)
    val tripState: StateFlow<TripState> = _tripState.asStateFlow()

    private val _stopCoincidences = MutableStateFlow<StopCoincidences>(emptyMap())
    val stopCoincidences: StateFlow<StopCoincidences> = _stopCoincidences.asStateFlow()

    val routeColorByName: StateFlow<Map<String, String>> = scheduleRepository.routes
        .map { routes -> routes.associate { it.name to it.color } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /// Live origin index — tracks the vehicle's current stop as it moves.
    /// Combines the static stop sequence with the RT vehicle feed: whenever
    /// the vehicleByTripId[tripId].currentStopId changes, the "Now" highlight
    /// jumps to that row. Falls back to `fromStopId` when no live vehicle is
    /// available (trip finished or not yet in service).
    val liveOriginIndex: StateFlow<Int> = combine(
        tripState,
        vehicleStore.vehicleByTripId,
    ) { state, byTripId ->
        val stops = (state as? TripState.Success)?.stops ?: return@combine 0
        val vehicle = byTripId[tripId]
        val currentStopId = vehicle?.currentStopId
        if (!currentStopId.isNullOrBlank()) {
            // RT feed gives us the GTFS-native stop_id. Match it against
            // each station's aggregated gtfsStopIds via the repo resolver,
            // or accept direct id equality as a lenient fallback.
            val resolvedStationId = scheduleRepository.resolveStop(currentStopId)?.id
            val idx = stops.indexOfFirst {
                it.stopId == currentStopId || it.stopId == resolvedStationId
            }
            if (idx >= 0) return@combine idx
        }
        // Fall back to the opening-stop-based origin
        (state as? TripState.Success)?.originIndex ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            try {
                // Wait for schedule to be loaded (may already be ready)
                val schedule = scheduleRepository.scheduleResponse
                    .filterNotNull()
                    .first()

                // Heavy work — three O(stops × deps) scans — runs off the main
                // thread. Without this, opening a trip detail blocked the UI
                // for tens of ms on AppalCART-sized schedules (~500 stops ×
                // ~10 deps each).
                val result = kotlinx.coroutines.withContext(Dispatchers.Default) {
                    // Build stopId → stop lookup once.
                    val stopById = schedule.stops.associateBy { it.id }

                    // Single pass over the schedule: collect every departure
                    // for this trip + record the currentRouteId. Avoids 3
                    // separate full-table scans of the previous version.
                    val tripStops = mutableListOf<StopTime>()
                    var currentRouteId: String? = null
                    schedule.stops.forEach { stop ->
                        stop.departures.forEach { dep ->
                            if (dep.tripId == tripId) {
                                if (currentRouteId == null) currentRouteId = dep.routeId
                                tripStops += StopTime(
                                    stopId = stop.id,
                                    stopName = stop.name,
                                    departureTime = dep.departureTime,
                                    sequenceNumber = timeToMinutes(dep.departureTime),
                                )
                            }
                        }
                    }
                    val stopTimes = tripStops.sortedBy { it.sequenceNumber }
                    val originIdx = stopTimes.indexOfFirst { it.stopId == fromStopId }.coerceAtLeast(0)
                    val coincidences = stopTimes.associate { st ->
                        val others = stopById[st.stopId]?.departures
                            ?.filter { it.routeId != currentRouteId }
                            ?.map { it.routeName }
                            ?.distinct()
                            ?: emptyList()
                        st.stopId to others
                    }
                    Triple(stopTimes, originIdx, coincidences)
                }

                val (stopTimes, originIdx, coincidences) = result
                if (stopTimes.isEmpty()) {
                    _tripState.value = TripState.Error(R.string.trip_error_no_stops)
                    return@launch
                }
                _tripState.value = TripState.Success(stopTimes, originIdx)
                _stopCoincidences.value = coincidences
            } catch (_: Exception) {
                _tripState.value = TripState.Error(R.string.trip_error_load_failed)
            }
        }
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }
}
