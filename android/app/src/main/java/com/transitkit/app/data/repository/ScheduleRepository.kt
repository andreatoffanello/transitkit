package com.transitkit.app.data.repository

import android.content.Context
import com.squareup.moshi.Moshi
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.data.model.ResolvedDeparture
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleResponse
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.model.ScheduleStop
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val config: OperatorConfig,
    @ApplicationContext private val context: Context,
) {
    val operatorTimezone: String get() = config.timezone

    private val _scheduleResponse = MutableStateFlow<ScheduleResponse?>(null)
    val scheduleResponse: StateFlow<ScheduleResponse?> = _scheduleResponse.asStateFlow()

    private val _lastUpdated = MutableStateFlow<String?>(null)
    val lastUpdated: StateFlow<String?> = _lastUpdated.asStateFlow()

    private val _stops = MutableStateFlow<List<ResolvedStop>>(emptyList())
    val stops: StateFlow<List<ResolvedStop>> = _stops.asStateFlow()

    private val _routes = MutableStateFlow<List<ScheduleRoute>>(emptyList())
    val routes: StateFlow<List<ScheduleRoute>> = _routes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    // Persistent O(1) indices — built once in parseAndApply, reused across all lookups
    @Volatile private var routeById: Map<String, ScheduleRoute> = emptyMap()
    @Volatile private var stopById: Map<String, ScheduleStop> = emptyMap()
    // Secondary index: original GTFS stop_id → station id (for GTFS-RT lookups).
    @Volatile private var stationIdByGtfsStopId: Map<String, String> = emptyMap()

    // Skip background CDN check when data was just fetched from network (parity iOS)
    @Volatile private var lastFetchedFromNetworkAt: Long = 0L
    private val CDN_FRESH_THRESHOLD_MS = 5 * 60 * 1000L // 5 min

    // Singleton-scoped scope for background work that outlives any ViewModel
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cacheFile: File get() = File(context.filesDir, "schedule_${config.id}_cache.json")
    private val etagFile: File get() = File(context.filesDir, "schedule_${config.id}_etag.txt")

    private fun saveToCache(json: String) {
        try { cacheFile.writeText(json) } catch (_: Exception) {}
    }

    private fun loadFromCache(): String? {
        return try { if (cacheFile.exists()) cacheFile.readText() else null } catch (_: Exception) { null }
    }

    private fun loadEtag(): String? = try {
        if (etagFile.exists()) etagFile.readText().trim() else null
    } catch (_: Exception) { null }

    private fun saveEtag(etag: String) = try { etagFile.writeText(etag) } catch (_: Exception) {}

    private suspend fun parseAndApply(json: String): ScheduleResponse? {
        // CPU-intensive parsing + index building on Default, StateFlow updates are thread-safe
        data class ParseResult(val schedule: ScheduleResponse, val rById: Map<String, ScheduleRoute>, val sById: Map<String, ScheduleStop>)
        val result = withContext(Dispatchers.Default) {
            val adapter = moshi.adapter(ScheduleResponse::class.java)
            val schedule = adapter.fromJson(json) ?: return@withContext null
            ParseResult(
                schedule = schedule,
                rById = schedule.routes.associateBy { it.id },
                sById = schedule.stops.associateBy { it.id },
            )
        } ?: return null
        routeById = result.rById
        stopById = result.sById
        stationIdByGtfsStopId = buildMap {
            for (stop in result.sById.values) {
                stop.gtfsStopIds?.forEach { gtfsId -> put(gtfsId, stop.id) }
            }
        }
        _scheduleResponse.value = result.schedule
        _lastUpdated.value = result.schedule.lastUpdated
        _routes.value = result.schedule.routes
        _stops.value = resolveStops(result.schedule, result.rById)
        return result.schedule
    }

    private suspend fun fetchFromCdn(): Pair<String, ScheduleResponse>? {
        val cdnBase = config.cdnUrl ?: return null
        val url = "$cdnBase/${config.id}/schedules.json"
        return withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url(url)
                loadEtag()?.let { requestBuilder.addHeader("If-None-Match", it) }
                val response = okHttpClient.newCall(requestBuilder.build()).execute()

                if (response.code == 304) {
                    val cached = loadFromCache() ?: return@withContext null
                    val adapter = moshi.adapter(ScheduleResponse::class.java)
                    val schedule = adapter.fromJson(cached) ?: return@withContext null
                    return@withContext cached to schedule
                }

                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null

                response.header("ETag")?.let { saveEtag(it) }

                val adapter = moshi.adapter(ScheduleResponse::class.java)
                val schedule = adapter.fromJson(body) ?: return@withContext null
                body to schedule
            } catch (_: Exception) { null }
        }
    }

    suspend fun load() {
        if (_isLoading.value) return
        if (_scheduleResponse.value != null) return // memory short-circuit (iOS parity)
        val cached = withContext(Dispatchers.IO) { loadFromCache() }
        if (cached != null) {
            // Show UI immediately from disk
            val cachedSchedule = parseAndApply(cached)
            // Skip background CDN check if data was just fetched from network (parity iOS)
            val isDataFresh = System.currentTimeMillis() - lastFetchedFromNetworkAt < CDN_FRESH_THRESHOLD_MS
            if (!isDataFresh) {
                repositoryScope.launch {
                    val (freshJson, freshSchedule) = fetchFromCdn() ?: return@launch
                    if (freshSchedule.lastUpdated != cachedSchedule?.lastUpdated) {
                        saveToCache(freshJson)
                        parseAndApply(freshJson)
                        lastFetchedFromNetworkAt = System.currentTimeMillis()
                    }
                }
            }
        } else {
            // Cold start — must wait for network
            _isLoading.value = true
            try {
                val result = fetchFromCdn()
                if (result == null) {
                    _loadError.value = "Impossibile caricare gli orari. Controlla la connessione."
                    return
                }
                val (freshJson, _) = result
                withContext(Dispatchers.IO) { saveToCache(freshJson) }
                parseAndApply(freshJson)
                lastFetchedFromNetworkAt = System.currentTimeMillis()
            } catch (_: Exception) {
                _loadError.value = "Impossibile caricare gli orari. Controlla la connessione."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun resolveStops(schedule: ScheduleResponse, routeById: Map<String, ScheduleRoute> = this.routeById): List<ResolvedStop> {
        return schedule.stops.map { stop ->
            val routeIds = stop.departures.map { it.routeId }.distinct()
            val routeNames = routeIds.mapNotNull { routeById[it]?.name }.distinct()
            val routeColors = routeIds.map { routeById[it]?.color ?: "" }
            val transitTypes = routeIds.mapNotNull { routeById[it]?.transitType }.distinct()
            ResolvedStop(
                id = stop.id,
                name = stop.name,
                lat = stop.lat,
                lon = stop.lon,
                routeNames = routeNames,
                routeIds = routeIds,
                routeColors = routeColors,
                transitTypes = transitTypes,
                gtfsStopIds = stop.gtfsStopIds ?: emptyList(),
            )
        }
    }

    /** Resolves a stop by either our synthetic station id or the original
     *  GTFS stop_id (the latter coming from GTFS-RT feeds). */
    fun resolveStop(anyStopId: String): ResolvedStop? {
        val stations = _stops.value
        stations.firstOrNull { it.id == anyStopId }?.let { return it }
        val stationId = stationIdByGtfsStopId[anyStopId] ?: return null
        return stations.firstOrNull { it.id == stationId }
    }

    /** Returns the scheduled departure epoch millis for `(tripId, anyStopId)`
     *  on today's operator calendar, or null when no match is found.
     *  `anyStopId` may be our synthetic station id or a GTFS stop_id. */
    fun scheduledDepartureEpochMs(tripId: String, anyStopId: String): Long? {
        val station = resolveStop(anyStopId) ?: return null
        val apiStop = stopById[station.id] ?: return null
        val dep = apiStop.departures.firstOrNull { it.tripId == tripId } ?: return null
        val parts = dep.departureTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val s = parts.getOrNull(2)?.toIntOrNull() ?: 0
        val tz = TimeZone.getTimeZone(config.timezone)
        val cal = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.SECOND, h * 3600 + m * 60 + s)
        }
        return cal.timeInMillis
    }

    /** All departures for a stop across all service days — unfiltered by time or day of week. For FullScheduleSheet. */
    fun allDepartures(stopId: String): List<ResolvedDeparture> {
        val stop = stopById[stopId] ?: return emptyList()
        val routeById = this.routeById
        return stop.departures.map { dep ->
            val parts = dep.departureTime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val mins = h * 60 + m
            val route = routeById[dep.routeId]
            ResolvedDeparture(
                routeId = dep.routeId,
                routeName = dep.routeName,
                tripId = dep.tripId,
                headsign = dep.headsign.takeIf { it.isNotBlank() }
                    ?: route?.directions?.firstOrNull { dir -> dir.stopIds.contains(stopId) }?.headsign
                    ?: route?.directions?.firstOrNull()?.headsign
                    ?: dep.routeName,
                departureTime = "%02d:%02d".format(h, m),
                minutesFromMidnight = mins,
                routeColor = route?.color ?: "",
                routeTextColor = route?.textColor ?: "",
                transitType = route?.transitType ?: 3,
                serviceDays = dep.serviceDays,
            )
        }.sortedBy { it.minutesFromMidnight }
    }

    fun upcomingDepartures(stopId: String, limit: Int = 10): List<ResolvedDeparture> {
        val stop = stopById[stopId] ?: return emptyList()
        val routeById = this.routeById

        val operatorTz = TimeZone.getTimeZone(config.timezone)
        val cal = Calendar.getInstance(operatorTz)
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val todayDow = java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH)
            .also { it.timeZone = operatorTz }
            .format(cal.time)
            .lowercase()

        return stop.departures
            .filter { dep ->
                dep.serviceDays.any { it.equals(todayDow, ignoreCase = true) }
            }
            .filter { dep ->
                val parts = dep.departureTime.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                (h * 60 + m) >= nowMinutes
            }
            .map { dep ->
                val parts = dep.departureTime.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val mins = h * 60 + m
                val route = routeById[dep.routeId]
                ResolvedDeparture(
                    routeId = dep.routeId,
                    routeName = dep.routeName,
                    tripId = dep.tripId,
                    headsign = dep.headsign.takeIf { it.isNotBlank() }
                        ?: route?.directions
                            ?.firstOrNull { dir -> dir.stopIds.contains(stopId) }
                            ?.headsign
                        ?: route?.directions?.firstOrNull()?.headsign
                        ?: dep.routeName,
                    departureTime = "%02d:%02d".format(h, m),
                    minutesFromMidnight = mins,
                    routeColor = route?.color ?: "",
                    routeTextColor = route?.textColor ?: "",
                    transitType = route?.transitType ?: 3,
                    serviceDays = dep.serviceDays,
                )
            }
            .sortedBy { it.minutesFromMidnight }
            // Dedup: GTFS often exposes multiple trip_ids that collapse to the
            // same scheduled run (overlapping service calendars). Keep the
            // first occurrence of each (time, route, headsign) — preserves
            // legitimate branching at the same minute, kills exact duplicates.
            .distinctBy { Triple(it.departureTime, it.routeId, it.headsign) }
            .take(limit)
    }
}
