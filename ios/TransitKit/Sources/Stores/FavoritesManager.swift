import Foundation
import SwiftUI

// MARK: - FavoritesManager

/// Manages favorite stop IDs and route IDs, persisted in UserDefaults.
/// Uses the `@Observable` pattern for seamless SwiftUI integration.
///
/// Route favorites are mirrored to FCM topic subscriptions via
/// `PushNotificationManager` — toggling a route on/off updates the device's
/// per-line push subscription. Stops are local-only (no per-stop push v1).
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
    /// Ordered list of favorite route IDs (most recently added first).
    private(set) var favoriteRouteIds: [String] = []

    // MARK: Private

    private let stopsKey: String
    private let routesKey: String
    private let defaults: UserDefaults
    private weak var pushManager: PushNotificationManager?

    // MARK: Init

    init(
        operatorId: String = "default",
        defaults: UserDefaults = .standard,
        pushManager: PushNotificationManager? = nil
    ) {
        self.stopsKey = "favorites_stops_\(operatorId)"
        self.routesKey = "favorites_routes_\(operatorId)"
        self.defaults = defaults
        self.pushManager = pushManager
        self.favoriteStopIds = defaults.stringArray(forKey: stopsKey) ?? []
        self.favoriteRouteIds = defaults.stringArray(forKey: routesKey) ?? []
    }

    // MARK: Queries — stops

    func isFavorite(_ stopId: String) -> Bool {
        favoriteStopIds.contains(stopId)
    }

    // MARK: Mutations — stops

    @discardableResult
    func toggle(_ stopId: String) -> Bool {
        if let index = favoriteStopIds.firstIndex(of: stopId) {
            favoriteStopIds.remove(at: index)
            saveStops()
            return false
        } else {
            favoriteStopIds.insert(stopId, at: 0)
            saveStops()
            return true
        }
    }

    func add(_ stopId: String) {
        guard !favoriteStopIds.contains(stopId) else { return }
        favoriteStopIds.insert(stopId, at: 0)
        saveStops()
    }

    func remove(_ stopId: String) {
        guard let index = favoriteStopIds.firstIndex(of: stopId) else { return }
        favoriteStopIds.remove(at: index)
        saveStops()
    }

    func removeAll() {
        favoriteStopIds.removeAll()
        saveStops()
    }

    // MARK: Queries — routes

    func isFavoriteRoute(_ routeId: String) -> Bool {
        favoriteRouteIds.contains(routeId)
    }

    // MARK: Mutations — routes

    @discardableResult
    func toggleRoute(_ routeId: String) -> Bool {
        if let index = favoriteRouteIds.firstIndex(of: routeId) {
            favoriteRouteIds.remove(at: index)
            saveRoutes()
            Task { [pushManager] in await pushManager?.unsubscribeRoute(routeId) }
            return false
        } else {
            favoriteRouteIds.insert(routeId, at: 0)
            saveRoutes()
            Task { [pushManager] in await pushManager?.subscribeRoute(routeId) }
            return true
        }
    }

    // MARK: Persistence

    private func saveStops() {
        defaults.set(favoriteStopIds, forKey: stopsKey)
    }

    private func saveRoutes() {
        defaults.set(favoriteRouteIds, forKey: routesKey)
    }
}
