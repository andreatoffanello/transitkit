package com.transitkit.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.push.PushNotificationManager
import com.transitkit.app.data.repository.ScheduleRepository
import com.transitkit.app.data.store.FavoritesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val config: OperatorConfig,
    private val favoritesStore: FavoritesStore,
    private val scheduleRepository: ScheduleRepository,
    private val pushManager: PushNotificationManager,
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

    private val _notificationsBusy = MutableStateFlow(false)
    val notificationsBusy: StateFlow<Boolean> = _notificationsBusy.asStateFlow()

    /** Called by SettingsScreen once the POST_NOTIFICATIONS runtime permission
     *  is granted (Android 13+; pre-13 the permission isn't required and this
     *  fires immediately). Subscribes to the operator-wide topic and caches
     *  the FCM token. */
    fun onNotificationsPermissionGranted() {
        viewModelScope.launch {
            _notificationsBusy.value = true
            try {
                pushManager.enableAndSubscribeAll()
                _notificationsEnabled.value = pushManager.fcmToken.value != null
            } finally {
                _notificationsBusy.value = false
            }
        }
    }

    /** Called when the user toggles notifications off. Unsubscribes from the
     *  operator-wide topic and from every line topic the user had favorited,
     *  then deletes the FCM token. */
    fun onNotificationsDisabled() {
        viewModelScope.launch {
            _notificationsBusy.value = true
            try {
                val routes = favoritesStore.favoriteRouteIds.first()
                pushManager.disableAndUnsubscribeAll(routes)
            } finally {
                _notificationsEnabled.value = false
                _notificationsBusy.value = false
            }
        }
    }

    /** Legacy fallback used by tests that don't go through the permission flow. */
    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun removeFavorite(stopId: String) {
        viewModelScope.launch { favoritesStore.removeFavorite(stopId) }
    }
}
