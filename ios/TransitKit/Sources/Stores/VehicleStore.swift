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
    /// Trip → per-stop arrival epoch seconds (when the feed publishes them).
    private var arrivalsByTripId: [String: [String: UInt64]] = [:]

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

    /// Predicted arrival time for a stop on a given trip, or nil if the feed
    /// does not publish per-stop ETAs. Matches either the synthetic station id
    /// or an original GTFS stop_id (the RT feed always uses the latter).
    func arrival(forTripId tripId: String, stopId: String) -> Date? {
        guard let map = arrivalsByTripId[tripId] else { return nil }
        if let t = map[stopId] { return Date(timeIntervalSince1970: TimeInterval(t)) }
        return nil
    }

    // MARK: - Fetch

    private func fetch() async {
        async let vehicles = fetchVehiclePositions()
        async let updates  = fetchTripUpdates()
        let (v, u) = await (vehicles, updates)
        apply(v, updates: u)
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

    private func fetchTripUpdates() async -> [String: GtfsRtTripDelay] {
        guard let urlString = tripUpdatesUrl,
              let url = URL(string: urlString) else { return [:] }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            return decodeGtfsRtTripUpdates(from: data)
        } catch {
            return [:]
        }
    }

    private func apply(_ all: [GtfsRtVehicle], updates: [String: GtfsRtTripDelay]) {
        vehicles = all
        lastFetchedAt = Date()
        delayByTripId = updates.mapValues { $0.delay }
        arrivalsByTripId = updates.mapValues { $0.arrivalByStopId }

        // Feeds occasionally emit duplicate or empty trip_ids (vehicle swaps, block
        // transitions). Skip empties and keep the most recently seen entry on collisions.
        vehicleByTripId = Dictionary(
            all.lazy.filter { !$0.tripId.isEmpty }.map { ($0.tripId, $0) },
            uniquingKeysWith: { _, new in new }
        )

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
