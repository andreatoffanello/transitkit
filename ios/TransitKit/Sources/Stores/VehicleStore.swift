import Foundation

// MARK: - Vehicle Store

/// Global real-time vehicle feed. Polls every 15s and provides indexed lookups.
/// Shared across all views via @Environment. Gracefully handles missing GTFS-RT URL.
@MainActor
@Observable
final class VehicleStore {
    // MARK: State
    private(set) var vehicles: [GtfsRtVehicle] = []
    private(set) var lastFetchedAt: Date? = nil

    // MARK: Private
    private let vehiclePositionsUrl: String?
    private var pollTask: Task<Void, Never>?

    /// Indexed for O(1) lookup: trip_id → vehicle
    private var vehicleByTripId: [String: GtfsRtVehicle] = [:]
    /// Indexed for O(1) lookup: route_id → vehicles
    private var vehiclesByRouteId: [String: [GtfsRtVehicle]] = [:]

    init(vehiclePositionsUrl: String?) {
        self.vehiclePositionsUrl = vehiclePositionsUrl
    }

    // MARK: - Lifecycle

    func startPolling() {
        guard vehiclePositionsUrl != nil else { return }
        pollTask?.cancel()
        pollTask = Task { [weak self] in
            while !Task.isCancelled {
                await self?.fetch()
                try? await Task.sleep(for: .seconds(15))
            }
        }
    }

    func stopPolling() {
        pollTask?.cancel()
        pollTask = nil
    }

    // MARK: - Lookups

    /// Returns the live vehicle for a specific trip ID, or nil if not tracked.
    func vehicle(forTripId tripId: String) -> GtfsRtVehicle? {
        vehicleByTripId[tripId]
    }

    /// Returns all live vehicles for a route.
    func vehicles(forRouteId routeId: String) -> [GtfsRtVehicle] {
        vehiclesByRouteId[routeId] ?? []
    }

    /// Returns the number of live vehicles for a route.
    func liveCount(forRouteId routeId: String) -> Int {
        vehiclesByRouteId[routeId]?.count ?? 0
    }

    /// True if a trip is currently tracked in the live feed.
    func isLive(tripId: String?) -> Bool {
        guard let tripId else { return false }
        return vehicleByTripId[tripId] != nil
    }

    // MARK: - Fetch

    private func fetch() async {
        guard let urlString = vehiclePositionsUrl,
              let url = URL(string: urlString) else { return }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let all = decodeGtfsRtVehicles(from: data)
            await MainActor.run { apply(all) }
        } catch {
            // Silently ignore — feed is optional
        }
    }

    private func apply(_ all: [GtfsRtVehicle]) {
        vehicles = all
        lastFetchedAt = Date()
        vehicleByTripId = Dictionary(uniqueKeysWithValues: all.map { ($0.tripId, $0) })
        vehiclesByRouteId = Dictionary(grouping: all, by: \.routeId)
            .filter { !$0.key.isEmpty }
    }
}
