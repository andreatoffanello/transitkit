package com.transitkit.app.data.gtfsrt

import com.transitkit.app.data.model.AlertCause
import com.transitkit.app.data.model.AlertEffect
import com.transitkit.app.data.model.AlertSeverity
import com.transitkit.app.data.model.AlertTimeRange
import com.transitkit.app.data.model.ServiceAlert
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.BufferedSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches and parses a GTFS-RT VehiclePositions feed (binary protobuf).
 *
 * Parsing is done manually via Okio's BufferedSource without generated Wire classes,
 * reading only the fields needed for map display:
 *   FeedMessage.entity (field 2) → FeedEntity
 *   FeedEntity.vehicle (field 4) → VehiclePosition message
 *   VehiclePosition.trip (field 1) → TripDescriptor
 *   VehiclePosition.position (field 2) → Position
 *   VehiclePosition.vehicle (field 3) → VehicleDescriptor
 *   TripDescriptor.trip_id (field 1), route_id (field 5)
 *   Position.latitude (field 1), longitude (field 2)
 *   VehicleDescriptor.id (field 1)
 */
@Singleton
class GtfsRtFetcher @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun fetchVehiclePositions(url: String): List<VehiclePosition> {
        val request = Request.Builder().url(url).build()
        val bytes = okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            response.body?.bytes() ?: return emptyList()
        }
        return parseFeedMessage(bytes)
    }

    /**
     * Fetches GTFS-RT TripUpdates feed and returns a map of tripId → delay in seconds.
     * Parses only StopTimeUpdate.departure.delay (field 2 → field 5 → field 2).
     *   FeedEntity.trip_update (field 3) → TripUpdate
     *   TripUpdate.trip (field 1) → TripDescriptor (trip_id = field 1)
     *   TripUpdate.stop_time_update (field 2) → StopTimeUpdate
     *   StopTimeUpdate.departure (field 2) → StopTimeEvent
     *   StopTimeEvent.delay (field 2) → int32
     */
    suspend fun fetchTripUpdates(url: String): Map<String, Int> {
        val request = Request.Builder().url(url).build()
        val bytes = okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyMap()
            response.body?.bytes() ?: return emptyMap()
        }
        return parseTripUpdateFeed(bytes)
    }

    /** Fetches the GTFS-RT service alerts feed. Returns empty on any network/HTTP failure. */
    suspend fun fetchAlerts(url: String): List<ServiceAlert> {
        val request = Request.Builder().url(url).build()
        val bytes = okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            response.body?.bytes() ?: return emptyList()
        }
        return parseAlertFeed(bytes)
    }

    private fun parseTripUpdateFeed(bytes: ByteArray): Map<String, Int> {
        val source = Buffer().write(bytes)
        val result = mutableMapOf<String, Int>()
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == FEED_ENTITY && wireType == WIRE_LEN -> {
                    val entityBytes = readLengthDelimited(source)
                    parseTripUpdateEntity(entityBytes)?.let { (tripId, delay) ->
                        result[tripId] = delay
                    }
                }
                else -> skip(source, wireType)
            }
        }
        return result
    }

    private fun parseTripUpdateEntity(bytes: ByteArray): Pair<String, Int>? {
        val source = Buffer().write(bytes)
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == ENTITY_TRIP_UPDATE && wireType == WIRE_LEN -> {
                    val tuBytes = readLengthDelimited(source)
                    return parseTripUpdate(tuBytes)
                }
                else -> skip(source, wireType)
            }
        }
        return null
    }

    private fun parseTripUpdate(bytes: ByteArray): Pair<String, Int>? {
        val source = Buffer().write(bytes)
        var tripId: String? = null
        var delay = 0
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == TU_TRIP && wireType == WIRE_LEN -> {
                    val tripDesc = parseTrip(readLengthDelimited(source))
                    tripId = tripDesc.first
                }
                fieldNumber == TU_STOP_TIME_UPDATE && wireType == WIRE_LEN -> {
                    val stu = readLengthDelimited(source)
                    delay = parseStopTimeUpdateDelay(stu)
                }
                else -> skip(source, wireType)
            }
        }
        // Drop physically implausible delays — see DELAY_*_BOUND_SEC for the
        // asymmetric thresholds and rationale.
        if (delay !in DELAY_LOWER_BOUND_SEC..DELAY_UPPER_BOUND_SEC) return null
        return tripId?.let { it to delay }
    }

    private fun parseStopTimeUpdateDelay(bytes: ByteArray): Int {
        val source = Buffer().write(bytes)
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == STU_DEPARTURE && wireType == WIRE_LEN -> {
                    val eventBytes = readLengthDelimited(source)
                    val eventSource = Buffer().write(eventBytes)
                    while (!eventSource.exhausted()) {
                        val (f, wt) = readTag(eventSource) ?: break
                        when {
                            f == STE_DELAY && wt == WIRE_VARINT -> return readVarint32(eventSource)
                            else -> skip(eventSource, wt)
                        }
                    }
                }
                else -> skip(source, wireType)
            }
        }
        return 0
    }

    // -------------------------------------------------------------------------
    // Protobuf wire-format constants
    // -------------------------------------------------------------------------

    private companion object {
        // GTFS-RT delay plausibility window (seconds). Asymmetric because:
        // - early > 5 min is almost always noise: trip mismatch (vehicle
        //   attached to wrong trip), vehicle waiting at terminal pre-departure,
        //   optimistic linear extrapolations on future stops, stale ghost trips;
        // - late up to 30 min is realistic for peripheral / overnight routes
        //   in trouble. Beyond, null (schedule-only) beats misinformation.
        // Measured on Movete's ATAC feed (633 trips, 12.8k stop_time_updates):
        // arrivalDelay median -136s, 33% of samples outside ±5 min.
        const val DELAY_LOWER_BOUND_SEC = -300
        const val DELAY_UPPER_BOUND_SEC = 1800

        // Wire types
        const val WIRE_VARINT = 0
        const val WIRE_64BIT  = 1
        const val WIRE_LEN    = 2
        const val WIRE_32BIT  = 5

        // FeedMessage
        const val FEED_ENTITY = 2

        // FeedEntity
        const val ENTITY_VEHICLE = 4
        const val ENTITY_TRIP_UPDATE = 3

        // TripUpdate
        const val TU_TRIP             = 1
        const val TU_STOP_TIME_UPDATE = 2

        // StopTimeUpdate
        const val STU_DEPARTURE = 2

        // StopTimeEvent
        const val STE_DELAY = 2

        // VehiclePosition — standard GTFS-RT field numbers
        // (https://gtfs.org/realtime/reference/#message-vehicleposition)
        const val VP_TRIP            = 1  // TripDescriptor
        const val VP_POSITION        = 2  // Position
        const val VP_CURRENT_STOP_SEQ = 3 // uint32 varint
        const val VP_CURRENT_STATUS  = 4  // varint enum: 0=INCOMING_AT, 1=STOPPED_AT, 2=IN_TRANSIT_TO
        const val VP_TIMESTAMP       = 5  // uint64 varint
        const val VP_CURRENT_STOP_ID = 7  // string
        const val VP_VEHICLE         = 8  // VehicleDescriptor
        const val VP_OCCUPANCY       = 9  // varint enum

        // TripDescriptor
        const val TRIP_TRIP_ID  = 1
        const val TRIP_ROUTE_ID = 5

        // Position
        const val POS_LATITUDE  = 1
        const val POS_LONGITUDE = 2
        const val POS_BEARING   = 3

        // VehicleDescriptor
        const val VD_ID = 1
        const val VD_LABEL = 2        // string
        const val VD_WHEELCHAIR = 4   // varint enum

        // FeedEntity.alert
        const val ENTITY_ALERT = 5

        // Alert message fields
        const val ALERT_ACTIVE_PERIOD     = 1
        const val ALERT_INFORMED_ENTITY   = 5
        const val ALERT_CAUSE             = 6
        const val ALERT_EFFECT            = 7
        const val ALERT_URL               = 8
        const val ALERT_HEADER_TEXT       = 10
        const val ALERT_DESCRIPTION_TEXT  = 11
        const val ALERT_SEVERITY_LEVEL    = 14

        // TimeRange
        const val TR_START = 1
        const val TR_END   = 2

        // EntitySelector
        const val ES_ROUTE_ID = 2
        const val ES_TRIP     = 4
        const val ES_STOP_ID  = 5

        // TranslatedString
        const val TS_TRANSLATION = 1
        // Translation
        const val TRN_TEXT     = 1
        const val TRN_LANGUAGE = 2
    }

    // -------------------------------------------------------------------------
    // Top-level parser
    // -------------------------------------------------------------------------

    private fun parseFeedMessage(bytes: ByteArray): List<VehiclePosition> {
        val source = Buffer().write(bytes)
        val positions = mutableListOf<VehiclePosition>()
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == FEED_ENTITY && wireType == WIRE_LEN -> {
                    val entityBytes = readLengthDelimited(source)
                    parseFeedEntity(entityBytes)?.let { positions.add(it) }
                }
                else -> skip(source, wireType)
            }
        }
        return positions
    }

    private fun parseFeedEntity(bytes: ByteArray): VehiclePosition? {
        val source = Buffer().write(bytes)
        var entityId = ""
        var vpBytes: ByteArray? = null
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == 1 && wireType == WIRE_LEN -> entityId = readUtf8(source)
                fieldNumber == ENTITY_VEHICLE && wireType == WIRE_LEN -> vpBytes = readLengthDelimited(source)
                else -> skip(source, wireType)
            }
        }
        return vpBytes?.let { parseVehiclePosition(it, entityId) }
    }

    private fun parseVehiclePosition(bytes: ByteArray, entityId: String = ""): VehiclePosition? {
        val source = Buffer().write(bytes)
        var tripId: String? = null
        var routeId: String? = null
        var lat: Float? = null
        var lon: Float? = null
        var bearing: Float? = null
        var vehicleId: String? = null
        var label: String? = null
        var currentStopId: String? = null
        var currentStatus = VehicleStatus.IN_TRANSIT_TO
        var timestamp = 0L
        var occupancyStatus: OccupancyStatus? = null
        var wheelchair: WheelchairStatus? = null

        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == VP_TRIP && wireType == WIRE_LEN -> {
                    val tripDesc = parseTrip(readLengthDelimited(source))
                    tripId = tripDesc.first
                    routeId = tripDesc.second
                }
                fieldNumber == VP_POSITION && wireType == WIRE_LEN -> {
                    val pos = parsePosition(readLengthDelimited(source))
                    lat = pos.first
                    lon = pos.second
                    bearing = pos.third
                }
                fieldNumber == VP_VEHICLE && wireType == WIRE_LEN -> {
                    val vd = parseVehicleDescriptor(readLengthDelimited(source))
                    vehicleId = vd.id
                    if (vd.label.isNotBlank()) label = vd.label
                    if (vd.wheelchair != null) wheelchair = vd.wheelchair
                }
                fieldNumber == VP_CURRENT_STOP_SEQ && wireType == WIRE_VARINT -> {
                    readVarint32(source) // currently unused
                }
                fieldNumber == VP_CURRENT_STOP_ID && wireType == WIRE_LEN -> {
                    currentStopId = readUtf8(source)
                }
                fieldNumber == VP_CURRENT_STATUS && wireType == WIRE_VARINT -> {
                    currentStatus = when (readVarint32(source)) {
                        0 -> VehicleStatus.INCOMING_AT
                        1 -> VehicleStatus.STOPPED_AT
                        else -> VehicleStatus.IN_TRANSIT_TO
                    }
                }
                fieldNumber == VP_TIMESTAMP && wireType == WIRE_VARINT -> {
                    timestamp = readVarint64(source)
                }
                fieldNumber == VP_OCCUPANCY && wireType == WIRE_VARINT -> {
                    occupancyStatus = when (readVarint32(source)) {
                        0 -> OccupancyStatus.EMPTY
                        1 -> OccupancyStatus.MANY_SEATS_AVAILABLE
                        2 -> OccupancyStatus.FEW_SEATS_AVAILABLE
                        3 -> OccupancyStatus.STANDING_ROOM_ONLY
                        4 -> OccupancyStatus.CRUSHED_STANDING_ROOM_ONLY
                        5 -> OccupancyStatus.FULL
                        6 -> OccupancyStatus.NOT_ACCEPTING_PASSENGERS
                        7 -> OccupancyStatus.NO_DATA_AVAILABLE
                        8 -> OccupancyStatus.NOT_BOARDABLE
                        else -> null
                    }
                }
                else -> skip(source, wireType)
            }
        }

        val finalLat = lat?.toDouble() ?: return null
        val finalLon = lon?.toDouble() ?: return null
        return VehiclePosition(
            // Prefer FeedEntity.id (matches iOS) so deeplinks and cross-platform
            // ids stay consistent. VehicleDescriptor.id is exposed via `label`.
            vehicleId = entityId.takeIf { it.isNotBlank() }
                ?: vehicleId?.takeIf { it.isNotBlank() }
                ?: tripId?.takeIf { it.isNotBlank() }
                ?: "",
            lat = finalLat,
            lon = finalLon,
            tripId = tripId,
            routeId = routeId,
            bearing = bearing ?: 0f,
            currentStopId = currentStopId,
            currentStatus = currentStatus,
            label = label ?: "",
            timestamp = timestamp,
            occupancyStatus = occupancyStatus,
            wheelchair = wheelchair,
        )
    }

    private fun parseTrip(bytes: ByteArray): Pair<String?, String?> {
        val source = Buffer().write(bytes)
        var tripId: String? = null
        var routeId: String? = null
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == TRIP_TRIP_ID  && wireType == WIRE_LEN -> tripId  = readUtf8(source)
                fieldNumber == TRIP_ROUTE_ID && wireType == WIRE_LEN -> routeId = readUtf8(source)
                else -> skip(source, wireType)
            }
        }
        return tripId to routeId
    }

    private fun parsePosition(bytes: ByteArray): Triple<Float?, Float?, Float?> {
        val source = Buffer().write(bytes)
        var lat: Float? = null
        var lon: Float? = null
        var bearing: Float? = null
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == POS_LATITUDE  && wireType == WIRE_32BIT -> lat     = readFloat(source)
                fieldNumber == POS_LONGITUDE && wireType == WIRE_32BIT -> lon     = readFloat(source)
                fieldNumber == POS_BEARING   && wireType == WIRE_32BIT -> bearing = readFloat(source)
                else -> skip(source, wireType)
            }
        }
        return Triple(lat, lon, bearing)
    }

    private data class VehicleDescriptorFields(
        val id: String?,
        val label: String,
        val wheelchair: WheelchairStatus?,
    )

    /**
     * Decodes GTFS-RT VehicleDescriptor:
     *   field 1 = id            (stable identifier, e.g. "B03")
     *   field 2 = label          (human-readable; AppalCART/ETA Transit put block ID here)
     *   field 4 = wheelchair_accessible
     * The returned [VehicleDescriptorFields.label] is the preferred display
     * string: it uses `id` when present (more meaningful across feeds),
     * falling back to `label` otherwise.
     */
    private fun parseVehicleDescriptor(bytes: ByteArray): VehicleDescriptorFields {
        val source = Buffer().write(bytes)
        var id: String? = null
        var rawLabel = ""
        var wheelchair: WheelchairStatus? = null
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == VD_ID && wireType == WIRE_LEN -> id = readUtf8(source)
                fieldNumber == VD_LABEL && wireType == WIRE_LEN -> rawLabel = readUtf8(source)
                fieldNumber == VD_WHEELCHAIR && wireType == WIRE_VARINT -> {
                    wheelchair = when (readVarint32(source)) {
                        0 -> WheelchairStatus.NO_VALUE
                        1 -> WheelchairStatus.UNKNOWN
                        2 -> WheelchairStatus.ACCESSIBLE
                        3 -> WheelchairStatus.INACCESSIBLE
                        else -> null
                    }
                }
                else -> skip(source, wireType)
            }
        }
        val displayLabel = id?.takeIf { it.isNotBlank() } ?: rawLabel
        return VehicleDescriptorFields(id, displayLabel, wheelchair)
    }

    // -------------------------------------------------------------------------
    // Alert parsing
    // -------------------------------------------------------------------------

    private fun parseAlertFeed(bytes: ByteArray): List<ServiceAlert> {
        val source = Buffer().write(bytes)
        val out = mutableListOf<ServiceAlert>()
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == FEED_ENTITY && wireType == WIRE_LEN -> {
                    parseAlertEntity(readLengthDelimited(source))?.let(out::add)
                }
                else -> skip(source, wireType)
            }
        }
        return out
    }

    private fun parseAlertEntity(bytes: ByteArray): ServiceAlert? {
        val source = Buffer().write(bytes)
        var entityId: String? = null
        var payload: AlertPayload? = null
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == 1 && wireType == WIRE_LEN -> entityId = readUtf8(source)
                fieldNumber == ENTITY_ALERT && wireType == WIRE_LEN -> {
                    payload = parseAlertPayload(readLengthDelimited(source))
                }
                else -> skip(source, wireType)
            }
        }
        val id = entityId ?: return null
        val p = payload ?: return null
        if (id.isEmpty()) return null
        return ServiceAlert(
            id = id,
            activePeriods = p.activePeriods,
            severity = p.severity,
            effect = p.effect,
            cause = p.cause,
            headerText = p.headerText,
            descriptionText = p.descriptionText,
            affectedStopIds = p.affectedStopIds,
            affectedRouteIds = p.affectedRouteIds,
            url = p.url,
        )
    }

    /** Intermediate mutable aggregator used while decoding Alert fields. */
    private class AlertPayload {
        val activePeriods = mutableListOf<AlertTimeRange>()
        var severity: AlertSeverity = AlertSeverity.UNKNOWN
        var effect: AlertEffect = AlertEffect.UNKNOWN_EFFECT
        var cause: AlertCause = AlertCause.UNKNOWN_CAUSE
        var headerText: Map<String, String> = emptyMap()
        var descriptionText: Map<String, String> = emptyMap()
        val affectedStopIds = mutableSetOf<String>()
        val affectedRouteIds = mutableSetOf<String>()
        var url: String? = null
    }

    private fun parseAlertPayload(bytes: ByteArray): AlertPayload {
        val source = Buffer().write(bytes)
        val payload = AlertPayload()
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == ALERT_ACTIVE_PERIOD && wireType == WIRE_LEN -> {
                    payload.activePeriods.add(parseTimeRange(readLengthDelimited(source)))
                }
                fieldNumber == ALERT_INFORMED_ENTITY && wireType == WIRE_LEN -> {
                    parseEntitySelector(readLengthDelimited(source)).let { (stops, routes) ->
                        payload.affectedStopIds.addAll(stops)
                        payload.affectedRouteIds.addAll(routes)
                    }
                }
                fieldNumber == ALERT_CAUSE && wireType == WIRE_VARINT -> {
                    payload.cause = AlertCause.fromRaw(readVarint32(source))
                }
                fieldNumber == ALERT_EFFECT && wireType == WIRE_VARINT -> {
                    payload.effect = AlertEffect.fromRaw(readVarint32(source))
                }
                fieldNumber == ALERT_URL && wireType == WIRE_LEN -> {
                    val translations = parseTranslatedString(readLengthDelimited(source))
                    payload.url = translations.values.firstOrNull()
                }
                fieldNumber == ALERT_HEADER_TEXT && wireType == WIRE_LEN -> {
                    payload.headerText = parseTranslatedString(readLengthDelimited(source))
                }
                fieldNumber == ALERT_DESCRIPTION_TEXT && wireType == WIRE_LEN -> {
                    payload.descriptionText = parseTranslatedString(readLengthDelimited(source))
                }
                fieldNumber == ALERT_SEVERITY_LEVEL && wireType == WIRE_VARINT -> {
                    payload.severity = AlertSeverity.fromRaw(readVarint32(source))
                }
                else -> skip(source, wireType)
            }
        }
        return payload
    }

    private fun parseTimeRange(bytes: ByteArray): AlertTimeRange {
        val source = Buffer().write(bytes)
        var start: Long? = null
        var end: Long? = null
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == TR_START && wireType == WIRE_VARINT -> start = readVarint64(source)
                fieldNumber == TR_END && wireType == WIRE_VARINT -> end = readVarint64(source)
                else -> skip(source, wireType)
            }
        }
        return AlertTimeRange(start, end)
    }

    /**
     * Returns (stopIds, routeIds) harvested from a single EntitySelector.
     * Nested TripDescriptor.route_id (field 5 inside trip) is also captured.
     */
    private fun parseEntitySelector(bytes: ByteArray): Pair<Set<String>, Set<String>> {
        val source = Buffer().write(bytes)
        val stops = mutableSetOf<String>()
        val routes = mutableSetOf<String>()
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == ES_ROUTE_ID && wireType == WIRE_LEN -> routes.add(readUtf8(source))
                fieldNumber == ES_TRIP && wireType == WIRE_LEN -> {
                    val (_, tripRouteId) = parseTrip(readLengthDelimited(source))
                    if (!tripRouteId.isNullOrEmpty()) routes.add(tripRouteId)
                }
                fieldNumber == ES_STOP_ID && wireType == WIRE_LEN -> stops.add(readUtf8(source))
                else -> skip(source, wireType)
            }
        }
        return stops to routes
    }

    /**
     * Decodes TranslatedString → BCP-47 primary tag (lowercase) → text map.
     * Entries without a language tag land under the empty-string key for fallback.
     */
    private fun parseTranslatedString(bytes: ByteArray): Map<String, String> {
        val source = Buffer().write(bytes)
        val out = mutableMapOf<String, String>()
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == TS_TRANSLATION && wireType == WIRE_LEN -> {
                    val (text, lang) = parseTranslation(readLengthDelimited(source))
                    if (text.isNotEmpty()) {
                        val key = lang.lowercase().substringBefore('-')
                        out[key] = text
                    }
                }
                else -> skip(source, wireType)
            }
        }
        return out
    }

    private fun parseTranslation(bytes: ByteArray): Pair<String, String> {
        val source = Buffer().write(bytes)
        var text = ""
        var language = ""
        while (!source.exhausted()) {
            val (fieldNumber, wireType) = readTag(source) ?: break
            when {
                fieldNumber == TRN_TEXT && wireType == WIRE_LEN -> text = readUtf8(source)
                fieldNumber == TRN_LANGUAGE && wireType == WIRE_LEN -> language = readUtf8(source)
                else -> skip(source, wireType)
            }
        }
        return sanitizeFeedText(text) to language
    }

    /**
     * Some operators encode alert text as Windows-1252 but "smart" punctuation bytes
     * leak through as raw C1 control codepoints (U+0080–U+009F): non-printing, so any
     * font renders them as a tofu box □. Remap the CP1252 punctuation table to the
     * intended Unicode glyph (en dash, curly quotes, apostrophe…), and drop the 5
     * codepoints CP1252 leaves undefined. Accented Latin (à è ì é ù ò) and the degree
     * sign decode as proper Unicode already and are left untouched.
     */
    private fun sanitizeFeedText(s: String): String {
        if (s.none { it.code in 0x80..0x9F }) return s
        val sb = StringBuilder(s.length)
        for (ch in s) {
            val code = ch.code
            if (code in 0x80..0x9F) {
                cp1252C1Map[code]?.let { sb.append(it) }   // undefined → dropped
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private val cp1252C1Map: Map<Int, Char> = mapOf(
        0x80 to '€', 0x82 to '‚', 0x83 to 'ƒ', 0x84 to '„',
        0x85 to '…', 0x86 to '†', 0x87 to '‡', 0x88 to 'ˆ',
        0x89 to '‰', 0x8A to 'Š', 0x8B to '‹', 0x8C to 'Œ',
        0x8E to 'Ž', 0x91 to '‘', 0x92 to '’', 0x93 to '“',
        0x94 to '”', 0x95 to '•', 0x96 to '–', 0x97 to '—',
        0x98 to '˜', 0x99 to '™', 0x9A to 'š', 0x9B to '›',
        0x9C to 'œ', 0x9E to 'ž', 0x9F to 'Ÿ',
    )

    // -------------------------------------------------------------------------
    // Protobuf primitives
    // -------------------------------------------------------------------------

    private fun readTag(source: BufferedSource): Pair<Int, Int>? {
        if (source.exhausted()) return null
        val varint = readVarint32(source)
        val fieldNumber = varint ushr 3
        val wireType = varint and 0x7
        return fieldNumber to wireType
    }

    private fun readVarint32(source: BufferedSource): Int {
        var result = 0
        var shift = 0
        while (true) {
            val b = source.readByte().toInt() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
            if (shift >= 64) break
        }
        return result
    }

    private fun readVarint64(source: BufferedSource): Long {
        var result = 0L
        var shift = 0
        while (true) {
            val b = source.readByte().toLong() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0L) break
            shift += 7
            if (shift >= 64) break
        }
        return result
    }

    private fun readLengthDelimited(source: BufferedSource): ByteArray {
        val length = readVarint32(source)
        return source.readByteArray(length.toLong())
    }

    private fun readUtf8(source: BufferedSource): String {
        val length = readVarint32(source)
        return source.readUtf8(length.toLong())
    }

    private fun readFloat(source: BufferedSource): Float {
        val bits = source.readIntLe()
        return java.lang.Float.intBitsToFloat(bits)
    }

    private fun skip(source: BufferedSource, wireType: Int) {
        when (wireType) {
            WIRE_VARINT -> readVarint64(source)
            WIRE_64BIT  -> source.skip(8)
            WIRE_LEN    -> {
                val length = readVarint32(source)
                source.skip(length.toLong())
            }
            WIRE_32BIT  -> source.skip(4)
            else        -> source.skip(1) // unknown — best effort
        }
    }
}

enum class VehicleStatus { IN_TRANSIT_TO, STOPPED_AT, INCOMING_AT }

enum class OccupancyStatus {
    EMPTY,
    MANY_SEATS_AVAILABLE,
    FEW_SEATS_AVAILABLE,
    STANDING_ROOM_ONLY,
    CRUSHED_STANDING_ROOM_ONLY,
    FULL,
    NOT_ACCEPTING_PASSENGERS,
    NO_DATA_AVAILABLE,
    NOT_BOARDABLE,
}

enum class WheelchairStatus { NO_VALUE, UNKNOWN, ACCESSIBLE, INACCESSIBLE }

data class VehiclePosition(
    val vehicleId: String,
    val lat: Double,
    val lon: Double,
    val tripId: String?,
    val routeId: String?,
    val bearing: Float = 0f,
    val currentStopId: String? = null,
    val currentStatus: VehicleStatus = VehicleStatus.IN_TRANSIT_TO,
    val label: String = "",
    val timestamp: Long = 0L,
    val occupancyStatus: OccupancyStatus? = null,
    val wheelchair: WheelchairStatus? = null,
)
