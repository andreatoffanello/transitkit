# Data Layer — Plan 3: Client Updates

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update iOS and web clients to consume the new API instead of the monolithic CDN JSON, removing all transformation logic from the client layer.

**Architecture:** iOS adds an APIClient that downloads the /schedule bulk endpoint, caches to disk, and registers for Background App Refresh. Web Nuxt server routes are updated to call granular API endpoints per page. GTFS-RT polling is unchanged in both clients.

**Tech Stack:** Swift (iOS), Nuxt 4 / Vue 3 (web)

---

## iOS Tasks

---

## Task 1 — Update `shared/operators/rfta/config.json` schema

Add the `apiUrl` field. This is the only structural change to the config.json format — all existing fields remain unchanged.

- [ ] Open `shared/operators/rfta/config.json` and add `"apiUrl"` as a top-level field, immediately after `"url"`:

```json
{
  "id": "rfta",
  "name": "RFTA",
  "fullName": "Roaring Fork Transportation Authority",
  "url": "https://www.rfta.com",
  "apiUrl": "https://transitkit-api-rfta.vercel.app",
  "region": "Aspen / Glenwood Springs, Colorado",
  "country": "US",
  "timezone": "America/Denver",
  "gtfs_url": "https://www.rfta.com/RFTAGTFSExport.zip",
  "locale": ["en"],
  "theme": {
    "primaryColor": "#1B5E20",
    "accentColor": "#4CAF50",
    "textOnPrimary": "#FFFFFF"
  },
  "store": {
    "title": "RFTA Transit - Aspen Bus",
    "subtitle": "Schedules & live departures for Roaring Fork Valley",
    "keywords": "rfta,aspen,bus,transit,glenwood springs,ski,velocirfta,brt"
  },
  "map": {
    "centerLat": 39.35,
    "centerLng": -107.0,
    "defaultZoom": 10
  },
  "features": {
    "enableMap": true,
    "enableGeolocation": true,
    "enableFavorites": true,
    "enableNotifications": true
  },
  "exclude_patterns": [],
  "terminal_overrides": {}
}
```

The URL format is: `https://{vercel-project-name}.vercel.app` — the root of the deployed `api/` project from Plan 2. No trailing slash. The iOS `APIClient` appends paths directly (e.g. `{apiUrl}/schedule`).

- [ ] Find where `OperatorConfig` is decoded in the iOS codebase and add `apiUrl` as an optional field:

```bash
grep -r "OperatorConfig" /Users/andreatoffanello/GitHub/transit-engine/ios --include="*.swift" -l
```

In the `OperatorConfig` struct, add:

```swift
let apiUrl: String?
```

---

## Task 2 — Create `ios/TransitKit/Sources/Services/APIClient.swift`

Async Swift class that reads `apiUrl` from `OperatorConfig` and provides typed fetch methods for each API endpoint.

- [ ] Create file `ios/TransitKit/Sources/Services/APIClient.swift`:

```swift
import Foundation

// MARK: - API Client

/// Fetches data from the TransitKit API (deployed Vercel Functions).
/// All methods are async and throw on network or decode failure.
/// Caching is handled by `ScheduleLoader` — `APIClient` is a pure HTTP layer.
actor APIClient {
    private let baseURL: URL
    private let session: URLSession

    init(apiUrl: String, session: URLSession = .shared) throws {
        guard let url = URL(string: apiUrl) else {
            throw APIError.invalidBaseURL(apiUrl)
        }
        self.baseURL = url
        self.session = session
    }

    // MARK: - /schedule

    /// Download the bulk schedule payload for iOS.
    /// Returns the full `ScheduleResponse` — routes, stops, and departures by stop.
    func fetchSchedule() async throws -> ScheduleResponse {
        let url = baseURL.appendingPathComponent("schedule")
        return try await fetch(ScheduleResponse.self, from: url)
    }

    // MARK: - /stops/{id}/departures

    /// Fetch upcoming departures from a specific stop.
    /// - Parameters:
    ///   - stopId: The stop's ID as stored in the DB (e.g. "rfta_main_street")
    ///   - date: The date to filter by (defaults to today). Formatted as YYYY-MM-DD.
    ///   - limit: Maximum number of departures to return (default 50, max 200).
    func fetchDepartures(
        stopId: String,
        date: Date = Date(),
        limit: Int = 50
    ) async throws -> [APIDeparture] {
        var components = URLComponents(
            url: baseURL.appendingPathComponent("stops/\(stopId)/departures"),
            resolvingAgainstBaseURL: false
        )!
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        components.queryItems = [
            URLQueryItem(name: "date",  value: formatter.string(from: date)),
            URLQueryItem(name: "limit", value: String(limit)),
        ]
        guard let url = components.url else {
            throw APIError.invalidURL("stops/\(stopId)/departures")
        }
        return try await fetch([APIDeparture].self, from: url)
    }

    // MARK: - /trips/{id}

    /// Fetch the complete stop sequence for a single trip.
    /// Call this on-demand when the user taps a departure to see the full route.
    func fetchTrip(tripId: String) async throws -> TripDetail {
        let url = baseURL.appendingPathComponent("trips/\(tripId)")
        return try await fetch(TripDetail.self, from: url)
    }

    // MARK: - /stops/nearby

    /// Find stops within `radius` metres of the given coordinate.
    /// - Parameters:
    ///   - lat: Latitude in decimal degrees
    ///   - lng: Longitude in decimal degrees
    ///   - radius: Search radius in metres (default 500, max 50000)
    func fetchNearbyStops(lat: Double, lng: Double, radius: Double = 500) async throws -> [APIStopWithDistance] {
        var components = URLComponents(
            url: baseURL.appendingPathComponent("stops/nearby"),
            resolvingAgainstBaseURL: false
        )!
        components.queryItems = [
            URLQueryItem(name: "lat",    value: String(lat)),
            URLQueryItem(name: "lng",    value: String(lng)),
            URLQueryItem(name: "radius", value: String(radius)),
        ]
        guard let url = components.url else {
            throw APIError.invalidURL("stops/nearby")
        }
        return try await fetch([APIStopWithDistance].self, from: url)
    }

    // MARK: - Private

    private func fetch<T: Decodable>(_ type: T.Type, from url: URL) async throws -> T {
        let (data, response) = try await session.data(from: url)
        guard let http = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        switch http.statusCode {
        case 200:
            do {
                return try JSONDecoder().decode(type, from: data)
            } catch {
                throw APIError.decodingFailed(url.path, error)
            }
        case 404:
            throw APIError.notFound(url.path)
        default:
            throw APIError.httpError(http.statusCode, url.path)
        }
    }

    // MARK: - Errors

    enum APIError: LocalizedError {
        case invalidBaseURL(String)
        case invalidURL(String)
        case invalidResponse
        case notFound(String)
        case httpError(Int, String)
        case decodingFailed(String, Error)

        var errorDescription: String? {
            switch self {
            case .invalidBaseURL(let u):        "Invalid API base URL: \(u)"
            case .invalidURL(let path):          "Could not build URL for path: \(path)"
            case .invalidResponse:               "Response was not an HTTP response"
            case .notFound(let path):            "Not found: \(path)"
            case .httpError(let code, let path): "HTTP \(code) for \(path)"
            case .decodingFailed(let path, let err): "Decoding failed for \(path): \(err.localizedDescription)"
            }
        }
    }
}
```

---

## Task 3 — Update `ScheduleData` model

Replace the compact indexed format (`JSONValue` arrays, `headsigns[]`, `lineNames[]`, `routeIds[]`, `tripIds[]`) with direct structs. Add the new API response types used by `APIClient`.

- [ ] Open `ios/TransitKit/Sources/Models/Schedule.swift` and replace the entire file contents with the following:

```swift
import Foundation

// MARK: - API Response Types (new — from /schedule endpoint)

/// Bulk schedule response from GET /schedule.
/// Replaces the old ScheduleData compact format.
struct ScheduleResponse: Codable {
    let operator_: APIOperator
    let lastUpdated: String
    let routes: [APIRoute]
    let stops: [APIStop]
    /// All departures keyed by stop ID. Each array covers all service days.
    let departuresByStop: [String: [APIDeparture]]

    enum CodingKeys: String, CodingKey {
        case operator_ = "operator"
        case lastUpdated, routes, stops, departuresByStop
    }
}

struct APIOperator: Codable {
    let id: String
    let name: String
    let url: String?
    let timezone: String
    let features: [String: Bool]
}

struct APIRoute: Codable, Identifiable, Hashable {
    static func == (lhs: APIRoute, rhs: APIRoute) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }

    let id: String
    let operatorId: String
    let name: String
    let longName: String?
    let color: String?
    let textColor: String?
    let transitType: Int
    let directions: [APIRouteDirection]

    var resolvedTransitType: TransitType {
        TransitType(gtfsRouteType: transitType)
    }
}

struct APIRouteDirection: Codable, Identifiable, Hashable {
    let routeId: String
    let directionId: Int
    let headsign: String?
    let stopIds: [String]
    let shapePolyline: String?

    var id: Int { directionId }
}

struct APIStop: Codable, Identifiable, Hashable {
    static func == (lhs: APIStop, rhs: APIStop) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }

    let id: String
    let operatorId: String
    let name: String
    let lat: Double
    let lng: Double
    let platformCode: String?
    let dockLetter: String?
}

struct APIStopWithDistance: Codable, Identifiable {
    let id: String
    let operatorId: String
    let name: String
    let lat: Double
    let lng: Double
    let platformCode: String?
    let dockLetter: String?
    let distanceMeters: Int
}

struct APIDeparture: Codable, Identifiable {
    let tripId: String
    let routeId: String
    let routeName: String
    let routeColor: String?
    let routeTextColor: String?
    let headsign: String?
    let departureTime: String   // HH:MM:SS
    let serviceDays: [String]

    var id: String { "\(tripId)_\(departureTime)" }

    /// Minutes from midnight for sorting. Handles times > 24:00.
    var minutesFromMidnight: Int {
        let parts = departureTime.split(separator: ":").compactMap { Int($0) }
        guard parts.count >= 2 else { return 0 }
        return parts[0] * 60 + parts[1]
    }
}

struct TripDetail: Codable, Identifiable {
    let id: String
    let operatorId: String
    let routeId: String
    let directionId: Int
    let headsign: String?
    let serviceDays: [String]
    let routeName: String
    let routeColor: String?
    let routeTextColor: String?
    let stopTimes: [APIStopTime]
}

struct APIStopTime: Codable {
    let tripId: String
    let stopId: String
    let arrivalTime: String    // HH:MM:SS
    let departureTime: String  // HH:MM:SS
    let stopSequence: Int
    let stopName: String
    let stopLat: Double
    let stopLng: Double
}

// MARK: - TransitType (kept, with updated initializer)

enum TransitType: String, Codable, CaseIterable {
    case bus
    case tram
    case metro
    case rail
    case ferry
    case cable_tram
    case gondola
    case funicular
    case trolleybus
    case monorail

    /// Initialize from GTFS route_type integer.
    init(gtfsRouteType: Int) {
        switch gtfsRouteType {
        case 0:  self = .tram
        case 1:  self = .metro
        case 2:  self = .rail
        case 3:  self = .bus
        case 4:  self = .ferry
        case 5:  self = .cable_tram
        case 6:  self = .gondola
        case 7:  self = .funicular
        case 11: self = .trolleybus
        case 12: self = .monorail
        default: self = .bus
        }
    }

    var displayName: String {
        switch self {
        case .bus:        "Bus"
        case .tram:       "Tram"
        case .metro:      "Metro"
        case .rail:       "Rail"
        case .ferry:      "Ferry"
        case .cable_tram: "Cable Tram"
        case .gondola:    "Gondola"
        case .funicular:  "Funicular"
        case .trolleybus: "Trolleybus"
        case .monorail:   "Monorail"
        }
    }

    var icon: LucideIcon {
        switch self {
        case .bus, .trolleybus:             .bus
        case .tram, .rail, .monorail:       .train
        case .metro:                        .train
        case .ferry:                        .ship
        case .cable_tram, .gondola, .funicular: .cableCar
        }
    }
}

// MARK: - Departure (UI model — replaces old compact-decoded Departure)

/// A human-readable departure ready for display. Created by ScheduleStore
/// directly from APIDeparture — no index lookups required.
struct Departure: Identifiable, Hashable {
    let id: String
    let time: String           // "07:35"
    let lineName: String       // "BRT"
    let routeId: String
    let headsign: String
    let color: String          // "#c1cd23"
    let textColor: String      // "#000000"
    let transitType: TransitType
    let dock: String           // "" if none
    let tripId: String?

    var minutesFromMidnight: Int {
        let parts = time.split(separator: ":").compactMap { Int($0) }
        guard parts.count == 2 else { return 0 }
        return parts[0] * 60 + parts[1]
    }

    func hash(into hasher: inout Hasher) { hasher.combine(id) }
    static func == (lhs: Departure, rhs: Departure) -> Bool { lhs.id == rhs.id }

    init(from apiDep: APIDeparture, route: APIRoute?) {
        let timeParts = apiDep.departureTime.split(separator: ":")
        let displayHour = timeParts.count >= 1 ? (Int(timeParts[0]) ?? 0) % 24 : 0
        let displayMin  = timeParts.count >= 2 ? (Int(timeParts[1]) ?? 0) : 0
        self.time = String(format: "%02d:%02d", displayHour, displayMin)

        self.id         = "\(self.time)_\(apiDep.routeName)_\(apiDep.headsign ?? "")_\(apiDep.tripId)"
        self.lineName   = apiDep.routeName
        self.routeId    = apiDep.routeId
        self.headsign   = apiDep.headsign ?? ""
        self.color      = apiDep.routeColor ?? "#000000"
        self.textColor  = apiDep.routeTextColor ?? "#FFFFFF"
        self.transitType = route.map { TransitType(gtfsRouteType: $0.transitType) } ?? .bus
        self.dock       = ""
        self.tripId     = apiDep.tripId
    }
}

// MARK: - DayGroup (unchanged — still used by ScheduleStore)

struct DayGroup: Identifiable, Hashable {
    let id: String
    let days: [Weekday]

    var displayLabel: String {
        if days.count == 7 { return String(localized: "every_day") }
        if days.count == 5 && days.allSatisfy({ $0.rawValue < 5 }) {
            return String(localized: "weekdays")
        }
        if days.count == 2 && Set(days) == Set([.sat, .sun]) {
            return String(localized: "weekends")
        }
        if days.count == 1 { return days[0].shortName }
        return days.map(\.shortName).joined(separator: ", ")
    }

    static func parse(_ key: String) -> DayGroup {
        let parts = key.split(separator: ",").map(String.init)
        let days = parts.compactMap { Weekday(abbreviation: $0) }
        return DayGroup(id: key, days: days)
    }
}

enum Weekday: Int, CaseIterable, Hashable {
    case mon = 0, tue, wed, thu, fri, sat, sun

    init?(abbreviation: String) {
        switch abbreviation {
        case "mon": self = .mon
        case "tue": self = .tue
        case "wed": self = .wed
        case "thu": self = .thu
        case "fri": self = .fri
        case "sat": self = .sat
        case "sun": self = .sun
        default: return nil
        }
    }

    var shortName: String {
        switch self {
        case .mon: String(localized: "day_mon")
        case .tue: String(localized: "day_tue")
        case .wed: String(localized: "day_wed")
        case .thu: String(localized: "day_thu")
        case .fri: String(localized: "day_fri")
        case .sat: String(localized: "day_sat")
        case .sun: String(localized: "day_sun")
        }
    }
}
```

**What was removed vs kept:**
- Removed: `ScheduleData`, `OperatorMeta`, `ScheduleStop`, `Route`, `RouteDirection`, `Dock`, `JSONValue`
- Added: `ScheduleResponse`, `APIOperator`, `APIRoute`, `APIRouteDirection`, `APIStop`, `APIStopWithDistance`, `APIDeparture`, `TripDetail`, `APIStopTime`
- Kept: `TransitType` (updated with `init(gtfsRouteType:)`), `Departure` (refactored, no more index fields), `DayGroup`, `Weekday`

---

## Task 4 — Refactor `ScheduleStore.swift`

Change `load()` to call `APIClient.fetchSchedule()` instead of `ScheduleLoader`. Remove all compact array decoding. Keep all computed properties and lookup methods working — they now operate on the new clean types.

- [ ] Open `ios/TransitKit/Sources/Stores/ScheduleStore.swift` and replace the entire file with:

```swift
import Foundation
import SwiftUI

// MARK: - Schedule Store

/// Main data store for the app. Loads schedule data via APIClient and provides
/// resolved stops, routes, and departures to the UI.
/// ScheduleStore is a cache + lookup layer — zero transformation logic.
@MainActor
@Observable
class ScheduleStore {
    // MARK: Published state
    var stops: [ResolvedStop] = []
    var routes: [APIRoute] = []
    var isLoading = false
    var error: String?
    var lastUpdated: String?

    // MARK: Lookup tables (built once on load)
    private(set) var scheduleResponse: ScheduleResponse?
    private var routeById: [String: APIRoute] = [:]
    private var stopById: [String: ResolvedStop] = [:]
    /// Pre-computed stop-sequence strings for each route (first direction).
    private(set) var routeStopSequences: [String: String] = [:]
    /// Pre-computed set of trip IDs per route ID (for GTFS-RT vehicle filtering).
    private(set) var tripIdsByRouteId: [String: Set<String>] = [:]

    private let loader: ScheduleLoader
    private var operatorConfig: OperatorConfig? = nil

    init(operatorId: String, apiUrl: String? = nil) {
        self.loader = ScheduleLoader(operatorId: operatorId, apiUrl: apiUrl)
    }

    /// Supplies the operator config. Call once from the app bootstrap sequence.
    func configure(with config: OperatorConfig) {
        self.operatorConfig = config
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

        // Build route lookup
        routeById = Dictionary(uniqueKeysWithValues: response.routes.map { ($0.id, $0) })
        routes = response.routes

        // Build resolved stops
        stops = response.stops.map { apiStop in
            // Determine which lines serve this stop from the departures dict
            let departuresForStop = response.departuresByStop[apiStop.id] ?? []
            let lineNames = Array(Set(departuresForStop.map(\.routeName))).sorted()
            let transitTypes = Set(departuresForStop.compactMap { dep in
                routeById[dep.routeId].map { TransitType(gtfsRouteType: $0.transitType) }
            })
            return ResolvedStop(
                id: apiStop.id,
                name: apiStop.name,
                lat: apiStop.lat,
                lng: apiStop.lng,
                lineNames: lineNames,
                transitTypes: transitTypes.isEmpty ? [.bus] : transitTypes,
                docks: []  // dock info available via apiStop.dockLetter if needed
            )
        }

        stopById = Dictionary(uniqueKeysWithValues: stops.map { ($0.id, $0) })

        // Pre-cache stop-sequence strings for MarqueeText (first direction per route)
        routeStopSequences = Dictionary(uniqueKeysWithValues: routes.compactMap { route in
            guard let dir = route.directions.first else { return nil }
            let names = dir.stopIds.compactMap { stopById[$0]?.name }
            guard !names.isEmpty else { return nil }
            return (route.id, names.joined(separator: " → "))
        })

        // Build trip ID → route ID mapping for GTFS-RT vehicle filtering
        var tripMap: [String: Set<String>] = [:]
        for (_, departures) in response.departuresByStop {
            for dep in departures {
                tripMap[dep.routeId, default: []].insert(dep.tripId)
            }
        }
        tripIdsByRouteId = tripMap
    }

    // MARK: - Departures for a stop

    /// Returns all departures for a stop, grouped by day.
    func departures(forStopId stopId: String) -> [DayGroup: [Departure]] {
        guard let response = scheduleResponse else { return [:] }
        let apiDeps = response.departuresByStop[stopId] ?? []
        if apiDeps.isEmpty { return [:] }

        // Group by service_days signature
        var grouped: [String: [APIDeparture]] = [:]
        for dep in apiDeps {
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

    /// Returns all stops served by a route, in direction order.
    func stopsForRoute(_ routeId: String, directionId: Int) -> [ResolvedStop] {
        guard let route = routeById[routeId],
              let direction = route.directions.first(where: { $0.directionId == directionId })
        else { return [] }
        return direction.stopIds.compactMap { stopById[$0] }
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

    private func parseDayGroup(from serviceDaysKey: String) -> DayGroup {
        // serviceDaysKey is like "monday,tuesday,wednesday,thursday,friday"
        // Convert GTFS day names to Weekday abbreviations
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
        case .mon: "mon"
        case .tue: "tue"
        case .wed: "wed"
        case .thu: "thu"
        case .fri: "fri"
        case .sat: "sat"
        case .sun: "sun"
        }
    }

    private func currentWeekday() -> Weekday {
        let cal = Calendar.current
        switch cal.component(.weekday, from: Date()) {
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

// MARK: - Resolved Stop (UI-ready, unchanged)

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

/// Lightweight dock model for ResolvedStop. The full dock detail
/// (dockLetter) is available directly on APIStop if needed.
struct APIDock: Codable, Hashable {
    let letter: String
}
```

**What changed vs old ScheduleStore.swift:**
- `init(operatorId:cdnBaseURL:)` → `init(operatorId:apiUrl:)` — pass apiUrl instead of CDN URL
- `load()` and `refresh()` now call `loader.load()` / `loader.refresh()` returning `ScheduleResponse` instead of `ScheduleData`
- `apply(_:)` takes `ScheduleResponse` — no index arrays, no `JSONValue` decoding
- `departures(forStopId:)` reads directly from `response.departuresByStop[stopId]` — no compact array decoding loop
- `route.directions.first(where:)` uses `.directionId` instead of `.id`
- `tripStops(patternIndex:)` removed — pattern index no longer exists; use `APIClient.fetchTrip()` on-demand
- All other public methods (`upcomingDepartures`, `stopsForRoute`, `nearbyStops`, etc.) are behaviorally identical

---

## Task 5 — Register Background App Refresh

In the app entry point or scene delegate, register a `BGAppRefreshTaskRequest` so iOS can update the schedule cache while the app is in the background.

- [ ] Find the app's entry point. Search for the `@main` attribute:

```bash
grep -r "@main" /Users/andreatoffanello/GitHub/transit-engine/ios --include="*.swift" -l
```

- [ ] In that file, add the `BackgroundTasks` import and register the task identifier. The complete pattern to add (adapt to the existing `@main` struct/class):

```swift
import BackgroundTasks

// Inside the App struct, add this to the app body or init:
// Register the background task identifier. This must be called before
// the app finishes launching (i.e., in init or the scene phase handler).
static let scheduleRefreshIdentifier = "com.transitkit.schedule-refresh"
```

- [ ] Add the background task registration. In the `scene(_:willConnectTo:options:)` or `.onAppear` modifier on the root view, add:

```swift
// Register Background App Refresh task
BGTaskScheduler.shared.register(
    forTaskWithIdentifier: "com.transitkit.schedule-refresh",
    using: nil
) { task in
    guard let refreshTask = task as? BGAppRefreshTask else {
        task.setTaskCompleted(success: false)
        return
    }
    handleScheduleRefresh(refreshTask)
}
```

- [ ] Implement `handleScheduleRefresh` as a free function or method in the same file:

```swift
/// Called by the system to refresh the schedule cache in the background.
/// The task expires after ~30 seconds — we must call setTaskCompleted before then.
func handleScheduleRefresh(_ task: BGAppRefreshTask) {
    // Schedule the next refresh before starting work
    scheduleNextBackgroundRefresh()

    let refreshOperation = Task {
        do {
            // Re-use the shared ScheduleStore from the app environment if accessible,
            // or create a standalone APIClient for the background context.
            // This example creates a standalone APIClient directly.
            guard let apiUrl = OperatorConfigLoader.shared.config?.apiUrl,
                  let operatorId = OperatorConfigLoader.shared.config?.id
            else {
                task.setTaskCompleted(success: false)
                return
            }

            let client = try APIClient(apiUrl: apiUrl)
            let response = try await client.fetchSchedule()

            // Persist to disk so ScheduleLoader finds it on next foreground launch
            let loader = ScheduleLoader(operatorId: operatorId, apiUrl: apiUrl)
            await loader.saveResponseToDisk(response)

            task.setTaskCompleted(success: true)
        } catch {
            task.setTaskCompleted(success: false)
        }
    }

    // If the system cancels the task (time limit), cancel our work
    task.expirationHandler = {
        refreshOperation.cancel()
    }
}

func scheduleNextBackgroundRefresh() {
    let request = BGAppRefreshTaskRequest(
        identifier: "com.transitkit.schedule-refresh"
    )
    // Request refresh no sooner than 6 hours from now
    request.earliestBeginDate = Date(timeIntervalSinceNow: 6 * 60 * 60)
    try? BGTaskScheduler.shared.submit(request)
}
```

- [ ] Add `"com.transitkit.schedule-refresh"` to the `BGTaskSchedulerPermittedIdentifiers` array in the app's `Info.plist`:

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>com.transitkit.schedule-refresh</string>
</array>
```

---

## Task 6 — Update disk cache in `ScheduleLoader.swift`

Update `ScheduleLoader` to save/load `ScheduleResponse` instead of the old `ScheduleData`. Add a `saveResponseToDisk` method (called from the background refresh handler). Freshness check uses `lastUpdated`.

- [ ] Open `ios/TransitKit/Sources/Services/ScheduleLoader.swift` and replace the entire file with:

```swift
import Foundation

// MARK: - Schedule Loader

/// Loads and caches the operator's schedule data.
/// Cache format: ScheduleResponse (new API format).
/// On first launch: downloads from API.
/// On subsequent launches: loads from disk cache, checks freshness in background.
actor ScheduleLoader {
    private var cached: ScheduleResponse?
    private let operatorId: String
    private let apiUrl: String?

    init(operatorId: String, apiUrl: String? = nil) {
        self.operatorId = operatorId
        self.apiUrl = apiUrl
    }

    /// Load schedule data. Tries: memory cache → disk cache → API download.
    func load() async throws -> ScheduleResponse {
        // 1. Memory cache
        if let cached { return cached }

        // 2. Disk cache
        if let diskData = loadFromDisk() {
            cached = diskData
            // Check for updates in background (don't block the UI)
            Task { await checkForUpdates() }
            return diskData
        }

        // 3. API download (first launch — no bundle fallback in new architecture)
        let downloaded = try await downloadFromAPI()
        cached = downloaded
        saveToDisk(downloaded)
        return downloaded
    }

    /// Force-refresh from API, update memory cache and disk.
    func refresh() async throws -> ScheduleResponse {
        let data = try await downloadFromAPI()
        cached = data
        saveToDisk(data)
        return data
    }

    /// Called by the Background App Refresh handler to persist a
    /// freshly-downloaded ScheduleResponse without going through load().
    func saveResponseToDisk(_ response: ScheduleResponse) {
        cached = response
        saveToDisk(response)
    }

    // MARK: - Disk Cache

    private var cacheURL: URL {
        let dir = FileManager.default.urls(
            for: .applicationSupportDirectory, in: .userDomainMask
        )[0].appendingPathComponent("TransitKit", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("\(operatorId)_schedule_v2.json")
        // Note: "_v2" suffix avoids loading old ScheduleData format from cache
    }

    private func loadFromDisk() -> ScheduleResponse? {
        guard let data = try? Data(contentsOf: cacheURL) else { return nil }
        return try? JSONDecoder().decode(ScheduleResponse.self, from: data)
    }

    private func saveToDisk(_ response: ScheduleResponse) {
        guard let data = try? JSONEncoder().encode(response) else { return }
        try? data.write(to: cacheURL, options: .atomic)
    }

    // MARK: - API Download

    private func downloadFromAPI() async throws -> ScheduleResponse {
        guard let apiUrl else {
            throw ScheduleError.noAPIURLConfigured
        }
        let client = try APIClient(apiUrl: apiUrl)
        return try await client.fetchSchedule()
    }

    private func checkForUpdates() async {
        guard let newData = try? await downloadFromAPI() else { return }
        // Only update if the API reports a newer lastUpdated timestamp
        if newData.lastUpdated != cached?.lastUpdated {
            cached = newData
            saveToDisk(newData)
        }
    }

    // MARK: - Errors

    enum ScheduleError: LocalizedError {
        case noAPIURLConfigured
        case downloadFailed

        var errorDescription: String? {
            switch self {
            case .noAPIURLConfigured: "No API URL configured for this operator"
            case .downloadFailed:     "Failed to download schedule"
            }
        }
    }
}
```

**What changed vs old ScheduleLoader.swift:**
- `init(operatorId:cdnBaseURL:)` → `init(operatorId:apiUrl:)` — parameter renamed
- Cache type changed from `ScheduleData` to `ScheduleResponse`
- Cache filename changed to `{operatorId}_schedule_v2.json` (avoids loading old format)
- `loadFromBundle()` removed — bundle fallback is no longer used; API is the source of truth
- `downloadFromCDN()` replaced by `downloadFromAPI()` using `APIClient.fetchSchedule()`
- `saveResponseToDisk(_:)` added as public method for background refresh handler
- All other behavior (memory cache, disk cache, background update check) is preserved

---

## Web Tasks

---

## Task 7 — Update `shared/operators/rfta/config.json`

Same change as iOS Task 1. The `apiUrl` field is shared between iOS and web — both read from the same `config.json`.

- [ ] Confirm `shared/operators/rfta/config.json` has `"apiUrl": "https://transitkit-api-rfta.vercel.app"` as added in Task 1. No additional changes needed for web.

---

## Task 8 — Update `web/composables/useOperator.ts`

Remove the bulk `schedules.json` download. Expose `apiUrl` from config. Pages fetch their own data via the API.

- [ ] Open `web/composables/useOperator.ts` and replace the entire file with:

```typescript
import { fetchWithRetry } from '~/utils/fetchWithRetry'
import type { OperatorConfig } from '~/types'

// Module-level controller: aborts in-flight config fetches on rapid re-navigation.
let currentAbortController: AbortController | null = null

async function fetchJson<T>(url: string, signal: AbortSignal): Promise<T> {
  const res = await fetchWithRetry(url, 3, 1000, signal)
  if (!res.ok) {
    throw Object.assign(new Error(`HTTP ${res.status} fetching ${url}`), { status: res.status })
  }
  return res.json() as Promise<T>
}

export async function useOperator() {
  const operatorId = useState<string>('operatorId')
  const id = operatorId.value
  const { public: { cdnBase } } = useRuntimeConfig()

  // Cancel any in-flight fetch session from a previous navigation
  currentAbortController?.abort()
  currentAbortController = new AbortController()
  const { signal } = currentAbortController

  // Fetch operator config only. Schedule data is no longer downloaded here.
  // Each page calls its own API endpoint via useAsyncData.
  const configAsync = useAsyncData<OperatorConfig>(
    'operator-config',
    async () => {
      const cfg = await fetchJson<OperatorConfig>(`${cdnBase}/${id}/config.json`, signal)
      // Normalize hex colors to lowercase so SSR style output matches client hydration.
      if (cfg.theme) {
        cfg.theme.primaryColor = cfg.theme.primaryColor.toLowerCase()
        cfg.theme.accentColor = cfg.theme.accentColor.toLowerCase()
        cfg.theme.textOnPrimary = cfg.theme.textOnPrimary.toLowerCase()
      }
      return cfg
    },
  )

  const { data: config, error: configError } = await configAsync

  if (configError.value) {
    const err = configError.value
    if (err instanceof Error && err.name === 'AbortError') {
      currentAbortController = null
      const pending = computed(() => !config.value)
      return { operatorId, config, pending }
    }
    const status = (err as { status?: number }).status
    const statusCode = status === 404 ? 404 : 502
    const statusMessage = statusCode === 404
      ? 'Operator not found'
      : 'Impossibile caricare i dati. Riprova tra qualche minuto.'
    throw createError({ statusCode, statusMessage })
  }

  currentAbortController = null

  const pending = computed(() => !config.value)

  // apiUrl is read from config.apiUrl by each page — no need to expose it separately.
  return { operatorId, config, pending }
}
```

**What changed vs old useOperator.ts:**
- Removed: `schedulesAsync` — the bulk `schedules.json` download is gone
- Removed: `schedules` from the return value
- Kept: `config` fetch, `operatorId`, `pending`, error handling, abort controller, hex color normalization
- The `config.value.apiUrl` field is now available to all pages via the returned `config` ref

- [ ] Update the `OperatorConfig` TypeScript type to include `apiUrl`. Find the type definition:

```bash
grep -r "OperatorConfig" /Users/andreatoffanello/GitHub/transit-engine/web --include="*.ts" -l
```

In that file, add `apiUrl` to the interface:

```typescript
export interface OperatorConfig {
  id: string
  name: string
  fullName?: string
  url?: string
  apiUrl: string          // <-- add this field
  region?: string
  country?: string
  timezone: string
  locale: string[]
  theme: {
    primaryColor: string
    accentColor: string
    textOnPrimary: string
  }
  store?: {
    title: string
    subtitle: string
    keywords: string
  }
  map: {
    centerLat: number
    centerLng: number
    defaultZoom: number
  }
  features: {
    enableMap: boolean
    enableGeolocation: boolean
    enableFavorites: boolean
    enableNotifications: boolean
  }
}
```

---

## Task 9 — Update stop page data fetching

The stop detail page calls `{apiUrl}/stops/{id}/departures` instead of reading from the bulk schedule.

- [ ] Find the stop detail page file:

```bash
find /Users/andreatoffanello/GitHub/transit-engine/web -name "*.vue" | xargs grep -l "stopId\|stop_id\|departures" 2>/dev/null
```

- [ ] In the stop detail page `<script setup>`, replace the schedule-based data fetching with a direct API call. The new pattern is:

```typescript
<script setup lang="ts">
import type { APIDeparture } from '~/types/api'

const { config } = await useOperator()
const route = useRoute()
const stopId = route.params.id as string

// Fetch departures for today from the API
const { data: departures, pending, error, refresh } = await useAsyncData<APIDeparture[]>(
  `departures-${stopId}`,
  async () => {
    const apiUrl = config.value?.apiUrl
    if (!apiUrl) throw new Error('apiUrl not configured')

    const today = new Date().toISOString().slice(0, 10)  // YYYY-MM-DD
    const res = await fetch(
      `${apiUrl}/stops/${encodeURIComponent(stopId)}/departures?date=${today}&limit=100`
    )
    if (!res.ok) {
      if (res.status === 404) throw createError({ statusCode: 404, statusMessage: 'Stop not found' })
      throw new Error(`HTTP ${res.status}`)
    }
    return res.json() as Promise<APIDeparture[]>
  },
  { server: true }
)
</script>
```

- [ ] Add the `APIDeparture` type to `web/types/api.ts` (create this file if it does not exist):

```typescript
// web/types/api.ts
// TypeScript types matching the TransitKit API responses.

export interface APIDeparture {
  tripId: string
  routeId: string
  routeName: string
  routeColor: string | null
  routeTextColor: string | null
  headsign: string | null
  departureTime: string   // HH:MM:SS
  serviceDays: string[]
}

export interface APIStop {
  id: string
  operatorId: string
  name: string
  lat: number
  lng: number
  platformCode: string | null
  dockLetter: string | null
}

export interface APIStopWithDistance extends APIStop {
  distanceMeters: number
}

export interface APIRoute {
  id: string
  operatorId: string
  name: string
  longName: string | null
  color: string | null
  textColor: string | null
  transitType: number
}

export interface APIRouteDirection {
  routeId: string
  directionId: number
  headsign: string | null
  stopIds: string[]
  shapePolyline: string | null
}

export interface APIRouteWithDirections extends APIRoute {
  directions: APIRouteDirection[]
}
```

---

## Task 10 — Update lines/routes page

The routes list page calls `{apiUrl}/routes` instead of reading routes from the bulk schedule.

- [ ] Find the routes list page file:

```bash
find /Users/andreatoffanello/GitHub/transit-engine/web -name "*.vue" | xargs grep -l "routes\|linee\|lines" 2>/dev/null | head -5
```

- [ ] In the routes list page `<script setup>`, replace the schedule-based data fetching with:

```typescript
<script setup lang="ts">
import type { APIRoute } from '~/types/api'

const { config } = await useOperator()

const { data: routes, pending, error } = await useAsyncData<APIRoute[]>(
  'routes-list',
  async () => {
    const apiUrl = config.value?.apiUrl
    if (!apiUrl) throw new Error('apiUrl not configured')

    const res = await fetch(`${apiUrl}/routes`)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    return res.json() as Promise<APIRoute[]>
  },
  { server: true }
)
</script>
```

- [ ] In the template, update any references from the old `schedules.value.routes` shape to `routes.value`. The `APIRoute` fields are:
  - `id`, `name`, `longName`, `color`, `textColor`, `transitType` — same names as before (camelCase)

---

## Task 11 — Update line detail page

The line detail page calls `{apiUrl}/routes/{id}` instead of reading from the bulk schedule.

- [ ] Find the line detail page file:

```bash
find /Users/andreatoffanello/GitHub/transit-engine/web -name "*.vue" | xargs grep -l "route.*detail\|linedetail\|routeId\|route_id" 2>/dev/null | head -5
```

- [ ] In the line detail page `<script setup>`, replace the schedule-based data fetching with:

```typescript
<script setup lang="ts">
import type { APIRouteWithDirections } from '~/types/api'

const { config } = await useOperator()
const route = useRoute()
const routeId = route.params.id as string

const { data: routeDetail, pending, error } = await useAsyncData<APIRouteWithDirections>(
  `route-detail-${routeId}`,
  async () => {
    const apiUrl = config.value?.apiUrl
    if (!apiUrl) throw new Error('apiUrl not configured')

    const res = await fetch(`${apiUrl}/routes/${encodeURIComponent(routeId)}`)
    if (!res.ok) {
      if (res.status === 404) throw createError({ statusCode: 404, statusMessage: 'Route not found' })
      throw new Error(`HTTP ${res.status}`)
    }
    return res.json() as Promise<APIRouteWithDirections>
  },
  { server: true }
)
</script>
```

- [ ] The `APIRouteWithDirections` type includes `directions: APIRouteDirection[]`. Each direction has:
  - `directionId: number` — used for tab switching
  - `headsign: string | null` — direction label
  - `stopIds: string[]` — ordered stop IDs for the stop list
  - `shapePolyline: string | null` — encoded polyline for the map (null if not available)

  Update template references from the old `route.directions[n].id` to `route.directions[n].directionId`.
