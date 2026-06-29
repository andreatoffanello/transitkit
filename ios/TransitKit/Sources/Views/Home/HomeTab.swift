import SwiftUI
import CoreLocation

// MARK: - HomeTab

struct HomeTab: View {
    @Binding var selectedTab: Int
    @Environment(ScheduleStore.self) private var store
    @Environment(FavoritesManager.self) private var favoritesManager
    @Environment(LocationManager.self) private var locationManager
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(AlertStore.self) private var alertStore
    @Environment(DeepLinkRouter.self) private var router
    @Environment(\.operatorConfig) private var config

    /// Brand dell'APP (AppalRider), localizzato via InfoPlist.xcstrings.
    /// Diverso da `config.name` (AppalCART) che è il nome dell'operatore di
    /// cui mostriamo i dati.
    private var appDisplayName: String {
        (Bundle.main.localizedInfoDictionary?["CFBundleDisplayName"] as? String)
            ?? (Bundle.main.infoDictionary?["CFBundleDisplayName"] as? String)
            ?? "AppalRider"
    }
    @State private var selectedMainStop: ResolvedStop?
    /// Memoized nearby stops. Recomputed only when location or stop count changes
    /// (see `.task(id: nearbyKey)`) — without this it ran on every body redraw,
    /// including the 30s TimelineView tick and every @Observable change.
    @State private var nearbyComputed: [(stop: ResolvedStop, distance: Double)] = []
    /// Memoized routes-by-stop map for nearby cards. O(L·R) per stop computed
    /// once per memoization cycle instead of inside each card body.
    @State private var routesByStopId: [String: [APIRoute]] = [:]
    @State private var showSettings = false
    @State private var showAlertList = false
    @State private var showServizi = false
    @AppStorage("hasSeenOnboarding") private var hasSeenOnboarding = false
    @State private var showOnboarding = false

    // MARK: Journey planner
    @State private var plannerLaunch: PlannerLaunch? = nil
    @State private var deeplinkPlannerLaunch: PendingPlannerLaunch? = nil

    // MARK: App update
    @State private var updateChecker = AppUpdateChecker.shared

    /// Anchor id used by ScrollViewReader to scroll to the favorites section
    /// when the user opens `transitkit://favorites`.
    private let favoritesAnchorId = "home_favorites_anchor"

    // MARK: - Greeting

    private var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 { return String(localized: "home_greeting_morning") }
        else if hour < 18 { return String(localized: "home_greeting_afternoon") }
        else { return String(localized: "home_greeting_evening") }
    }

    // Background shader brandizzato condiviso con l'Onboarding.
    // Implementazione in `Components/OperatorShaderBackground.swift`.

    var body: some View {
        NavigationStack {
            ZStack {
                // Shader animato SOLO quando Home è il tab attivo.
                // Fuori dalla Home (mappa, orari, ecc.) il bg è statico: non
                // è visibile sotto gli altri hub e tenerlo a 30fps friggeva
                // i device piccoli. Invariante: mappa idle = zero shader attivo.
                OperatorShaderBackground(animated: selectedTab == 0).ignoresSafeArea()

                ScrollViewReader { proxy in
                    ScrollView(showsIndicators: false) {
                        VStack(spacing: 0) {
                            // Alert chip sopra l'header (safety-critical)
                            if !alertStore.activeAlerts.isEmpty {
                                alertChip
                                    .padding(.top, 8)
                                    .padding(.bottom, 4)
                            }

                            VStack(spacing: 20) {
                                PlannerHomeBox { origin, dest, when in
                                    plannerLaunch = PlannerLaunch(origin: origin, destination: dest, when: when)
                                }
                                if let soft = updateChecker.softUpdate {
                                    SoftUpdateBanner(softUpdate: soft)
                                        .transition(.asymmetric(
                                            insertion: .move(edge: .top).combined(with: .opacity),
                                            removal: .opacity.combined(with: .scale(scale: 0.95))
                                        ))
                                }
                                favoritesSection.id(favoritesAnchorId)
                                nearbyStopsSection
                                operatorInfoSection
                                serviziLinkSection
                                footerDisclaimer
                            }
                            .padding(.horizontal, 16)
                            .padding(.top, 16)
                            .padding(.bottom, 100)
                            .animation(.spring(duration: 0.4, bounce: 0.2), value: updateChecker.softUpdate)
                        }
                    }
                    .background(.clear)
                    .onChange(of: router.pendingFocusFavorites) { _, id in
                        guard id != nil else { return }
                        router.pendingFocusFavorites = nil
                        withAnimation(.spring(response: 0.45, dampingFraction: 0.85)) {
                            proxy.scrollTo(favoritesAnchorId, anchor: .top)
                        }
                    }
                }

                // Niente fade bottom: la tab bar ha già il suo material —
                // la banda gradiente extra sporcava le card in dark
                // (parità con Android, rimossa su entrambe).
            }
            .background(AppTheme.background.ignoresSafeArea())
            .fullScreenCover(isPresented: $showOnboarding) {
                OnboardingStoriesView()
            }
            .onAppear {
                // Avvia gli updates se già autorizzato (no prompt).
                switch locationManager.authorizationStatus {
                case .authorizedWhenInUse, .authorizedAlways:
                    locationManager.requestPermissionAndStart()
                default:
                    break
                }
                if !hasSeenOnboarding {
                    Task { @MainActor in
                        try? await Task.sleep(nanoseconds: 350_000_000)
                        showOnboarding = true
                        hasSeenOnboarding = true
                    }
                }
            }
            .toolbar(.hidden, for: .navigationBar)
            .safeAreaInset(edge: .top, spacing: 0) {
                homeTopBar
            }
            .task(id: nearbyKey) { recomputeNearby() }
            .fullScreenCover(isPresented: $showSettings) { SettingsTab() }
            .fullScreenCover(isPresented: $showAlertList) {
                NavigationStack { AlertListView() }
            }
            .fullScreenCover(isPresented: $showServizi) {
                if let config { ServiziTab(config: config) }
            }
            .navigationDestination(item: $plannerLaunch) { launch in
                PlannerScreen(
                    initialOrigin: launch.origin,
                    initialDestination: launch.destination,
                    initialWhen: launch.when
                )
            }
            .navigationDestination(item: $deeplinkPlannerLaunch) { launch in
                PlannerScreen(
                    initialOrigin: launch.origin,
                    initialDestination: launch.destination,
                    initialWhen: launch.when
                )
            }
            .navigationDestination(item: $selectedMainStop) { stop in
                StopDetailView(stop: stop)
            }
            .onChange(of: router.pendingSettingsOpen) { _, id in
                guard id != nil else { return }
                router.pendingSettingsOpen = nil
                showSettings = true
            }
            .onChange(of: router.pendingServiziOpen) { _, id in
                guard id != nil else { return }
                router.pendingServiziOpen = nil
                showServizi = true
            }
            .onChange(of: router.pendingOnboardingOpen) { _, id in
                guard id != nil else { return }
                router.pendingOnboardingOpen = nil
                showOnboarding = true
            }
            .onChange(of: router.pendingPlannerOpen) { _, id in
                guard id != nil else { return }
                router.pendingPlannerOpen = nil
                presentPlannerDeeplink(
                    PendingPlannerLaunch(origin: nil, destination: nil, when: .now)
                )
            }
            // Any in-app "go to map" intent (e.g. ServiceDetail "See live buses
            // on map" CTA) sets `pendingMapOpen`. The TabView root already
            // switches selectedTab; we just need to dismiss any HomeTab-owned
            // fullScreenCover sitting on top, otherwise the user lands on the
            // map but the Servizi/Settings overlay is still glued in front.
            .onChange(of: router.pendingMapOpen) { _, id in
                guard id != nil else { return }
                showServizi = false
                showSettings = false
                showAlertList = false
            }
            .onChange(of: router.pendingPlannerLaunch) { _, launch in
                guard let launch else { return }
                router.pendingPlannerLaunch = nil
                presentPlannerDeeplink(launch)
            }
        }
    }

    // MARK: - Top bar (brand + settings, sotto la status bar)

    private var homeTopBar: some View {
        HStack(spacing: 10) {
            if UIImage(named: "OperatorLogo") != nil {
                Image("OperatorLogo")
                    .resizable()
                    .scaledToFill()
                    .frame(width: 28, height: 28)
                    .clipShape(Circle())
            }
            if let config {
                VStack(alignment: .leading, spacing: 0) {
                    Text(appDisplayName)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)
                    if !config.region.isEmpty {
                        Text(config.region)
                            .font(.system(size: 11))
                            .foregroundStyle(AppTheme.textSecondary)
                            .lineLimit(1)
                    }
                }
            }
            Spacer(minLength: 0)
            Button {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                showSettings = true
            } label: {
                LucideIcon.settings.sized(20)
                    .foregroundStyle(AppTheme.textSecondary)
                    .frame(width: 36, height: 36)
                    .contentShape(Rectangle())
            }
            .accessibilityIdentifier("btn_settings")
            .accessibilityLabel(String(localized: "tab_settings"))
        }
        .padding(.horizontal, 16)
        .frame(height: 44)
        // Sticky header opaco: senza un background il contenuto scrollato (la card
        // planner è adaptiveGlass, traslucida) trasparirebbe dietro "AppalRider /
        // città" creando un overlap di testo. Material frosted esteso nella safe
        // area top per occludere e dare gerarchia (blur/vibrancy).
        .background {
            Rectangle()
                .fill(.regularMaterial)
                .ignoresSafeArea(edges: .top)
        }
    }

    // MARK: - Alert Chip

    private var alertChip: some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showAlertList = true
        } label: {
            HStack(spacing: 8) {
                LucideIcon.alertTriangle.sized(13)
                    .foregroundStyle(chipAlertColor)
                Text(alertChipLabel)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(1)
                Spacer(minLength: 0)
                LucideIcon.chevronRight.sized(12)
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 10))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .strokeBorder(chipAlertColor.opacity(0.35), lineWidth: 1)
            )
        }
        .buttonStyle(PressableButtonStyle())
        .accessibilityIdentifier("home_alert_chip")
        .padding(.horizontal, 16)
    }

    private var chipAlertColor: Color {
        switch highestSeverity(alertStore.activeAlerts) {
        case .severe:  return .red
        case .warning: return .orange
        default:       return AppTheme.accent
        }
    }

    private var alertChipLabel: String {
        let count = alertStore.activeAlerts.count
        return count == 1
            ? String(localized: "alerts_banner_one")
            : String(format: String(localized: "alerts_banner_many"), count)
    }

    private func highestSeverity(_ alerts: [GtfsRtAlert]) -> AlertSeverity {
        alerts.map(\.severity).max(by: { $0.rawValue < $1.rawValue }) ?? .unknown
    }

    /// Drive `.navigationDestination(item:)` correctly when a Planner is already
    /// on the stack. Assigning a fresh `PendingPlannerLaunch` while another is
    /// already presented can let SwiftUI coalesce the change into "still the
    /// same destination", so the @State of `PlannerScreen` (whenSelection in
    /// particular) is NOT reinitialized and the new deeplink's prefill is lost.
    /// Pattern from local memory `feedback_navstack_concurrent_destinations.md`:
    /// pop first, hop the runloop ~320 ms, then push fresh.
    private func presentPlannerDeeplink(_ launch: PendingPlannerLaunch) {
        if deeplinkPlannerLaunch != nil || plannerLaunch != nil {
            deeplinkPlannerLaunch = nil
            plannerLaunch = nil
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(320))
                deeplinkPlannerLaunch = launch
            }
        } else {
            deeplinkPlannerLaunch = launch
        }
    }

    // MARK: - Favorites

    private var favoriteStops: [ResolvedStop] {
        favoritesManager.favoriteStopIds.prefix(5).compactMap { stopId in
            store.stops.first { $0.id == stopId }
        }
    }

    @ViewBuilder
    private var favoritesSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(String(localized: "home_section_favorites"), icon: .star)

            if favoriteStops.isEmpty {
                onboardingCard
            } else {
                VStack(spacing: 8) {
                    ForEach(favoriteStops) { stop in
                        Button {
                            selectedMainStop = stop
                        } label: {
                            stopCard(stop, showLiveBadge: false)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    // MARK: - Nearby Stops (GPS)

    /// Cache-invalidation key for `nearbyComputed` + `routesByStopId`. The task
    /// fires only when one of these inputs changes — the body still re-runs at
    /// every TimelineView tick but the O(N·L·R) work happens only on real input
    /// changes.
    private struct NearbyKey: Hashable {
        let lat: Double
        let lng: Double
        let stopsCount: Int
        let routesCount: Int
    }

    private var nearbyKey: NearbyKey {
        NearbyKey(
            lat: locationManager.location?.coordinate.latitude ?? 0,
            lng: locationManager.location?.coordinate.longitude ?? 0,
            stopsCount: store.stops.count,
            routesCount: store.routes.count
        )
    }

    /// Recomputes the memoized nearby stops + routesByStopId map. Cheap on
    /// AppalCART (~128 stops) but called only on real input changes.
    private func recomputeNearby() {
        guard let location = locationManager.location else {
            nearbyComputed = []
            routesByStopId = [:]
            return
        }
        let userLat = location.coordinate.latitude
        let userLng = location.coordinate.longitude
        let ranked = store.stops
            .map { stop -> (ResolvedStop, Double) in
                let dlat = stop.lat - userLat
                let dlng = stop.lng - userLng
                let dist = sqrt(dlat * dlat + dlng * dlng) * 111_320
                return (stop, dist)
            }
            .sorted { $0.1 < $1.1 }
            .prefix(8)
        nearbyComputed = ranked.map { (stop: $0.0, distance: $0.1) }

        // Routes-by-stop map only for the 8 cards we'll render.
        var map: [String: [APIRoute]] = [:]
        for (stop, _) in ranked {
            map[stop.id] = stop.lineNames.compactMap { lineName in
                store.routes.first { $0.name == lineName }
            }
            .uniqued(by: \.id)
        }
        routesByStopId = map
    }

    private var enableLocationChip: some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            locationManager.requestPermissionAndStart()
        } label: {
            HStack(spacing: 8) {
                LucideIcon.mapPin.sized(14)
                    .foregroundStyle(AppTheme.accent)
                Text(String(localized: "home_enable_location_chip"))
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(AppTheme.textPrimary)
                Spacer(minLength: 0)
                LucideIcon.chevronRight.sized(12)
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(PressableButtonStyle())
        .accessibilityIdentifier("home_enable_location_chip")
    }

    @ViewBuilder
    private var nearbyStopsSection: some View {
        switch locationManager.authorizationStatus {
        case .notDetermined:
            enableLocationChip
        case .authorizedWhenInUse, .authorizedAlways:
            let nearby = nearbyComputed.filter { $0.distance <= 400 }
            if !nearby.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    sectionHeader(String(localized: "home_section_nearby"), icon: .mapPin)
                    // Horizontal scroll, edge-to-edge so the first card aligns with
                    // the section title and the last one peeks past the right edge.
                    ScrollView(.horizontal, showsIndicators: false) {
                        LazyHStack(spacing: 10) {
                            ForEach(nearby, id: \.stop.id) { entry in
                                NearbyStopCard(
                                    stop: entry.stop,
                                    distanceMeters: entry.distance,
                                    routes: routesByStopId[entry.stop.id] ?? [],
                                    onTap: { selectedMainStop = entry.stop }
                                )
                            }
                        }
                        .padding(.vertical, 2) // breathing room for shadow
                    }
                    .scrollClipDisabled()
                }
            }
        default:
            EmptyView()
        }
    }

    // MARK: - Stop Card (favorites)

    private func stopCard(_ stop: ResolvedStop, showLiveBadge: Bool, distanceMeters: Double? = nil) -> some View {
        let departures = store.upcomingDepartures(forStopId: stop.id, limit: 3) { tripId in
            tripId.flatMap { vehicleStore.reliableDelayMinutes(forTripId: $0) }
        }
        let transitTypeIcon: Image = stopPinIcon(transitTypes: stop.transitTypes).image
        let isImminent = departures.first.map { isWithinFiveMinutes($0) } ?? false

        return VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                transitTypeIcon
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textTertiary)
                    .frame(width: 14, height: 14)
                Text(stop.name)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(AppTheme.textSecondary)
                    .lineLimit(1)
                Spacer()
                if let distanceMeters {
                    Text(walkingTime(meters: distanceMeters))
                        .font(.caption.weight(.medium))
                        .foregroundStyle(AppTheme.textTertiary)
                }
            }

            if departures.isEmpty {
                Text(String(localized: "no_departures_today"))
                    .font(.caption)
                    .foregroundStyle(AppTheme.textTertiary)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(departures.enumerated()), id: \.element.id) { index, dep in
                        HStack(spacing: 8) {
                            LineBadge(departure: dep, size: .medium)
                            Text(dep.headsign)
                                .font(.system(size: 13))
                                .foregroundStyle(AppTheme.textSecondary)
                                .lineLimit(1)
                                .truncationMode(.tail)
                            Spacer()
                            if showLiveBadge && vehicleStore.isLive(tripId: dep.tripId) {
                                LiveBadge()
                            }
                            TimelineView(.periodic(from: .now, by: 30)) { _ in
                                let rtDelay = dep.tripId.flatMap { vehicleStore.reliableDelayMinutes(forTripId: $0) }
                                TimeDisplay(state: store.timeState(for: dep, delayMinutes: rtDelay))
                            }
                        }
                        .padding(.vertical, 6)
                        if index < departures.count - 1 {
                            Divider().overlay(AppTheme.separatorLine)
                        }
                    }
                }
            }
        }
        .padding(14)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 14), withShadow: true)
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(
                    isImminent ? AppTheme.accent.opacity(0.6) : Color.clear,
                    lineWidth: isImminent ? 1.5 : 0
                )
        )
    }

    /// True se la partenza è entro 5 minuti — usato per evidenziare la card.
    private func isWithinFiveMinutes(_ dep: Departure) -> Bool {
        let now = Date()
        let cal = Calendar.current
        let comps = dep.time.split(separator: ":").compactMap { Int($0) }
        guard comps.count == 2 else { return false }
        guard let depDate = cal.date(bySettingHour: comps[0], minute: comps[1], second: 0, of: now) else {
            return false
        }
        let delta = depDate.timeIntervalSince(now)
        return delta >= 0 && delta <= 300
    }

    // MARK: - Onboarding Card

    private var onboardingCard: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(AppTheme.accent.opacity(0.12))
                    .frame(width: 56, height: 56)
                LucideIcon.star.sized(24)
                    .foregroundStyle(AppTheme.accent)
            }

            VStack(spacing: 6) {
                Text(String(localized: "home_empty_favorites_title"))
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .multilineTextAlignment(.center)
                Text(String(localized: "home_empty_favorites_body"))
                    .font(.system(size: 14))
                    .foregroundStyle(AppTheme.textSecondary)
                    .multilineTextAlignment(.center)
            }

            Button {
                selectedTab = 1   // tab Orari
            } label: {
                Text(String(localized: "home_empty_favorites_cta"))
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(AppTheme.accent, in: RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 16), withShadow: true)
    }

    // MARK: - Helpers

    private func sectionHeader(_ title: String, icon: LucideIcon? = nil) -> some View {
        HStack(spacing: 7) {
            if let icon {
                icon.sized(14)
                    .foregroundStyle(AppTheme.textSecondary)
            }
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(AppTheme.textPrimary)
        }
        .padding(.leading, 2)
    }

    // MARK: - Operator Info Card

    @ViewBuilder
    private var operatorInfoSection: some View {
        if let config {
            VStack(alignment: .leading, spacing: 10) {
                // Section heading + attribution copy: positions the card below
                // as "the people who actually move the city", not a disclaimer.
                VStack(alignment: .leading, spacing: 4) {
                    Text(String(localized: "home_operators_section_title"))
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                    Text(String(format: String(localized: "home_operators_attribution"), config.name))
                        .font(.system(size: 12))
                        .foregroundStyle(AppTheme.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.leading, 2)

                operatorInfoCard(config: config)
            }
        }
    }

    private func operatorInfoCard(config: OperatorConfig) -> some View {
        let liveCount = vehicleStore.vehicles.count
        let routeCount = store.routes.count

        return Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showServizi = true
        } label: {
            HStack(spacing: 14) {
                // Logo reale dell'operatore sorgente — questa card attribuisce
                // i dati al servizio pubblico (AppalCART), non al brand bianco
                // dell'app. Fallback al gradient+bus se l'asset manca.
                Group {
                    if UIImage(named: "SourceOperatorLogo") != nil {
                        Image("SourceOperatorLogo")
                            .resizable()
                            .scaledToFit()
                    } else {
                        ZStack {
                            LinearGradient(
                                colors: [AppTheme.accent, Color(hex: config.theme.primaryColor)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                            LucideIcon.bus.sized(20)
                                .foregroundStyle(.white)
                        }
                    }
                }
                .frame(width: 44, height: 44)
                .clipShape(RoundedRectangle(cornerRadius: 12))

                VStack(alignment: .leading, spacing: 3) {
                    Text(config.name)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(AppTheme.textPrimary)
                    if liveCount > 0 {
                        HStack(spacing: 5) {
                            Circle()
                                .fill(AppTheme.realtimeGreen)
                                .frame(width: 6, height: 6)
                            Text(String(format: String(localized: "home_operators_live_count"), liveCount))
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundStyle(AppTheme.realtimeGreen)
                            if routeCount > 0 {
                                Text("·")
                                    .font(.system(size: 11))
                                    .foregroundStyle(AppTheme.textTertiary)
                                Text(String(format: String(localized: "home_operator_routes"), routeCount))
                                    .font(.system(size: 11))
                                    .foregroundStyle(AppTheme.textTertiary)
                            }
                        }
                    } else if routeCount > 0 {
                        Text(String(format: String(localized: "home_operator_routes"), routeCount))
                            .font(.system(size: 11))
                            .foregroundStyle(AppTheme.textTertiary)
                    } else {
                        Text(String(localized: "home_operators_schedule_only"))
                            .font(.system(size: 11))
                            .foregroundStyle(AppTheme.textTertiary)
                    }
                }

                Spacer(minLength: 0)

                LucideIcon.chevronRight.sized(14)
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(14)
            .adaptiveGlass(in: RoundedRectangle(cornerRadius: 14), withShadow: true)
        }
        .buttonStyle(PressableCardStyle())
        .accessibilityIdentifier("home_operator_info_card")
        .accessibilityLabel(String(localized: "services_title"))
    }

    // MARK: - Servizi link

    /// Tile linking to the full "Servizi" screen (services, fares, accessibility,
    /// contacts, operator). Lives in Home now that the tab-bar slot is reserved
    /// for the live alerts counter — the operator card above also opens Servizi,
    /// but this tile gives users an explicit, labelled entry point.
    @ViewBuilder
    private var serviziLinkSection: some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showServizi = true
        } label: {
            HStack(spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(AppTheme.accent.opacity(0.12))
                    LucideIcon.grid2x2Plus.sized(20)
                        .foregroundStyle(AppTheme.accent)
                }
                .frame(width: 40, height: 40)

                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "tab_services"))
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                    Text(String(localized: "home_servizi_subtitle"))
                        .font(.system(size: 12))
                        .foregroundStyle(AppTheme.textSecondary)
                        .lineLimit(1)
                }

                Spacer()

                LucideIcon.chevronRight.sized(14)
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(AppTheme.bgSecondary)
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("btn_home_servizi")
    }

    // MARK: - Footer Disclaimer

    @ViewBuilder
    private var footerDisclaimer: some View {
        if let config {
            Text(String(format: String(localized: "home_footer_disclaimer"), config.name))
                .font(.system(size: 11))
                .foregroundStyle(AppTheme.textTertiary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
        }
    }
}

// MARK: - PlannerLaunch

struct PlannerLaunch: Identifiable, Hashable {
    let id = UUID()
    let origin: PlannerLocation
    let destination: PlannerLocation
    let when: WhenSelection
}
