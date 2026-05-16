package com.transitkit.app.data.store

import com.transitkit.app.data.model.IntermediateStop
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.JourneyLeg
import com.transitkit.app.data.model.PlannerStop
import com.transitkit.app.data.model.TransitLeg
import com.transitkit.app.data.model.WalkingLeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID

class RemoteRoutingProvider(
    private val baseUrl: String,
    private val apiKey: String,
    private val client: OkHttpClient,
) {
    suspend fun query(origin: PlannerStop, destination: PlannerStop, afterMs: Long): List<Journey> =
        fetch(origin, destination, afterMs, arriveBy = false)

    suspend fun queryArriveBy(origin: PlannerStop, destination: PlannerStop, beforeMs: Long): List<Journey> =
        fetch(origin, destination, beforeMs, arriveBy = true)

    // MARK: - Fetch

    private suspend fun fetch(
        origin: PlannerStop,
        destination: PlannerStop,
        timeMs: Long,
        arriveBy: Boolean,
    ): List<Journey> = withContext(Dispatchers.IO) {
        val timeIso = Instant.ofEpochMilli(timeMs).toString()
        val url = buildString {
            append(baseUrl.trimEnd('/'))
            append("/v1/route?fromPlace=")
            append(origin.lat); append(","); append(origin.lng)
            append("&toPlace=")
            append(destination.lat); append(","); append(destination.lng)
            append("&time=")
            append(URLEncoder.encode(timeIso, "UTF-8"))
            append("&arriveBy=")
            append(arriveBy)
        }
        val request = Request.Builder().url(url).header("X-API-Key", apiKey).build()
        val body = runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                response.body?.string() ?: return@withContext emptyList()
            }
        }.getOrNull() ?: return@withContext emptyList()
        parseResponse(body, origin, destination)
    }

    // MARK: - Parsing

    private fun parseResponse(json: String, origin: PlannerStop, destination: PlannerStop): List<Journey> =
        runCatching {
            val root = JSONObject(json)
            val out = mutableListOf<Journey>()
            // Direct (walk-only) journeys come back when no transit is needed.
            root.optJSONArray("direct")?.let { arr ->
                (0 until arr.length()).forEach { i ->
                    mapItinerary(arr.getJSONObject(i), origin, destination)?.let(out::add)
                }
            }
            root.optJSONArray("itineraries")?.let { arr ->
                (0 until arr.length()).forEach { i ->
                    mapItinerary(arr.getJSONObject(i), origin, destination)?.let(out::add)
                }
            }
            out
        }.getOrDefault(emptyList())

    private fun mapItinerary(it: JSONObject, origin: PlannerStop, destination: PlannerStop): Journey? {
        val dep = parseIso(it.optString("startTime")) ?: return null
        val arr = parseIso(it.optString("endTime")) ?: return null
        val legsArr = it.optJSONArray("legs") ?: return null
        val count = legsArr.length()
        val legs = mutableListOf<JourneyLeg>()

        for (idx in 0 until count) {
            val leg = legsArr.getJSONObject(idx)
            val mode = leg.optString("mode")
            val from = leg.optJSONObject("from") ?: continue
            val to = leg.optJSONObject("to") ?: continue

            if (mode == "WALK") {
                val fromStop = if (idx == 0) origin else plannerStop(from)
                val toStop = if (idx == count - 1) destination else plannerStop(to)
                legs.add(WalkingLeg(
                    fromStop = fromStop,
                    toStop = toStop,
                    walkSeconds = leg.optInt("duration"),
                    walkMeters = leg.optInt("distance"),
                    polyline = leg.optJSONObject("legGeometry")?.optString("points")
                        ?.takeIf { it.isNotEmpty() },
                ))
            } else {
                val boardMs = parseIso(from.optString("departure")) ?: continue
                val alightMs = parseIso(to.optString("arrival")) ?: continue
                val color = leg.optString("routeColor").takeIf { it.isNotEmpty() } ?: "808080"
                val textColor = leg.optString("routeTextColor").takeIf { it.isNotEmpty() } ?: contrastHex(color)
                val intermediates = leg.optJSONArray("intermediateStops")?.let { ia ->
                    (0 until ia.length()).map { j ->
                        val s = ia.getJSONObject(j)
                        IntermediateStop(
                            id = s.optString("stopId"),
                            name = s.optString("name"),
                            time = timeComponent(s.optString("arrival")),
                            lat = s.optDouble("lat", 0.0),
                            lon = s.optDouble("lon", 0.0),
                        )
                    }
                } ?: emptyList()
                legs.add(TransitLeg(
                    boardStop = plannerStop(from),
                    alightStop = plannerStop(to),
                    departureTime = boardMs,
                    arrivalTime = alightMs,
                    routeName = leg.optString("routeShortName").takeIf { it.isNotEmpty() }
                        ?: leg.optString("agencyName"),
                    routeColor = color,
                    routeTextColor = textColor,
                    tripId = leg.optString("tripId"),
                    intermediateStops = intermediates,
                    headsign = leg.optString("headsign"),
                    polyline = leg.optJSONObject("legGeometry")?.optString("points")
                        ?.takeIf { it.isNotEmpty() },
                ))
            }
        }
        if (legs.isEmpty()) return null
        return Journey(id = UUID.randomUUID().toString(), departureTime = dep, arrivalTime = arr, legs = legs)
    }

    private fun plannerStop(obj: JSONObject): PlannerStop = PlannerStop(
        id = obj.optString("stopId").takeIf { it.isNotEmpty() }
            ?: "${obj.optDouble("lat")},${obj.optDouble("lon")}",
        name = obj.optString("name"),
        lat = obj.optDouble("lat"),
        lng = obj.optDouble("lon"),
    )

    private fun parseIso(iso: String): Long? = runCatching {
        Instant.parse(iso).toEpochMilli()
    }.getOrNull()

    // "2026-05-05T14:06:00Z" → "14:06"
    private fun timeComponent(iso: String): String {
        val t = iso.indexOf('T')
        return if (t >= 0 && t + 6 <= iso.length) iso.substring(t + 1, t + 6) else ""
    }

    private fun contrastHex(hex: String): String {
        val clean = hex.trimStart('#')
        if (clean.length != 6) return "FFFFFF"
        val r = clean.substring(0, 2).toIntOrNull(16) ?: 0
        val g = clean.substring(2, 4).toIntOrNull(16) ?: 0
        val b = clean.substring(4, 6).toIntOrNull(16) ?: 0
        val lin = { c: Int -> val v = c / 255.0; if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4) }
        val lum = 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
        return if (lum > 0.179) "000000" else "FFFFFF"
    }
}
