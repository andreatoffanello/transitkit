package com.transitkit.app.ui.home

import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.model.Departure
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.model.toDeparture
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.model.ServiceAlert
import com.transitkit.app.data.store.AlertStore
import com.transitkit.app.data.store.FavoritesStore
import com.transitkit.app.data.store.VehicleStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val config: OperatorConfig? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    val operatorConfig: OperatorConfig,
    private val scheduleRepository: ScheduleRepository,
    private val favoritesStore: FavoritesStore,
    private val vehicleStore: VehicleStore,
    private val alertStore: AlertStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(config = operatorConfig, isLoading = false))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val favoriteStopIds: StateFlow<List<String>> = favoritesStore.favoriteStopIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val resolvedFavoriteStops: StateFlow<List<ResolvedStop>> = combine(
        favoritesStore.favoriteStopIds,
        scheduleRepository.stops,
    ) { ids, stops ->
        val stopsById = stops.associateBy { it.id }
        ids.mapNotNull { stopsById[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _favoriteDepartures = MutableStateFlow<Map<String, List<Departure>>>(emptyMap())
    val favoriteDepartures: StateFlow<Map<String, List<Departure>>> = _favoriteDepartures.asStateFlow()

    private val _nearbyDepartures = MutableStateFlow<Map<String, List<Departure>>>(emptyMap())
    val nearbyDepartures: StateFlow<Map<String, List<Departure>>> = _nearbyDepartures.asStateFlow()

    private val _shouldShowLocationPrimer = MutableStateFlow(false)
    val shouldShowLocationPrimer: StateFlow<Boolean> = _shouldShowLocationPrimer.asStateFlow()

    val scheduleLoadError: StateFlow<String?> = scheduleRepository.loadError
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val scheduleIsLoading: StateFlow<Boolean> = scheduleRepository.isLoading
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val suggestedRoutes: StateFlow<List<ScheduleRoute>> = scheduleRepository.routes
        .map { it.take(3) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)

    /** TripId delle corse con veicolo live — usato per LiveBadge nelle card Home. */
    val liveTripIds: StateFlow<Set<String>> = vehicleStore.vehicleByTripId
        .map { it.keys }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Currently-active service alerts, surfaced in the Today card on Home. */
    val activeAlerts: StateFlow<List<ServiceAlert>> = alertStore.activeAlerts

    /** Up to 3 closest stops with distance in meters (not capped by radius). */
    val nearbyStops: StateFlow<List<Pair<ResolvedStop, Double>>> = combine(
        _userLocation,
        scheduleRepository.stops,
    ) { location, stops ->
        if (location == null) return@combine emptyList()
        val (userLat, userLon) = location
        stops.map { stop ->
            val dLat = stop.lat - userLat
            val dLon = stop.lon - userLon
            val distM = kotlin.math.sqrt(dLat * dLat + dLon * dLon) * 111_320.0
            stop to distM
        }
            .filter { it.second <= 400.0 }
            .sortedBy { it.second }
            .take(3)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateLocation(lat: Double, lon: Double) {
        _userLocation.value = lat to lon
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            vehicleStore.stopPolling()
            alertStore.stopPolling()
        }
        override fun onStart(owner: LifecycleOwner) {
            vehicleStore.startPolling()
            alertStore.startPolling()
        }
    }

    init {
        vehicleStore.startPolling()
        alertStore.startPolling()
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        viewModelScope.launch {
            scheduleRepository.load()
            favoritesStore.favoriteStopIds.collectLatest { stopIds ->
                loadDepartures(stopIds)
            }
        }
        viewModelScope.launch {
            nearbyStops.collectLatest { nearby ->
                val result = mutableMapOf<String, List<Departure>>()
                for ((stop, _) in nearby) {
                    result[stop.id] = scheduleRepository
                        .upcomingDepartures(stop.id, limit = 3)
                        .map { it.toDeparture() }
                }
                _nearbyDepartures.value = result
            }
        }
    }

    fun checkLocationPrimer(prefs: SharedPreferences, currentPermissionGranted: Boolean) {
        val hasSeen = prefs.getBoolean("has_seen_location_primer", false)
        _shouldShowLocationPrimer.value = !hasSeen && !currentPermissionGranted
    }

    fun markLocationPrimerSeen(prefs: SharedPreferences) {
        prefs.edit().putBoolean("has_seen_location_primer", true).apply()
        _shouldShowLocationPrimer.value = false
    }

    override fun onCleared() {
        super.onCleared()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        vehicleStore.stopPolling()
        alertStore.stopPolling()
    }

    private fun loadDepartures(stopIds: List<String>) {
        val result = mutableMapOf<String, List<Departure>>()
        for (stopId in stopIds) {
            result[stopId] = scheduleRepository.upcomingDepartures(stopId, limit = 3)
                .map { it.toDeparture() }
        }
        _favoriteDepartures.value = result
    }

    fun refresh() {
        viewModelScope.launch {
            scheduleRepository.load()
            val stopIds = favoritesStore.favoriteStopIds
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .value
            loadDepartures(stopIds)
        }
    }
}
