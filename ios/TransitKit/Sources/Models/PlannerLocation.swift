import Foundation
import CoreLocation

/// Posizione unificata per il journey planner — accetta fermate GTFS,
/// punti liberi sulla mappa (geocodati) e la posizione GPS dell'utente.
///
/// MOTIS pianifica direttamente da lat/lng e calcola la walking leg fino alla
/// fermata più vicina, quindi a query time:
///   - `.stop` → usa `stopId` + lat/lng del stop
///   - `.place` / `.userLocation` → passa le coordinate scelte dall'utente, niente snap
///
/// Un service-area guard in `PlannerScreen.triggerSearch` rifiuta coordinate libere
/// oltre una soglia di distanza dalla fermata più vicina (vedi
/// `maxDistanceFromServiceAreaMeters`).
struct PlannerLocation: Identifiable, Hashable {
    enum Kind: String, Hashable {
        case stop          // GTFS stop scelta dall'utente
        case place         // pin libero su mappa / indirizzo geocodato
        case userLocation  // fix GPS attuale
    }

    let kind: Kind
    let name: String
    let coordinate: CLLocationCoordinate2D
    /// Stop ID GTFS, presente solo quando `kind == .stop`.
    let stopId: String?

    var id: String {
        switch kind {
        case .stop:
            return "stop-\(stopId ?? "?")"
        case .place:
            return "place-\(String(format: "%.6f,%.6f", coordinate.latitude, coordinate.longitude))"
        case .userLocation:
            return "user-loc"
        }
    }

    // MARK: - Factories

    static func stop(_ s: ResolvedStop) -> PlannerLocation {
        PlannerLocation(
            kind: .stop,
            name: s.name,
            coordinate: CLLocationCoordinate2D(latitude: s.lat, longitude: s.lng),
            stopId: s.id
        )
    }

    static func place(name: String, coordinate: CLLocationCoordinate2D) -> PlannerLocation {
        PlannerLocation(kind: .place, name: name, coordinate: coordinate, stopId: nil)
    }

    static func userLocation(name: String, coordinate: CLLocationCoordinate2D) -> PlannerLocation {
        PlannerLocation(kind: .userLocation, name: name, coordinate: coordinate, stopId: nil)
    }

    // MARK: - Hashable / Equatable

    static func == (lhs: PlannerLocation, rhs: PlannerLocation) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}
