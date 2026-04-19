package com.transitkit.app.data.store

import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.gtfsrt.GtfsRtFetcher
import com.transitkit.app.data.model.ServiceAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val POLL_INTERVAL_MS = 60_000L
private const val MAX_POLL_INTERVAL_MS = 300_000L

/**
 * Global GTFS-RT service alerts store. Polls every 60s and exposes O(1) lookups
 * by stop_id / route_id, already filtered by `activePeriods` against "now".
 *
 * Consumers observe `activeAlerts`, `byStopId`, `byRouteId` via StateFlow. The
 * store tracks `previouslyActiveIds` so callers (toast in Phase E) can detect
 * new alerts across poll cycles.
 */
@Singleton
class AlertStore @Inject constructor(
    private val fetcher: GtfsRtFetcher,
    private val config: OperatorConfig,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _allAlerts = MutableStateFlow<List<ServiceAlert>>(emptyList())

    /** All currently-active alerts (activePeriods filter applied). */
    private val _activeAlerts = MutableStateFlow<List<ServiceAlert>>(emptyList())
    val activeAlerts: StateFlow<List<ServiceAlert>> = _activeAlerts.asStateFlow()

    /** active alerts indexed by stop_id. */
    private val _byStopId = MutableStateFlow<Map<String, List<ServiceAlert>>>(emptyMap())
    val byStopId: StateFlow<Map<String, List<ServiceAlert>>> = _byStopId.asStateFlow()

    /** active alerts indexed by route_id. */
    private val _byRouteId = MutableStateFlow<Map<String, List<ServiceAlert>>>(emptyMap())
    val byRouteId: StateFlow<Map<String, List<ServiceAlert>>> = _byRouteId.asStateFlow()

    /** IDs that were active in the previous poll cycle. Used for new-alert detection. */
    private val _previouslyActiveIds = MutableStateFlow<Set<String>>(emptySet())
    val previouslyActiveIds: StateFlow<Set<String>> = _previouslyActiveIds.asStateFlow()

    private var pollingJob: Job? = null
    private var consecutiveErrors = 0

    fun startPolling() {
        val url = config.gtfsRt?.serviceAlertsUrl ?: return
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch {
            while (isActive) {
                val result = runCatching { fetcher.fetchAlerts(url) }
                if (result.isSuccess) {
                    apply(result.getOrThrow())
                    consecutiveErrors = 0
                } else {
                    consecutiveErrors++
                }
                val backoffMs = minOf(
                    POLL_INTERVAL_MS shl consecutiveErrors.coerceAtMost(3),
                    MAX_POLL_INTERVAL_MS,
                )
                delay(backoffMs)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        consecutiveErrors = 0
    }

    // -------------------------------------------------------------------------
    // Apply

    private fun apply(alerts: List<ServiceAlert>) {
        val epoch = System.currentTimeMillis() / 1000
        val previouslyActive = _activeAlerts.value.map { it.id }.toSet()
        val active = alerts.filter { it.isActive(epoch) }

        _previouslyActiveIds.value = previouslyActive
        _allAlerts.value = alerts
        _activeAlerts.value = active

        val stopIndex = mutableMapOf<String, MutableList<ServiceAlert>>()
        val routeIndex = mutableMapOf<String, MutableList<ServiceAlert>>()
        for (a in active) {
            for (s in a.affectedStopIds) stopIndex.getOrPut(s) { mutableListOf() }.add(a)
            for (r in a.affectedRouteIds) routeIndex.getOrPut(r) { mutableListOf() }.add(a)
        }
        _byStopId.value = stopIndex
        _byRouteId.value = routeIndex
    }
}
