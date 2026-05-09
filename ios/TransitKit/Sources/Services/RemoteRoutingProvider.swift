import Foundation

// MARK: - RemoteRoutingProvider
// HTTP client for api.transitkit.app/v1/route — maps MOTIS OTP JSON to [Journey].

final class RemoteRoutingProvider: Sendable {

    private let baseURL: String
    private let apiKey: String
    private let session: URLSession

    nonisolated(unsafe) private static let iso8601: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()

    init(baseURL: String, apiKey: String) {
        self.baseURL = baseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        self.apiKey = apiKey
        let cfg = URLSessionConfiguration.default
        cfg.timeoutIntervalForRequest = 20
        self.session = URLSession(configuration: cfg)
    }

    func query(origin: PlannerStop, destination: PlannerStop, after: Date) async -> [Journey] {
        await fetch(origin: origin, destination: destination, time: after, arriveBy: false)
    }

    func queryArriveBy(origin: PlannerStop, destination: PlannerStop, before: Date) async -> [Journey] {
        await fetch(origin: origin, destination: destination, time: before, arriveBy: true)
    }

    // MARK: - Fetch

    private func fetch(origin: PlannerStop, destination: PlannerStop, time: Date, arriveBy: Bool) async -> [Journey] {
        var comps = URLComponents(string: "\(baseURL)/v1/route")!
        comps.queryItems = [
            URLQueryItem(name: "fromPlace", value: "\(origin.lat),\(origin.lng)"),
            URLQueryItem(name: "toPlace", value: "\(destination.lat),\(destination.lng)"),
            URLQueryItem(name: "time", value: Self.iso8601.string(from: time)),
            URLQueryItem(name: "arriveBy", value: arriveBy ? "true" : "false"),
        ]
        guard let url = comps.url else { return [] }
        var request = URLRequest(url: url)
        request.setValue(apiKey, forHTTPHeaderField: "X-API-Key")

        guard let (data, response) = try? await session.data(for: request),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let decoded = try? JSONDecoder().decode(MOTISResponse.self, from: data) else {
            return []
        }
        return decoded.itineraries.compactMap { map($0, origin: origin, destination: destination) }
    }

    // MARK: - Mapping

    private func map(_ it: MOTISItinerary, origin: PlannerStop, destination: PlannerStop) -> Journey? {
        guard let dep = Self.iso8601.date(from: it.startTime),
              let arr = Self.iso8601.date(from: it.endTime) else { return nil }

        var legs: [Leg] = []
        var totalWalk = 0

        for (idx, ml) in it.legs.enumerated() {
            if ml.mode == "WALK" {
                let from: PlannerStop = idx == 0 ? origin : plannerStop(ml.from)
                let to: PlannerStop = idx == it.legs.count - 1 ? destination : plannerStop(ml.to)
                totalWalk += ml.duration
                legs.append(.walking(WalkingLeg(id: UUID(), fromStop: from, toStop: to, walkSeconds: ml.duration)))
            } else {
                guard let board = ml.from.departure.flatMap({ Self.iso8601.date(from: $0) }),
                      let alight = ml.to.arrival.flatMap({ Self.iso8601.date(from: $0) }) else { continue }
                let color = resolvedColor(ml.routeColor)
                let textColor = ml.routeTextColor ?? contrastHex(for: color)
                let intermediates: [IntermediateStop] = (ml.intermediateStops ?? []).map {
                    IntermediateStop(id: $0.stopId, name: $0.name, time: timeComponent($0.arrival))
                }
                legs.append(.transit(TransitLeg(
                    id: UUID(),
                    lineName: ml.routeShortName ?? ml.agencyName ?? "",
                    routeColor: color,
                    routeTextColor: textColor,
                    headsign: ml.headsign ?? "",
                    boardStop: plannerStop(ml.from),
                    alightStop: plannerStop(ml.to),
                    boardTime: board,
                    alightTime: alight,
                    tripId: ml.tripId ?? "",
                    intermediateStops: intermediates
                )))
            }
        }
        guard !legs.isEmpty else { return nil }
        return Journey(id: UUID(), legs: legs, departureTime: dep, arrivalTime: arr, totalWalkSeconds: totalWalk)
    }

    private func plannerStop(_ place: MOTISPlace) -> PlannerStop {
        PlannerStop(
            id: place.stopId ?? "\(place.lat),\(place.lon)",
            name: place.name,
            lat: place.lat,
            lng: place.lon
        )
    }

    private func resolvedColor(_ hex: String?) -> String {
        guard let hex, !hex.isEmpty else { return "808080" }
        return hex
    }

    private func contrastHex(for hex: String) -> String {
        let clean = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var raw: UInt64 = 0
        Scanner(string: clean).scanHexInt64(&raw)
        let r = Double((raw >> 16) & 0xFF) / 255.0
        let g = Double((raw >> 8) & 0xFF) / 255.0
        let b = Double(raw & 0xFF) / 255.0
        let lin: (Double) -> Double = { c in c <= 0.04045 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4) }
        let lum = 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
        return lum > 0.179 ? "000000" : "FFFFFF"
    }

    // "2026-05-05T14:06:00Z" → "14:06"
    private func timeComponent(_ iso: String) -> String {
        let parts = iso.split(separator: "T")
        guard parts.count == 2 else { return "" }
        return String(parts[1].prefix(5))
    }
}

// MARK: - MOTIS Decodable types

private struct MOTISResponse: Decodable {
    let itineraries: [MOTISItinerary]
}

private struct MOTISItinerary: Decodable {
    let startTime: String
    let endTime: String
    let legs: [MOTISLeg]
}

private struct MOTISLeg: Decodable {
    let mode: String
    let duration: Int
    let from: MOTISPlace
    let to: MOTISPlace
    let routeShortName: String?
    let agencyName: String?
    let headsign: String?
    let tripId: String?
    let routeColor: String?
    let routeTextColor: String?
    let intermediateStops: [MOTISIntermediateStop]?
}

private struct MOTISPlace: Decodable {
    let name: String
    let stopId: String?
    let lat: Double
    let lon: Double
    let departure: String?  // board time on transit legs (ISO8601)
    let arrival: String?    // alight time on transit legs (ISO8601)
}

private struct MOTISIntermediateStop: Decodable {
    let stopId: String
    let name: String
    let arrival: String     // ISO8601
}
