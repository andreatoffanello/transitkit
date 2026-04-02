import Foundation
import SwiftUI

// MARK: - FavoritesManager

/// Manages favorite stop IDs, persisted in UserDefaults.
/// Uses the `@Observable` pattern for seamless SwiftUI integration.
///
/// Usage:
/// ```swift
/// let manager = FavoritesManager()
/// manager.toggle("stop_123")
/// manager.isFavorite("stop_123") // true
/// ```
@MainActor
@Observable
final class FavoritesManager {
    // MARK: State

    /// Ordered list of favorite stop IDs (most recently added first).
    private(set) var favoriteStopIds: [String] = []

    // MARK: Private

    private let key: String
    private let defaults: UserDefaults

    // MARK: Init

    /// Creates a favorites manager.
    /// - Parameters:
    ///   - suiteName: Optional app group suite name for shared UserDefaults.
    ///   - operatorId: Operator identifier for namespacing the storage key.
    init(operatorId: String = "default", defaults: UserDefaults = .standard) {
        self.key = "favorites_stops_\(operatorId)"
        self.defaults = defaults
        self.favoriteStopIds = defaults.stringArray(forKey: key) ?? []
    }

    // MARK: Queries

    /// Returns `true` if the given stop ID is in the favorites list.
    func isFavorite(_ stopId: String) -> Bool {
        favoriteStopIds.contains(stopId)
    }

    // MARK: Mutations

    /// Toggles a stop ID in/out of favorites. Returns the new state.
    @discardableResult
    func toggle(_ stopId: String) -> Bool {
        if let index = favoriteStopIds.firstIndex(of: stopId) {
            favoriteStopIds.remove(at: index)
            save()
            return false
        } else {
            favoriteStopIds.insert(stopId, at: 0)
            save()
            return true
        }
    }

    /// Adds a stop ID to favorites if not already present.
    func add(_ stopId: String) {
        guard !favoriteStopIds.contains(stopId) else { return }
        favoriteStopIds.insert(stopId, at: 0)
        save()
    }

    /// Removes a stop ID from favorites.
    func remove(_ stopId: String) {
        guard let index = favoriteStopIds.firstIndex(of: stopId) else { return }
        favoriteStopIds.remove(at: index)
        save()
    }

    /// Removes all favorites.
    func removeAll() {
        favoriteStopIds.removeAll()
        save()
    }

    // MARK: Persistence

    private func save() {
        defaults.set(favoriteStopIds, forKey: key)
    }
}
