import Foundation

// MARK: - Resolved Departure (decoded from compact format)

/// A human-readable departure, resolved from the compact indexed format.
/// Created by `ScheduleStore` when loading departures for a specific stop.
struct Departure: Identifiable, Hashable {
    let id: String  // "{time}_{lineName}_{headsign}"
    let time: String         // "07:35"
    let lineName: String     // "BRT"
    let routeId: String      // "BRT"
    let headsign: String     // "Glenwood-27th St"
    let color: String        // "#c1cd23"
    let textColor: String    // "#000000"
    let transitType: TransitType
    let dock: String         // "" if none
    let patternIndex: Int?   // index into stopPatterns
    let tripIdIndex: Int?    // index into tripIds

    /// Minutes from midnight (for sorting; handles >24h for after-midnight)
    var minutesFromMidnight: Int {
        let parts = time.split(separator: ":")
        guard parts.count == 2, let h = Int(parts[0]), let m = Int(parts[1]) else { return 0 }
        return h * 60 + m
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }

    static func == (lhs: Departure, rhs: Departure) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Day Group

/// Represents a group of days with the same schedule (e.g. "mon,tue,wed,thu,fri").
struct DayGroup: Identifiable, Hashable {
    let id: String  // raw key like "mon,tue,wed,thu,fri"
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
        // Group consecutive days: "Mon–Fri"
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
