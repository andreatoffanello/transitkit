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

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    // Hero — full-width, no padding
                    operatorHeroSection

                    // Content sections
                    VStack(spacing: 20) {
                        favoritesSection
                        nearbyStopsSection
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 20)
                    .padding(.bottom, 100)
                }
            }
            .background(AppTheme.background.ignoresSafeArea())
            .onAppear { locationManager.requestPermissionAndStart() }
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
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
            .sheet(isPresented: $showSettings) {
                SettingsTab()
            }
            .sheet(isPresented: $showAlertList) {
                NavigationStack {
                    AlertListView()
                }
            }
            .navigationDestination(item: $selectedMainStop) { stop in
                StopDetailView(stop: stop)
            }
        }
    }

    // MARK: - Operator Hero

    @ViewBuilder
    private var operatorHeroSection: some View {
        if let config {
            heroCard(config: config)
        } else {
            heroSkeleton
        }
    }

    private func heroCard(config: OperatorConfig) -> some View {
        ZStack(alignment: .bottomLeading) {
            // Gradient background
            LinearGradient(
                colors: [AppTheme.accent, AppTheme.accent.opacity(0.75)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            // Dot-pattern texture
            Canvas { ctx, size in
                let dotColor = GraphicsContext.Shading.color(.white.opacity(0.06))
                let spacing: CGFloat = 32
                var y: CGFloat = 0
                while y < size.height + spacing {
                    var x: CGFloat = 0
                    while x < size.width + spacing {
                        let rect = CGRect(x: x - 2, y: y - 2, width: 4, height: 4)
                        ctx.fill(Path(ellipseIn: rect), with: dotColor)
                        x += spacing
                    }
                    y += spacing
                }
            }

            VStack(alignment: .leading, spacing: 16) {
                // Top row: avatar + greeting + operator
                HStack(spacing: 16) {
                    // Avatar — logo or initials
                    ZStack {
                        Circle().fill(.white.opacity(0.2))
                        if UIImage(named: "OperatorLogo") != nil {
                            Image("OperatorLogo")
                                .resizable()
                                .scaledToFill()
                                .frame(width: 64, height: 64)
                                .clipShape(Circle())
                        } else {
                            Text(initials(from: config.name))
                                .font(.title3.weight(.bold))
                                .foregroundStyle(.white)
                        }
                    }
                    .frame(width: 64, height: 64)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(greeting)
                            .font(.subheadline)
                            .foregroundStyle(.white.opacity(0.75))
                        Text(config.name)
                            .font(.title2.weight(.bold))
                            .foregroundStyle(.white)
                            .lineLimit(1)
                        if !config.region.isEmpty {
                            Text(config.region)
                                .font(.caption)
                                .foregroundStyle(.white.opacity(0.65))
                                .lineLimit(1)
                        }
                    }

                    Spacer()
                }

                // Alert banner — shows only when active alerts exist
                if !alertStore.activeAlerts.isEmpty {
                    alertBanner(count: alertStore.activeAlerts.count,
                                severity: highestSeverity(alertStore.activeAlerts))
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 28)
        }
        .frame(maxWidth: .infinity)
        .shadow(color: AppTheme.accent.opacity(0.25), radius: 16, y: 8)
    }

    private func highestSeverity(_ alerts: [GtfsRtAlert]) -> AlertSeverity {
        alerts.map(\.severity).max(by: { $0.rawValue < $1.rawValue }) ?? .unknown
    }

    /// Tappable pill inside the hero that opens the alert list sheet.
    /// Color follows severity: SEVERE/WARNING = warm on-white, INFO/UNKNOWN = neutral on-white.
    private func alertBanner(count: Int, severity: AlertSeverity) -> some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showAlertList = true
        } label: {
            HStack(spacing: 10) {
                LucideIcon.alertTriangle.sized(16)
                    .foregroundStyle(.white)
                Text(alertBannerLabel(count: count))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                Spacer(minLength: 0)
                LucideIcon.chevronRight.sized(14)
                    .foregroundStyle(.white.opacity(0.7))
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(.white.opacity(severity == .severe ? 0.22 : 0.16))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(.white.opacity(0.25), lineWidth: 0.5)
            )
        }
        .buttonStyle(PressableButtonStyle())
        .accessibilityIdentifier("home_alert_banner")
    }

    private func alertBannerLabel(count: Int) -> String {
        count == 1
            ? String(localized: "alerts_banner_one")
            : String(format: String(localized: "alerts_banner_many"), count)
    }

    private var heroSkeleton: some View {
        HStack(spacing: 16) {
            Circle()
                .fill(AppTheme.glassFill)
                .frame(width: 64, height: 64)
            VStack(alignment: .leading, spacing: 6) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(AppTheme.glassFill)
                    .frame(width: 100, height: 13)
                RoundedRectangle(cornerRadius: 4)
                    .fill(AppTheme.glassFill)
                    .frame(width: 160, height: 20)
            }
            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 28)
        .background(AppTheme.glassFill)
        .frame(maxWidth: .infinity)
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

    private func initials(from name: String) -> String {
        let words = name.split(separator: " ").prefix(2)
        return words.compactMap { $0.first }.map(String.init).joined()
    }
}
