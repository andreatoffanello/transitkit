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
            let t = Float(ctx.date.timeIntervalSinceReferenceDate)
            let isDark: Float = colorScheme == .dark ? 1.0 : 0.0
            GeometryReader { geo in
                Image("OperatorBackground")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: geo.size.width, height: geo.size.height)
                    .clipped()
                    .colorEffect(
                        ShaderLibrary.mapGlowEffect(
                            .float2(geo.size),
                            .float(t),
                            .float(isDark)
                        )
                    )
            }
        }
        .allowsHitTesting(false)
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
                locationManager.requestPermissionAndStart()
                if !hasSeenLocationPrimer &&
                   locationManager.authorizationStatus == .notDetermined {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                        showLocationPrimer = true
                        hasSeenLocationPrimer = true
                    }
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
        favoritesManager.favoriteStopIds.prefix(3).compactMap { stopId in
            store.stops.first { $0.id == stopId }
        }
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

    /// Formats a distance in meters as a short localized string (e.g. "420 m", "1.2 km").
    private func formatDistance(_ meters: Double) -> String {
        if meters < 1000 {
            return String(format: String(localized: "distance_meters"), Int(meters.rounded()))
        }
        let km = meters / 1000
        let formatter = NumberFormatter()
        formatter.locale = .current
        formatter.minimumFractionDigits = 1
        formatter.maximumFractionDigits = 1
        let number = formatter.string(from: NSNumber(value: km)) ?? String(format: "%.1f", km)
        return String(format: String(localized: "distance_kilometers"), number)
    }

    @ViewBuilder
    private var nearbyStopsSection: some View {
        switch locationManager.authorizationStatus {
        case .notDetermined:
            permissionPromptCard
        case .denied, .restricted:
            permissionDeniedCard
        case .authorizedWhenInUse, .authorizedAlways:
            if !nearbyStopsWithDistance.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    sectionHeader(String(localized: "nearby_you"))
                    VStack(spacing: 8) {
                        ForEach(nearbyStopsWithDistance, id: \.0.id) { (stop, distance) in
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
        @unknown default:
            EmptyView()
        }
    }

    private var permissionPromptCard: some View {
        Button {
            locationManager.requestPermissionAndStart()
        } label: {
            HStack(spacing: 10) {
                LucideIcon.mapPin.sized(20)
                    .foregroundStyle(AppTheme.accent)
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "nearby_enable_title"))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                    Text(String(localized: "nearby_enable_subtitle"))
                        .font(.system(size: 12))
                        .foregroundStyle(AppTheme.textSecondary)
                }
                Spacer()
                LucideIcon.chevronRight.sized(14)
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(14)
            .adaptiveGlass(in: RoundedRectangle(cornerRadius: 12), withShadow: false)
        }
        .buttonStyle(.plain)
    }

    /// Shown when user previously denied the location permission. Opens Settings app.
    private var permissionDeniedCard: some View {
        Button {
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url)
            }
        } label: {
            HStack(spacing: 10) {
                LucideIcon.mapPinOff.sized(20)
                    .foregroundStyle(AppTheme.textSecondary)
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "nearby_denied_title"))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                    Text(String(localized: "nearby_denied_subtitle"))
                        .font(.system(size: 12))
                        .foregroundStyle(AppTheme.textSecondary)
                }
                Spacer()
                LucideIcon.chevronRight.sized(14)
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(14)
            .adaptiveGlass(in: RoundedRectangle(cornerRadius: 12), withShadow: false)
        }
        .buttonStyle(.plain)
    }


    // MARK: - Stop Card (shared for favorites and nearby)

    private func stopCard(_ stop: ResolvedStop, showLiveBadge: Bool, distanceMeters: Double? = nil) -> some View {
        let departures = store.upcomingDepartures(forStopId: stop.id, limit: 2)
        // A stop is represented by a signpost icon regardless of transit type —
        // the visual metaphor is "the sign at the corner", not the vehicle.
        let transitTypeIcon: Image = stopPinIcon(transitTypes: stop.transitTypes).image

        return VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                transitTypeIcon
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textTertiary)
                    .frame(width: 16, height: 16)
                Text(stop.name)
                    .font(.subheadline.bold())
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(1)
                Spacer()
                if let distanceMeters {
                    Text(formatDistance(distanceMeters))
                        .font(.caption.weight(.medium))
                        .foregroundStyle(AppTheme.textTertiary)
                        .monospacedDigit()
                }
            }

            if departures.isEmpty {
                Text(String(localized: "no_departures_today"))
                    .font(.caption)
                    .foregroundStyle(AppTheme.textTertiary)
            } else {
                VStack(spacing: 6) {
                    ForEach(departures) { dep in
                        HStack(spacing: 6) {
                            LineBadge(departure: dep, size: .large)
                            Text(dep.headsign)
                                .font(.caption)
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
                    }
                }
            }
        }
        .padding(14)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 14), withShadow: true)
    }

    // MARK: - Onboarding Card

    private var onboardingCard: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(AppTheme.accent.opacity(0.12))
                    .frame(width: 56, height: 56)
                LucideIcon.mapPin.sized(24)
                    .foregroundStyle(AppTheme.accent)
            }

            VStack(spacing: 6) {
                Text(String(localized: "onboarding_title"))
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(AppTheme.textPrimary)
                Text(String(localized: "onboarding_subtitle"))
                    .font(.system(size: 14))
                    .foregroundStyle(AppTheme.textSecondary)
                    .multilineTextAlignment(.center)
            }

            Button {
                selectedTab = 1
            } label: {
                Text(String(localized: "onboarding_cta"))
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(AppTheme.accent, in: RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .background(AppTheme.glassFill, in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).strokeBorder(AppTheme.glassBorder))
    }

    // MARK: - Helpers

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.footnote.weight(.semibold))
            .foregroundStyle(AppTheme.textTertiary)
            .textCase(.uppercase)
            .kerning(0.5)
    }
}
