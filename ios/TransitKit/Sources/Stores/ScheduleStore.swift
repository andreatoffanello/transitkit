import Foundation
import SwiftUI

// MARK: - Schedule Store

/// Main data store for the app. Loads schedule data via ScheduleLoader (CDN or legacy API) and provides
/// resolved stops, routes, and departures to the UI.
@MainActor
@Observable
class ScheduleStore {
    // MARK: Published state
    var stops: [ResolvedStop] = []
    var routes: [APIRoute] = []
    var isLoading = false
    var error: String?
    var lastUpdated: String?

    // MARK: Lookup tables
    private(set) var scheduleResponse: ScheduleResponse?
    private var routeById: [String: APIRoute] = [:]
    private var routeByName: [String: APIRoute] = [:]
    private var stopById: [String: ResolvedStop] = [:]
    /// Secondary index mapping original GTFS stop_ids (from the feed's
    /// VehiclePosition.stop_id and TripUpdate.stop_id) back to the
    /// aggregated station. Multiple pontili (GTFS stop_ids) point to the
    /// same ResolvedStop.
    private var stopByGtfsId: [String: ResolvedStop] = [:]
    private(set) var routeStopSequences: [String: String] = [:]
    private(set) var tripIdsByRouteId: [String: Set<String>] = [:]
    private(set) var routeIdByTripId: [String: String] = [:]
    /// Maps tripId → directionId (0/1). Derived from `APIDeparture.headsign`
    /// matched against `APIRoute.directions[*].headsign`. Linee circolari
    /// (1 direction) collassano sempre su 0.
    private(set) var directionByTripId: [String: Int] = [:]

    private var loader: ScheduleLoader
    private(set) var apiUrl: String?
    private var operatorConfig: OperatorConfig? = nil
    private(set) var operatorTimezone: TimeZone = .current

    init(operatorId: String, apiUrl: String? = nil) {
        self.apiUrl = apiUrl
        self.loader = ScheduleLoader(operatorId: operatorId, apiUrl: apiUrl)
    }

    func configure(with config: OperatorConfig) {
        self.operatorConfig = config
        self.operatorTimezone = TimeZone(identifier: config.timezone) ?? .current
        // Re-create loader with config so it can resolve the CDN URL
        self.loader = ScheduleLoader(
            operatorId: config.id,
            apiUrl: config.apiUrl,
            operatorConfig: config
        )
    }

    // MARK: - Load

    func load() async {
        guard !isLoading else { return }
        isLoading = true
        error = nil
        do {
            let (response, fromCache) = try await loader.load()
            apply(response)
            // Only check CDN for fresher data when we loaded from disk (potentially stale).
            // If we just downloaded from CDN, the data is already current — skip the check.
            if fromCache {
                Task { [weak self] in
                    guard let self else { return }
                    if let updated = await self.loader.fetchUpdateIfNewer(than: response.lastUpdated) {
                        self.apply(updated)
                    }
                }
            }
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func refresh() async {
        isLoading = true
        error = nil
        do {
            let response = try await loader.refresh()
            apply(response)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    private func apply(_ response: ScheduleResponse) {
        scheduleResponse = response
        lastUpdated = response.lastUpdated

        routeById = Dictionary(uniqueKeysWithValues: response.routes.map { ($0.id, $0) })
        // Name-based index for UI contexts that only carry the line name
        // (e.g. ResolvedStop.lineNames in the Planner stop search). Last
        // wins on duplicate names — acceptable since GTFS short_names are
        // intended to be unique per agency.
        routeByName = Dictionary(response.routes.map { ($0.name, $0) }, uniquingKeysWith: { _, last in last })
        routes = response.routes

        stops = response.stops.map { apiStop in
            let lineNames = Array(Set(apiStop.departures.map(\.routeName))).sorted()
            let transitTypes = Set(apiStop.departures.compactMap { dep in
                routeById[dep.routeId].map { TransitType(gtfsRouteType: $0.transitType) }
            })
            return ResolvedStop(
                id: apiStop.id,
                name: apiStop.name,
                lat: apiStop.lat,
                lng: apiStop.lng,
                lineNames: lineNames,
                transitTypes: transitTypes.isEmpty ? [.bus] : transitTypes,
                docks: [],
                gtfsStopIds: apiStop.gtfsStopIds ?? []
            )
        }

        stopById = Dictionary(uniqueKeysWithValues: stops.map { ($0.id, $0) })
        var gtfsIndex: [String: ResolvedStop] = [:]
        for stop in stops {
            for gtfsId in stop.gtfsStopIds {
                gtfsIndex[gtfsId] = stop
            }
        }
        stopByGtfsId = gtfsIndex

        routeStopSequences = Dictionary(uniqueKeysWithValues: routes.compactMap { route in
            guard let dir = route.directions.first else { return nil }
            let names = dir.stopIds.compactMap { stopById[$0]?.name }
            guard !names.isEmpty else { return nil }
            // Circular/loop routes have a single direction — use · instead of → to avoid
            // implying one-way directionality. Linear routes keep the → arrow.
            let sep = route.directions.count == 1 ? " · " : " → "
            return (route.id, names.joined(separator: sep))
        })

        var tripMap: [String: Set<String>] = [:]
        for stop in response.stops {
            for dep in stop.departures {
                tripMap[dep.routeId, default: []].insert(dep.tripId)
            }
        }
        tripIdsByRouteId = tripMap

        var routeByTrip: [String: String] = [:]
        var directionByTrip: [String: Int] = [:]
        for stop in response.stops {
            for dep in stop.departures {
                guard !dep.tripId.isEmpty, !dep.routeId.isEmpty else { continue }
                routeByTrip[dep.tripId] = dep.routeId
                // Match headsign against route directions to resolve directionId.
                // Linee circolari (route.directions.count == 1) collapsano su 0.
                if let route = routeById[dep.routeId] {
                    if route.directions.count <= 1 {
                        directionByTrip[dep.tripId] = 0
                    } else if let head = dep.headsign,
                              let dir = route.directions.first(where: { $0.headsign == head }) {
                        directionByTrip[dep.tripId] = dir.directionId
                    }
                }
            }
        }
        routeIdByTripId = routeByTrip
        directionByTripId = directionByTrip
    }

    // MARK: - Departures for a stop

    func departures(forStopId stopId: String) -> [DayGroup: [Departure]] {
        guard let response = scheduleResponse,
              let apiStop = response.stops.first(where: { $0.id == stopId })
        else { return [:] }

        var grouped: [String: [APIDeparture]] = [:]
        for dep in apiStop.departures {
            let key = dep.serviceDays.sorted().joined(separator: ",")
            grouped[key, default: []].append(dep)
        }

        var result: [DayGroup: [Departure]] = [:]
        for (dayKey, deps) in grouped {
            let dayGroup = parseDayGroup(from: dayKey)
            let resolved = deps.map { Departure(from: $0, route: routeById[$0.routeId]) }
            result[dayGroup] = resolved.sorted { $0.minutesFromMidnight < $1.minutesFromMidnight }
        }
        return result
    }

    func todayDepartures(forStopId stopId: String) -> [Departure] {
        let allDeps = departures(forStopId: stopId)
        let today = currentWeekday()
        // Merge ALL day groups that include today — a stop can have multiple service patterns
        // (e.g. "mon-sun" all-week + "fri,sat,sun" weekend extras). Returning only the first
        // match was non-deterministic because Swift Dictionary iteration order is random.
        var merged: [Departure] = []
        for (dayGroup, deps) in allDeps {
            if dayGroup.days.contains(today) {
                merged.append(contentsOf: deps)
            }
        }
        let sorted = merged.sorted { $0.minutesFromMidnight < $1.minutesFromMidnight }
        // Dedup: GTFS feeds often expose multiple trip_ids that collapse to the
        // same scheduled run (overlapping service calendars, imported mirrors,
        // etc. AppalCART has up to 7 duplicates per time slot). Keep the first
        // occurrence of each (time, route, headsign) tuple — this preserves
        // legitimate branching at the same minute (e.g. A → X and A → Y) while
        // killing exact duplicates.
        var seen: Set<String> = []
        seen.reserveCapacity(sorted.count)
        var deduped: [Departure] = []
        deduped.reserveCapacity(sorted.count)
        for dep in sorted {
            let key = "\(dep.time)|\(dep.routeId)|\(dep.headsign)"
            if seen.insert(key).inserted {
                deduped.append(dep)
            }
        }
        return deduped
    }

    func upcomingDepartures(forStopId stopId: String, limit: Int = 10) -> [Departure] {
        let deps = todayDepartures(forStopId: stopId)
        let nowMinutes = currentMinutesFromMidnight()
        guard let idx = deps.firstIndex(where: { $0.minutesFromMidnight >= nowMinutes }) else {
            return []  // Service ended for today — don't show past buses as "upcoming"
        }
        let result = Array(deps[idx..<min(idx + limit, deps.count)])
        return result
    }

    // MARK: - Route details

    var availableTransitTypes: Set<TransitType> {
        Set(routes.map { TransitType(gtfsRouteType: $0.transitType) })
    }

    func route(forId routeId: String) -> APIRoute? {
        routeById[routeId]
    }

    func route(forName name: String) -> APIRoute? {
        routeByName[name]
    }

    func stop(forId stopId: String) -> ResolvedStop? {
        stopById[stopId] ?? stopByGtfsId[stopId]
    }

    /// Returns the directionId (0/1) for a given tripId, or nil if unresolved.
    /// Used by LineDetail to filter live vehicle cards by selected direction.
    func direction(forTripId tripId: String) -> Int? {
        directionByTripId[tripId]
    }

    func stopsForRoute(_ routeId: String, directionId: Int) -> [ResolvedStop] {
        guard let route = routeById[routeId],
              let direction = route.directions.first(where: { $0.directionId == directionId })
        else { return [] }
        return direction.stopIds.compactMap { stopById[$0] }
    }

    // MARK: - Spatial Queries

    func nearbyStops(to stop: ResolvedStop, radiusMeters: Double = 400) -> [ResolvedStop] {
        let latPerMeter = 1.0 / 111_320.0
        let lngPerMeter = 1.0 / (111_320.0 * cos(stop.lat * .pi / 180.0))
        return stops
            .filter { $0.id != stop.id }
            .compactMap { candidate -> (ResolvedStop, Double)? in
                let dlat = (candidate.lat - stop.lat) / latPerMeter
                let dlng = (candidate.lng - stop.lng) / lngPerMeter
                let distMeters = sqrt(dlat * dlat + dlng * dlng)
                guard distMeters >= 30 && distMeters <= radiusMeters else { return nil }
                return (candidate, distMeters)
            }
            .sorted { $0.1 < $1.1 }
            .prefix(5)
            .map(\.0)
    }

    // MARK: - Helpers

    private func parseDayGroup(from serviceDaysKey: String) -> DayGroup {
        let parts = serviceDaysKey.split(separator: ",").map(String.init)
        let days: [Weekday] = parts.compactMap { gtfsDay in
            switch gtfsDay {
            case "monday":    return .mon
            case "tuesday":   return .tue
            case "wednesday": return .wed
            case "thursday":  return .thu
            case "friday":    return .fri
            case "saturday":  return .sat
            case "sunday":    return .sun
            default:          return nil
            }
        }
        let abbrevKey = days.map { abbrev(for: $0) }.joined(separator: ",")
        return DayGroup(id: abbrevKey, days: days)
    }

    private func abbrev(for day: Weekday) -> String {
        switch day {
        case .mon: "mon"; case .tue: "tue"; case .wed: "wed"
        case .thu: "thu"; case .fri: "fri"; case .sat: "sat"; case .sun: "sun"
        }
    }

    private func currentWeekday() -> Weekday {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = operatorTimezone
        switch cal.component(.weekday, from: Date()) {
        case 1: return .sun; case 2: return .mon; case 3: return .tue
        case 4: return .wed; case 5: return .thu; case 6: return .fri
        case 7: return .sat; default: return .mon
        }
    }

    func currentMinutesFromMidnight() -> Int {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = operatorTimezone
        let now = Date()
        return cal.component(.hour, from: now) * 60 + cal.component(.minute, from: now)
    }
}

// MARK: - Resolved Stop (UI-ready)

struct ResolvedStop: Identifiable, Hashable {
    let id: String
    let name: String
    let lat: Double
    let lng: Double
    let lineNames: [String]
    let transitTypes: Set<TransitType>
    let docks: [APIDock]
    /// Original GTFS stop_ids aggregated under this station. Used to match
    /// GTFS-RT VehiclePosition.stop_id back to our synthetic station id.
    let gtfsStopIds: [String]

    func hash(into hasher: inout Hasher) { hasher.combine(id) }
    static func == (lhs: ResolvedStop, rhs: ResolvedStop) -> Bool { lhs.id == rhs.id }
}

struct APIDock: Codable, Hashable {
    let letter: String
}
