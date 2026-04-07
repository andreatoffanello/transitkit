import SwiftUI

// MARK: - App Entry Point

@main
struct TransitKitApp: App {
    @State private var store: ScheduleStore?
    @State private var favoritesManager: FavoritesManager?
    @State private var searchHistoryStore: SearchHistoryStore?
    @State private var locationManager = LocationManager()
    @State private var vehicleStore: VehicleStore = VehicleStore(vehiclePositionsUrl: nil)
    @State private var operatorConfig: OperatorConfig?
    @State private var loadingConfig: OperatorConfig?
    @State private var configError: String?
    @State private var router = DeepLinkRouter()

    var body: some Scene {
        WindowGroup {
            Group {
                if let store, let favoritesManager, let searchHistoryStore, let operatorConfig {
                    ContentView(config: operatorConfig)
                        .environment(store)
                        .environment(favoritesManager)
                        .environment(searchHistoryStore)
                        .environment(locationManager)
                        .environment(vehicleStore)
                        .environment(router)
                        .tint(AppTheme.accent)
                } else if let configError {
                    errorView(message: configError)
                } else {
                    loadingView
                }
            }
            .task { await bootstrap() }
            .onOpenURL { url in handleDeepLink(url) }
        }
    }

    // MARK: - Deep Link Handling

    private func handleDeepLink(_ url: URL) {
        guard url.scheme == "transitkit" else { return }
        if let store {
            resolve(url: url, store: store)
        } else {
            // App still loading — queue URL, processed after bootstrap
            router.pendingUrl = url
        }
    }

    private func resolve(url: URL, store: ScheduleStore) {
        // URL structure: transitkit://<command>/<arg1>/<arg2>/...
        // host = command, pathComponents (filtered) = args
        let command = url.host ?? ""
        let parts = url.pathComponents.filter { $0 != "/" }
        guard !parts.isEmpty else { return }

        switch command {

        // transitkit://line/<routeId>
        // transitkit://line/<routeId>/map
        // transitkit://line/<routeId>/map/<directionId>
        case "line":
            guard let route = store.route(forId: parts[0]) else { return }
            let openMap = parts.count >= 2 && parts[1] == "map"
            router.autoOpenMap = openMap
            router.pendingDirectionId = openMap && parts.count >= 3 ? Int(parts[2]) : nil
            router.pendingRoute = route

        // transitkit://stop/<stopId>
        // transitkit://stop/<stopId>/schedule
        case "stop":
            guard let stop = store.stops.first(where: { $0.id == parts[0] }) else { return }
            router.openScheduleForStop = parts.count >= 2 && parts[1] == "schedule" ? stop.id : nil
            router.pendingStop = stop

        // transitkit://trip/<stopId>/<routeId>/<time>   (time = "HH:MM")
        case "trip":
            guard parts.count >= 3,
                  let stop = store.stops.first(where: { $0.id == parts[0] })
            else { return }
            let routeId = parts[1]
            let time = parts[2]
            let deps = store.todayDepartures(forStopId: stop.id)
            guard let dep = deps.first(where: { $0.routeId == routeId && $0.time == time })
            else { return }
            router.pendingTrip = TripTarget(departure: dep, fromStop: stop)

        default:
            break
        }
    }

    // MARK: - Bootstrap

    private func bootstrap() async {
        do {
            let config = try ConfigLoader.load()
            AppTheme.configure(from: config)
            loadingConfig = config
            let scheduleStore = ScheduleStore(operatorId: config.id, apiUrl: config.apiUrl)
            scheduleStore.configure(with: config)
            favoritesManager = FavoritesManager(operatorId: config.id)
            searchHistoryStore = SearchHistoryStore(operatorId: config.id)
            await scheduleStore.load()
            store = scheduleStore
            operatorConfig = config
            vehicleStore = VehicleStore(vehiclePositionsUrl: config.gtfsRt?.vehiclePositionsUrl)
            vehicleStore.startPolling()
            if let pending = router.pendingUrl {
                router.pendingUrl = nil
                resolve(url: pending, store: scheduleStore)
            }
        } catch {
            configError = error.localizedDescription
        }
    }

    // MARK: - Loading View

    private var operatorInitials: String {
        guard let name = loadingConfig?.name else { return "" }
        let words = name.split(separator: " ").prefix(2)
        return words.compactMap { $0.first }.map(String.init).joined()
    }

    private var loadingView: some View {
        VStack(spacing: 20) {
            if let config = loadingConfig {
                // Avatar circle with operator initials
                ZStack {
                    Circle()
                        .fill(AppTheme.accent)
                        .frame(width: 64, height: 64)
                    Text(operatorInitials)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(.white)
                }
                // Operator name
                Text(config.name)
                    .font(.title2.bold())
                    .foregroundStyle(AppTheme.textPrimary)
            }
            // Subtle loading indicator
            ProgressView()
                .tint(AppTheme.accent)
            if loadingConfig == nil {
                Text(String(localized: "powered_by_transitkit"))
                    .font(.system(.caption, weight: .medium))
                    .foregroundStyle(AppTheme.textTertiary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppTheme.background.ignoresSafeArea())
    }

    // MARK: - Error View

    private func errorView(message: String) -> some View {
        VStack(spacing: 20) {
            LucideIcon.alertTriangle.sized(40)
                .foregroundStyle(AppTheme.realtimeRed)

            Text(String(localized: "error_loading"))
                .font(.system(.headline))
                .foregroundStyle(AppTheme.textPrimary)

            Text(message)
                .font(.system(.subheadline))
                .foregroundStyle(AppTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button {
                configError = nil
                Task { await bootstrap() }
            } label: {
                Text(String(localized: "error_retry"))
                    .font(.system(.body, weight: .semibold))
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppTheme.accent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppTheme.background.ignoresSafeArea())
    }
}
