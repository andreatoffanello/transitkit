import Foundation

// MARK: - PlannerStop
// Lightweight stop representation used in journey models.
// Decoupled from ResolvedStop so the routing engine is self-contained.

struct PlannerStop: Identifiable, Hashable {
    let id: String
    let name: String
    let lat: Double
    let lng: Double

    static func == (lhs: PlannerStop, rhs: PlannerStop) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

// MARK: - Journey

struct Journey: Identifiable, Hashable {
    static func == (lhs: Journey, rhs: Journey) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }

    let id: UUID
    let legs: [Leg]
    let departureTime: Date
    let arrivalTime: Date
    let totalWalkSeconds: Int

    var transfers: Int { max(0, transitLegs.count - 1) }

    var durationMinutes: Int {
        Int(arrivalTime.timeIntervalSince(departureTime) / 60)
    }

    var transitLegs: [TransitLeg] {
        legs.compactMap { if case .transit(let l) = $0 { return l } else { return nil } }
    }

    var minutesUntilDeparture: Int {
        let diff = departureTime.timeIntervalSinceNow
        return max(0, Int(diff / 60))
    }
}

// MARK: - Leg

enum Leg: Identifiable {
    case transit(TransitLeg)
    case walking(WalkingLeg)

    var id: String {
        switch self {
        case .transit(let l): "t_\(l.id)"
        case .walking(let l): "w_\(l.id)"
        }
    }

    var durationSeconds: Int {
        switch self {
        case .transit(let l): max(0, Int(l.alightTime.timeIntervalSince(l.boardTime)))
        case .walking(let l): l.walkSeconds
        }
    }

    var routeColor: String? {
        switch self {
        case .transit(let l): l.routeColor
        case .walking: nil
        }
    }
}

// MARK: - TransitLeg

struct TransitLeg: Identifiable {
    let id: UUID
    let lineName: String
    /// Hex without #, e.g. "AD2B3C". May be "000000" if unknown.
    let routeColor: String
    /// Hex without #. Auto-derived from routeColor if empty.
    let routeTextColor: String
    let headsign: String
    let boardStop: PlannerStop
    let alightStop: PlannerStop
    let boardTime: Date
    let alightTime: Date
    let tripId: String
    let intermediateStops: [IntermediateStop]
}

// MARK: - IntermediateStop

struct IntermediateStop: Identifiable {
    let id: String   // stopId
    let name: String
    let time: String // "HH:MM"
}

// MARK: - WalkingLeg

struct WalkingLeg: Identifiable {
    let id: UUID
    let fromStop: PlannerStop
    let toStop: PlannerStop
    let walkSeconds: Int
}
