package com.transitkit.app.data.store

import android.content.Context
import com.transitkit.app.BuildConfig
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.PlannerStop
import com.transitkit.app.data.model.ScheduleRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ConnectionsStore @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context,
) {
    enum class LoadState { IDLE, DOWNLOADING, LOADING, READY, UNAVAILABLE }

    private val _state = MutableStateFlow(LoadState.IDLE)
    val state: StateFlow<LoadState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isReady: Boolean get() = _state.value == LoadState.READY
    val loadState: LoadState get() = _state.value

    private val engine = RoutingEngine()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var remoteProvider: RemoteRoutingProvider? = null

    fun load(config: OperatorConfig, routes: List<ScheduleRoute>) {
        if (_state.value == LoadState.READY || _state.value == LoadState.DOWNLOADING || _state.value == LoadState.LOADING) return

        // Remote routing: skip local data entirely
        val apiKey = BuildConfig.ROUTING_API_KEY
        if (config.features.useRemoteEngine && config.routingEndpoint != null && apiKey.isNotEmpty()) {
            remoteProvider = RemoteRoutingProvider(config.routingEndpoint, apiKey, okHttpClient)
            _state.value = LoadState.READY
            return
        }

        scope.launch {
            val cacheFile = File(context.filesDir, "connections_${config.id}.zlib")
            val cdnUrl = buildUrl(config)

            if (cacheFile.exists()) {
                // Load from disk cache immediately
                _state.value = LoadState.LOADING
                runCatching {
                    engine.load(cacheFile.readBytes(), routes)
                    _state.value = LoadState.READY
                }.onFailure {
                    cacheFile.delete()
                }

                // Background refresh from CDN
                if (_state.value == LoadState.READY) {
                    refreshFromCdn(cdnUrl, cacheFile, routes)
                } else {
                    downloadAndLoad(cdnUrl, cacheFile, routes)
                }
            } else {
                downloadAndLoad(cdnUrl, cacheFile, routes)
            }
        }
    }

    private suspend fun downloadAndLoad(url: String, cacheFile: File, routes: List<ScheduleRoute>) {
        _state.value = LoadState.DOWNLOADING
        _errorMessage.value = null
        runCatching {
            val bytes = fetchBytes(url)
            cacheFile.writeBytes(bytes)
            _state.value = LoadState.LOADING
            engine.load(bytes, routes)
            _state.value = LoadState.READY
        }.onFailure { e ->
            _state.value = LoadState.UNAVAILABLE
            _errorMessage.value = e.message ?: "Download failed"
        }
    }

    private fun refreshFromCdn(url: String, cacheFile: File, routes: List<ScheduleRoute>) {
        scope.launch {
            runCatching {
                val bytes = fetchBytes(url)
                cacheFile.writeBytes(bytes)
                engine.load(bytes, routes)
                // State stays READY — silent refresh
            }
        }
    }

    private fun fetchBytes(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            return response.body?.bytes() ?: throw Exception("Empty body")
        }
    }

    private fun buildUrl(config: OperatorConfig): String {
        val base = config.cdnUrl?.trimEnd('/') ?: "https://cdn.transitkit.app"
        return "$base/${config.id}/connections.json.zlib"
    }

    suspend fun query(origin: PlannerStop, destination: PlannerStop, afterMs: Long, timezoneId: String): List<Journey> {
        remoteProvider?.let { return it.query(origin, destination, afterMs) }
        return if (engine.isReady) engine.query(origin.id, destination.id, afterMs, timezoneId) else emptyList()
    }

    suspend fun queryArriveBy(origin: PlannerStop, destination: PlannerStop, beforeMs: Long, timezoneId: String): List<Journey> {
        remoteProvider?.let { return it.queryArriveBy(origin, destination, beforeMs) }
        return if (engine.isReady) engine.queryArriveBy(origin.id, destination.id, beforeMs, timezoneId) else emptyList()
    }
}
