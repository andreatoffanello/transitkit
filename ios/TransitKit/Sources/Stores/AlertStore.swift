import Foundation

// MARK: - Alert Store

/// Global GTFS-RT service alerts feed. Polls every 60s and provides indexed lookups
/// by stop_id / route_id. Active-period filtering is applied on the way out so
/// callers see only currently-valid alerts.
@MainActor
@Observable
final class AlertStore {
    // MARK: State
    private(set) var allAlerts: [GtfsRtAlert] = []
    private(set) var lastFetchedAt: Date? = nil

    // MARK: Private
    private var serviceAlertsUrl: String?
    private var pollTask: Task<Void, Never>?

    /// IDs of alerts that were active during the previous fetch. Used by the
    /// consumer to detect *new* alerts (for the in-app toast in Phase E).
    private(set) var previouslyActiveIds: Set<String> = []

    // MARK: - Derived
    /// Currently-active alerts, filtered by activePeriods at "now".
    var activeAlerts: [GtfsRtAlert] {
        let epoch = UInt64(Date().timeIntervalSince1970)
        return allAlerts.filter { $0.isActive(at: epoch) }
    }

    /// Alerts touching a given stop_id.
    func alerts(forStopId stopId: String) -> [GtfsRtAlert] {
        activeAlerts.filter { $0.affectedStopIds.contains(stopId) }
    }

    /// Alerts touching a given route_id.
    func alerts(forRouteId routeId: String) -> [GtfsRtAlert] {
        activeAlerts.filter { $0.affectedRouteIds.contains(routeId) }
    }

    /// True if the given stop has any active alert. O(n) in active alert count.
    func hasAlert(forStopId stopId: String) -> Bool {
        activeAlerts.contains { $0.affectedStopIds.contains(stopId) }
    }

    /// True if the given route has any active alert.
    func hasAlert(forRouteId routeId: String) -> Bool {
        activeAlerts.contains { $0.affectedRouteIds.contains(routeId) }
    }

    // MARK: - Lifecycle

    func configure(serviceAlertsUrl: String?) {
        self.serviceAlertsUrl = serviceAlertsUrl
        startPolling()
    }

    func startPolling() {
        guard serviceAlertsUrl != nil else { return }
        pollTask?.cancel()
        pollTask = Task { [weak self] in
            while !Task.isCancelled {
                await self?.fetch()
                try? await Task.sleep(for: .seconds(60))
            }
        }
    }

    func stopPolling() {
        pollTask?.cancel()
        pollTask = nil
    }

    // MARK: - Fetch

    private func fetch() async {
        guard let urlString = serviceAlertsUrl,
              let url = URL(string: urlString) else { return }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            apply(decodeGtfsRtAlerts(from: data))
        } catch {
            // Silent: a transient feed failure shouldn't empty out the UI.
        }
    }

    private func apply(_ alerts: [GtfsRtAlert]) {
        let epoch = UInt64(Date().timeIntervalSince1970)
        previouslyActiveIds = Set(allAlerts
            .filter { $0.isActive(at: epoch) }
            .map(\.id))
        allAlerts = alerts
        lastFetchedAt = Date()
    }
}
