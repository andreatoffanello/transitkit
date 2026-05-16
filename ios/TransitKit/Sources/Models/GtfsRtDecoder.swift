import Foundation

// MARK: - Vehicle status enum

enum VehicleStatus: UInt64 {
    case incomingAt  = 0   // INCOMING_AT
    case stoppedAt   = 1   // STOPPED_AT
    case inTransitTo = 2   // IN_TRANSIT_TO
}

/// GTFS-RT OccupancyStatus enum (VehiclePosition.occupancy_status).
enum OccupancyStatus: UInt64 {
    case empty = 0
    case manySeatsAvailable = 1
    case fewSeatsAvailable = 2
    case standingRoomOnly = 3
    case crushedStandingRoomOnly = 4
    case full = 5
    case notAcceptingPassengers = 6
    case noDataAvailable = 7
    case notBoardable = 8
}

/// GTFS-RT WheelchairAccessible enum (VehicleDescriptor.wheelchair_accessible).
enum WheelchairStatus: UInt64 {
    case noValue = 0
    case unknown = 1
    case accessible = 2
    case inaccessible = 3
}

// MARK: - GTFS-RT Vehicle Position model

struct GtfsRtVehicle: Identifiable, Equatable {
    let id: String          // FeedEntity.id
    let tripId: String
    let routeId: String
    let label: String
    let latitude: Float
    let longitude: Float
    let bearing: Float      // degrees clockwise from north
    let timestamp: UInt64
    let currentStopId: String       // stop_id field in VehiclePosition
    let currentStatus: VehicleStatus // INCOMING_AT / STOPPED_AT / IN_TRANSIT_TO
    let occupancyStatus: OccupancyStatus?   // may be absent from feed
    let wheelchairAccessible: WheelchairStatus? // from VehicleDescriptor
}

// MARK: - GTFS-RT Trip Delay model

struct GtfsRtTripDelay {
    let tripId: String
    let delay: Int32   // seconds; positive = late, negative = early
    /// Map of stop_id → predicted arrival epoch seconds (if feed provides per-stop ETAs).
    let arrivalByStopId: [String: UInt64]
}

// MARK: - GTFS-RT Alert model

/// GTFS-RT SeverityLevel enum. Default UNKNOWN when feed omits it.
enum AlertSeverity: UInt64 {
    case unknown = 1
    case info = 2
    case warning = 3
    case severe = 4
}

/// GTFS-RT Effect enum. Not exhaustively modelled — we only branch on a handful.
enum AlertEffect: UInt64 {
    case noService = 1
    case reducedService = 2
    case significantDelays = 3
    case detour = 4
    case additionalService = 5
    case modifiedService = 6
    case otherEffect = 7
    case unknownEffect = 8
    case stopMoved = 9
    case noEffect = 10
    case accessibilityIssue = 11
}

/// A single time window during which the alert applies.
/// Either endpoint may be missing (±infinity); epoch seconds.
struct AlertTimeRange: Equatable {
    let start: UInt64?
    let end: UInt64?

    func contains(_ epoch: UInt64) -> Bool {
        if let s = start, epoch < s { return false }
        if let e = end, epoch >= e { return false }
        return true
    }
}

/// Decoded GTFS-RT service alert. `headerText` / `descriptionText` are
/// multilingual maps (language code → text); consumers resolve via `LocalizedText.resolved()`.
struct GtfsRtAlert: Identifiable, Equatable {
    let id: String                            // FeedEntity.id
    let activePeriods: [AlertTimeRange]       // empty = always active
    let severity: AlertSeverity
    let effect: AlertEffect
    let cause: AlertCause                     // GTFS-RT Cause; UNKNOWN_CAUSE when feed omits it
    let headerText: [String: String]
    let descriptionText: [String: String]
    let affectedStopIds: Set<String>
    let affectedRouteIds: Set<String>
    let url: String?

    /// True if any active period covers the given epoch, or if no period is set.
    func isActive(at epoch: UInt64) -> Bool {
        if activePeriods.isEmpty { return true }
        return activePeriods.contains { $0.contains(epoch) }
    }

    /// Earliest active-period start (epoch seconds), used to sort recent alerts first.
    var firstActiveStart: UInt64? {
        activePeriods.compactMap(\.start).min()
    }

    func isRelevant(forStop stopId: String) -> Bool {
        affectedStopIds.contains(stopId)
    }

    func isRelevant(forRoutes routeIds: Set<String>) -> Bool {
        !affectedRouteIds.isDisjoint(with: routeIds)
    }
}

/// GTFS-RT Cause enum. Mirrors the protobuf values; `unknownCause` is the
/// default when the feed omits the field.
enum AlertCause: UInt64 {
    case unknownCause = 1
    case otherCause = 2
    case technicalProblem = 3
    case strike = 4
    case demonstration = 5
    case accident = 6
    case holiday = 7
    case weather = 8
    case maintenance = 9
    case construction = 10
    case policeActivity = 11
    case medicalEmergency = 12
}

// MARK: - Minimal protobuf binary decoders

/// Decodes a GTFS-RT FeedMessage (.pb) and returns all VehiclePosition entities.
func decodeGtfsRtVehicles(from data: Data) -> [GtfsRtVehicle] {
    var reader = ProtoReader(data: data)
    var vehicles: [GtfsRtVehicle] = []
    while let tag = reader.readTag() {
        if tag.field == 2 && tag.wire == 2 {
            if let v = decodeFeedEntity(reader.readLengthDelimited()) {
                vehicles.append(v)
            }
        } else {
            reader.skipField(wireType: tag.wire)
        }
    }
    return vehicles
}

/// Decodes a GTFS-RT FeedMessage (.pb) and returns all active Alert entities.
/// Caller is responsible for filtering by activePeriods / current time.
func decodeGtfsRtAlerts(from data: Data) -> [GtfsRtAlert] {
    var reader = ProtoReader(data: data)
    var alerts: [GtfsRtAlert] = []
    while let tag = reader.readTag() {
        if tag.field == 2 && tag.wire == 2 {
            if let a = decodeAlertEntity(reader.readLengthDelimited()) {
                alerts.append(a)
            }
        } else {
            reader.skipField(wireType: tag.wire)
        }
    }
    return alerts
}

/// Decodes a GTFS-RT FeedMessage (.pb) and returns tripId → delay (seconds) for all TripUpdate entities.
func decodeGtfsRtTripDelays(from data: Data) -> [String: Int32] {
    var reader = ProtoReader(data: data)
    var result: [String: Int32] = [:]
    while let tag = reader.readTag() {
        if tag.field == 2 && tag.wire == 2 {
            if let d = decodeTripUpdateEntity(reader.readLengthDelimited()) {
                result[d.tripId] = d.delay
            }
        } else {
            reader.skipField(wireType: tag.wire)
        }
    }
    return result
}

/// Full TripUpdate decode: returns the richer `GtfsRtTripDelay` per trip,
/// including per-stop arrival epochs when the feed provides them.
func decodeGtfsRtTripUpdates(from data: Data) -> [String: GtfsRtTripDelay] {
    var reader = ProtoReader(data: data)
    var result: [String: GtfsRtTripDelay] = [:]
    while let tag = reader.readTag() {
        if tag.field == 2 && tag.wire == 2 {
            if let d = decodeTripUpdateEntity(reader.readLengthDelimited()) {
                result[d.tripId] = d
            }
        } else {
            reader.skipField(wireType: tag.wire)
        }
    }
    return result
}

// MARK: - Private decoders — VehiclePosition

private func decodeFeedEntity(_ data: Data) -> GtfsRtVehicle? {
    var r = ProtoReader(data: data)
    var entityId = ""
    var vehicle: GtfsRtVehicle?
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 2): entityId = r.readString()
        case (4, 2): vehicle = decodeVehiclePosition(r.readLengthDelimited())
        default: r.skipField(wireType: tag.wire)
        }
    }
    guard let v = vehicle else { return nil }
    return GtfsRtVehicle(id: entityId, tripId: v.tripId, routeId: v.routeId, label: v.label,
                         latitude: v.latitude, longitude: v.longitude, bearing: v.bearing,
                         timestamp: v.timestamp, currentStopId: v.currentStopId,
                         currentStatus: v.currentStatus,
                         occupancyStatus: v.occupancyStatus,
                         wheelchairAccessible: v.wheelchairAccessible)
}

/// Decodes `VehiclePosition` using **standard GTFS-RT field numbers**
/// (https://gtfs.org/realtime/reference/#message-vehicleposition).
/// Non-standard feeds that put Position at field 2 AND vehicle at a
/// different slot still work: we try to decode field 2 as Position, and
/// fall back to VehicleDescriptor only if lat/lng are zero.
private func decodeVehiclePosition(_ data: Data) -> GtfsRtVehicle? {
    var r = ProtoReader(data: data)
    var tripId = "", routeId = "", label = "", currentStopId = ""
    var lat: Float = 0, lng: Float = 0, bearing: Float = 0
    var timestamp: UInt64 = 0
    var currentStatus: VehicleStatus = .inTransitTo
    var occupancyStatus: OccupancyStatus? = nil
    var wheelchair: WheelchairStatus? = nil
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 2):
            let (t, ro) = decodeTripDescriptor(r.readLengthDelimited())
            tripId = t; routeId = ro
        case (2, 2):
            // Standard: Position. Fallback: some feeds put VehicleDescriptor here.
            let raw = r.readLengthDelimited()
            let (la, lo, be) = decodePosition(raw)
            if la != 0 || lo != 0 {
                lat = la; lng = lo; bearing = be
            } else {
                let (lbl, wa) = decodeVehicleDescriptor(raw)
                if !lbl.isEmpty { label = lbl }
                if wa != nil { wheelchair = wa }
            }
        case (3, 0):
            // current_stop_sequence (uint32, varint) — useful for trip progress.
            _ = r.readVarint()
        case (4, 0):
            // current_status enum
            currentStatus = VehicleStatus(rawValue: r.readVarint()) ?? .inTransitTo
        case (5, 0):
            // timestamp (uint64, varint)
            timestamp = r.readVarint()
        case (6, 0):
            // congestion_level — not used yet
            _ = r.readVarint()
        case (7, 2):
            // stop_id (string)
            currentStopId = r.readString()
        case (8, 2):
            // VehicleDescriptor { id, label, license_plate, wheelchair_accessible }
            let (lbl, wa) = decodeVehicleDescriptor(r.readLengthDelimited())
            if !lbl.isEmpty { label = lbl }
            if wa != nil { wheelchair = wa }
        case (9, 0):
            // occupancy_status enum
            occupancyStatus = OccupancyStatus(rawValue: r.readVarint())
        default:
            r.skipField(wireType: tag.wire)
        }
    }
    guard lat != 0 || lng != 0 else { return nil }
    return GtfsRtVehicle(id: "", tripId: tripId, routeId: routeId, label: label,
                         latitude: lat, longitude: lng, bearing: bearing, timestamp: timestamp,
                         currentStopId: currentStopId, currentStatus: currentStatus,
                         occupancyStatus: occupancyStatus,
                         wheelchairAccessible: wheelchair)
}

// MARK: - Private decoders — TripUpdate

private func decodeTripUpdateEntity(_ data: Data) -> GtfsRtTripDelay? {
    var r = ProtoReader(data: data)
    var tripUpdate: GtfsRtTripDelay?
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (3, 2): tripUpdate = decodeTripUpdate(r.readLengthDelimited())
        default: r.skipField(wireType: tag.wire)
        }
    }
    return tripUpdate
}

/// Decodes a TripUpdate message.
/// - `delay` preference order: `TripUpdate.delay` → first stop's delay.
/// - Collects every `stop_time_update.stop_id → arrival.time` for ETA display.
private func decodeTripUpdate(_ data: Data) -> GtfsRtTripDelay? {
    var r = ProtoReader(data: data)
    var tripId = ""
    var delay: Int32? = nil
    var firstStopDelay: Int32? = nil
    var arrivals: [String: UInt64] = [:]

    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 2):
            let (t, _) = decodeTripDescriptor(r.readLengthDelimited())
            tripId = t
        case (2, 2):
            let stu = decodeStopTimeUpdate(r.readLengthDelimited())
            if firstStopDelay == nil, let d = stu.delay {
                firstStopDelay = d
            }
            if let sid = stu.stopId, let arr = stu.arrivalTime {
                arrivals[sid] = arr
            }
        case (5, 0):
            // TripUpdate.delay — int32 (plain varint, not zigzag)
            delay = Int32(truncatingIfNeeded: r.readVarint())
        default:
            r.skipField(wireType: tag.wire)
        }
    }

    guard !tripId.isEmpty else { return nil }
    let resolvedDelay = delay ?? firstStopDelay ?? 0
    // Emit even when no delay so per-stop ETAs aren't lost.
    return GtfsRtTripDelay(tripId: tripId, delay: resolvedDelay, arrivalByStopId: arrivals)
}

/// Aggregates the fields we care about from a single StopTimeUpdate message.
private struct StopTimeUpdateFields {
    var stopId: String?
    var delay: Int32?
    var arrivalTime: UInt64?
}

/// Decodes a StopTimeUpdate — extracts stop_id, arrival.time (epoch) and a
/// delay (preferring departure, falling back to arrival).
/// GTFS-RT StopTimeUpdate field layout:
///   1: stop_sequence (uint32)
///   2: arrival (StopTimeEvent)   — NOTE: spec says arrival=2 legacy / 3 current; we accept both
///   3: arrival (StopTimeEvent)
///   4: departure (StopTimeEvent)
///   5: schedule_relationship (enum)
///   7: stop_id (string) — spec puts it at 4 in legacy, 7 in newer; we accept both via wire-type
private func decodeStopTimeUpdate(_ data: Data) -> StopTimeUpdateFields {
    var r = ProtoReader(data: data)
    var out = StopTimeUpdateFields()
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 0):
            _ = r.readVarint() // stop_sequence
        case (2, 2), (3, 2):
            // arrival — StopTimeEvent (spec varies: some feeds use 2, newer 3)
            let event = decodeStopTimeEvent(r.readLengthDelimited())
            if out.arrivalTime == nil, let t = event.time { out.arrivalTime = t }
            if out.delay == nil, let d = event.delay { out.delay = d }
        case (4, 2):
            // departure — StopTimeEvent; prefer its delay over arrival's
            let event = decodeStopTimeEvent(r.readLengthDelimited())
            if let d = event.delay { out.delay = d }
        case (7, 2):
            out.stopId = r.readString()
        default:
            r.skipField(wireType: tag.wire)
        }
    }
    return out
}

/// Extracts time (epoch seconds) and delay from a StopTimeEvent.
/// - GTFS-RT spec: delay is `int32` (plain varint, signed via bit pattern),
///   NOT sint32 — so no zigzag decode. Use `truncatingIfNeeded` to read
///   negative values (10-byte varints) without trapping.
private func decodeStopTimeEvent(_ data: Data) -> (delay: Int32?, time: UInt64?) {
    var r = ProtoReader(data: data)
    var delay: Int32? = nil
    var time: UInt64? = nil
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 0):
            // delay (int32) — plain varint
            let raw = r.readVarint()
            delay = Int32(truncatingIfNeeded: raw)
        case (2, 0):
            // time (int64) — plain varint epoch seconds
            time = r.readVarint()
        default:
            r.skipField(wireType: tag.wire)
        }
    }
    return (delay, time)
}

/// Extracts departure.delay (or arrival.delay) from a StopTimeUpdate message.
private func decodeStopTimeUpdateDelay(_ data: Data) -> Int32? {
    var r = ProtoReader(data: data)
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (3, 2), (4, 2):
            // arrival (field 3) or departure (field 4) — StopTimeEvent
            if let d = decodeStopTimeEventDelay(r.readLengthDelimited()) {
                return d
            }
        default:
            r.skipField(wireType: tag.wire)
        }
    }
    return nil
}

/// Extracts delay from a StopTimeEvent message (field 1 = delay, int32).
private func decodeStopTimeEventDelay(_ data: Data) -> Int32? {
    var r = ProtoReader(data: data)
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 0):
            return Int32(truncatingIfNeeded: r.readVarint())
        default:
            r.skipField(wireType: tag.wire)
        }
    }
    return nil
}

// MARK: - Shared decoders

private func decodeTripDescriptor(_ data: Data) -> (tripId: String, routeId: String) {
    var r = ProtoReader(data: data)
    var tripId = "", routeId = ""
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 2): tripId = r.readString()
        case (3, 2): routeId = r.readString()
        default: r.skipField(wireType: tag.wire)
        }
    }
    return (tripId, routeId)
}

private func decodeVehicleDescriptor(_ data: Data) -> (label: String, wheelchair: WheelchairStatus?) {
    // Standard GTFS-RT VehicleDescriptor:
    //   field 1 = id            (stable internal identifier, e.g. "B03")
    //   field 2 = label          (human-readable, but some feeds put block/run info here)
    //   field 3 = license_plate
    //   field 4 = wheelchair_accessible
    // AppalCART/ETA Transit: id="B03", label="14:15:00-126" (block start time).
    // The `id` field is the more useful "vehicle number" for display — prefer it
    // and fall back to `label` when absent.
    var r = ProtoReader(data: data)
    var descriptorId = ""
    var descriptorLabel = ""
    var wheelchair: WheelchairStatus? = nil
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 2): descriptorId = r.readString()
        case (2, 2): descriptorLabel = r.readString()
        case (4, 0):
            // wheelchair_accessible enum (NO_VALUE / UNKNOWN / ACCESSIBLE / INACCESSIBLE)
            wheelchair = WheelchairStatus(rawValue: r.readVarint())
        default: r.skipField(wireType: tag.wire)
        }
    }
    let preferred = descriptorId.isEmpty ? descriptorLabel : descriptorId
    return (preferred, wheelchair)
}

private func decodePosition(_ data: Data) -> (lat: Float, lng: Float, bearing: Float) {
    var r = ProtoReader(data: data)
    var lat: Float = 0, lng: Float = 0, bearing: Float = 0
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 5): lat = Float(bitPattern: r.readFixed32())
        case (2, 5): lng = Float(bitPattern: r.readFixed32())
        case (3, 5): bearing = Float(bitPattern: r.readFixed32())
        default: r.skipField(wireType: tag.wire)
        }
    }
    return (lat, lng, bearing)
}

// Protobuf zigzag decoding for sint32/sint64
private func zigzagDecode(_ n: UInt64) -> Int64 {
    Int64(bitPattern: (n >> 1) ^ (~(n & 1) &+ 1))
}

// MARK: - Private decoders — Alert

private func decodeAlertEntity(_ data: Data) -> GtfsRtAlert? {
    var r = ProtoReader(data: data)
    var entityId = ""
    var alert: AlertPayload?
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 2): entityId = r.readString()
        case (5, 2): alert = decodeAlertPayload(r.readLengthDelimited())
        default: r.skipField(wireType: tag.wire)
        }
    }
    guard let a = alert, !entityId.isEmpty else { return nil }
    return GtfsRtAlert(
        id: entityId,
        activePeriods: a.activePeriods,
        severity: a.severity,
        effect: a.effect,
        cause: a.cause,
        headerText: a.headerText,
        descriptionText: a.descriptionText,
        affectedStopIds: a.affectedStopIds,
        affectedRouteIds: a.affectedRouteIds,
        url: a.url
    )
}

/// Intermediate aggregator used while decoding Alert fields.
private struct AlertPayload {
    var activePeriods: [AlertTimeRange] = []
    var severity: AlertSeverity = .unknown
    var effect: AlertEffect = .unknownEffect
    var cause: AlertCause = .unknownCause
    var headerText: [String: String] = [:]
    var descriptionText: [String: String] = [:]
    var affectedStopIds: Set<String> = []
    var affectedRouteIds: Set<String> = []
    var url: String? = nil
}

private func decodeAlertPayload(_ data: Data) -> AlertPayload {
    var r = ProtoReader(data: data)
    var payload = AlertPayload()
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 2):
            payload.activePeriods.append(decodeTimeRange(r.readLengthDelimited()))
        case (5, 2):
            let (stops, routes) = decodeEntitySelector(r.readLengthDelimited())
            payload.affectedStopIds.formUnion(stops)
            payload.affectedRouteIds.formUnion(routes)
        case (6, 0):
            payload.cause = AlertCause(rawValue: r.readVarint()) ?? .unknownCause
        case (7, 0):
            payload.effect = AlertEffect(rawValue: r.readVarint()) ?? .unknownEffect
        case (8, 2):
            // url TranslatedString — take first translation
            let translations = decodeTranslatedString(r.readLengthDelimited())
            payload.url = translations.values.first
        case (10, 2):
            payload.headerText = decodeTranslatedString(r.readLengthDelimited())
        case (11, 2):
            payload.descriptionText = decodeTranslatedString(r.readLengthDelimited())
        case (14, 0):
            payload.severity = AlertSeverity(rawValue: r.readVarint()) ?? .unknown
        default:
            r.skipField(wireType: tag.wire)
        }
    }
    return payload
}

private func decodeTimeRange(_ data: Data) -> AlertTimeRange {
    var r = ProtoReader(data: data)
    var start: UInt64? = nil
    var end: UInt64? = nil
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 0): start = r.readVarint()
        case (2, 0): end = r.readVarint()
        default: r.skipField(wireType: tag.wire)
        }
    }
    return AlertTimeRange(start: start, end: end)
}

/// Returns (affectedStopIds, affectedRouteIds) harvested from one EntitySelector.
/// Nested TripDescriptor.route_id is also captured.
private func decodeEntitySelector(_ data: Data) -> (stops: Set<String>, routes: Set<String>) {
    var r = ProtoReader(data: data)
    var stops: Set<String> = []
    var routes: Set<String> = []
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (2, 2): routes.insert(r.readString())
        case (4, 2):
            let (_, tripRouteId) = decodeTripDescriptor(r.readLengthDelimited())
            if !tripRouteId.isEmpty { routes.insert(tripRouteId) }
        case (5, 2): stops.insert(r.readString())
        default: r.skipField(wireType: tag.wire)
        }
    }
    return (stops, routes)
}

/// Decodes TranslatedString into a map of BCP-47 language code → text.
/// Entries without a language tag are stored under an empty string key ""
/// so callers can still use them as fallback.
private func decodeTranslatedString(_ data: Data) -> [String: String] {
    var r = ProtoReader(data: data)
    var result: [String: String] = [:]
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 2):
            let (text, lang) = decodeTranslation(r.readLengthDelimited())
            if !text.isEmpty {
                // Normalize BCP-47 to lowercase primary tag to match LocalizedText keys ("en", "es", "it").
                let key = lang.lowercased().split(separator: "-").first.map(String.init) ?? ""
                result[key] = text
            }
        default:
            r.skipField(wireType: tag.wire)
        }
    }
    return result
}

private func decodeTranslation(_ data: Data) -> (text: String, language: String) {
    var r = ProtoReader(data: data)
    var text = ""
    var language = ""
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 2): text = r.readString()
        case (2, 2): language = r.readString()
        default: r.skipField(wireType: tag.wire)
        }
    }
    return (text, language)
}

// MARK: - Proto binary reader

private struct ProtoReader {
    let data: Data
    var pos: Int = 0

    var hasMore: Bool { pos < data.count }

    mutating func readByte() -> UInt8? {
        guard pos < data.count else { return nil }
        defer { pos += 1 }
        return data[pos]
    }

    mutating func readVarint() -> UInt64 {
        var result: UInt64 = 0
        var shift = 0
        while let byte = readByte() {
            result |= UInt64(byte & 0x7F) << shift
            if byte & 0x80 == 0 { break }
            shift += 7
        }
        return result
    }

    mutating func readFixed32() -> UInt32 {
        guard pos + 4 <= data.count else { pos = data.count; return 0 }
        let value = data[pos..<pos+4].withUnsafeBytes { $0.loadUnaligned(as: UInt32.self) }
        pos += 4
        return value.littleEndian
    }

    mutating func readFixed64() -> UInt64 {
        guard pos + 8 <= data.count else { pos = data.count; return 0 }
        let value = data[pos..<pos+8].withUnsafeBytes { $0.loadUnaligned(as: UInt64.self) }
        pos += 8
        return value.littleEndian
    }

    mutating func readLengthDelimited() -> Data {
        let len = Int(readVarint())
        guard pos + len <= data.count else { pos = data.count; return Data() }
        let slice = Data(data[pos..<pos+len])
        pos += len
        return slice
    }

    mutating func readString() -> String {
        let bytes = readLengthDelimited()
        return String(data: bytes, encoding: .utf8) ?? ""
    }

    mutating func skipField(wireType: Int) {
        switch wireType {
        case 0: _ = readVarint()
        case 1: _ = readFixed64()
        case 2: _ = readLengthDelimited()
        case 5: _ = readFixed32()
        default: pos = data.count
        }
    }

    mutating func readTag() -> (field: Int, wire: Int)? {
        guard hasMore else { return nil }
        let v = readVarint()
        return (field: Int(v >> 3), wire: Int(v & 0x7))
    }
}
