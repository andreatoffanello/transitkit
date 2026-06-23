import Foundation
import CoreLocation
import Observation

/// Saved address (home / work) for planner shortcuts.
/// Only name + coordinates are stored — MOTIS only needs coordinates for routing.
struct SavedPlace: Codable, Hashable {
    let name: String
    let lat: Double
    let lon: Double

    var coordinate: CLLocationCoordinate2D {
        .init(latitude: lat, longitude: lon)
    }
}

/// Canonical keys for the two saved places. Mirrors Movete's SavedPlaceKey.
enum SavedPlaceKey: String, CaseIterable {
    case home
    case work
}

/// Persists Home and Work addresses for planner shortcuts, per operator.
/// Stored as JSON in UserDefaults (key: `{operatorId}_saved_places`).
@Observable
@MainActor
final class SavedPlacesStore {
    private let defaultsKey: String

    /// Saved places keyed by `SavedPlaceKey.rawValue`.
    private(set) var savedPlaces: [String: SavedPlace] = [:]

    init(operatorId: String) {
        defaultsKey = "\(operatorId)_saved_places"
        if let data = UserDefaults.standard.data(forKey: defaultsKey),
           let decoded = try? JSONDecoder().decode([String: SavedPlace].self, from: data) {
            savedPlaces = decoded
        }
    }

    func savedPlace(_ key: SavedPlaceKey) -> SavedPlace? {
        savedPlaces[key.rawValue]
    }

    func setPlace(_ key: SavedPlaceKey, name: String, coordinate: CLLocationCoordinate2D) {
        savedPlaces[key.rawValue] = SavedPlace(
            name: name,
            lat: coordinate.latitude,
            lon: coordinate.longitude
        )
        persist()
    }

    func removePlace(_ key: SavedPlaceKey) {
        savedPlaces.removeValue(forKey: key.rawValue)
        persist()
    }

    private func persist() {
        if let data = try? JSONEncoder().encode(savedPlaces) {
            UserDefaults.standard.set(data, forKey: defaultsKey)
        }
    }
}
