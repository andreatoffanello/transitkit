package com.transitkit.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.FavoritesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val config: OperatorConfig,
    private val favoritesStore: FavoritesStore,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    val operatorConfig: OperatorConfig = config

    val favoriteStops: StateFlow<List<ResolvedStop>> = combine(
        favoritesStore.favoriteStopIds,
        scheduleRepository.stops,
    ) { ids, stops ->
        val stopsById = stops.associateBy { it.id }
        ids.sorted().mapNotNull { stopsById[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun removeFavorite(stopId: String) {
        viewModelScope.launch { favoritesStore.removeFavorite(stopId) }
    }
}
