import SwiftUI
import CoreLocation

// MARK: - HomeTab

/// Landing screen with operator hero, quick-access shortcuts, and favorite stops.
/// Receives a `selectedTab` binding so quick-access cards can switch the active tab.
struct HomeTab: View {
    @Binding var selectedTab: Int
    @Environment(ScheduleStore.self) private var store
    @Environment(FavoritesManager.self) private var favoritesManager
    @Environment(LocationManager.self) private var locationManager

    private var config: OperatorConfig? { try? ConfigLoader.load() }
    @State private var selectedMainStop: ResolvedStop?

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
                VStack(spacing: 20) {
                    // Alert Banner slot — reserved for future use
                    EmptyView()

                    // Operator hero
                    operatorHeroSection

                    // Quick-access shortcuts
                    quickAccessSection

                    // Favorite stops
                    favoritesSection

                    // Near you (GPS-based)
                    nearbyStopsSection
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 100)
            }
            .background(AppTheme.background.ignoresSafeArea())
            .onAppear { locationManager.requestPermissionAndStart() }
            .navigationTitle(String(localized: "tab_home"))
            .navigationBarTitleDisplayMode(.large)
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
        HStack(spacing: 14) {
            // Avatar circle with operator initials
            ZStack {
                Circle()
                    .fill(AppTheme.accent)
                Text(initials(from: config.name))
                    .font(.title3.weight(.bold))
                    .foregroundStyle(.white)
            }
            .frame(width: 56, height: 56)
            .shadow(color: AppTheme.accent.opacity(0.35), radius: 8, y: 3)

            VStack(alignment: .leading, spacing: 4) {
                Text(greeting)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
                    .lineLimit(1)
                Text(config.name)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    LucideIcon.clock.sized(10)
                        .foregroundStyle(AppTheme.textSecondary)
                    Text("\(store.stops.count) \(String(localized: "hero_stops"))")
                        .font(.caption)
                        .foregroundStyle(AppTheme.textSecondary)
                    Text("\u{00B7}")
                        .font(.caption)
                        .foregroundStyle(AppTheme.textSecondary)
                    LucideIcon.bus.sized(10)
                        .foregroundStyle(AppTheme.textSecondary)
                    Text("\(store.routes.count) \(String(localized: "hero_lines"))")
                        .font(.caption)
                        .foregroundStyle(AppTheme.textSecondary)
                }
            }

            Spacer()
        }
        .padding(16)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 16), withShadow: true)
    }

    private var heroSkeleton: some View {
        HStack(spacing: 14) {
            Circle()
                .fill(AppTheme.glassFill)
                .frame(width: 56, height: 56)
            VStack(alignment: .leading, spacing: 6) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(AppTheme.glassFill)
                    .frame(width: 140, height: 16)
                RoundedRectangle(cornerRadius: 4)
                    .fill(AppTheme.glassFill)
                    .frame(width: 90, height: 12)
            }
            Spacer()
        }
        .padding(16)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Quick Access

    private var quickAccessSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(String(localized: "home_quick_access"))

            HStack(spacing: 12) {
                quickCard(
                    icon: LucideIcon.clock.image,
                    label: String(localized: "tab_schedules"),
                    subtitle: String(localized: "quick_find_stop"),
                    tab: 1
                )
                quickCard(
                    icon: LucideIcon.map.image,
                    label: String(localized: "tab_map"),
                    subtitle: String(localized: "quick_explore_map"),
                    tab: 2
                )
                quickCard(
                    icon: LucideIcon.info.image,
                    label: String(localized: "tab_info"),
                    subtitle: config?.name ?? "",
                    tab: 3
                )
            }
        }
    }

    private func quickCard(icon: Image, label: String, subtitle: String, tab: Int) -> some View {
        Button {
            selectedTab = tab
        } label: {
            VStack(spacing: 8) {
                icon
                    .font(.title3.weight(.medium))
                    .foregroundStyle(AppTheme.accent)
                    .frame(width: 28, height: 28)
                Text(label)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(1)
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.caption2)
                        .foregroundStyle(AppTheme.textTertiary)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(minWidth: 90)
            .padding(.vertical, 16)
            .padding(.horizontal, 8)
            .adaptiveGlass(in: RoundedRectangle(cornerRadius: 12), withShadow: false)
        }
        .buttonStyle(.plain)
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
                        favoriteStopCard(stop)
                    }
                }
            }
        }
    }

    private func favoriteStopCard(_ stop: ResolvedStop) -> some View {
        let departures = store.upcomingDepartures(forStopId: stop.id, limit: 2)
        let transitTypeIcon: Image = stop.transitTypes.first.map { $0.icon.image }
            ?? LucideIcon.bus.image

        return VStack(alignment: .leading, spacing: 8) {
            // Top row: stop name + transit type icon
            HStack(spacing: 8) {
                Text(stop.name)
                    .font(.subheadline.bold())
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(1)
                Spacer()
                transitTypeIcon
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textSecondary)
            }

            // Departure rows
            if departures.isEmpty {
                Text(String(localized: "no_departures_today"))
                    .font(.caption)
                    .foregroundStyle(AppTheme.textTertiary)
            } else {
                ForEach(departures) { dep in
                    HStack(spacing: 6) {
                        LineBadge(departure: dep, size: .small)
                        Text(dep.headsign)
                            .font(.caption)
                            .foregroundStyle(AppTheme.textSecondary)
                            .lineLimit(1)
                            .truncationMode(.tail)
                        Spacer()
                        TimelineView(.periodic(from: .now, by: 30)) { _ in
                            TimeDisplay(departure: dep)
                        }
                    }
                }
            }
        }
        .padding(12)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 12), withShadow: false)
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
    }

    private var onboardingCard: some View {
        VStack(spacing: 16) {
            // Icon
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
                selectedTab = 1  // Navigate to Orari tab
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
        .padding(.horizontal, 16)
    }

    // MARK: - Nearby Stops (GPS-based)

    private var nearbyStops: [ResolvedStop] {
        guard let location = locationManager.location else { return [] }
        let userLat = location.coordinate.latitude
        let userLng = location.coordinate.longitude

        let radiusMeters: Double = 600
        let latDeg = radiusMeters / 111_320
        let lngDeg = radiusMeters / (111_320 * cos(userLat * .pi / 180))

        return store.stops
            .compactMap { stop -> (ResolvedStop, Double)? in
                let dlat = stop.lat - userLat
                let dlng = stop.lng - userLng
                let dist = sqrt(dlat * dlat + dlng * dlng) * 111_320
                guard dist <= radiusMeters else { return nil }
                return (stop, dist)
            }
            .sorted { $0.1 < $1.1 }
            .prefix(3)
            .map(\.0)
    }

    @ViewBuilder
    private var nearbyStopsSection: some View {
        if locationManager.authorizationStatus == .notDetermined {
            Button {
                locationManager.requestPermissionAndStart()
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "location.circle")
                        .font(.system(size: 20))
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
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12))
                        .foregroundStyle(AppTheme.textTertiary)
                }
                .padding(14)
                .adaptiveGlass(in: RoundedRectangle(cornerRadius: 12), withShadow: false)
            }
            .buttonStyle(.plain)
        } else if !nearbyStops.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                sectionHeader(String(localized: "nearby_you"))
                VStack(spacing: 8) {
                    ForEach(nearbyStops) { stop in
                        Button {
                            selectedMainStop = stop
                        } label: {
                            mainStopCard(stop)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        // If denied or no nearby stops: section hidden silently
    }

    private func mainStopCard(_ stop: ResolvedStop) -> some View {
        let departures = store.upcomingDepartures(forStopId: stop.id, limit: 2)
        let transitTypeIcon: Image = stop.transitTypes.first.map { $0.icon.image }
            ?? LucideIcon.bus.image

        return VStack(alignment: .leading, spacing: 8) {
            // Top row: stop name + transit type icon
            HStack(spacing: 8) {
                Text(stop.name)
                    .font(.subheadline.bold())
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(1)
                Spacer()
                transitTypeIcon
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textSecondary)
            }

            // Departure rows
            if departures.isEmpty {
                Text(String(localized: "no_departures_today"))
                    .font(.caption)
                    .foregroundStyle(AppTheme.textTertiary)
            } else {
                ForEach(departures) { dep in
                    HStack(spacing: 6) {
                        LineBadge(departure: dep, size: .small)
                        Text(dep.headsign)
                            .font(.caption)
                            .foregroundStyle(AppTheme.textSecondary)
                            .lineLimit(1)
                            .truncationMode(.tail)
                        Spacer()
                        TimelineView(.periodic(from: .now, by: 30)) { _ in
                            TimeDisplay(departure: dep)
                        }
                    }
                }
            }
        }
        .padding(12)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 12), withShadow: false)
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
    }


    private var emptyFavoritesCard: some View {
        VStack(spacing: 8) {
            LucideIcon.star.image
                .font(.title2)
                .foregroundStyle(AppTheme.textTertiary)
            Text(String(localized: "home_favorites_empty"))
                .font(.subheadline.weight(.medium))
                .foregroundStyle(AppTheme.textSecondary)
            Text(String(localized: "home_favorites_empty_hint"))
                .font(.caption)
                .foregroundStyle(AppTheme.textTertiary)
                .multilineTextAlignment(.center)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 12))
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
