import Foundation

// MARK: - API Response Types (from /schedule endpoint)

/// Bulk schedule response from GET /schedule.
struct ScheduleResponse: Codable {
    let operator_: APIOperator
    let lastUpdated: String
    let routes: [APIRoute]
    let stops: [APIStop]

    enum CodingKeys: String, CodingKey {
        case operator_ = "operator"
        case lastUpdated, routes, stops
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
    let routeId: String?
    let directionId: Int
    let headsign: String?
    let stopIds: [String]
    let shapePolyline: String?

    var id: Int { directionId }

    enum CodingKeys: String, CodingKey {
        case routeId, directionId, headsign, stopIds, shapePolyline
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        routeId = try c.decodeIfPresent(String.self, forKey: .routeId)
        directionId = try c.decodeIfPresent(Int.self, forKey: .directionId) ?? 0
        headsign = try c.decodeIfPresent(String.self, forKey: .headsign)
        stopIds = try c.decodeIfPresent([String].self, forKey: .stopIds) ?? []
        shapePolyline = try c.decodeIfPresent(String.self, forKey: .shapePolyline)
    }
}

struct APIStop: Codable, Identifiable, Hashable {
    static func == (lhs: APIStop, rhs: APIStop) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }

    let id: String
    let name: String
    let lat: Double
    let lng: Double
    let platformCode: String?
    let dockLetter: String?
    let departures: [APIDeparture]
}

struct APIStopWithDistance: Codable, Identifiable {
    let id: String
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
    let arrivalTime: String
    let departureTime: String
    let stopSequence: Int
    let stopName: String
    let stopLat: Double
    let stopLng: Double
}

// MARK: - TransitType

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

// MARK: - Departure (UI model — created by ScheduleStore from APIDeparture)

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
        let t = String(format: "%02d:%02d", displayHour, displayMin)
        self.time = t
        self.id         = "\(t)_\(apiDep.routeName)_\(apiDep.headsign ?? "")_\(apiDep.tripId)"
        self.lineName   = apiDep.routeName
        self.routeId    = apiDep.routeId
        self.headsign   = HeadsignNormalizer.resolve(apiDep.headsign ?? "", route: route)
        self.color      = apiDep.routeColor.map { "#\($0)" } ?? "#000000"
        self.textColor  = apiDep.routeTextColor.map { "#\($0)" } ?? "#FFFFFF"
        self.transitType = route.map { TransitType(gtfsRouteType: $0.transitType) } ?? .bus
        self.dock       = ""
        self.tripId     = apiDep.tripId
    }
}

// MARK: - DayGroup / Weekday (unchanged)

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
