package com.transitkit.app.data.store

import com.transitkit.app.BuildConfig
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.PlannerStop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routing facade — MOTIS only. The remote engine accepts `fromPlace=lat,lng` /
 * `toPlace=lat,lng` natively and returns walking + transit legs. No local CSA,
 * no zlib bundle to download, no nearest-stop snap.
 */
@Singleton
class ConnectionsStore @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    enum class LoadState { IDLE, READY, UNAVAILABLE }

    private val _state = MutableStateFlow(LoadState.IDLE)
    val state: StateFlow<LoadState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isReady: Boolean get() = _state.value == LoadState.READY
    val loadState: LoadState get() = _state.value

    @Volatile private var provider: RemoteRoutingProvider? = null

    fun load(config: OperatorConfig) {
        if (_state.value == LoadState.READY) return
        val endpoint = config.routingEndpoint
        val apiKey = BuildConfig.ROUTING_API_KEY
        if (endpoint.isNullOrBlank() || apiKey.isBlank()) {
            _state.value = LoadState.UNAVAILABLE
            _errorMessage.value = "Routing not configured"
            return
        }
        provider = RemoteRoutingProvider(endpoint, apiKey, okHttpClient)
        _state.value = LoadState.READY
    }

    suspend fun query(origin: PlannerStop, destination: PlannerStop, afterMs: Long): List<Journey> =
        provider?.query(origin, destination, afterMs) ?: emptyList()

    suspend fun queryArriveBy(origin: PlannerStop, destination: PlannerStop, beforeMs: Long): List<Journey> =
        provider?.queryArriveBy(origin, destination, beforeMs) ?: emptyList()
}
