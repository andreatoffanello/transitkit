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
    @Environment(\.colorScheme) private var colorScheme

    private var config: OperatorConfig? { try? ConfigLoader.load() }
    @State private var selectedMainStop: ResolvedStop?
    @State private var showSettings = false
    @State private var showAlertList = false
    @AppStorage("hasSeenLocationPrimer") private var hasSeenLocationPrimer = false
    @State private var showLocationPrimer = false

    // MARK: - Greeting

    private var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 { return String(localized: "home_greeting_morning") }
        else if hour < 18 { return String(localized: "home_greeting_afternoon") }
        else { return String(localized: "home_greeting_evening") }
    }

    @ViewBuilder
    private var operatorMapBackground: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 24.0)) { ctx in
            // Wrap time a 0..1000 per evitare precision loss Float32 GPU
            let t = Float(ctx.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 1000.0))
            let (ar, ag, ab) = Self.rgbComponents(of: AppTheme.accent)
            let isDark = colorScheme == .dark
            GeometryReader { geo in
                ZStack {
                    // Layer 1: Deep fog (always present)
                    homeMapLayer(size: geo.size, time: t, sharpness: 0.0, accent: (ar, ag, ab))
                        .blur(radius: 28)
                        .opacity(isDark ? 0.70 : 0.55)

                    // Layer 2: Medium fog
                    homeMapLayer(size: geo.size, time: t, sharpness: 0.3, accent: (ar, ag, ab))
                        .blur(radius: 12)
                        .opacity(isDark ? 0.50 : 0.42)

                    // Layer 3: Forming lines
                    homeMapLayer(size: geo.size, time: t, sharpness: 0.7, accent: (ar, ag, ab))
                        .blur(radius: 4)
                        .opacity(isDark ? 0.45 : 0.36)

                    // Layer 4: Crisp lines (peak breathing only)
                    homeMapLayer(size: geo.size, time: t, sharpness: 1.0, accent: (ar, ag, ab))
                        .opacity(isDark ? 0.50 : 0.38)
                }
            }
        }
        .allowsHitTesting(false)
    }

    @ViewBuilder
    private func homeMapLayer(
        size: CGSize,
        time: Float,
        sharpness: Float,
        accent: (Float, Float, Float)
    ) -> some View {
        Image("OperatorBackground")
            .resizable()
            .aspectRatio(contentMode: .fill)
            .frame(width: size.width, height: size.height)
            .clipped()
            .colorEffect(
                ShaderLibrary.mapGlowEffect(
                    .float2(size),
                    .float(time),
                    .float(sharpness),
                    .float(accent.0),
                    .float(accent.1),
                    .float(accent.2)
                )
            )
    }

    /// Estrae componenti RGB 0..1 da una Color SwiftUI risolvendola via UIColor.
    /// Usata per passare l'accent color all'shader Metal come uniform.
    private static func rgbComponents(of color: Color) -> (Float, Float, Float) {
        let ui = UIColor(color)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        ui.getRed(&r, green: &g, blue: &b, alpha: &a)
        return (Float(r), Float(g), Float(b))
    }

    var body: some View {
        NavigationStack {
            ZStack {
                operatorMapBackground.ignoresSafeArea()

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        // Alert chip sopra l'header (safety-critical)
                        if !alertStore.activeAlerts.isEmpty {
                            alertChip
                                .padding(.top, 8)
                                .padding(.bottom, 4)
                        }
                        homeMinimalHeader

                        VStack(spacing: 20) {
                            favoritesSection
                            nearbyStopsSection
                            footerDisclaimer
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 16)
                        .padding(.bottom, 100)
                    }
                }
                .background(.clear)

                // Footer gradient fade per leggibilità disclaimer sullo sfondo
                VStack {
                    Spacer()
                    LinearGradient(
                        colors: [AppTheme.background.opacity(0), AppTheme.background.opacity(0.9)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 120)
                    .allowsHitTesting(false)
                }
                .ignoresSafeArea()
            }
            .background(AppTheme.background.ignoresSafeArea())
            .fullScreenCover(isPresented: $showLocationPrimer) {
                LocationPrimerView()
            }
            .onAppear {
                switch locationManager.authorizationStatus {
                case .authorizedWhenInUse, .authorizedAlways:
                    // Gia' autorizzato: avvia gli updates (non mostra prompt)
                    locationManager.requestPermissionAndStart()
                case .notDetermined:
                    // Primo launch: mostra il primer, NON triggerare il prompt sistema
                    if !hasSeenLocationPrimer {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                            showLocationPrimer = true
                            hasSeenLocationPrimer = true
                        }
                    }
                default:
                    // .denied / .restricted: niente, l'utente gestisce da Settings > Privacy
                    break
                }
            }
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.hidden, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                        showSettings = true
                    } label: {
                        LucideIcon.settings.sized(20)
                            .foregroundStyle(AppTheme.textSecondary)
                    }
                    .accessibilityIdentifier("btn_settings")
                    .accessibilityLabel(String(localized: "tab_settings"))
                }
            }
            .sheet(isPresented: $showSettings) { SettingsTab() }
            .sheet(isPresented: $showAlertList) {
                NavigationStack { AlertListView() }
            }
            .navigationDestination(item: $selectedMainStop) { stop in
                StopDetailView(stop: stop)
            }
        }
    }

    // MARK: - Minimal Header

    private var homeMinimalHeader: some View {
        HStack(spacing: 12) {
            if UIImage(named: "OperatorLogo") != nil {
                Image("OperatorLogo")
                    .resizable()
                    .scaledToFill()
                    .frame(width: 32, height: 32)
                    .clipShape(Circle())
            }
            if let config {
                VStack(alignment: .leading, spacing: 1) {
                    Text(config.name)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                    if !config.region.isEmpty {
                        Text(config.region)
                            .font(.system(size: 12))
                            .foregroundStyle(AppTheme.textSecondary)
                    }
                }
            }
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 8)
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

    // MARK: - Favorites

    private var favoriteStops: [ResolvedStop] {
        favoritesManager.favoriteStopIds.prefix(5).compactMap { stopId in
            store.stops.first { $0.id == stopId }
        }
    }

    private func walkingTime(meters: Double) -> String {
        let minutes = Int((meters / 80.0).rounded(.up))
        if minutes <= 1 { return String(localized: "walking_1_min") }
        if minutes > 10 { return String(localized: "walking_10_plus_min") }
        return String(format: String(localized: "walking_n_min"), minutes)
    }

    @ViewBuilder
    private var favoritesSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(String(localized: "favorites"))

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

    /// Returns the 3 closest stops to the user's location, paired with meters.
    /// No distance cutoff — the soft "far from transit" state is decided by the view.
    private var nearbyStopsWithDistance: [(ResolvedStop, Double)] {
        guard let location = locationManager.location else { return [] }
        let userLat = location.coordinate.latitude
        let userLng = location.coordinate.longitude
        return store.stops
            .map { stop -> (ResolvedStop, Double) in
                let dlat = stop.lat - userLat
                let dlng = stop.lng - userLng
                let dist = sqrt(dlat * dlat + dlng * dlng) * 111_320
                return (stop, dist)
            }
            .sorted { $0.1 < $1.1 }
            .prefix(3)
            .map { ($0.0, $0.1) }
    }

    private var enableLocationChip: some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showLocationPrimer = true
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
            let nearby = nearbyStopsWithDistance.filter { $0.1 <= 400 }
            if !nearby.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    sectionHeader(String(localized: "nearby_you"))
                    VStack(spacing: 8) {
                        ForEach(nearby, id: \.0.id) { (stop, distance) in
                            Button {
                                selectedMainStop = stop
                            } label: {
                                stopCard(stop, showLiveBadge: true, distanceMeters: distance)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        default:
            EmptyView()
        }
    }


    // MARK: - Stop Card (shared for favorites and nearby)

    private func stopCard(_ stop: ResolvedStop, showLiveBadge: Bool, distanceMeters: Double? = nil) -> some View {
        let departures = store.upcomingDepartures(forStopId: stop.id, limit: 3)
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
                                TimeDisplay(departure: dep)
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

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.footnote.weight(.semibold))
            .foregroundStyle(AppTheme.textTertiary)
            .textCase(.uppercase)
            .kerning(0.5)
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
