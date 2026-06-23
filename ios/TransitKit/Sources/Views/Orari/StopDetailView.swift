import SwiftUI
import MapKit

private enum PlannerEntry: Identifiable {
    case origin(ResolvedStop)
    case destination(ResolvedStop)
    var id: String { switch self { case .origin: "origin"; case .destination: "destination" } }
}

/// Full stop detail screen: inline 3D map header + hero expand overlay showing departures.
/// THE MOST IMPORTANT VIEW — shows next departures, full schedule by day, line filtering, dock indicators.
///
/// Sub-views extracted into `StopDetail/`:
/// - `FullScheduleSheet` — fullScreenCover with day/line filter + hourly departure board
/// - `ExpandedMapOverlay` — full-screen immersive map shown when the compact map is expanded
struct StopDetailView: View {
    let stop: ResolvedStop
    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(AlertStore.self) private var alertStore
    @Environment(DeepLinkRouter.self) private var router
    @State private var mapPosition: MapCameraPosition = .automatic
    @State private var showFullSchedule = false
    @State private var showMoreDepartures = false
    @State private var filterLine: String?
    @Environment(FavoritesManager.self) private var favoritesManager

    /// Anchor id for the bottom AVVISI section — used by the chip below the
    /// map header to scroll the user down to the inline alert cards.
    private let alertsAnchorId = "stop-alerts-section"

    @State private var mapExpanded: Bool = false
    @State private var expandedMapPosition: MapCameraPosition = .automatic
    @State private var showMapAppPicker = false
    @State private var mapReady = false
    @State private var plannerEntry: PlannerEntry? = nil
    /// Incremented every 30s to force re-evaluation of departure times and remove past entries.
    @State private var refreshTick: Int = 0
    /// Memoized active alerts touching this stop or its serving routes.
    /// Without caching, the underlying `Set` construction + `filter` runs 3 times
    /// per body (chip presence check, list render, count badge).
    @State private var relevantAlertsCache: [GtfsRtAlert] = []

    private let initialVisibleCount = 5

    private var stopCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng)
    }

    // Camera for the compact map: 3D pitched, center offset north to compensate pitch perspective.
    private var compactCamera: MapCameraPosition {
        let offsetLat = stop.lat + 0.0006
        return .camera(MapCamera(
            centerCoordinate: CLLocationCoordinate2D(latitude: offsetLat, longitude: stop.lng),
            distance: 600,
            heading: 0,
            pitch: 60
        ))
    }

    // Initial camera — starts zoomed out for the fly-in animation on appear.
    private var flyInStartCamera: MapCameraPosition {
        .camera(MapCamera(
            centerCoordinate: stopCoordinate,
            distance: 1200,
            heading: 0,
            pitch: 40
        ))
    }

    // MARK: - Departures

    private var upcomingDepartures: [Departure] {
        _ = refreshTick // ensures SwiftUI re-evaluates this when the tick fires
        let deps = store.upcomingDepartures(forStopId: stop.id, limit: 15) { tripId in
            tripId.flatMap { vehicleStore.reliableDelayMinutes(forTripId: $0) }
        }
        guard let line = filterLine else { return deps }
        return deps.filter { $0.lineName == line }
    }

    private var allDayGroups: [DayGroup: [Departure]] {
        store.departures(forStopId: stop.id)
    }

    /// Available line names at this stop (for filtering).
    /// Uses the static line list from stop data — stable across the day, not dependent on
    /// which lines have upcoming departures right now. Prevents chips from disappearing as
    /// service ends for individual lines.
    private var availableLines: [String] {
        stop.lineNames
    }

    // MARK: - Alerts (stopId OR any of the stop's lines)

    /// Route ids served by this stop, resolved from `lineNames` via the schedule store.
    /// Used to surface alerts that affect a route stopping here even when the alert's
    /// `affectedStopIds` doesn't list this station explicitly.
    private var stopRouteIds: Set<String> {
        Set(stop.lineNames.compactMap { name in
            store.routes.first(where: { $0.name == name })?.id
        })
    }

    /// Recomputes `relevantAlertsCache`. Driven by `.task(id: relevantAlertsKey)`
    /// so the filter runs only when the alert feed refreshes (every 60s) or the
    /// stop changes — not on every body redraw.
    private func recomputeRelevantAlerts() {
        let routeIds = stopRouteIds
        let stationIds = Set([stop.id] + stop.gtfsStopIds)
        relevantAlertsCache = alertStore.activeAlerts.filter { a in
            !a.affectedStopIds.isDisjoint(with: stationIds)
                || a.isRelevant(forRoutes: routeIds)
        }
    }

    private struct RelevantAlertsKey: Hashable {
        let stopId: String
        let lastFetchedAt: Date?
        let routesCount: Int
    }

    private var relevantAlertsKey: RelevantAlertsKey {
        RelevantAlertsKey(
            stopId: stop.id,
            lastFetchedAt: alertStore.lastFetchedAt,
            routesCount: store.routes.count
        )
    }

    // MARK: - Body

    var body: some View {
        ZStack {
            ScrollViewReader { proxy in
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        mapHeader
                        if !relevantAlertsCache.isEmpty {
                            alertsChip(scrollProxy: proxy)
                                .padding(.horizontal, 16)
                                .padding(.top, 12)
                        }
                        stopInlineContent
                        StopAlertsSection(alerts: relevantAlertsCache)
                            .id(alertsAnchorId)
                    }
                }
                .background(AppTheme.background.ignoresSafeArea())
                .ignoresSafeArea(edges: .top)
            }

            if mapExpanded {
                ExpandedMapOverlay(
                    stop: stop,
                    expandedMapPosition: $expandedMapPosition,
                    mapExpanded: $mapExpanded
                )
                .transition(
                    .asymmetric(
                        insertion: .scale(scale: 0.92, anchor: UnitPoint(x: 0.88, y: 0.22))
                            .combined(with: .opacity),
                        removal: .scale(scale: 0.92, anchor: UnitPoint(x: 0.88, y: 0.22))
                            .combined(with: .opacity)
                    )
                )
                .zIndex(10)
            }
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .adaptiveNavBarBackground()
        .toolbar(.hidden, for: .tabBar)
        // Nav bar nascosta quando la mappa è espansa — l'overlay è immersivo
        // e il track badge linee vive nella zona alta (Android parity).
        .toolbar(mapExpanded ? .hidden : .visible, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .principal) {
                VStack(spacing: 1) {
                    Text(stop.name)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    favoritesManager.toggle(stop.id)
                } label: {
                    let isFav = favoritesManager.isFavorite(stop.id)
                    Group {
                        if isFav {
                            LucideIcon.starFilled.sized(18)
                                .foregroundStyle(AppTheme.accent)
                        } else {
                            LucideIcon.star.sized(18)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .contentTransition(.symbolEffect(.replace))
                }
                .accessibilityLabel(favoritesManager.isFavorite(stop.id)
                    ? String(localized: "remove_from_favorites")
                    : String(localized: "add_to_favorites"))
                .accessibilityIdentifier("btn_favorite")
            }
        }
        .confirmationDialog(Text(String(localized: "open_in_prompt")), isPresented: $showMapAppPicker) {
            Button("Apple Maps") { openInAppleMaps() }
            if UIApplication.shared.canOpenURL(URL(string: "comgooglemaps://")!) {
                Button("Google Maps") { openInGoogleMaps() }
            }
            if UIApplication.shared.canOpenURL(URL(string: "waze://")!) {
                Button("Waze") { openInWaze() }
            }
            Button(String(localized: "cancel"), role: .cancel) { }
        }
        .fullScreenCover(isPresented: $showFullSchedule) {
            FullScheduleSheet(stop: stop)
        }
        .fullScreenCover(item: $plannerEntry) { entry in
            NavigationStack {
                switch entry {
                case .origin(let s):
                    PlannerScreen(initialOrigin: .stop(s))
                case .destination(let s):
                    PlannerScreen(initialDestination: .stop(s))
                }
            }
        }
        .onAppear {
            mapPosition = flyInStartCamera
        }
        .onDisappear { }
        // React both on first appear AND on a fresh deeplink coming in while
        // this StopDetailView is already on screen (e.g. user is on stop A and
        // a `transitkit://stop/B/schedule` arrives). `onAppear` alone misses
        // the second case because the view stays mounted across the push.
        .task(id: router.openScheduleForStop) {
            guard router.openScheduleForStop == stop.id else { return }
            router.openScheduleForStop = nil
            try? await Task.sleep(for: .milliseconds(400))
            showFullSchedule = true
        }
        .task(id: stop.id) {
            // Fly-in: let tiles load at far distance, then zoom in
            mapPosition = flyInStartCamera
            try? await Task.sleep(for: .milliseconds(500))
            mapReady = true
            withAnimation(.easeInOut(duration: 1.2)) {
                mapPosition = compactCamera
            }
        }
        .task {
            // Refresh departure times every 15s so past entries disappear automatically
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(15))
                refreshTick &+= 1
            }
        }
        .task(id: relevantAlertsKey) { recomputeRelevantAlerts() }
    }

    // MARK: - Map Header

    @ViewBuilder
    private var mapHeader: some View {
        ZStack(alignment: .bottomTrailing) {
            Map(position: $mapPosition) {
                if mapReady, stop.docks.isEmpty {
                    Annotation(stop.name, coordinate: stopCoordinate, anchor: .bottom) {
                        ZStack {
                            Circle()
                                .fill(AppTheme.accent)
                                .frame(width: 28, height: 28)
                            LucideIcon.signpost.sized(13)
                                .foregroundStyle(.white)
                        }
                        .shadow(color: .black.opacity(0.2), radius: 3, y: 1)
                    }
                }
            }
            .mapStyle(.standard(pointsOfInterest: .excludingAll))
            .allowsHitTesting(false)

            MapCircleButton(icon: .maximize2) {
                withAnimation(.spring(duration: 0.35)) {
                    mapExpanded = true
                }
            }
            .padding(12)
            .accessibilityLabel(Text(String(localized: "a11y_expand_map")))
            .accessibilityIdentifier("btn_expand_map")
        }
        .frame(height: UIScreen.main.bounds.height * 0.4)
        .clipped()
    }

    // MARK: - Inline Content (below map header)

    private var stopInlineContent: some View {
        let allNext = upcomingDepartures
        let visible = showMoreDepartures ? allNext : Array(allNext.prefix(initialVisibleCount))
        let extraCount = allNext.count - initialVisibleCount
        // Group upcoming departures by headsign so a multi-direction stop
        // doesn't interleave the two directions by time. `allNext` is already
        // sorted upstream; `Dictionary(grouping:by:)` is unordered, so we
        // collect group keys in first-occurrence order. Falls back to the
        // existing flat 5+N rendering when there's a single direction.
        // Pattern from Movete `133091db2`.
        let directionGroups: [(headsign: String, deps: [Departure])] = {
            var seen: [String: [Departure]] = [:]
            var order: [String] = []
            for dep in allNext {
                if seen[dep.headsign] == nil {
                    seen[dep.headsign] = []
                    order.append(dep.headsign)
                }
                seen[dep.headsign]?.append(dep)
            }
            return order.map { ($0, seen[$0] ?? []) }
        }()
        let useGroups = directionGroups.count >= 2

        return VStack(spacing: 0) {
            // Stop name header
            inlineStopHeader

            // Line badge filter row — badges act as filter chips; single tap selects/deselects
            if availableLines.count >= 1 {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        if availableLines.count > 1 {
                            Button {
                                withAnimation(.smooth(duration: 0.2)) { filterLine = nil }
                            } label: {
                                Text(String(localized: "filter_all"))
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundStyle(filterLine == nil ? .white : AppTheme.textSecondary)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 5)
                                    .background(
                                        filterLine == nil ? AppTheme.accent : AppTheme.bgSecondary,
                                        in: RoundedRectangle(cornerRadius: 8, style: .continuous)
                                    )
                            }
                            .buttonStyle(.plain)
                            .accessibilityIdentifier("btn_filter_all_lines")
                        }

                        ForEach(availableLines, id: \.self) { line in
                            let route = store.routes.first { $0.name == line }
                            let isSelected = filterLine == line
                            Button {
                                withAnimation(.smooth(duration: 0.2)) {
                                    filterLine = (filterLine == line) ? nil : line
                                }
                            } label: {
                                LineBadge(
                                    name: line,
                                    color: route?.color ?? "#666666",
                                    textColor: route?.textColor ?? "#FFFFFF",
                                    transitType: route?.resolvedTransitType ?? .bus,
                                    size: .medium
                                )
                            }
                            .buttonStyle(.plain)
                            .opacity(filterLine != nil && !isSelected ? 0.3 : 1.0)
                            .animation(.smooth(duration: 0.2), value: filterLine)
                            .accessibilityIdentifier("btn_filter_line_\(line)")
                        }
                    }
                    .padding(.horizontal, 20)
                }
                .padding(.bottom, 14)
            }

            // Departure rows, empty states
            if useGroups {
                ForEach(Array(directionGroups.enumerated()), id: \.offset) { idx, group in
                    directionGroupSection(headsign: group.headsign,
                                          deps: Array(group.deps.prefix(8)),
                                          isFirst: idx == 0)
                }
            } else if !visible.isEmpty {
                nextDeparturesSection(visible)

                if !showMoreDepartures && extraCount > 0 {
                    Button {
                        withAnimation(.spring(duration: 0.3)) {
                            showMoreDepartures = true
                        }
                    } label: {
                        HStack(spacing: 4) {
                            LucideIcon.chevronDown.sized(11)
                            Text(String(format: NSLocalizedString("show_more", comment: ""), extraCount))
                                .font(.system(size: 13, weight: .semibold))
                        }
                        .foregroundStyle(AppTheme.accent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(AppTheme.accent.opacity(0.06))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 4)
                }
            } else if filterLine != nil && upcomingDepartures.isEmpty {
                Text(String(format: NSLocalizedString("no_departures_for_line", comment: ""), filterLine ?? ""))
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
                    .padding(.horizontal, 20)
            } else if !store.isLoading {
                VStack(spacing: 8) {
                    LucideIcon.clock.sized(28)
                        .foregroundStyle(AppTheme.textTertiary)
                    Text(String(localized: "no_departures"))
                        .font(.system(size: 14))
                        .foregroundStyle(AppTheme.textSecondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 32)
            }

            // Full schedule button
            if !allDayGroups.isEmpty {
                Button {
                    showFullSchedule = true
                } label: {
                    HStack(spacing: 6) {
                        LucideIcon.clock.sized(14)
                        Text(String(localized: "full_schedule"))
                            .font(.system(size: 14, weight: .semibold))
                    }
                    .foregroundStyle(AppTheme.accent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(AppTheme.accent.opacity(0.08))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .accessibilityIdentifier("btn_full_schedule")
                .padding(.horizontal, 20)
                .padding(.top, 4)
                .padding(.bottom, 100)
            }
        }
    }

    // MARK: - Inline Stop Header

    private var inlineStopHeader: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(stop.name)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(AppTheme.textPrimary)

                HStack(spacing: 6) {
                    ForEach(Array(stop.transitTypes).sorted(by: { $0.rawValue < $1.rawValue }), id: \.self) { type in
                        HStack(spacing: 4) {
                            type.icon.sized(11)
                            Text(type.displayName)
                                .font(.system(size: 12, weight: .medium))
                        }
                        .foregroundStyle(AppTheme.textSecondary)
                    }
                }
            }

            Spacer()

            Menu {
                Button {
                    plannerEntry = .origin(stop)
                } label: {
                    Label { Text(String(localized: "planner_depart_from_here")) } icon: { LucideIcon.navigation.sized(16) }
                }
                Button {
                    plannerEntry = .destination(stop)
                } label: {
                    Label { Text(String(localized: "planner_arrive_here")) } icon: { LucideIcon.mapPin.sized(16) }
                }
                Divider()
                Button {
                    openInMaps()
                } label: {
                    Label { Text(String(localized: "a11y_navigate_to_stop")) } icon: { LucideIcon.map.sized(16) }
                }
            } label: {
                LucideIcon.navigation.sized(18)
                    .foregroundStyle(AppTheme.accent)
                    .frame(width: 44, height: 44)
                    .background(AppTheme.accent.opacity(0.1))
                    .clipShape(Circle())
            }
            .accessibilityLabel(String(localized: "a11y_navigate_to_stop"))
            .accessibilityIdentifier("btn_navigate_sheet")
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 12)
    }

    // MARK: - Next Departures

    private func nextDeparturesSection(_ departures: [Departure]) -> some View {
        VStack(spacing: 0) {
            // Section label
            Text(String(localized: "today_label"))
                .font(.footnote.weight(.semibold))
                .foregroundStyle(AppTheme.textTertiary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 4)

            // Departure rows
            VStack(spacing: 0) {
                ForEach(Array(departures.enumerated()), id: \.element.id) { index, dep in
                    let isFirst = index == 0

                    NavigationLink(value: TripTarget(departure: dep, fromStop: stop)) {
                        DepartureRow(departure: dep, isFirst: isFirst, hideBadge: filterLine != nil)
                            .padding(.horizontal, 20)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("departure_row_\(dep.id)")
                }
            }
            .background(AppTheme.bgSecondary.opacity(0.4))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
        }
    }

    /// Single direction section: "→ {headsign}" header + capped departure list.
    /// Used when a stop has ≥2 distinct headsigns (multi-direction). Pattern
    /// from Movete `133091db2`. The first section's first row is the "next"
    /// row globally (gets the prominent treatment in `DepartureRow`).
    private func directionGroupSection(headsign: String, deps: [Departure], isFirst: Bool) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 4) {
                LucideIcon.arrowRight.sized(11)
                    .foregroundStyle(AppTheme.textSecondary)
                Text(headsign)
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(AppTheme.textSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.top, isFirst ? 8 : 14)
            .padding(.bottom, 6)

            VStack(spacing: 0) {
                ForEach(Array(deps.enumerated()), id: \.element.id) { index, dep in
                    let isHeroRow = isFirst && index == 0
                    NavigationLink(value: TripTarget(departure: dep, fromStop: stop)) {
                        DepartureRow(departure: dep, isFirst: isHeroRow, hideBadge: filterLine != nil)
                            .padding(.horizontal, 20)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("departure_row_\(dep.id)")
                }
            }
            .background(AppTheme.bgSecondary.opacity(0.4))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .padding(.horizontal, 16)
        }
    }

    // MARK: - Helpers

    private func openInMaps() {
        showMapAppPicker = true
    }

    private func openInAppleMaps() {
        let mapItem = MKMapItem(placemark: MKPlacemark(coordinate: stopCoordinate))
        mapItem.name = stop.name
        mapItem.openInMaps(launchOptions: [
            MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeWalking
        ])
    }

    private func openInGoogleMaps() {
        let url = URL(string: "comgooglemaps://?q=\(stopCoordinate.latitude),\(stopCoordinate.longitude)&zoom=17")!
        UIApplication.shared.open(url)
    }

    private func openInWaze() {
        let url = URL(string: "waze://?ll=\(stopCoordinate.latitude),\(stopCoordinate.longitude)&navigate=false")!
        UIApplication.shared.open(url)
    }

    // MARK: - Alerts chip (jumps to bottom AVVISI section)

    @ViewBuilder
    private func alertsChip(scrollProxy: ScrollViewProxy) -> some View {
        let count = relevantAlertsCache.count
        Button {
            withAnimation(.smooth(duration: 0.5)) {
                scrollProxy.scrollTo(alertsAnchorId, anchor: .top)
            }
        } label: {
            HStack(spacing: 8) {
                LucideIcon.alertTriangle.sized(14)
                    .foregroundStyle(.orange)
                Text(
                    count == 1
                        ? String(localized: "stop_alerts_chip_one")
                        : String(format: NSLocalizedString("stop_alerts_chip_other", comment: ""), count)
                )
                .font(.footnote.weight(.semibold))
                .foregroundStyle(AppTheme.textPrimary)
                Spacer(minLength: 0)
                LucideIcon.chevronDown.sized(11)
                    .foregroundStyle(AppTheme.textSecondary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(AppTheme.bgSecondary)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color.orange.opacity(0.4), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .sensoryFeedback(.selection, trigger: count)
        .accessibilityIdentifier("stop_alerts_chip")
    }
}
