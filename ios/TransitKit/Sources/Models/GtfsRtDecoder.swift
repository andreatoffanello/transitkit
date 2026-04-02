import Foundation

// MARK: - GTFS-RT Vehicle Position model

struct GtfsRtVehicle: Identifiable {
    let id: String          // FeedEntity.id
    let tripId: String
    let routeId: String
    let label: String
    let latitude: Float
    let longitude: Float
    let bearing: Float      // degrees clockwise from north
    let timestamp: UInt64
}

// MARK: - Minimal protobuf binary decoder

/// Decodes a GTFS-RT FeedMessage (.pb) and returns all VehiclePosition entities.
/// Handles only the fields needed for map display — skips everything else.
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

// MARK: - Private decoders

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
                         timestamp: v.timestamp)
}

private func decodeVehiclePosition(_ data: Data) -> GtfsRtVehicle? {
    var r = ProtoReader(data: data)
    var tripId = "", routeId = "", label = ""
    var lat: Float = 0, lng: Float = 0, bearing: Float = 0
    var timestamp: UInt64 = 0
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (1, 2):
            let (t, ro) = decodeTripDescriptor(r.readLengthDelimited())
            tripId = t; routeId = ro
        case (2, 2):
            // Standard spec: VehicleDescriptor.
            // Some feeds (e.g. ETA Transit/AppalCART) put Position here instead.
            // Try Position first; if no valid coords, fall back to VehicleDescriptor.
            let raw = r.readLengthDelimited()
            let (la, lo, be) = decodePosition(raw)
            if la != 0 || lo != 0 {
                lat = la; lng = lo; bearing = be
            } else {
                label = decodeVehicleDescriptor(raw)
            }
        case (3, 2):
            // Standard position field.
            let (la, lo, be) = decodePosition(r.readLengthDelimited())
            lat = la; lng = lo; bearing = be
        case (9, 0):
            timestamp = r.readVarint()
        default:
            r.skipField(wireType: tag.wire)
        }
    }
    guard lat != 0 || lng != 0 else { return nil }
    return GtfsRtVehicle(id: "", tripId: tripId, routeId: routeId, label: label,
                         latitude: lat, longitude: lng, bearing: bearing, timestamp: timestamp)
}

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

private func decodeVehicleDescriptor(_ data: Data) -> String {
    var r = ProtoReader(data: data)
    var label = ""
    while let tag = r.readTag() {
        switch (tag.field, tag.wire) {
        case (2, 2): label = r.readString()
        default: r.skipField(wireType: tag.wire)
        }
    }
    return label
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
