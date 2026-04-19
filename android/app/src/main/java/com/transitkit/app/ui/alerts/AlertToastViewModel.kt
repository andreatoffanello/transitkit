package com.transitkit.app.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.data.model.ServiceAlert
import com.transitkit.app.data.store.AlertStore
import com.transitkit.app.data.store.FavoritesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Watches active alerts + favorite stops; emits toast events for brand-new alerts
 * (id never seen in this app session) whose affected set intersects the favorites.
 *
 * One alert at a time — the next one fires on the next poll cycle. The in-memory
 * `shownIds` Set dedups across polls so the same alert doesn't re-announce while
 * still active in the feed.
 */
@HiltViewModel
class AlertToastViewModel @Inject constructor(
    private val alertStore: AlertStore,
    private val favoritesStore: FavoritesStore,
) : ViewModel() {

    private val shownIds = mutableSetOf<String>()
    private val _pendingAlert = MutableStateFlow<ServiceAlert?>(null)
    val pendingAlert: StateFlow<ServiceAlert?> = _pendingAlert.asStateFlow()

    init {
        combine(
            alertStore.activeAlerts,
            alertStore.previouslyActiveIds,
            favoritesStore.favoriteStopIds,
        ) { active, previouslyActive, favIds ->
            Triple(active, previouslyActive, favIds.toSet())
        }
            .onEach { (active, previouslyActive, favorites) ->
                if (favorites.isEmpty()) return@onEach
                val candidate = active.firstOrNull { alert ->
                    alert.id !in previouslyActive &&
                        alert.id !in shownIds &&
                        alert.affectedStopIds.any { it in favorites }
                }
                if (candidate != null) {
                    shownIds += candidate.id
                    _pendingAlert.value = candidate
                }
            }
            .launchIn(viewModelScope)
    }

    fun dismiss() {
        _pendingAlert.value = null
    }
}
