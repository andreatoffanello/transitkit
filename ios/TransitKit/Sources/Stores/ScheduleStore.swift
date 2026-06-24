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
    /// Maps tripId → earliest (start-of-run) `(APIStop, APIDeparture)` pair.
    /// Replaces `O(stops × deps)` scans previously done on the main thread by
    /// `firstDepartureAndStop(forTripId:)` and `MappaTab.makeTripTarget`.
    private(set) var apiDepartureByTripId: [String: (apiStop: APIStop, departure: APIDeparture)] = [:]

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

        // Single pass over `response.stops × departures` builds all trip-keyed
        // indices at once: tripIdsByRouteId, routeIdByTripId, directionByTripId,
        // and apiDepartureByTripId (earliest stop/departure for each trip).
        var tripMap: [String: Set<String>] = [:]
        var routeByTrip: [String: String] = [:]
        var directionByTrip: [String: Int] = [:]
        var firstDepartureByTrip: [String: (apiStop: APIStop, departure: APIDeparture)] = [:]
        for stop in response.stops {
            for dep in stop.departures {
                tripMap[dep.routeId, default: []].insert(dep.tripId)

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

                // Track earliest (start-of-run) stop per trip — needed by
                // `firstDepartureAndStop(forTripId:)` and `MappaTab.makeTripTarget`.
                // Replaces O(stops × deps) scans on the main thread.
                if let current = firstDepartureByTrip[dep.tripId] {
                    if dep.departureTime < current.departure.departureTime {
                        firstDepartureByTrip[dep.tripId] = (stop, dep)
                    }
                } else {
                    firstDepartureByTrip[dep.tripId] = (stop, dep)
                }
            }
        }
        tripIdsByRouteId = tripMap
        routeIdByTripId = routeByTrip
        directionByTripId = directionByTrip
        apiDepartureByTripId = firstDepartureByTrip
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
            let resolved = deps.map { Departure(from: $0, route: routeById[$0.routeId], headsignMap: operatorConfig?.headsignMap) }
            result[dayGroup] = resolved.sorted { $0.minutesFromMidnight < $1.minutesFromMidnight }
        }
        return result
    }

    /// Departures bucketed by DERIVED day-type (Weekdays / Saturday / Sunday) for
    /// the full-schedule sheet — NOT by the raw GTFS service-calendar signature.
    /// A trip whose service runs all 7 days used to land in its own bucket,
    /// fragmenting a line's weekday service (the lone line-E 07:17 under "every
    /// day") and producing unreadable raw-day chips ("Mo/Su"). Each departure is
    /// fanned into every day-type its service days touch, then deduped by
    /// (time, route, headsign). Mirrors the Android fix.
    ///
    /// Intentionally SEPARATE from `departures(forStopId:)`, which keeps exact-day
    /// grouping so `todayDepartures` stays day-precise (a Mon-only trip must not
    /// leak into Tuesday via a coarse Weekdays bucket). Bucket ids "0_/1_/2_"
    /// drive the chip order (Weekdays → Sat → Sun); the label comes from `days`.
    func fullScheduleDepartures(forStopId stopId: String) -> [DayGroup: [Departure]] {
        guard let response = scheduleResponse,
              let apiStop = response.stops.first(where: { $0.id == stopId })
        else { return [:] }

        let buckets: [(DayGroup, Set<Weekday>)] = [
            (DayGroup(id: "0_weekdays", days: [.mon, .tue, .wed, .thu, .fri]), [.mon, .tue, .wed, .thu, .fri]),
            (DayGroup(id: "1_sat", days: [.sat]), [.sat]),
            (DayGroup(id: "2_sun", days: [.sun]), [.sun]),
        ]

        var grouped: [DayGroup: [APIDeparture]] = [:]
        for dep in apiStop.departures {
            let serviceDays = Set(parseDayGroup(from: dep.serviceDays.sorted().joined(separator: ",")).days)
            for (group, members) in buckets where !serviceDays.isDisjoint(with: members) {
                grouped[group, default: []].append(dep)
            }
        }

        var result: [DayGroup: [Departure]] = [:]
        for (group, deps) in grouped {
            let resolved = deps
                .map { Departure(from: $0, route: routeById[$0.routeId], headsignMap: operatorConfig?.headsignMap) }
                .sorted { $0.minutesFromMidnight < $1.minutesFromMidnight }
            // Dedup by (time, route, headsign) — same as todayDepartures.
            var seen: Set<String> = []
            var deduped: [Departure] = []
            for dep in resolved {
                let key = "\(dep.time)|\(dep.routeId)|\(dep.headsign)"
                if seen.insert(key).inserted { deduped.append(dep) }
            }
            result[group] = deduped
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

    /// Returns upcoming departures for a stop, sorted by EFFECTIVE departure time
    /// (scheduled + RT delay). Late trips that have not yet physically arrived stay
    /// in the list; early trips that have already left are dropped.
    ///
    /// - Parameter delayLookup: optional closure mapping `tripId → reliableDelayMinutes`.
    ///   When `nil` (or the closure returns `nil` for a trip) the behaviour is
    ///   byte-identical to the pre-RT path — no regression for the normal case.
    ///   Mirrors DoVe `WaterBusViewModel.nextDepartures` sort + filter logic.
    func upcomingDepartures(
        forStopId stopId: String,
        limit: Int = 10,
        delayLookup: ((_ tripId: String?) -> Int?)? = nil
    ) -> [Departure] {
        let deps = todayDepartures(forStopId: stopId)
        let nowMinutes = currentMinutesFromMidnight()
        // Effective minutes = scheduled + RT delay. With no delay lookup the
        // effective value equals the scheduled one → same filter as before.
        func effectiveMinutes(_ dep: Departure) -> Int {
            let delay = delayLookup.flatMap { $0(dep.tripId) } ?? 0
            return dep.minutesFromMidnight + delay
        }
        // Filter: keep departures whose EFFECTIVE time is still in the future.
        // A late trip whose scheduled slot just passed but is physically still
        // arriving stays in; an early trip that has already left is dropped.
        let upcoming = deps.filter { effectiveMinutes($0) >= nowMinutes }
        guard !upcoming.isEmpty else {
            return []  // Service ended for today — don't show past buses as "upcoming"
        }
        // Sort by effective time so countdowns are monotonically increasing
        // regardless of RT delay. Mirrors DoVe's re-sort after applying rtDelay.
        let sorted = upcoming.sorted { effectiveMinutes($0) < effectiveMinutes($1) }
        return Array(sorted.prefix(limit))
    }

    // MARK: - Route details

    var availableTransitTypes: Set<TransitType> {
        Set(routes.map { TransitType(gtfsRouteType: $0.transitType) })
    }

    /// Builds the (Departure, ResolvedStop) pair needed by `TripDetailView` from
    /// just a GTFS trip id. Picks the earliest stop in the trip so the trip
    /// detail view scrolls from the start of the run; the view itself centers
    /// the live vehicle position on top of that. Returns nil if the trip isn't
    /// represented in the cached schedule.
    func firstDepartureAndStop(forTripId tripId: String) -> (Departure, ResolvedStop)? {
        guard !tripId.isEmpty,
              let entry = apiDepartureByTripId[tripId],
              let resolved = stop(forId: entry.apiStop.id) else { return nil }
        let route = routeById[entry.departure.routeId]
        return (Departure(from: entry.departure, route: route, headsignMap: operatorConfig?.headsignMap), resolved)
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

    /// Single source of truth for "in how many minutes does this depart".
    /// Always uses the operator's timezone — never the device's — so countdowns
    /// stay consistent across views regardless of where the user is.
    /// Threshold: ≤60 min → minutes countdown, >60 min → absolute clock time.
    ///
    /// When `delayMinutes` is provided (non-nil, plausibility already checked by
    /// the caller via `VehicleStore.reliableDelayMinutes`) the countdown and the
    /// absolute clock are both shifted by that amount via
    /// `departure.shiftedTime(byMinutes:)`. Mirrors DoVe's `liveMinutesUntil()`.
    func timeState(for departure: Departure, delayMinutes: Int? = nil, now: Date = Date()) -> DepartureTimeState {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = operatorTimezone
        let nowMinutes = cal.component(.hour, from: now) * 60 + cal.component(.minute, from: now)
        let delay = delayMinutes ?? 0
        let diff = departure.minutesFromMidnight + delay - nowMinutes
        let displayTime = departure.shiftedTime(byMinutes: delay)
        if diff < 0 { return .passed(displayTime) }
        if diff == 0 { return .departing }
        if diff <= 60 { return .minutes(diff) }
        return .absolute(displayTime)
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
