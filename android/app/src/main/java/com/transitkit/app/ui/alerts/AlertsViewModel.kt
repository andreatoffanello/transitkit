package com.transitkit.app.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.model.ServiceAlert
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.AlertStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Exposes the currently-active alerts plus resolver maps for routes/stops so
 * AlertDetailScreen can chip-render affected entities without touching stores directly.
 */
@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertStore: AlertStore,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    val alerts: StateFlow<List<ServiceAlert>> = alertStore.activeAlerts

    val routesById: StateFlow<Map<String, ScheduleRoute>> = scheduleRepository.routes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        .let { routesFlow ->
            combine(routesFlow, alertStore.activeAlerts) { routes, _ ->
                routes.associateBy { it.id }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
        }

    val stopsById: StateFlow<Map<String, ResolvedStop>> = scheduleRepository.stops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        .let { stopsFlow ->
            combine(stopsFlow, alertStore.activeAlerts) { stops, _ ->
                stops.associateBy { it.id }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
        }
}
