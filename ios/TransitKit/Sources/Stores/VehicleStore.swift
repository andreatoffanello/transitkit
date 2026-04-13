import Foundation

// MARK: - Vehicle Store

/// Global real-time vehicle feed. Polls every 15s and provides indexed lookups.
/// Shared across all views via @Environment. Gracefully handles missing GTFS-RT URLs.
@MainActor
@Observable
final class VehicleStore {
    // MARK: State
    private(set) var vehicles: [GtfsRtVehicle] = []
    private(set) var lastFetchedAt: Date? = nil

    // MARK: Private
    private(set) var vehiclePositionsUrl: String?
    private var tripUpdatesUrl: String?
    private var pollTask: Task<Void, Never>?

    /// Indexed for O(1) lookup: trip_id → vehicle
    private var vehicleByTripId: [String: GtfsRtVehicle] = [:]
    /// Indexed for O(1) lookup: route_id → vehicles
    private var vehiclesByRouteId: [String: [GtfsRtVehicle]] = [:]
    /// Trip → route mapping from ScheduleStore, used to resolve vehicles that have empty routeId.
    private var routeIdByTripId: [String: String] = [:]
    /// Trip → delay in seconds (positive = late, negative = early) from TripUpdate feed.
    private var delayByTripId: [String: Int32] = [:]

    init(vehiclePositionsUrl: String? = nil) {
        self.vehiclePositionsUrl = vehiclePositionsUrl
    }

    /// Configure URLs and trip→route mapping, then (re)start polling.
    func configure(vehiclePositionsUrl: String?, tripUpdatesUrl: String? = nil, routeIdByTripId: [String: String] = [:]) {
        self.vehiclePositionsUrl = vehiclePositionsUrl
        self.tripUpdatesUrl = tripUpdatesUrl
        self.routeIdByTripId = routeIdByTripId
        startPolling()
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

    /// Delay in seconds for a trip (positive = late, negative = early). Nil if unknown.
    func delay(forTripId tripId: String) -> Int32? {
        delayByTripId[tripId]
    }

    // MARK: - Fetch

    private func fetch() async {
        async let vehicles = fetchVehiclePositions()
        async let delays = fetchTripDelays()
        let (v, d) = await (vehicles, delays)
        apply(v, delays: d)
    }

    private func fetchVehiclePositions() async -> [GtfsRtVehicle] {
        guard let urlString = vehiclePositionsUrl,
              let url = URL(string: urlString) else { return [] }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            return decodeGtfsRtVehicles(from: data)
        } catch {
            return []
        }
    }

    private func fetchTripDelays() async -> [String: Int32] {
        guard let urlString = tripUpdatesUrl,
              let url = URL(string: urlString) else { return [:] }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            return decodeGtfsRtTripDelays(from: data)
        } catch {
            return [:]
        }
    }

    private func apply(_ all: [GtfsRtVehicle], delays: [String: Int32]) {
        vehicles = all
        lastFetchedAt = Date()
        delayByTripId = delays

        vehicleByTripId = Dictionary(uniqueKeysWithValues: all.map { ($0.tripId, $0) })

        // Build route index — many feeds set routeId="" and only populate tripId.
        // Fall back to routeIdByTripId (from ScheduleStore) in that case.
        var byRoute: [String: [GtfsRtVehicle]] = [:]
        for v in all {
            let routeId = v.routeId.isEmpty
                ? (routeIdByTripId[v.tripId] ?? "")
                : v.routeId
            guard !routeId.isEmpty else { continue }
            byRoute[routeId, default: []].append(v)
        }
        vehiclesByRouteId = byRoute
    }
}
