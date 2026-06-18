package com.transitkit.app.data.store

import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.gtfsrt.GtfsRtFetcher
import com.transitkit.app.data.gtfsrt.VehiclePosition
import com.transitkit.app.data.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val POLL_INTERVAL_MS = 15_000L
private const val MAX_POLL_INTERVAL_MS = 120_000L

@Singleton
class VehicleStore @Inject constructor(
    private val fetcher: GtfsRtFetcher,
    private val config: OperatorConfig,
    private val scheduleRepository: ScheduleRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _vehicles = MutableStateFlow<List<VehiclePosition>>(emptyList())
    val vehicles: StateFlow<List<VehiclePosition>> = _vehicles.asStateFlow()

    // O(1) lookup indices
    private val _vehicleByTripId = MutableStateFlow<Map<String, VehiclePosition>>(emptyMap())
    val vehicleByTripId: StateFlow<Map<String, VehiclePosition>> = _vehicleByTripId.asStateFlow()

    private val _vehiclesByRouteId = MutableStateFlow<Map<String, List<VehiclePosition>>>(emptyMap())
    val vehiclesByRouteId: StateFlow<Map<String, List<VehiclePosition>>> = _vehiclesByRouteId.asStateFlow()

    // tripId → delay in seconds, populated when tripUpdatesUrl is configured
    private val _tripDelays = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tripDelays: StateFlow<Map<String, Int>> = _tripDelays.asStateFlow()

    /**
     * Plausibility window for RT delays: −5 min … +30 min (in seconds).
     * Ghost trips or stale predictions often carry outliers (observed up to
     * +93 min) that are noise, not information. Matches iOS VehicleStore
     * and DoVe StopTimeUpdate.PLAUSIBLE_DELAY_RANGE (-300..1800 s).
     */
    companion object {
        private val PLAUSIBLE_DELAY_RANGE = -300..1800
    }

    /**
     * Delay in **minutes** for a trip, filtered through the plausibility window.
     * Returns null when the trip is untracked or the raw delay is an outlier.
     * Mirrors iOS `VehicleStore.reliableDelayMinutes(forTripId:)` and DoVe's
     * `StopTimeUpdate.reliableDelay`.
     */
    fun reliableDelayMinutes(tripId: String): Int? {
        val delaySec = _tripDelays.value[tripId] ?: return null
        if (delaySec !in PLAUSIBLE_DELAY_RANGE) return null
        return Math.round(delaySec / 60.0).toInt()
    }

    private var pollingJob: Job? = null
    private var consecutiveErrors = 0
    private var scheduleObserverJob: Job? = null

    // Class-level cached trip→route index — rebuilt only when schedule changes, not every poll cycle
    @Volatile private var cachedTripRouteIndex: Map<String, String> = emptyMap()
    @Volatile private var indexBuiltForScheduleKey: String? = null

    init {
        // Re-resolve vehicle route ids whenever the schedule lands/changes.
        // Without this, the first vehicle fetch that wins the race against
        // scheduleRepository.load() leaves vehicles with routeId=null until
        // the next 15s poll cycle — causing the vehicle card to show as
        // "Live vehicle / Bus" instead of the actual line.
        scheduleObserverJob = scope.launch {
            scheduleRepository.scheduleResponse.collect { response ->
                if (response != null && _vehicles.value.isNotEmpty()) {
                    updateVehicles(_vehicles.value)
                }
            }
        }
    }

    fun startPolling() {
        val vehiclePositionsUrl = config.gtfsRt?.vehiclePositionsUrl ?: return
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch {
            while (isActive) {
                val result = runCatching {
                    coroutineScope {
                        val tripUpdatesUrl = config.gtfsRt?.tripUpdatesUrl
                        val vehiclesDeferred = async { fetcher.fetchVehiclePositions(vehiclePositionsUrl) }
                        val tripUpdatesDeferred = tripUpdatesUrl?.let { async { fetcher.fetchTripUpdates(it) } }
                        val vehicles = vehiclesDeferred.await()
                        val tripUpdates = tripUpdatesDeferred?.await() ?: emptyMap()
                        vehicles to tripUpdates
                    }
                }
                if (result.isSuccess) {
                    val (vehicles, tripUpdates) = result.getOrThrow()
                    updateVehicles(vehicles)
                    _tripDelays.value = tripUpdates
                    consecutiveErrors = 0
                } else {
                    consecutiveErrors++
                }
                val backoffMs = minOf(POLL_INTERVAL_MS shl consecutiveErrors.coerceAtMost(3), MAX_POLL_INTERVAL_MS)
                delay(backoffMs)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        consecutiveErrors = 0
    }

    private fun getTripRouteIndex(): Map<String, String> {
        val schedule = scheduleRepository.scheduleResponse.value ?: return emptyMap()
        val scheduleKey = schedule.lastUpdated
        if (scheduleKey != indexBuiltForScheduleKey) {
            val index = mutableMapOf<String, String>()
            for (stop in schedule.stops) {
                for (dep in stop.departures) {
                    if (dep.tripId.isNotEmpty() && dep.routeId.isNotEmpty()) {
                        index[dep.tripId] = dep.routeId
                    }
                }
            }
            cachedTripRouteIndex = index
            indexBuiltForScheduleKey = scheduleKey
        }
        return cachedTripRouteIndex
    }

    private fun updateVehicles(newVehicles: List<VehiclePosition>) {
        val tripRouteIndex = getTripRouteIndex()
        val resolved = newVehicles.map { vehicle ->
            if (vehicle.routeId.isNullOrBlank() && vehicle.tripId != null) {
                val fallbackRouteId = tripRouteIndex[vehicle.tripId]
                if (fallbackRouteId != null) vehicle.copy(routeId = fallbackRouteId) else vehicle
            } else {
                vehicle
            }
        }
        _vehicles.value = resolved
        _vehicleByTripId.value = resolved
            .filter { it.tripId != null }
            .associateBy { it.tripId!! }
        _vehiclesByRouteId.value = resolved
            .filter { it.routeId != null }
            .groupBy { it.routeId!! }
    }
}
