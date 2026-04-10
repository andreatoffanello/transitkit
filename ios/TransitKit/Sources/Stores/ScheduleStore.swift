import Foundation
import SwiftUI

// MARK: - Schedule Store

/// Main data store for the app. Loads schedule data via APIClient and provides
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
    private var stopById: [String: ResolvedStop] = [:]
    private(set) var routeStopSequences: [String: String] = [:]
    private(set) var tripIdsByRouteId: [String: Set<String>] = [:]

    private var loader: ScheduleLoader
    private(set) var apiUrl: String?
    private var operatorConfig: OperatorConfig? = nil

    init(operatorId: String, apiUrl: String? = nil) {
        self.apiUrl = apiUrl
        self.loader = ScheduleLoader(operatorId: operatorId, apiUrl: apiUrl)
    }

    func configure(with config: OperatorConfig) {
        self.operatorConfig = config
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
            let response = try await loader.load()
            apply(response)
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
                docks: []
            )
        }

        stopById = Dictionary(uniqueKeysWithValues: stops.map { ($0.id, $0) })

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
        for (dayGroup, deps) in allDeps {
            if dayGroup.days.contains(today) {
                return deps.sorted { $0.minutesFromMidnight < $1.minutesFromMidnight }
            }
        }
        return []
    }

    func upcomingDepartures(forStopId stopId: String, limit: Int = 10) -> [Departure] {
        let deps = todayDepartures(forStopId: stopId)
        let nowMinutes = currentMinutesFromMidnight()
        if let idx = deps.firstIndex(where: { $0.minutesFromMidnight >= nowMinutes }) {
            return Array(deps[idx..<min(idx + limit, deps.count)])
        }
        return Array(deps.prefix(limit))
    }

    // MARK: - Route details

    var availableTransitTypes: Set<TransitType> {
        Set(routes.map { TransitType(gtfsRouteType: $0.transitType) })
    }

    func route(forId routeId: String) -> APIRoute? {
        routeById[routeId]
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
        switch Calendar.current.component(.weekday, from: Date()) {
        case 1: return .sun; case 2: return .mon; case 3: return .tue
        case 4: return .wed; case 5: return .thu; case 6: return .fri
        case 7: return .sat; default: return .mon
        }
    }

    private func currentMinutesFromMidnight() -> Int {
        let cal = Calendar.current
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

    func hash(into hasher: inout Hasher) { hasher.combine(id) }
    static func == (lhs: ResolvedStop, rhs: ResolvedStop) -> Bool { lhs.id == rhs.id }
}

struct APIDock: Codable, Hashable {
    let letter: String
}
