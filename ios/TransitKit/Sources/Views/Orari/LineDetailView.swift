import SwiftUI

/// Route detail screen — Movete-parity layout (May 2026).
/// Order: alerts → direction pills → live vehicles (filtered by direction) → stops timeline.
/// No hero map: the toolbar maximize button switches to the Mappa tab focused on this line.
struct LineDetailView: View {
    let route: APIRoute
    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(AlertStore.self) private var alertStore
    @Environment(FavoritesManager.self) private var favoritesManager
    @Environment(DeepLinkRouter.self) private var router
    @State private var selectedDirectionId: Int = 0
    @State private var pushedTrip: TripTarget?

    private var lineColor: Color { Color(hex: route.color ?? "#000000") }

    private var selectedDirection: APIRouteDirection? {
        route.directions.first { $0.directionId == selectedDirectionId }
    }

    private var stopsInDirection: [ResolvedStop] {
        store.stopsForRoute(route.id, directionId: selectedDirectionId)
    }

    /// Vehicles on this route, split by selected direction.
    /// Vehicles whose tripId doesn't resolve to a known direction fall through to "this".
    private var vehiclesInThisDirection: [GtfsRtVehicle] {
        vehicleStore.vehicles(forRouteId: route.id).filter {
            (store.direction(forTripId: $0.tripId) ?? selectedDirectionId) == selectedDirectionId
        }
    }

    private var oppositeDirectionCount: Int {
        guard route.directions.count > 1 else { return 0 }
        return vehicleStore.vehicles(forRouteId: route.id).filter {
            (store.direction(forTripId: $0.tripId) ?? selectedDirectionId) != selectedDirectionId
        }.count
    }

    private var anyVehiclesOnRoute: Bool {
        !vehicleStore.vehicles(forRouteId: route.id).isEmpty
    }

    // MARK: - Body

    var body: some View {
        ZStack(alignment: .top) {
            AppTheme.background.ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    lineAlertsSection

                    if route.directions.count > 1 {
                        DirectionPills(
                            directions: route.directions,
                            lineColor: lineColor,
                            selectedDirectionId: $selectedDirectionId
                        )
                    }

                    if anyVehiclesOnRoute {
                        LiveVehiclesSection(
                            vehicles: vehiclesInThisDirection,
                            oppositeDirectionCount: oppositeDirectionCount,
                            lineColor: lineColor,
                            onVehicleTap: { vehicle in
                                // Tap mirrors Movete: drill into the trip timeline so the
                                // user follows this vehicle stop-by-stop. Fall back to the
                                // map preview only if the trip isn't in the loaded
                                // schedule (rare race during cold start).
                                if let pair = store.firstDepartureAndStop(forTripId: vehicle.tripId) {
                                    pushedTrip = TripTarget(departure: pair.0, fromStop: pair.1)
                                } else {
                                    router.pendingMapPreviewVehicleId = vehicle.id
                                }
                            },
                            onSwitchDirection: {
                                guard let other = route.directions
                                    .first(where: { $0.directionId != selectedDirectionId }) else { return }
                                withAnimation(.spring(response: 0.32, dampingFraction: 0.82)) {
                                    selectedDirectionId = other.directionId
                                }
                            },
                            resolveNextStop: { vehicle in
                                store.stop(forId: vehicle.currentStopId)?.name
                            }
                        )
                    }

                    if !stopsInDirection.isEmpty {
                        stopsListHeader
                        stopsTimeline
                    }

                    Spacer(minLength: 100)
                }
                .padding(.top, 8)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .adaptiveNavBarBackground()
        .toolbar(.hidden, for: .tabBar)
        .toolbar {
            ToolbarItem(placement: .principal) {
                HStack(spacing: 8) {
                    LineBadge(
                        name: route.name,
                        color: route.color ?? "#000000",
                        textColor: route.textColor ?? "#FFFFFF",
                        size: .small
                    )
                    Text(route.longName ?? route.name)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    favoritesManager.toggleRoute(route.id)
                } label: {
                    LucideIcon.star.sized(18)
                        .foregroundStyle(favoritesManager.isFavoriteRoute(route.id)
                                         ? Color.accentColor
                                         : AppTheme.textSecondary)
                }
                .accessibilityLabel(favoritesManager.isFavoriteRoute(route.id)
                    ? String(localized: "remove_line_from_favorites")
                    : String(localized: "add_line_to_favorites"))
                .accessibilityIdentifier("btn_favorite_line")
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    router.pendingMapPreviewRouteId = route.id
                    router.pendingMapPreviewDirectionId = selectedDirectionId
                } label: {
                    LucideIcon.maximize2.sized(18)
                        .foregroundStyle(AppTheme.textPrimary)
                }
                .accessibilityLabel(Text(String(localized: "a11y_line_map")))
                .accessibilityIdentifier("btn_expand_line_map")
            }
        }
        .navigationDestination(for: ResolvedStop.self) { stop in
            StopDetailView(stop: stop)
        }
        .navigationDestination(item: $pushedTrip) { target in
            TripDetailView(departure: target.departure, fromStop: target.fromStop)
        }
        .onAppear {
            if let dirId = router.pendingDirectionId,
               route.directions.contains(where: { $0.directionId == dirId }) {
                selectedDirectionId = dirId
                router.pendingDirectionId = nil
            } else if let first = route.directions.first {
                selectedDirectionId = first.directionId
            }
            if router.autoOpenMap {
                router.autoOpenMap = false
                router.pendingMapPreviewRouteId = route.id
                router.pendingMapPreviewDirectionId = selectedDirectionId
            }
        }
    }

    // MARK: - Line Alerts Section

    @ViewBuilder
    private var lineAlertsSection: some View {
        let alerts = alertStore.alerts(forRouteId: route.id)
        if !alerts.isEmpty {
            VStack(spacing: 8) {
                ForEach(alerts) { alert in
                    NavigationLink {
                        AlertDetailView(alert: alert)
                    } label: {
                        AlertCard(alert: alert)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 8)
        }
    }

    // MARK: - Stops List Header

    private var stopsListHeader: some View {
        HStack(spacing: 6) {
            LucideIcon.mapPin.sized(11)
                .foregroundStyle(lineColor)
            Text(String(localized: "stops_served"))
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(AppTheme.textSecondary)
            Spacer()
            Text("\(stopsInDirection.count)")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(lineColor)
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(lineColor.opacity(0.12), in: Capsule())
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 8)
    }

    // MARK: - Stops Timeline

    private var stopsTimeline: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(stopsInDirection.enumerated()), id: \.element.id) { idx, stop in
                let isFirst = idx == 0
                let isLast = idx == stopsInDirection.count - 1
                let isTerminal = isFirst || isLast
                let coincidences = stop.lineNames.filter { $0 != route.name }

                NavigationLink(value: stop) {
                    HStack(alignment: .top, spacing: 0) {
                        VStack(spacing: 0) {
                            if isFirst {
                                Color.clear.frame(width: 3, height: 14)
                            } else {
                                Rectangle().fill(lineColor).frame(width: 3, height: 14)
                            }

                            ZStack {
                                Circle()
                                    .fill(lineColor)
                                    .frame(width: isTerminal ? 12 : 8, height: isTerminal ? 12 : 8)
                                if isTerminal {
                                    Circle()
                                        .fill(AppTheme.background)
                                        .frame(width: 5, height: 5)
                                }
                            }

                            if isLast {
                                Color.clear.frame(width: 3).frame(maxHeight: .infinity)
                            } else {
                                Rectangle().fill(lineColor).frame(width: 3).frame(maxHeight: .infinity)
                            }
                        }
                        .frame(width: 32)

                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 6) {
                                Text(stop.name)
                                    .font(.system(size: 15, weight: isTerminal ? .semibold : .regular))
                                    .foregroundStyle(AppTheme.textPrimary)
                                    .lineLimit(1)

                                Spacer(minLength: 4)

                                LucideIcon.chevronRight.sized(10)
                                    .foregroundStyle(AppTheme.textTertiary)
                            }

                            if !coincidences.isEmpty {
                                coincidenceBadges(lines: coincidences)
                            }
                        }
                        .padding(.vertical, isTerminal ? 14 : 10)
                        .padding(.trailing, 16)
                    }
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("line_stop_\(stop.id)")
            }
        }
        .padding(.horizontal, 16)
    }

    // MARK: - Coincidence Badges

    @ViewBuilder
    private func coincidenceBadges(lines: [String]) -> some View {
        let maxVisible = 4
        let visible = Array(lines.prefix(maxVisible))
        let overflow = lines.count - visible.count

        HStack(spacing: 4) {
            ForEach(visible, id: \.self) { name in
                let r = store.routes.first { $0.name == name }
                LineBadge(
                    name: name,
                    color: r?.color ?? "#666666",
                    textColor: r?.textColor ?? "#FFFFFF",
                    transitType: r?.resolvedTransitType ?? .bus,
                    size: .medium
                )
            }
            if overflow > 0 {
                OverflowBadge(count: overflow)
            }
        }
    }
}
