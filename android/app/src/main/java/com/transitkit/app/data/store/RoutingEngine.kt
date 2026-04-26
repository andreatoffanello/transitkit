package com.transitkit.app.data.store

import com.transitkit.app.data.model.IntermediateStop
import com.transitkit.app.data.model.Journey
import com.transitkit.app.data.model.JourneyLeg
import com.transitkit.app.data.model.transfers
import com.transitkit.app.data.model.PlannerStop
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.model.TransitLeg
import com.transitkit.app.data.model.WalkingLeg
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.TimeZone
import java.util.zip.InflaterInputStream

class RoutingEngine {

    private var stops: List<PlannerStop> = emptyList()
    private var connections: List<Conn> = emptyList()
    private var footpaths: Map<Int, List<FP>> = emptyMap()
    private var lineNames: List<String> = emptyList()
    private var lineColors: List<String> = emptyList()
    private var lineTextColors: List<String> = emptyList()
    private var tripIds: List<String> = emptyList()
    private var stopIdxMap: Map<String, Int> = emptyMap()

    var isReady: Boolean = false
        private set

    private data class Conn(
        val depStop: Int, val arrStop: Int,
        val depSec: Int, val arrSec: Int,
        val tripIdx: Int, val lineIdx: Int,
    )

    private data class FP(val to: Int, val walkSec: Int)

    // ─── Via markers ─────────────────────────────────────────────────────────

    private sealed class Via
    private data class TransitVia(
        val fromStop: Int, val tripIdx: Int, val lineIdx: Int,
        val depSec: Int, val arrSec: Int,
    ) : Via()
    private data class WalkVia(val fromStop: Int, val walkSec: Int) : Via()

    // ─── Load ─────────────────────────────────────────────────────────────────

    fun load(connectionsData: ByteArray, @Suppress("UNUSED_PARAMETER") allRoutes: List<ScheduleRoute>) {
        val json = inflate(connectionsData)
        val root = JSONObject(json)

        // Stops
        val stopsArr = root.getJSONArray("stops")
        val stopsOut = ArrayList<PlannerStop>(stopsArr.length())
        for (i in 0 until stopsArr.length()) {
            val s = stopsArr.getJSONObject(i)
            stopsOut += PlannerStop(
                id = s.getString("id"),
                name = s.getString("name"),
                lat = s.getDouble("lat"),
                lng = s.getDouble("lng"),
            )
        }
        stops = stopsOut
        stopIdxMap = stops.mapIndexed { idx, s -> s.id to idx }.toMap()

        // Lookup tables
        lineNames = root.getJSONArray("line_names").toStringList()
        lineColors = root.getJSONArray("line_colors").toStringList()
        lineTextColors = root.getJSONArray("line_text_colors").toStringList()
        tripIds = root.getJSONArray("trip_ids").toStringList()

        // Connections (sorted by dep_sec ascending in the feed)
        val connArr = root.getJSONArray("connections")
        val connsOut = ArrayList<Conn>(connArr.length())
        for (i in 0 until connArr.length()) {
            val c = connArr.getJSONArray(i)
            connsOut += Conn(
                depStop = c.getInt(0),
                arrStop = c.getInt(1),
                depSec = c.getInt(2),
                arrSec = c.getInt(3),
                tripIdx = c.getInt(4),
                lineIdx = c.getInt(5),
            )
        }
        connections = connsOut

        // Footpaths (bidirectional: each pair appears twice)
        val fpArr = root.optJSONArray("footpaths")
        val fpMap = HashMap<Int, MutableList<FP>>()
        if (fpArr != null) {
            for (i in 0 until fpArr.length()) {
                val fp = fpArr.getJSONArray(i)
                val a = fp.getInt(0); val b = fp.getInt(1); val w = fp.getInt(2)
                fpMap.getOrPut(a) { mutableListOf() } += FP(b, w)
                fpMap.getOrPut(b) { mutableListOf() } += FP(a, w)
            }
        }
        footpaths = fpMap

        isReady = true
    }

    // ─── Public query API ─────────────────────────────────────────────────────

    fun query(originId: String, destId: String, afterEpochMs: Long, timezoneId: String): List<Journey> {
        val tz = TimeZone.getTimeZone(timezoneId)
        val startOfDay = startOfDayEpochMs(afterEpochMs, tz)
        val afterSec = ((afterEpochMs - startOfDay) / 1000L).toInt()
        return findJourneys(originId, destId, afterSec, startOfDay, arriveBy = false)
    }

    fun queryArriveBy(originId: String, destId: String, beforeEpochMs: Long, timezoneId: String): List<Journey> {
        val tz = TimeZone.getTimeZone(timezoneId)
        val startOfDay = startOfDayEpochMs(beforeEpochMs, tz)
        val beforeSec = ((beforeEpochMs - startOfDay) / 1000L).toInt()
        return findJourneys(originId, destId, beforeSec, startOfDay, arriveBy = true)
    }

    // ─── Core CSA logic ───────────────────────────────────────────────────────

    private fun findJourneys(
        originId: String, destId: String,
        querySec: Int, startOfDayEpochMs: Long,
        arriveBy: Boolean,
    ): List<Journey> {
        val originIdx = stopIdxMap[originId] ?: return emptyList()
        val destIdx = stopIdxMap[destId] ?: return emptyList()
        if (originIdx == destIdx) return emptyList()

        val journeys = mutableListOf<Journey>()
        var currentAfterSec = querySec
        var minTransfers = Int.MAX_VALUE

        repeat(5) {
            val result = if (arriveBy)
                runCsaBackward(originIdx, destIdx, currentAfterSec, startOfDayEpochMs)
            else
                runCsaForward(originIdx, destIdx, currentAfterSec, startOfDayEpochMs)

            if (result != null) {
                val t = result.transfers
                if (minTransfers == Int.MAX_VALUE) minTransfers = t
                if (t <= minTransfers + 1) {
                    // Deduplicate by departure time
                    if (journeys.none { it.departureTime == result.departureTime }) {
                        journeys += result
                    }
                }
                // Advance cursor by 1 minute past this departure
                val depSec = ((result.departureTime - startOfDayEpochMs) / 1000L).toInt()
                currentAfterSec = depSec + 60
            } else {
                return@repeat
            }
        }

        return journeys.sortedBy { it.departureTime }
    }

    private fun runCsaForward(
        originIdx: Int, destIdx: Int,
        afterSec: Int, startOfDayEpochMs: Long,
    ): Journey? {
        val INF = Int.MAX_VALUE / 2
        val TRANSFER_PENALTY = 360
        val window = afterSec + 5400  // 90 min

        val earliest = HashMap<Int, Int>()
        val effective = HashMap<Int, Int>()
        val tripsCount = HashMap<Int, Int>()
        val via = HashMap<Int, Via>()

        earliest[originIdx] = afterSec
        effective[originIdx] = afterSec
        tripsCount[originIdx] = 0

        // Apply origin footpaths
        footpaths[originIdx]?.forEach { fp ->
            val walkArr = afterSec + fp.walkSec
            earliest[fp.to] = walkArr
            effective[fp.to] = walkArr
            tripsCount[fp.to] = 0
            via[fp.to] = WalkVia(originIdx, fp.walkSec)
        }

        for (conn in connections) {
            if (conn.depSec > window) break
            val depEarliest = earliest[conn.depStop] ?: continue
            if (depEarliest > conn.depSec) continue

            val prevVia = via[conn.depStop]
            val isExtension = prevVia is TransitVia && prevVia.tripIdx == conn.tripIdx
            val currentTrips = tripsCount[conn.depStop] ?: 0
            val newTrips = currentTrips + if (isExtension) 0 else 1
            val newXfer = (newTrips - 1).coerceAtLeast(0)
            val effArr = if (conn.arrSec == INF) INF else conn.arrSec + newXfer * TRANSFER_PENALTY

            val prevEff = effective[conn.arrStop] ?: INF
            if (effArr < prevEff) {
                earliest[conn.arrStop] = conn.arrSec
                effective[conn.arrStop] = effArr
                tripsCount[conn.arrStop] = newTrips
                via[conn.arrStop] = TransitVia(
                    fromStop = conn.depStop,
                    tripIdx = conn.tripIdx,
                    lineIdx = conn.lineIdx,
                    depSec = conn.depSec,
                    arrSec = conn.arrSec,
                )
            }

            // Propagate footpaths from arrival stop
            footpaths[conn.arrStop]?.forEach { fp ->
                val walkArr = conn.arrSec + fp.walkSec
                val effWalk = walkArr + newXfer * TRANSFER_PENALTY
                val prevEffFp = effective[fp.to] ?: INF
                if (effWalk < prevEffFp) {
                    earliest[fp.to] = walkArr
                    effective[fp.to] = effWalk
                    tripsCount[fp.to] = newTrips
                    via[fp.to] = WalkVia(conn.arrStop, fp.walkSec)
                }
            }
        }

        if (!via.containsKey(destIdx) && earliest[destIdx] == null) return null

        return reconstructJourney(destIdx, originIdx, via, startOfDayEpochMs)
    }

    private fun runCsaBackward(
        originIdx: Int, destIdx: Int,
        beforeSec: Int, startOfDayEpochMs: Long,
    ): Journey? {
        // Backward CSA: scan reversed, find journeys arriving ≤ beforeSec
        val INF = Int.MAX_VALUE / 2
        val TRANSFER_PENALTY = 360

        val latest = HashMap<Int, Int>()    // latest departure to reach dest
        val effective = HashMap<Int, Int>() // adjusted for transfers
        val tripsCount = HashMap<Int, Int>()
        val via = HashMap<Int, Via>()

        latest[destIdx] = beforeSec
        effective[destIdx] = beforeSec
        tripsCount[destIdx] = 0

        footpaths[destIdx]?.forEach { fp ->
            val walkDep = beforeSec - fp.walkSec
            latest[fp.to] = walkDep
            effective[fp.to] = walkDep
            tripsCount[fp.to] = 0
            via[fp.to] = WalkVia(destIdx, fp.walkSec)
        }

        for (conn in connections.asReversed()) {
            if (conn.arrSec < beforeSec - 5400) break
            val arrLatest = latest[conn.arrStop] ?: continue
            if (conn.arrSec > arrLatest) continue

            val prevVia = via[conn.arrStop]
            val isExtension = prevVia is TransitVia && prevVia.tripIdx == conn.tripIdx
            val currentTrips = tripsCount[conn.arrStop] ?: 0
            val newTrips = currentTrips + if (isExtension) 0 else 1
            val newXfer = (newTrips - 1).coerceAtLeast(0)
            val effDep = conn.depSec - newXfer * TRANSFER_PENALTY

            val prevEff = effective[conn.depStop] ?: -INF
            if (effDep > prevEff) {
                latest[conn.depStop] = conn.depSec
                effective[conn.depStop] = effDep
                tripsCount[conn.depStop] = newTrips
                via[conn.depStop] = TransitVia(
                    fromStop = conn.arrStop,
                    tripIdx = conn.tripIdx,
                    lineIdx = conn.lineIdx,
                    depSec = conn.depSec,
                    arrSec = conn.arrSec,
                )
            }

            footpaths[conn.depStop]?.forEach { fp ->
                val walkDep = conn.depSec - fp.walkSec
                val effWalk = walkDep - newXfer * TRANSFER_PENALTY
                val prevEffFp = effective[fp.to] ?: -INF
                if (effWalk > prevEffFp) {
                    latest[fp.to] = walkDep
                    effective[fp.to] = effWalk
                    tripsCount[fp.to] = newTrips
                    via[fp.to] = WalkVia(conn.depStop, fp.walkSec)
                }
            }
        }

        if (!via.containsKey(originIdx) && latest[originIdx] == null) return null

        return reconstructJourney(destIdx, originIdx, via, startOfDayEpochMs)
    }

    // ─── Journey reconstruction ───────────────────────────────────────────────

    private fun reconstructJourney(
        destIdx: Int, originIdx: Int,
        via: Map<Int, Via>,
        startOfDayEpochMs: Long,
    ): Journey? {
        // Walk via pointers from dest back to origin
        val segments = mutableListOf<Pair<Int, Via>>() // (arrivalStop, via)
        var current = destIdx
        var safetyCount = 0
        while (current != originIdx && safetyCount++ < 200) {
            val v = via[current] ?: return null
            segments += current to v
            current = when (v) {
                is TransitVia -> v.fromStop
                is WalkVia -> v.fromStop
            }
        }
        if (current != originIdx) return null
        segments.reverse()

        // Merge consecutive connections of same trip into TransitLeg
        val legs = mutableListOf<JourneyLeg>()
        var i = 0
        while (i < segments.size) {
            val (arrStop, v) = segments[i]
            when (v) {
                is WalkVia -> {
                    val fromStop = stops.getOrNull(v.fromStop) ?: break
                    legs += WalkingLeg(fromStop, v.walkSec)
                    i++
                }
                is TransitVia -> {
                    // Gather all consecutive segments with same tripIdx
                    val boardStopIdx = v.fromStop
                    val boardSec = v.depSec
                    val lineIdx = v.lineIdx
                    val tripIdx = v.tripIdx
                    var alightStopIdx = arrStop
                    var alightSec = v.arrSec
                    val intermediates = mutableListOf<IntermediateStop>()

                    // Walk forward to merge same-trip continuations
                    var j = i + 1
                    while (j < segments.size) {
                        val (nextArr, nextVia) = segments[j]
                        if (nextVia is TransitVia && nextVia.tripIdx == tripIdx) {
                            // The current alightStop becomes intermediate
                            val interStop = stops.getOrNull(alightStopIdx)
                            if (interStop != null) {
                                intermediates += IntermediateStop(
                                    id = interStop.id,
                                    name = interStop.name,
                                    time = secToHHMM(alightSec),
                                )
                            }
                            alightStopIdx = nextArr
                            alightSec = nextVia.arrSec
                            j++
                        } else break
                    }

                    val boardStop = stops.getOrNull(boardStopIdx) ?: break
                    val alightStop = stops.getOrNull(alightStopIdx) ?: break
                    legs += TransitLeg(
                        boardStop = boardStop,
                        alightStop = alightStop,
                        departureTime = startOfDayEpochMs + boardSec * 1000L,
                        arrivalTime = startOfDayEpochMs + alightSec * 1000L,
                        routeName = lineNames.getOrElse(lineIdx) { "?" },
                        routeColor = lineColors.getOrElse(lineIdx) { "3B82F6" },
                        routeTextColor = lineTextColors.getOrElse(lineIdx) { "FFFFFF" },
                        tripId = tripIds.getOrElse(tripIdx) { "" },
                        intermediateStops = intermediates,
                    )
                    i = j
                }
            }
        }

        if (legs.isEmpty()) return null

        val depTime = when (val first = legs.first()) {
            is TransitLeg -> first.departureTime
            is WalkingLeg -> {
                val nextTransit = legs.filterIsInstance<TransitLeg>().firstOrNull()
                nextTransit?.departureTime?.minus(first.walkSeconds * 1000L)
                    ?: startOfDayEpochMs
            }
        }
        val arrTime = when (val last = legs.last()) {
            is TransitLeg -> last.arrivalTime
            is WalkingLeg -> {
                val prevTransit = legs.filterIsInstance<TransitLeg>().lastOrNull()
                prevTransit?.arrivalTime?.plus(last.walkSeconds * 1000L)
                    ?: startOfDayEpochMs
            }
        }

        return Journey(
            id = "$depTime-$destIdx",
            departureTime = depTime,
            arrivalTime = arrTime,
            legs = legs,
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun startOfDayEpochMs(epochMs: Long, tz: TimeZone): Long {
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = epochMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun secToHHMM(sec: Int): String {
        val h = (sec / 3600) % 24
        val m = (sec % 3600) / 60
        return "%02d:%02d".format(h, m)
    }

    private fun inflate(data: ByteArray): String {
        val bos = ByteArrayOutputStream()
        InflaterInputStream(ByteArrayInputStream(data)).use { it.copyTo(bos) }
        return bos.toByteArray().toString(Charsets.UTF_8)
    }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }
}
