import SwiftUI

// MARK: - App Entry Point

@main
struct TransitKitApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @State private var store: ScheduleStore?
    @State private var favoritesManager: FavoritesManager?
    @State private var searchHistoryStore: SearchHistoryStore?
    @State private var savedPlacesStore: SavedPlacesStore?
    @State private var locationManager = LocationManager()
    @State private var vehicleStore: VehicleStore = VehicleStore(vehiclePositionsUrl: nil)
    @State private var alertStore: AlertStore = AlertStore()
    @State private var pushManager: PushNotificationManager?
    @State private var operatorConfig: OperatorConfig?
    @State private var loadingConfig: OperatorConfig?
    @State private var configError: String?
    @State private var router = DeepLinkRouter()
    @State private var connectionsStore = ConnectionsStore()
    @State private var updateChecker = AppUpdateChecker.shared
    @Environment(\.scenePhase) private var scenePhase

    init() {
        if CommandLine.arguments.contains("--reset-schedule-cache") {
            let cacheDir = FileManager.default.urls(
                for: .applicationSupportDirectory, in: .userDomainMask
            )[0].appendingPathComponent("TransitKit", isDirectory: true)
            try? FileManager.default.removeItem(at: cacheDir)
        }
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                Group {
                    if let store, let favoritesManager, let searchHistoryStore, let savedPlacesStore, let operatorConfig, let pushManager {
                        ContentView(config: operatorConfig)
                            .environment(store)
                            .environment(favoritesManager)
                            .environment(searchHistoryStore)
                            .environment(savedPlacesStore)
                            .environment(locationManager)
                            .environment(vehicleStore)
                            .environment(alertStore)
                            .environment(pushManager)
                            .environment(router)
                            .environment(connectionsStore)
                            .environment(\.operatorConfig, operatorConfig)
                            .environment(\.operatorTimeZone, TimeZone(identifier: operatorConfig.timezone) ?? .current)
                            .tint(AppTheme.accent)
                    } else if let configError {
                        errorView(message: configError)
                    } else {
                        loadingView
                    }
                }

                // Force-update overlay: blocks the entire UI when the operator
                // config declares a minimum version the user hasn't reached,
                // with force:true. No dismiss — the only action is to update.
                if case let .forced(message, storeUrl) = updateChecker.requirement {
                    ForceUpdateView(message: message, storeUrl: storeUrl)
                        .transition(.opacity)
                        .zIndex(10)
                }
            }
            .animation(.easeInOut(duration: 0.4), value: updateChecker.requirement)
            .task { await bootstrap() }
            .onOpenURL { url in handleDeepLink(url) }
            .onChange(of: scenePhase) { _, newPhase in
                if newPhase == .active {
                    updateChecker.check(config: operatorConfig)
                }
            }
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
        // URL structure: transitkit://<command>[/<arg1>/<arg2>/...][?key=val&...]
        let command = url.host ?? ""
        let parts = url.pathComponents.filter { $0 != "/" }
        let queryItems = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems
        func q(_ name: String) -> String? {
            queryItems?.first(where: { $0.name == name })?.value
        }

        switch command {

        // MARK: Tab switches (no args)

        case "home":
            router.pendingTabSwitch = TabSwitch(index: 0)

        case "schedules", "orari":
            router.pendingTabSwitch = TabSwitch(index: 1)
            if let query = q("q"), !query.isEmpty {
                router.pendingSearchScope = .stops
                router.pendingSearchQuery = query
            }

        case "lines", "linee":
            router.pendingTabSwitch = TabSwitch(index: 2)
            if let query = q("q"), !query.isEmpty {
                router.pendingSearchScope = .lines
                router.pendingSearchQuery = query
            }

        case "alerts", "avvisi":
            router.pendingTabSwitch = TabSwitch(index: 4)

        // MARK: Map (zero-arg + subcommands)

        case "map":
            if parts.isEmpty {
                // Bare `transitkit://map` means "give me the clean Map tab".
                // Clear any leftover preview/filter pendings (route, stop, vehicle)
                // before the tab switches, otherwise a previous `line/X/map` deeplink
                // leaves the route chip glued on top of the supposedly-bare Map.
                router.pendingMapPreviewStop = nil
                router.pendingMapPreviewRouteId = nil
                router.pendingMapPreviewDirectionId = nil
                router.pendingMapPreviewVehicleId = nil
                router.pendingMapOpen = UUID()
                return
            }
            switch parts[0] {
            case "stop":
                guard parts.count >= 2,
                      let stop = store.stops.first(where: { $0.id == parts[1] })
                else { return }
                router.pendingMapPreviewStop = stop
            case "vehicle":
                guard parts.count >= 2 else { return }
                router.pendingMapPreviewVehicleId = parts[1]
            case "route", "line":
                guard parts.count >= 2 else { return }
                router.pendingMapPreviewRouteId = parts[1]
                router.pendingMapPreviewDirectionId = parts.count >= 3 ? Int(parts[2]) : nil
            default:
                break
            }

        // MARK: Home secondary sheets

        case "favorites":
            router.pendingTabSwitch = TabSwitch(index: 0)
            router.pendingFocusFavorites = UUID()

        case "settings":
            router.pendingSettingsAnchor = parts.first
            router.pendingSettingsOpen = UUID()

        case "servizi", "services":
            router.pendingServiziId = parts.first
            router.pendingServiziOpen = UUID()

        case "onboarding":
            router.pendingOnboardingOpen = UUID()

        // MARK: Planner

        case "planner":
            let origin = q("from").flatMap { stopId in
                store.stops.first(where: { $0.id == stopId })
            }.map(PlannerLocation.stop)
            let destination = q("to").flatMap { stopId in
                store.stops.first(where: { $0.id == stopId })
            }.map(PlannerLocation.stop)
            let tz = operatorConfig.flatMap { TimeZone(identifier: $0.timezone) } ?? .current
            let when = Self.parseWhen(q("when") ?? q("time"), timeZone: tz)
            if origin != nil || destination != nil || when != .now {
                router.pendingPlannerLaunch = PendingPlannerLaunch(
                    origin: origin, destination: destination, when: when
                )
            } else {
                router.pendingPlannerOpen = UUID()
            }

        // MARK: Universal search

        case "search":
            let query = q("q") ?? parts.first ?? ""
            let scope: SearchScope = (q("scope") == "lines" || q("scope") == "linee") ? .lines : .stops
            router.pendingSearchScope = scope
            router.pendingSearchQuery = query
            router.pendingTabSwitch = TabSwitch(index: scope == .lines ? 2 : 1)

        // MARK: Alerts

        case "alert":
            guard let alertId = parts.first else { return }
            router.pendingAlertId = alertId
            router.pendingTabSwitch = TabSwitch(index: 4)

        // MARK: Existing commands with required path args

        // transitkit://line/<routeId>[/map[/<directionId>]]
        case "line":
            guard let routeId = parts.first else { Self.fallbackToHome(router: router); return }
            guard let route = store.route(forId: routeId) else {
                #if DEBUG
                print("[DeepLink] line/\(routeId) — route id not found, falling back to Home")
                #endif
                Self.fallbackToHome(router: router)
                return
            }
            let openMap = parts.count >= 2 && parts[1] == "map"
            router.autoOpenMap = openMap
            router.pendingDirectionId = openMap && parts.count >= 3 ? Int(parts[2]) : nil
            router.pendingRoute = route

        // transitkit://stop/<stopId>[/schedule]
        case "stop":
            guard let stopId = parts.first else { Self.fallbackToHome(router: router); return }
            guard let stop = store.stops.first(where: { $0.id == stopId }) else {
                #if DEBUG
                print("[DeepLink] stop/\(stopId) — stop id not found, falling back to Home")
                #endif
                Self.fallbackToHome(router: router)
                return
            }
            router.openScheduleForStop = parts.count >= 2 && parts[1] == "schedule" ? stop.id : nil
            router.pendingStop = stop

        // transitkit://trip/<stopId>/<routeId>/<time>   (time = "HH:MM")
        case "trip":
            guard parts.count >= 3,
                  let stop = store.stops.first(where: { $0.id == parts[0] })
            else {
                #if DEBUG
                print("[DeepLink] trip/\(parts.joined(separator: "/")) — stop not found, falling back to Home")
                #endif
                Self.fallbackToHome(router: router)
                return
            }
            let routeId = parts[1]
            let time = parts[2]
            let deps = store.todayDepartures(forStopId: stop.id)
            guard let dep = deps.first(where: { $0.routeId == routeId && $0.time == time }) else {
                #if DEBUG
                print("[DeepLink] trip/\(parts[0])/\(routeId)/\(time) — departure not found, falling back to Home")
                #endif
                Self.fallbackToHome(router: router)
                return
            }
            router.pendingTrip = TripTarget(departure: dep, fromStop: stop)

        // MARK: Dev

        #if DEBUG
        case "shader":
            router.showShaderPlayground = true
        #endif

        default:
            break
        }
    }

    /// Soft failure for unrecognised deeplink targets — switch to Home so the
    /// user lands somewhere sensible instead of "nothing happened". Better than
    /// silent ignore: from the user's perspective the click did do something.
    private static func fallbackToHome(router: DeepLinkRouter) {
        router.pendingTabSwitch = TabSwitch(index: 0)
    }

    /// Parses a `when=` deeplink value into a `WhenSelection`, resolving wall-clock
    /// times in the operator's `timeZone` (so that `when=14:30` always means 14:30
    /// in the city the operator runs, regardless of where the user / simulator is).
    /// Accepts: nil/"" / "now" → .now; "HH:MM" → today at HH:MM departAt;
    /// "arrive:HH:MM" / "depart:HH:MM" → with explicit mode; ISO8601 also accepted.
    private static func parseWhen(_ raw: String?, timeZone: TimeZone) -> WhenSelection {
        guard let raw = raw?.trimmingCharacters(in: .whitespaces), !raw.isEmpty, raw.lowercased() != "now" else {
            return .now
        }
        let (mode, value): (String, String) = {
            if raw.hasPrefix("arrive:") { return ("arrive", String(raw.dropFirst(7))) }
            if raw.hasPrefix("depart:") { return ("depart", String(raw.dropFirst(7))) }
            return ("depart", raw)
        }()
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let date: Date? = {
            // Try HH:MM in operator timezone
            if let colonIdx = value.firstIndex(of: ":"),
               let h = Int(value[..<colonIdx]),
               let m = Int(value[value.index(after: colonIdx)...].prefix(2)),
               (0...23).contains(h), (0...59).contains(m) {
                var comp = calendar.dateComponents([.year, .month, .day], from: Date())
                comp.timeZone = timeZone
                comp.hour = h
                comp.minute = m
                return calendar.date(from: comp)
            }
            // Fallback ISO8601 (absolute, no TZ interpretation needed)
            return ISO8601DateFormatter().date(from: value)
        }()
        guard let date else { return .now }
        return mode == "arrive" ? .arriveBy(date) : .departAt(date)
    }

    // MARK: - Bootstrap

    private func bootstrap() async {
        do {
            let config = try ConfigLoader.load()
            AppTheme.configure(from: config)
            loadingConfig = config
            let scheduleStore = ScheduleStore(operatorId: config.id, apiUrl: config.apiUrl)
            scheduleStore.configure(with: config)
            let push = PushNotificationManager(operatorId: config.id)
            AppDelegate.pushManager = push
            pushManager = push
            let favoritesMgr = FavoritesManager(operatorId: config.id, pushManager: push)
            favoritesManager = favoritesMgr
            searchHistoryStore = SearchHistoryStore(operatorId: config.id)
            savedPlacesStore = SavedPlacesStore(operatorId: config.id)
            await scheduleStore.load()
            store = scheduleStore
            operatorConfig = config
            vehicleStore.configure(
                vehiclePositionsUrl: config.gtfsRt?.vehiclePositionsUrl,
                tripUpdatesUrl: config.gtfsRt?.tripUpdatesUrl,
                routeIdByTripId: scheduleStore.routeIdByTripId
            )
            alertStore.configure(serviceAlertsUrl: config.gtfsRt?.serviceAlertsUrl)
            Task { await connectionsStore.load(stops: scheduleStore.stops) }
            // Check for forced update immediately after config loads (synchronous,
            // no network). Soft banner check fires asynchronously inside.
            updateChecker.check(config: config)
            if let pending = router.pendingUrl {
                router.pendingUrl = nil
                resolve(url: pending, store: scheduleStore)
            }
        } catch {
            configError = error.localizedDescription
        }
    }

    // MARK: - Loading View

    private var loadingView: some View {
        VStack(spacing: 20) {
            // Mostra l'icona dell'APP (bus AppalRider), MAI il logo o le iniziali
            // dell'operatore: l'app non è ufficiale dell'operatore e mostrare il
            // loro brand qui può far pensare a un'impersonazione.
            Image("OperatorLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 96, height: 96)
            // Brand dell'APP (es. "AppalRider"), non il nome dell'operatore.
            if let config = loadingConfig {
                Text(config.brandName ?? config.name)
                    .font(.title2.bold())
                    .foregroundStyle(AppTheme.textPrimary)
            }
            // Subtle loading indicator
            ProgressView()
                .tint(AppTheme.accent)
            Text(String(localized: "powered_by_transitkit"))
                .font(.system(.caption, weight: .medium))
                .foregroundStyle(AppTheme.textTertiary)
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
