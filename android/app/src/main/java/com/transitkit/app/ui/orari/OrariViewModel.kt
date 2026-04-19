package com.transitkit.app.ui.orari

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.SearchHistoryStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OrariTab { STOPS, LINES }

data class OrariUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class OrariViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    private val historyStore: SearchHistoryStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _searchQuery = savedStateHandle.getStateFlow("search_query", "")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTab = MutableStateFlow(OrariTab.STOPS)
    val selectedTab: StateFlow<OrariTab> = _selectedTab.asStateFlow()

    private val _selectedTransitType = MutableStateFlow<Int?>(null)
    val selectedTransitType: StateFlow<Int?> = _selectedTransitType.asStateFlow()

    val availableTransitTypes: StateFlow<List<Int>> = combine(
        repository.stops, _selectedTab
    ) { stops, tab ->
        if (tab != OrariTab.STOPS) return@combine emptyList()
        stops.flatMap { it.transitTypes }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(FlowPreview::class)
    val stops: StateFlow<List<ResolvedStop>> = combine(
        repository.stops, _searchQuery.debounce(300), _selectedTab, _selectedTransitType
    ) { stops, query, tab, transitType ->
        if (tab != OrariTab.STOPS) return@combine emptyList()
        val typeFiltered = if (transitType == null) stops
            else stops.filter { it.transitTypes.contains(transitType) }
        if (query.isBlank()) {
            typeFiltered.sortedBy { it.name }
        } else if (query.length < 2) {
            typeFiltered.filter { it.name.contains(query, ignoreCase = true) }
        } else {
            typeFiltered
                .map { stop -> stop to fuzzyScore(stop.name, query) }
                .filter { (_, score) -> score > 0 }
                .sortedByDescending { (_, score) -> score }
                .map { (stop, _) -> stop }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val groupedStops: StateFlow<Map<Int, List<ResolvedStop>>> = combine(
        repository.stops,
        _searchQuery,
        _selectedTransitType,
    ) { allStops, query, transitType ->
        if (query.isNotBlank() || transitType != null) return@combine emptyMap()
        allStops
            .groupBy { stop -> stop.transitTypes.firstOrNull() ?: 3 }
            .toSortedMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val stopNamesByRouteId: StateFlow<Map<String, String>> = repository.scheduleResponse
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

    @OptIn(FlowPreview::class)
    val routes: StateFlow<List<ScheduleRoute>> = combine(
        repository.routes, _searchQuery.debounce(300), _selectedTab
    ) { list, query, tab ->
        if (tab != OrariTab.LINES) return@combine emptyList()
        if (query.isBlank()) list.sortedBy { it.longName.ifBlank { it.name } }
        else if (query.length < 2) list.filter { r ->
            r.name.contains(query, ignoreCase = true) || r.longName.contains(query, ignoreCase = true)
        }
        else list
            .filter { r -> maxOf(fuzzyScore(r.longName.ifBlank { r.name }, query), fuzzyScore(r.name, query)) > 0 }
            .sortedByDescending { r -> maxOf(fuzzyScore(r.longName.ifBlank { r.name }, query), fuzzyScore(r.name, query)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val scheduleLoadError: StateFlow<String?> = repository.loadError
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isLoading: StateFlow<Boolean> = repository.isLoading
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val recentStopIds: StateFlow<List<String>> = historyStore.recentStopIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun recordStopVisit(stopId: String) {
        viewModelScope.launch { historyStore.recordStop(stopId) }
    }

    val recentRouteIds: StateFlow<List<String>> = historyStore.recentRouteIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun recordRouteVisit(routeId: String) {
        viewModelScope.launch { historyStore.recordRoute(routeId) }
    }

    init {
        viewModelScope.launch { repository.load() }
    }

    fun onSearchQueryChanged(query: String) {
        savedStateHandle["search_query"] = query
    }

    fun onTabSelected(tab: OrariTab) {
        _selectedTab.value = tab
        savedStateHandle["search_query"] = ""
        _selectedTransitType.value = null
    }

    fun selectTransitType(type: Int?) {
        _selectedTransitType.value = if (_selectedTransitType.value == type) null else type
    }

    // Simple fuzzy scorer: prefix > contains > subsequence
    private fun fuzzyScore(text: String, query: String): Int {
        val t = text.lowercase()
        val q = query.lowercase()
        if (t.startsWith(q)) return 100
        if (t.contains(q)) return 80
        var qi = 0
        for (char in t) {
            if (qi < q.length && char == q[qi]) qi++
        }
        return if (qi == q.length) 50 else 0
    }
}
