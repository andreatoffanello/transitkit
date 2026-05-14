import Foundation
import CoreLocation

/// Provider di journey planning remoto contro il BFF TransitKit
/// (`api.transitkit.app/v1/route`, MOTIS upstream).
///
/// Sostituisce il CSA in-app: tutte le query pianificazione vanno qui.
/// La firma di `query`/`queryArriveBy` è compatibile con quella che il
/// `PlannerScreen`/ViewModel usa già, così l'integrazione resta isolata.
///
/// Architettura clonata da `movete/.../RemoteRoutingProvider.swift` — qualunque
/// fix lì applicato va portato qui pari pari (e viceversa).
///
/// MOTIS è RT-aware nativo: il delay viene popolato direttamente nel mapping
/// dal confronto `startTime` vs `scheduledStartTime`. Niente `applyRealtime`
/// successivo.
actor RemoteRoutingProvider {

    // MARK: - State

    /// Catalogo Stop indicizzati per GTFS stop_id (raw del feed, es. `BD11`).
    /// MOTIS prefissa con `<region>_` — strippato in `resolveStop`. Configurato
    /// da `ConnectionsStore` dopo il load di `ScheduleStore`.
    private var stopByGtfsId: [String: ResolvedStop] = [:]

    var isReady: Bool { !stopByGtfsId.isEmpty }

    // MARK: - Setup

    /// Da chiamare dopo che `ScheduleStore` ha caricato le fermate. Costruisce
    /// l'indice gtfs_id → ResolvedStop per la risoluzione MOTIS → domain.
    func configure(stops: [ResolvedStop]) {
        var sMap: [String: ResolvedStop] = [:]
        sMap.reserveCapacity(stops.count * 2)
        for s in stops {
            sMap[s.id] = s                              // canonical id
            for gid in s.gtfsStopIds { sMap[gid] = s }  // raw GTFS ids
        }
        self.stopByGtfsId = sMap
    }

    // MARK: - Public query API

    func query(origin: PlannerStop, destination: PlannerStop, after: Date) async -> [Journey] {
        await fetchSafe(origin: origin, destination: destination, time: after, arriveBy: false)
    }

    func queryArriveBy(origin: PlannerStop, destination: PlannerStop, before: Date) async -> [Journey] {
        await fetchSafe(origin: origin, destination: destination, time: before, arriveBy: true)
    }

    // MARK: - Networking

    private func fetchSafe(
        origin: PlannerStop, destination: PlannerStop, time: Date, arriveBy: Bool
    ) async -> [Journey] {
        do {
            return try await fetch(origin: origin, destination: destination, time: time, arriveBy: arriveBy)
        } catch {
            #if DEBUG
            print("[RemoteRoutingProvider] error: \(error)")
            #endif
            return []
        }
    }

    private func fetch(
        origin: PlannerStop, destination: PlannerStop, time: Date, arriveBy: Bool
    ) async throws -> [Journey] {
        var components = URLComponents(string: TransitKitSecrets.baseURL + "/v1/route")
        components?.queryItems = [
            URLQueryItem(name: "fromPlace", value: "\(origin.lat),\(origin.lng)"),
            URLQueryItem(name: "toPlace",   value: "\(destination.lat),\(destination.lng)"),
            URLQueryItem(name: "time",      value: Self.iso.string(from: time)),
            URLQueryItem(name: "mode",      value: "TRANSIT,WALK"),
        ]
        if arriveBy {
            components?.queryItems?.append(URLQueryItem(name: "arriveBy", value: "true"))
        }
        guard let url = components?.url else { throw RemoteRoutingError.invalidURL }
        var req = URLRequest(url: url)
        req.timeoutInterval = 10
        req.setValue(TransitKitSecrets.apiKey, forHTTPHeaderField: "X-API-Key")
        req.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await URLSession.shared.data(for: req)
        guard let http = response as? HTTPURLResponse,
              (200..<300).contains(http.statusCode) else {
            throw RemoteRoutingError.http((response as? HTTPURLResponse)?.statusCode ?? 0)
        }
        let payload = try JSONDecoder().decode(MOTISResponse.self, from: data)
        let mapped = mapJourneys(payload, origin: origin, destination: destination)
        return Array(mapped.prefix(5))
    }

    // MARK: - Mapping MOTIS → Domain

    private func mapJourneys(
        _ payload: MOTISResponse,
        origin: PlannerStop, destination: PlannerStop
    ) -> [Journey] {
        var out: [Journey] = []
        out.reserveCapacity((payload.itineraries?.count ?? 0) + (payload.direct?.count ?? 0))
        for it in (payload.itineraries ?? []) + (payload.direct ?? []) {
            if let j = mapItinerary(it, origin: origin, destination: destination) {
                out.append(j)
            }
        }
        return out.sorted(by: { $0.departureTime < $1.departureTime })
    }

    private func mapItinerary(
        _ it: MOTISItinerary,
        origin: PlannerStop, destination: PlannerStop
    ) -> Journey? {
        var legs: [Leg] = []
        var totalWalk = 0
        for raw in it.legs {
            switch raw.mode {
            case "WALK":
                let from = resolveStop(raw.from, fallback: origin)
                let to   = resolveStop(raw.to,   fallback: destination)
                let secs = raw.duration ?? 0
                let meters = raw.distance ?? Int(Double(secs) * 1.2) // 4.3 km/h walking speed
                legs.append(.walking(WalkingLeg(
                    id: UUID(), fromStop: from, toStop: to,
                    walkSeconds: secs, distanceMeters: meters
                )))
                totalWalk += secs

            case "BUS", "TRAM", "TRAMWAY", "CABLE_CAR",
                 "SUBWAY", "METRO",
                 "RAIL", "REGIONAL_RAIL", "COMMUTER_RAIL", "HIGHSPEED_RAIL",
                 "FERRY":
                let from = resolveStop(raw.from, fallback: origin)
                let to   = resolveStop(raw.to,   fallback: destination)
                let scheduledBoard  = parseDate(raw.scheduledStartTime ?? raw.startTime)
                let scheduledAlight = parseDate(raw.scheduledEndTime   ?? raw.endTime)
                guard let board = scheduledBoard, let alight = scheduledAlight else { continue }

                let color = normalizeHex(raw.routeColor) ?? "808080"
                let textColor = normalizeHex(raw.routeTextColor) ?? contrastHex(for: color)

                let inter: [IntermediateStop] = (raw.intermediateStops ?? []).map { p in
                    let s = resolveStop(p, fallback: from)
                    return IntermediateStop(
                        id: s.id,
                        name: s.name,
                        time: timeComponent(p.scheduledArrival ?? p.arrival ?? ""),
                        lat: s.lat,
                        lng: s.lng
                    )
                }

                legs.append(.transit(TransitLeg(
                    id: UUID(),
                    lineName: raw.routeShortName ?? raw.displayName ?? raw.routeLongName ?? "",
                    routeColor: color,
                    routeTextColor: textColor,
                    headsign: cleanHeadsign(raw.headsign) ?? "",
                    boardStop: from,
                    alightStop: to,
                    boardTime: board,
                    alightTime: alight,
                    tripId: raw.tripId ?? "",
                    intermediateStops: inter
                )))

            default:
                continue // mode sconosciuto: salta la leg, non l'itinerario
            }
        }
        guard !legs.isEmpty else { return nil }

        let transitLegs = legs.compactMap { (l: Leg) -> TransitLeg? in
            if case .transit(let t) = l { return t } else { return nil }
        }
        let firstT = transitLegs.first
        let lastT  = transitLegs.last

        // Se l'itinerario è interamente walk (proviene da `direct[]` MOTIS),
        // dep/arr sono i tempi della camminata stessa. Altrimenti estende
        // dep/arr coi tempi di walking pre/post le leg transit.
        let dep: Date
        let arr: Date
        if let firstT = firstT, let lastT = lastT {
            var leadingWalk = 0
            for leg in legs {
                if case .transit = leg { break }
                if case .walking(let wl) = leg { leadingWalk += wl.walkSeconds }
            }
            var trailingWalk = 0
            for leg in legs.reversed() {
                if case .transit = leg { break }
                if case .walking(let wl) = leg { trailingWalk += wl.walkSeconds }
            }
            dep = firstT.boardTime.addingTimeInterval(TimeInterval(-leadingWalk))
            arr = lastT.alightTime.addingTimeInterval(TimeInterval(trailingWalk))
        } else {
            guard let d = parseDate(it.startTime), let a = parseDate(it.endTime) else { return nil }
            dep = d; arr = a
        }

        return Journey(
            id: UUID(),
            legs: legs,
            departureTime: dep,
            arrivalTime: arr,
            totalWalkSeconds: totalWalk
        )
    }

    // MARK: - Resolve helpers

    /// MOTIS stopId è prefissato `<region>_` (es. `appalcart_BD11`). Se trovo
    /// un match nel catalogo (per gtfs_id o id canonical) restituisco quello;
    /// altrimenti costruisco un PlannerStop ad-hoc dai dati MOTIS.
    private func resolveStop(_ p: MOTISPlace?, fallback: PlannerStop) -> PlannerStop {
        guard let p = p else { return fallback }
        if let raw = p.stopId {
            // Strip qualsiasi prefisso `<region>_` (es. `appalcart_`, `romamobilita_`)
            let stripped = stripRegionPrefix(raw)
            if let known = stopByGtfsId[stripped] {
                return PlannerStop(id: known.id, name: known.name, lat: known.lat, lng: known.lng)
            }
        }
        // MOTIS usa "START"/"END" come placeholder per fromPlace/toPlace —
        // non sono nomi reali, prendiamo il name dell'origin/dest scelto.
        let name: String = {
            guard let n = p.name, n != "START", n != "END" else { return fallback.name }
            return n
        }()
        return PlannerStop(
            id: p.stopId ?? "virtual_\(p.lat ?? 0)_\(p.lon ?? 0)",
            name: name,
            lat: p.lat ?? fallback.lat,
            lng: p.lon ?? fallback.lng
        )
    }

    /// Rimuove un eventuale prefisso `<word>_` (es. `appalcart_X1` → `X1`).
    /// Coerente con la convenzione MOTIS che prefissa gli stop_id del feed
    /// col nome della region/dataset.
    private func stripRegionPrefix(_ raw: String) -> String {
        if let idx = raw.firstIndex(of: "_") {
            return String(raw[raw.index(after: idx)...])
        }
        return raw
    }

    // MARK: - Helpers

    private func cleanHeadsign(_ s: String?) -> String? {
        guard let s = s else { return nil }
        return s.replacingOccurrences(of: "\"\"", with: "\"")
                .trimmingCharacters(in: .whitespaces)
    }

    private func normalizeHex(_ h: String?) -> String? {
        guard let h = h, !h.isEmpty else { return nil }
        let trimmed = h.hasPrefix("#") ? String(h.dropFirst()) : h
        return trimmed.uppercased()
    }

    private func contrastHex(for hex: String) -> String {
        let clean = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var raw: UInt64 = 0
        Scanner(string: clean).scanHexInt64(&raw)
        let r = Double((raw >> 16) & 0xFF) / 255.0
        let g = Double((raw >> 8)  & 0xFF) / 255.0
        let b = Double( raw        & 0xFF) / 255.0
        let lin: (Double) -> Double = { c in c <= 0.04045 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4) }
        let lum = 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
        return lum > 0.179 ? "000000" : "FFFFFF"
    }

    private func timeComponent(_ iso: String) -> String {
        let parts = iso.split(separator: "T")
        guard parts.count == 2 else { return "" }
        return String(parts[1].prefix(5))
    }

    private func parseDate(_ iso: String?) -> Date? {
        guard let iso = iso else { return nil }
        if let d = Self.iso.date(from: iso) { return d }
        if let d = Self.isoFractional.date(from: iso) { return d }
        return nil
    }

    // MARK: - Static formatters (thread-safe in actor)

    private nonisolated(unsafe) static let iso: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()

    private nonisolated(unsafe) static let isoFractional: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()
}

// MARK: - Errors

enum RemoteRoutingError: Error {
    case invalidURL
    case http(Int)
}

// MARK: - MOTIS DTOs (private, decoder-only)

private struct MOTISResponse: Decodable {
    let itineraries: [MOTISItinerary]?
    let direct: [MOTISItinerary]?
}

private struct MOTISItinerary: Decodable {
    let duration: Int?
    let startTime: String
    let endTime: String
    let transfers: Int?
    let legs: [MOTISLeg]
}

private struct MOTISLeg: Decodable {
    let mode: String
    let duration: Int?
    let distance: Int?
    let startTime: String
    let endTime: String
    let scheduledStartTime: String?
    let scheduledEndTime: String?
    let realTime: Bool?
    let from: MOTISPlace?
    let to: MOTISPlace?
    let routeId: String?
    let routeShortName: String?
    let routeLongName: String?
    let routeColor: String?
    let routeTextColor: String?
    let displayName: String?
    let headsign: String?
    let agencyId: String?
    let agencyName: String?
    let tripId: String?
    let intermediateStops: [MOTISPlace]?
}

private struct MOTISPlace: Decodable {
    let stopId: String?
    let name: String?
    let lat: Double?
    let lon: Double?
    let scheduledArrival: String?
    let arrival: String?
    let scheduledDeparture: String?
    let departure: String?
}
