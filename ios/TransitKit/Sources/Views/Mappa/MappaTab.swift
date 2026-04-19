import SwiftUI
import MapKit

// MARK: - Mappa Tab

/// Full-screen map tab showing all transit stops with annotations.
///
/// Features:
/// - Initial center and zoom from `OperatorConfig.map`
/// - Stop annotations with transit type icons
/// - Tap annotation to show floating preview card with next departures
/// - Route polyline overlay when a line is selected
/// - Live GTFS-RT vehicles with directional bearing
/// - Search pill at top that opens the line picker sheet
///
/// No clustering (movete parity 2026-04-17): at `.city` tier the map shows nothing,
/// at `.neighborhood` only +-square stop markers, at `.street` full pins + vehicles.
struct MappaTab: View {
    let config: OperatorConfig
    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(DeepLinkRouter.self) private var router

    // MARK: Map state
    @State private var mapRegion: MKCoordinateRegion
    @State private var zoomLevel: MapZoomLevel = .far
    @State private var visibleRegion: MKCoordinateRegion?
    @State private var mapReady = false
    @State private var cameraTask: Task<Void, Never>?

    // MARK: Rendered annotations (computed async off-main, never in body)
    @State private var renderedStops: [ResolvedStop] = []
    private let maxIndividualAnnotations = 250

    /// Background task for annotation filtering — cancelled on each camera/data change.
    @State private var annotationUpdateTask: Task<Void, Never>?

    // MARK: Route polyline cache
    /// Pre-decoded polylines for the selected route, computed once on background thread.
    /// Avoids running `decodeGooglePolyline` on every Map body re-render (60 fps during pan).
    @State private var cachedRoutePolylines: [CachedPolyline] = []
    @State private var routePolylineTask: Task<Void, Never>?

    // MARK: Selection
    /// Used only for the highlighted-annotation visual state (isSelected).
    @State private var selectedStop: ResolvedStop?
    /// Drives direct push navigation to StopDetailView (Batch C — 1-tap flow).
    @State private var navigationDestinationStop: ResolvedStop? = nil

    // MARK: Fullscreen expand
    @State private var isExpanded = false

    // MARK: Route overlay
    @State private var selectedRoute: APIRoute?
    @State private var selectedDirectionId: Int?

    // MARK: Line picker
    @State private var showLinePicker = false

    // MARK: Vehicle selection
    private struct VehicleSelection: Identifiable {
        let vehicle: GtfsRtVehicle
        let route: APIRoute?
        var id: String { vehicle.id }
    }
    @State private var selectedVehicle: VehicleSelection?
    /// When non-nil, presents TripDetailView as a sheet (opened via the vehicle card).
    @State private var tripSheetTarget: TripTarget?

    // MARK: Vehicle display
    /// Smoothly-animated vehicle list — updated with withAnimation on each feed refresh
    /// and on camera-end so new vehicles pan into view without jumping.
    @State private var displayedVehicles: [GtfsRtVehicle] = []

    // MARK: Vehicle follow
    /// True while the map camera is locked to the selected vehicle's live position.
    @State private var isFollowingVehicle = false
    /// True when the active route overlay was auto-selected by tapping a vehicle
    /// (vs. manually via line picker). Determines whether closing the vehicle card
    /// should also tear down the route overlay.
    @State private var routeSelectedByVehicle = false

    // MARK: Init

    init(config: OperatorConfig) {
        self.config = config
        let center = CLLocationCoordinate2D(
            latitude: config.map.centerLat,
            longitude: config.map.centerLng
        )
        let span = Self.spanForZoom(config.map.defaultZoom)
        _mapRegion = State(initialValue: MKCoordinateRegion(center: center, span: span))
    }

    // MARK: - Body

    var body: some View {
        ZStack {
            // Full-bleed map — extends under status bar and lateral edges
            mapContent

            // Search pill at top — centered horizontally, below status bar.
            // Tap opens the line picker sheet. Not shown while a route is active
            // (the active-line chip below takes over) nor in fullscreen mode.
            if !isExpanded && selectedRoute == nil {
                VStack {
                    searchPill
                        .padding(.top, 8)
                    Spacer()
                }
            }

            // Active-line chip (top-left) when a route is selected
            if !isExpanded, let route = selectedRoute {
                VStack {
                    HStack {
                        // Outer paddings reduced by 8pt to compensate for the
                        // 8pt hit-slop added inside `activeRouteChip` — the
                        // visible chip stays in the same screen position.
                        activeRouteChip(route)
                            .padding(.leading, 8)
                            .padding(.top, 0)
                        Spacer()
                    }
                    Spacer()
                }
            }

            // MARK: Compact controls — centered vertically on right edge
            if !isExpanded {
                HStack {
                    Spacer()
                    mapControls
                }
            }

            // MARK: Expanded controls (right + close bottom-center)
            if isExpanded {
                expandedControls
            }

            // MARK: Vehicle card overlay — floating (margins + shadow)
            if let selection = selectedVehicle {
                VStack {
                    Spacer()
                    VehicleDetailSheet(
                        vehicle: selection.vehicle,
                        route: selection.route,
                        isFollowing: isFollowingVehicle,
                        onToggleFollow: {
                            isFollowingVehicle.toggle()
                            if isFollowingVehicle { followSelectedVehicleIfNeeded() }
                        },
                        onOpenTrip: {
                            tripSheetTarget = makeTripTarget(for: selection)
                        },
                        onDismiss: {
                            isFollowingVehicle = false
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                selectedVehicle = nil
                                if routeSelectedByVehicle {
                                    selectedRoute = nil
                                    selectedDirectionId = nil
                                    routeSelectedByVehicle = false
                                }
                            }
                        }
                    )
                    .padding(.horizontal, 12)
                    .padding(.bottom, 90) // above tab bar
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                .allowsHitTesting(true)
                .ignoresSafeArea(edges: .bottom)
            }

            // MARK: Stop preview card overlay — floating
            if let stop = selectedStop, selectedVehicle == nil {
                VStack {
                    Spacer()
                    StopPreviewCard(
                        stop: stop,
                        onDismiss: {
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                selectedStop = nil
                            }
                        },
                        onOpenStop: {
                            navigationDestinationStop = stop
                            selectedStop = nil
                        }
                    )
                    .padding(.horizontal, 12)
                    .padding(.bottom, 90)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                .allowsHitTesting(true)
                .ignoresSafeArea(edges: .bottom)
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .toolbar(isExpanded ? .hidden : .visible, for: .tabBar)
        .navigationDestination(item: $navigationDestinationStop) { stop in
            StopDetailView(stop: stop)
        }
        .sheet(isPresented: $showLinePicker) {
            LinePickerSheet { route in
                // Manual line selection — stop any active vehicle follow.
                isFollowingVehicle = false
                routeSelectedByVehicle = false
                selectedVehicle = nil
                selectRoute(route, directionId: route.directions.first?.directionId)
                fitMapToRoute(route)
            }
        }
        .task(id: router.pendingMapPreviewStop?.id) {
            guard let stop = router.pendingMapPreviewStop else { return }
            router.pendingMapPreviewStop = nil
            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                selectedVehicle = nil
                selectedStop = stop
            }
            mapRegion = MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng),
                span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
            )
        }
        .task(id: router.pendingMapPreviewVehicleId) {
            guard let vid = router.pendingMapPreviewVehicleId else { return }
            router.pendingMapPreviewVehicleId = nil
            // Wait up to 3s for the vehicle feed to contain the requested id
            // (covers the cold-start path where MappaTab mounts before the feed lands).
            for _ in 0..<30 {
                if vehicleStore.vehicles.contains(where: { $0.id == vid }) { break }
                try? await Task.sleep(for: .milliseconds(100))
            }
            guard let vehicle = vehicleStore.vehicles.first(where: { $0.id == vid }) else { return }
            let resolvedRouteId = vehicle.routeId.isEmpty
                ? (store.routeIdByTripId[vehicle.tripId] ?? "")
                : vehicle.routeId
            let route = store.route(forId: resolvedRouteId)
            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                selectedStop = nil
                selectedVehicle = VehicleSelection(vehicle: vehicle, route: route)
            }
            mapRegion = MKCoordinateRegion(
                center: CLLocationCoordinate2D(
                    latitude: Double(vehicle.latitude),
                    longitude: Double(vehicle.longitude)
                ),
                span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
            )
        }
        .onChange(of: mapReady) { _, ready in
            guard ready else { return }
            updateAnnotations()
        }
        .onChange(of: store.stops.count) { _, _ in
            guard mapReady else { return }
            updateAnnotations()
        }
        .onChange(of: selectedRoute?.id) { _, _ in
            guard mapReady else { return }
            updateAnnotations()
            recomputeRoutePolylines()
            refreshDisplayedVehicles()
        }
        .onChange(of: selectedDirectionId) { _, _ in
            recomputeRoutePolylines()
        }
        // MARK: Vehicle follow + display — update on every feed refresh.
        .onChange(of: vehicleStore.lastFetchedAt) { _, _ in
            refreshDisplayedVehicles()
            followSelectedVehicleIfNeeded()
            refreshSelectedVehicle()
        }
        .accessibilityIdentifier("mappa_tab")
        .sheet(item: $tripSheetTarget) { target in
            NavigationStack {
                TripDetailView(departure: target.departure, fromStop: target.fromStop)
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button(String(localized: "action_chiudi")) {
                                tripSheetTarget = nil
                            }
                        }
                    }
            }
        }
    }

    // MARK: - Trip sheet helper

    /// Builds a TripTarget from a selected vehicle, resolving the scheduled
    /// departure + origin stop from the ScheduleStore. Falls back to a stub
    /// Departure when exact schedule match isn't found so the sheet still
    /// opens (the view gracefully handles unknown stop timings).
    private func makeTripTarget(for selection: VehicleSelection) -> TripTarget? {
        let tripId = selection.vehicle.tripId
        let route = selection.route
        // Try to match a scheduled departure by tripId — pick the first one.
        let apiDep: APIDeparture? = store.scheduleResponse?.stops
            .lazy
            .flatMap(\.departures)
            .first(where: { $0.tripId == tripId })
        guard let apiDep else { return nil }
        let dep = Departure(from: apiDep, route: route)
        // Pick the current stop as origin when available; otherwise use the
        // vehicle's nearest stop on the map or the first stop of any direction.
        let currentStopId = selection.vehicle.currentStopId
        let origin: ResolvedStop? = currentStopId.isEmpty
            ? store.stops.first
            : store.stop(forId: currentStopId)
        guard let origin else { return nil }
        return TripTarget(departure: dep, fromStop: origin)
    }

    // MARK: - Search Pill

    private var searchPill: some View {
        Button {
            showLinePicker = true
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(.secondary)
                Text(String(localized: "map_search_placeholder"))
                    .font(.system(size: 15))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: 360)
            .background(.regularMaterial, in: Capsule())
            .overlay(Capsule().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
            .shadow(color: .black.opacity(0.18), radius: 8, y: 3)
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 24)
        .accessibilityIdentifier("btn_map_search_pill")
        .accessibilityLabel(Text(String(localized: "a11y_search_line_or_stop")))
    }

    // MARK: - Active Route Chip

    private func activeRouteChip(_ route: APIRoute) -> some View {
        Button {
            selectedRoute = nil
            selectedDirectionId = nil
            isFollowingVehicle = false
            routeSelectedByVehicle = false
        } label: {
            HStack(spacing: 8) {
                LineBadge(route: route, size: .medium)
                if vehicleStore.liveCount(forRouteId: route.id) > 0 {
                    HStack(spacing: 3) {
                        Circle().fill(AppTheme.realtimeGreen).frame(width: 6, height: 6)
                        Text("\(vehicleStore.liveCount(forRouteId: route.id))")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(.secondary)
                    }
                }
                Image(systemName: "xmark")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(.secondary)
                    .frame(width: 18, height: 18)
                    .background(Color.primary.opacity(0.08), in: Circle())
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(.regularMaterial, in: Capsule())
            .overlay(Capsule().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
            .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
            // Hit-slop: expand tappable surface around the chip without
            // enlarging the visual pill. Guarantees a >=44x44pt target.
            .padding(8)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("btn_map_clear_route")
        .accessibilityLabel(Text(String(localized: "a11y_remove_selected_line")))
    }

    // MARK: - Map Content

    /// Current zoom tier derived from the live region span. `.city` renders nothing
    /// (clean high-zoom view — no clusters, no pins, no vehicles). `.neighborhood`
    /// shows stop +-square markers. `.street` shows full pin stops + live vehicles.
    private var currentTier: MapZoomTier {
        MapZoomTier(latitudeDelta: mapRegion.span.latitudeDelta)
    }

    private var mapContent: some View {
        TransitMapView(
            region: $mapRegion,
            stops: currentTier == .city ? [] : renderedStops,
            vehicles: displayedVehicles,
            polylines: cachedRoutePolylines,
            zoomLevel: zoomLevel,
            tier: currentTier,
            selectedStopId: selectedStop?.id,
            selectedVehicleId: selectedVehicle?.vehicle.id,
            selectedRouteColor: selectedRoute?.color,
            showsUserLocation: config.features.enableGeolocation,
            routeIdByTripId: store.routeIdByTripId,
            routeLookup: { store.route(forId: $0) },
            transitTypeForRoute: { route in
                route.map { TransitType(gtfsRouteType: $0.transitType) } ?? .bus
            },
            onStopTap: { stop in
                isFollowingVehicle = false
                withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                    selectedVehicle = nil
                    if routeSelectedByVehicle {
                        selectedRoute = nil
                        selectedDirectionId = nil
                        routeSelectedByVehicle = false
                    }
                    selectedStop = stop
                }
                mapRegion = MKCoordinateRegion(
                    center: CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng),
                    span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
                )
                if isExpanded {
                    withAnimation { isExpanded = false }
                }
            },
            onVehicleTap: { vehicle in
                let resolvedRouteId = vehicle.routeId.isEmpty
                    ? (store.routeIdByTripId[vehicle.tripId] ?? "")
                    : vehicle.routeId
                let route = store.route(forId: resolvedRouteId)
                let selection = VehicleSelection(vehicle: vehicle, route: route)
                withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                    selectedVehicle = selection
                    selectedStop = nil
                }
                mapRegion = MKCoordinateRegion(
                    center: CLLocationCoordinate2D(
                        latitude: Double(vehicle.latitude),
                        longitude: Double(vehicle.longitude)
                    ),
                    span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
                )
                if let r = route {
                    selectRoute(r, directionId: r.directions.first?.directionId)
                    routeSelectedByVehicle = true
                }
                isFollowingVehicle = true
            },
            onRegionChange: { region in
                cameraTask?.cancel()
                cameraTask = Task {
                    try? await Task.sleep(for: .milliseconds(150))
                    guard !Task.isCancelled else { return }
                    let newZoom = MapZoomLevel(latitudeDelta: region.span.latitudeDelta)
                    if newZoom != zoomLevel { zoomLevel = newZoom }
                    visibleRegion = region
                    updateAnnotations()
                    refreshDisplayedVehicles()
                }
            }
        )
        .onAppear {
            if !mapReady {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                    withAnimation(.easeIn(duration: 0.2)) {
                        mapReady = true
                    }
                }
            }
        }
        .ignoresSafeArea()
    }

    // MARK: - Map Controls

    private var mapControls: some View {
        VStack(spacing: 8) {
            if config.features.enableGeolocation {
                Button {
                    mapRegion = MKCoordinateRegion(
                        center: CLLocationCoordinate2D(
                            latitude: config.map.centerLat,
                            longitude: config.map.centerLng
                        ),
                        span: MKCoordinateSpan(latitudeDelta: 0.015, longitudeDelta: 0.015)
                    )
                } label: {
                    LucideIcon.navigation.sized(16)
                        .foregroundStyle(.primary)
                        .frame(width: 44, height: 44)
                        .background(.regularMaterial)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                        .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
                }
                .accessibilityLabel(String(localized: "center_on_location"))
                .accessibilityIdentifier("btn_map_recenter")
            }

            // Reset to default view
            Button {
                let center = CLLocationCoordinate2D(
                    latitude: config.map.centerLat,
                    longitude: config.map.centerLng
                )
                let span = Self.spanForZoom(config.map.defaultZoom)
                mapRegion = MKCoordinateRegion(center: center, span: span)
            } label: {
                LucideIcon.map.sized(16)
                    .foregroundStyle(.primary)
                    .frame(width: 44, height: 44)
                    .background(.regularMaterial)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                    .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
            }
            .accessibilityLabel(String(localized: "reset_map_view"))
            .accessibilityIdentifier("btn_map_reset")

            // Expand to fullscreen
            Button {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                    isExpanded = true
                }
            } label: {
                LucideIcon.maximize2.sized(16)
                    .foregroundStyle(.primary)
                    .frame(width: 44, height: 44)
                    .background(.regularMaterial)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                    .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
            }
            .accessibilityLabel(Text(String(localized: "a11y_expand_map")))
            .accessibilityIdentifier("btn_map_expand")
        }
        .padding(.trailing, 16)
    }

    // MARK: - Expanded Overlay Controls

    private var expandedControls: some View {
        ZStack {
            // Controls — vertically centered on the right (HStack inside ZStack centers naturally)
            HStack {
                Spacer()
                VStack(spacing: 8) {
                    if config.features.enableGeolocation {
                        Button {
                            mapRegion = MKCoordinateRegion(
                                center: CLLocationCoordinate2D(
                                    latitude: config.map.centerLat,
                                    longitude: config.map.centerLng
                                ),
                                span: MKCoordinateSpan(latitudeDelta: 0.015, longitudeDelta: 0.015)
                            )
                        } label: {
                            LucideIcon.navigation.sized(16)
                                .foregroundStyle(.primary)
                                .frame(width: 44, height: 44)
                                .background(.regularMaterial)
                                .clipShape(Circle())
                                .overlay(Circle().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                                .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
                        }
                        .accessibilityLabel(String(localized: "center_on_location"))
                    }

                    Button {
                        let center = CLLocationCoordinate2D(
                            latitude: config.map.centerLat,
                            longitude: config.map.centerLng
                        )
                        let span = Self.spanForZoom(config.map.defaultZoom)
                        mapRegion = MKCoordinateRegion(center: center, span: span)
                    } label: {
                        LucideIcon.map.sized(16)
                            .foregroundStyle(.primary)
                            .frame(width: 44, height: 44)
                            .background(.regularMaterial)
                            .clipShape(Circle())
                            .overlay(Circle().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                            .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
                    }
                    .accessibilityLabel(String(localized: "reset_map_view"))
                }
                .padding(.trailing, 16)
            }

            // Close button — bottom center
            VStack {
                Spacer()
                Button {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                        isExpanded = false
                    }
                } label: {
                    LucideIcon.x.sized(20)
                        .foregroundStyle(.primary)
                        .frame(width: 56, height: 56)
                        .background(.regularMaterial)
                        .clipShape(Circle())
                        .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
                }
                .accessibilityLabel(Text(String(localized: "a11y_close_map")))
                .accessibilityIdentifier("btn_map_collapse")
                .padding(.bottom, 44)
            }
        }
    }

    // MARK: - Annotation update

    /// Schedules an async annotation update, cancelling any in-flight computation.
    ///
    /// Captures all required state on the main thread, then dispatches the heavy
    /// filter work to a background thread, writing results back on main.
    private func updateAnnotations() {
        guard mapReady else { return }

        // Capture @MainActor state before leaving the actor.
        let allStops: [ResolvedStop]
        if let route = selectedRoute {
            allStops = store.stopsForRoute(route.id, directionId: selectedDirectionId ?? 0)
        } else {
            allStops = store.stops
        }
        let region = visibleRegion
        let latDelta = region?.span.latitudeDelta ?? mapRegion.span.latitudeDelta
        let tier = MapZoomTier(latitudeDelta: latDelta)
        let maxAnnot = maxIndividualAnnotations
        let hasRoute = selectedRoute != nil

        annotationUpdateTask?.cancel()
        annotationUpdateTask = Task {
            let stops = await Task.detached(priority: .userInitiated) {
                // movete parity: stops visible at every tier. At .city they render
                // as ultra-compact squares so the user still sees the network shape.

                // Viewport filter (skipped when a route is selected — already pre-filtered).
                let visible: [ResolvedStop]
                if hasRoute {
                    visible = allStops
                } else if let region {
                    let margin = region.span.latitudeDelta * 0.3
                    let minLat = region.center.latitude - region.span.latitudeDelta / 2 - margin
                    let maxLat = region.center.latitude + region.span.latitudeDelta / 2 + margin
                    let minLng = region.center.longitude - region.span.longitudeDelta / 2 - margin
                    let maxLng = region.center.longitude + region.span.longitudeDelta / 2 + margin
                    visible = allStops.filter {
                        $0.lat >= minLat && $0.lat <= maxLat &&
                        $0.lng >= minLng && $0.lng <= maxLng
                    }
                } else {
                    visible = allStops
                }

                return Array(visible.prefix(maxAnnot))
            }.value

            guard !Task.isCancelled else { return }
            renderedStops = stops
        }
    }

    // MARK: - Route polyline precomputation

    /// Decodes polylines for the selected route on a background thread and caches them.
    ///
    /// Called when `selectedRoute` or `selectedDirectionId` changes. After this returns,
    /// `cachedRoutePolylines` is ready and `RouteOverlay` renders with zero decode cost.
    private func recomputeRoutePolylines() {
        routePolylineTask?.cancel()
        routePolylineTask = Task {
            guard let route = selectedRoute else {
                cachedRoutePolylines = []
                return
            }

            // Filter directions on main thread (APIRouteDirection access).
            let directions = route.directions.filter { d in
                guard let dirId = selectedDirectionId else { return true }
                return d.directionId == dirId
            }

            // Resolve stop-coordinate fallbacks on main thread (store access).
            let fallbackCoords: [Int: [CLLocationCoordinate2D]] = Dictionary(
                uniqueKeysWithValues: directions.map { d in
                    let coords = store.stopsForRoute(route.id, directionId: d.directionId)
                        .map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng) }
                    return (d.directionId, coords)
                }
            )

            guard !Task.isCancelled else { return }

            // Decode on background thread — can be expensive for routes with 1000+ shape points.
            let result = await Task.detached(priority: .utility) {
                directions.compactMap { direction -> CachedPolyline? in
                    if let encoded = direction.shapePolyline, !encoded.isEmpty {
                        let coords = decodeGooglePolyline(encoded)
                        if coords.count >= 2 {
                            return CachedPolyline(id: direction.directionId, coordinates: coords)
                        }
                    }
                    if let rawShape = direction.shape, rawShape.count >= 2 {
                        let coords = rawShape.compactMap { pair -> CLLocationCoordinate2D? in
                            guard pair.count >= 2 else { return nil }
                            return CLLocationCoordinate2D(latitude: pair[0], longitude: pair[1])
                        }
                        if coords.count >= 2 {
                            return CachedPolyline(id: direction.directionId, coordinates: coords)
                        }
                    }
                    let coords = fallbackCoords[direction.directionId] ?? []
                    guard coords.count >= 2 else { return nil }
                    return CachedPolyline(id: direction.directionId, coordinates: coords)
                }
            }.value

            guard !Task.isCancelled else { return }
            cachedRoutePolylines = result
        }
    }

    // MARK: - Helpers

    /// Converts a GTFS-style zoom level (roughly Google Maps zoom 0-20) to a MapKit span.
    static func spanForZoom(_ zoom: Double) -> MKCoordinateSpan {
        // zoom 12 ≈ 0.04 lat delta (city), zoom 14 ≈ 0.01 (neighborhood)
        let latDelta = 360.0 / pow(2.0, zoom)
        return MKCoordinateSpan(latitudeDelta: latDelta, longitudeDelta: latDelta)
    }

    // MARK: - Public API for route selection

    /// Animates the map camera to fit all stops of a route.
    private func fitMapToRoute(_ route: APIRoute) {
        let stops = store.stopsForRoute(route.id, directionId: route.directions.first?.directionId ?? 0)
        guard !stops.isEmpty else { return }
        let lats = stops.map(\.lat)
        let lngs = stops.map(\.lng)
        let minLat = lats.min()!, maxLat = lats.max()!
        let minLng = lngs.min()!, maxLng = lngs.max()!
        let center = CLLocationCoordinate2D(latitude: (minLat + maxLat) / 2, longitude: (minLng + maxLng) / 2)
        let span = MKCoordinateSpan(
            latitudeDelta: max((maxLat - minLat) * 1.4, 0.01),
            longitudeDelta: max((maxLng - minLng) * 1.4, 0.01)
        )
        mapRegion = MKCoordinateRegion(center: center, span: span)
    }

    /// Call this to show a route overlay on the map (e.g. from a line detail view).
    func selectRoute(_ route: APIRoute, directionId: Int? = nil) {
        selectedRoute = route
        selectedDirectionId = directionId
    }

    // MARK: - Vehicle display

    /// Computes which vehicles to show and updates displayedVehicles.
    /// - Route selected: all vehicles for that route regardless of zoom/viewport.
    /// - No route: vehicles in viewport at street-level zoom only.
    ///
    /// Tier is derived from `visibleRegion` (freshest, updated synchronously in
    /// `onRegionChange`) with fallback to `mapRegion`.
    private func refreshDisplayedVehicles() {
        let effectiveRegion = visibleRegion ?? mapRegion
        let effectiveTier = MapZoomTier(latitudeDelta: effectiveRegion.span.latitudeDelta)
        let updated: [GtfsRtVehicle]
        if let route = selectedRoute {
            // Route-selected: ignore tier/viewport so vehicle follow keeps working
            // when the user zooms out to see the whole line.
            updated = vehicleStore.vehicles(forRouteId: route.id)
        } else {
            // movete parity: vehicles visible at every tier as plain GTFS-colored dots;
            // shape and badge switch come from VehicleAnnotationView's tier logic.
            let region = effectiveRegion
            let margin = region.span.latitudeDelta * 0.5
            let minLat = region.center.latitude - region.span.latitudeDelta / 2 - margin
            let maxLat = region.center.latitude + region.span.latitudeDelta / 2 + margin
            let minLng = region.center.longitude - region.span.longitudeDelta / 2 - margin
            let maxLng = region.center.longitude + region.span.longitudeDelta / 2 + margin
            let inViewport = vehicleStore.vehicles.filter {
                let lat = Double($0.latitude), lng = Double($0.longitude)
                return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng
            }
            // Cap harder at wider zooms to avoid dogpiling at city overview.
            let cap: Int
            switch effectiveTier {
            case .city:         cap = 120
            case .neighborhood: cap = 90
            case .street:       cap = 60
            }
            updated = Array(inViewport.prefix(cap))
        }
        guard updated != displayedVehicles else { return }
        displayedVehicles = updated
    }

    // MARK: - Vehicle follow

    /// Re-centers the map on the selected vehicle's latest position after each feed refresh.
    /// Uses the same animation as the marker update so camera and pin move in sync.
    private func followSelectedVehicleIfNeeded() {
        guard isFollowingVehicle, let vehicleId = selectedVehicle?.vehicle.id else { return }
        guard let updated = vehicleStore.vehicles.first(where: { $0.id == vehicleId }) else {
            isFollowingVehicle = false
            return
        }
        mapRegion = MKCoordinateRegion(
            center: CLLocationCoordinate2D(
                latitude: Double(updated.latitude),
                longitude: Double(updated.longitude)
            ),
            span: MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
        )
    }

    /// Rebinds the open vehicle card to the latest GtfsRtVehicle snapshot after
    /// a feed refresh so position, currentStopId, status and timestamp stay
    /// current. If the vehicle dropped out of the feed (trip finished, block
    /// transition) the selection is cleared.
    private func refreshSelectedVehicle() {
        guard let current = selectedVehicle else { return }
        if let updated = vehicleStore.vehicles.first(where: { $0.id == current.vehicle.id }) {
            if updated != current.vehicle {
                selectedVehicle = VehicleSelection(vehicle: updated, route: current.route)
            }
        } else {
            selectedVehicle = nil
        }
    }
}
