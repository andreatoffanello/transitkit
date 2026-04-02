import Foundation
import SwiftUI

// MARK: - Schedule Store

/// Main data store for the app. Loads schedule data and provides
/// resolved stops, routes, and departures to the UI.
@MainActor
@Observable
class ScheduleStore {
    // MARK: Published state
    var stops: [ResolvedStop] = []
    var routes: [Route] = []
    var isLoading = false
    var error: String?
    var lastUpdated: String?

    // MARK: Lookup tables (built once on load)
    private(set) var scheduleData: ScheduleData?
    private var routeById: [String: Route] = [:]

    private let loader: ScheduleLoader
    private var operatorConfig: OperatorConfig? = nil

    init(operatorId: String, cdnBaseURL: String? = nil) {
        self.loader = ScheduleLoader(operatorId: operatorId, cdnBaseURL: cdnBaseURL)
    }

    /// Supplies the operator config so headsign normalization can use the operator's map.
    /// Call this once from the app's bootstrap sequence, before or after `load()`.
    func configure(with config: OperatorConfig) {
        self.operatorConfig = config
    }

    // MARK: - Load

    func load() async {
        guard !isLoading else { return }
        isLoading = true
        error = nil

        do {
            let data = try await loader.load()
            apply(data)
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    func refresh() async {
        isLoading = true
        error = nil

        do {
            let data = try await loader.refresh()
            apply(data)
        } catch {
            self.error = error.localizedDescription
        }

        isLoading = false
    }

    private func apply(_ data: ScheduleData) {
        scheduleData = data
        lastUpdated = data.lastUpdated

        // Build route lookup
        routeById = Dictionary(uniqueKeysWithValues: data.routes.map { ($0.id, $0) })

        // Build resolved routes
        routes = data.routes

        // Build resolved stops
        stops = data.stops.map { stop in
            let transitTypes = resolveTransitTypes(for: stop, data: data)
            return ResolvedStop(
                id: stop.id,
                name: stop.name,
                lat: stop.lat,
                lng: stop.lng,
                lineNames: stop.lines,
                transitTypes: transitTypes,
                docks: stop.docks ?? []
            )
        }
    }

    // MARK: - Departures for a stop

    /// Returns departures for a stop, grouped by day.
    func departures(forStopId stopId: String) -> [DayGroup: [Departure]] {
        guard let data = scheduleData,
              let stop = data.stops.first(where: { $0.id == stopId })
        else { return [:] }

        var result: [DayGroup: [Departure]] = [:]

        for (dayKey, compactDeps) in stop.departures {
            let dayGroup = DayGroup.parse(dayKey)
            var deps: [Departure] = []

            for compact in compactDeps {
                guard compact.count >= 3 else { continue }

                let time = compact[0].stringValue
                let lineIdx = compact[1].intValue
                let headsignIdx = compact[2].intValue

                guard lineIdx < data.lineNames.count,
                      headsignIdx < data.headsigns.count,
                      lineIdx < data.routeIds.count
                else { continue }

                let lineName = data.lineNames[lineIdx]
                let routeId = data.routeIds[lineIdx]
                let rawHeadsign = data.headsigns[headsignIdx]
                let headsign = HeadsignNormalizer.normalize(rawHeadsign, map: operatorConfig?.headsignMap)
                let route = routeById[routeId]

                let dock = compact.count > 3 ? compact[3].stringValue : ""
                let patternIdx = compact.count > 4 ? compact[4].intValue : nil
                let tripIdIdx = compact.count > 5 ? compact[5].intValue : nil

                let resolvedTripId: String? = tripIdIdx.flatMap { idx in
                    idx < data.tripIds.count ? data.tripIds[idx] : nil
                }
                let dep = Departure(
                    id: "\(time)_\(lineName)_\(headsign)_\(dock)",
                    time: time,
                    lineName: lineName,
                    routeId: routeId,
                    headsign: headsign,
                    color: route?.color ?? "#000000",
                    textColor: route?.textColor ?? "#FFFFFF",
                    transitType: route?.transitType ?? .bus,
                    dock: dock,
                    patternIndex: patternIdx,
                    tripIdIndex: tripIdIdx,
                    tripId: resolvedTripId
                )
                deps.append(dep)
            }

            result[dayGroup] = deps
        }

        return result
    }

    /// Returns departures for the current day of week, sorted ascending by time.
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

    /// Returns upcoming departures from now (or the next ones if past last departure).
    func upcomingDepartures(forStopId stopId: String, limit: Int = 10) -> [Departure] {
        let deps = todayDepartures(forStopId: stopId) // already sorted ascending
        let nowMinutes = currentMinutesFromMidnight()

        // Find first departure at or after now
        if let idx = deps.firstIndex(where: { $0.minutesFromMidnight >= nowMinutes }) {
            return Array(deps[idx..<min(idx + limit, deps.count)])
        }

        // All departures are past → show first ones (next day preview)
        return Array(deps.prefix(limit))
    }

    // MARK: - Route details

    var availableTransitTypes: Set<TransitType> {
        Set(routes.map(\.transitType))
    }

    func route(forId routeId: String) -> Route? {
        routeById[routeId]
    }

    /// Returns all stops served by a route, in direction order.
    func stopsForRoute(_ routeId: String, directionId: Int) -> [ResolvedStop] {
        guard let route = routeById[routeId],
              let direction = route.directions.first(where: { $0.id == directionId })
        else { return [] }

        return direction.stopIds.compactMap { stopId in
            stops.first { $0.id == stopId }
        }
    }

    // MARK: - Trip detail

    /// Returns the stop sequence for a specific trip.
    func tripStops(patternIndex: Int) -> [ResolvedStop]? {
        guard let patterns = scheduleData?.stopPatterns,
              patternIndex >= 0 && patternIndex < patterns.count
        else { return nil }

        let stationIds = patterns[patternIndex]
        return stationIds.compactMap { sid in
            stops.first { $0.id == sid }
        }
    }

    // MARK: - Spatial Queries

    /// Returns up to 5 stops within `radiusMeters` of the given stop,
    /// sorted by distance, excluding the stop itself and duplicates < 30m away.
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

    private func resolveTransitTypes(for stop: ScheduleStop, data: ScheduleData) -> Set<TransitType> {
        var types = Set<TransitType>()
        for lineName in stop.lines {
            if let idx = data.lineNames.firstIndex(of: lineName),
               idx < data.routeIds.count {
                let routeId = data.routeIds[idx]
                if let route = routeById[routeId] {
                    types.insert(route.transitType)
                }
            }
        }
        return types
    }

    private func currentWeekday() -> Weekday {
        let cal = Calendar.current
        let dow = cal.component(.weekday, from: Date())
        // Calendar weekday: 1=Sun, 2=Mon, ... 7=Sat
        switch dow {
        case 1: return .sun
        case 2: return .mon
        case 3: return .tue
        case 4: return .wed
        case 5: return .thu
        case 6: return .fri
        case 7: return .sat
        default: return .mon
        }
    }

    private func currentMinutesFromMidnight() -> Int {
        let cal = Calendar.current
        let now = Date()
        return cal.component(.hour, from: now) * 60 + cal.component(.minute, from: now)
    }
}

// MARK: - Resolved Stop (UI-ready)

/// A stop ready for display, with resolved transit types and line names.
struct ResolvedStop: Identifiable, Hashable {
    let id: String
    let name: String
    let lat: Double
    let lng: Double
    let lineNames: [String]
    let transitTypes: Set<TransitType>
    let docks: [Dock]

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }

    static func == (lhs: ResolvedStop, rhs: ResolvedStop) -> Bool {
        lhs.id == rhs.id
    }
}
