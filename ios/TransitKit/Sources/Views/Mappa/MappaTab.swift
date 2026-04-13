import SwiftUI
import MapKit

// MARK: - Mappa Tab

/// Full-screen map tab showing all transit stops with annotations.
///
/// Features:
/// - Initial center and zoom from `OperatorConfig.map`
/// - Stop annotations with transit type icons (clustered at far zoom)
/// - Tap annotation to show bottom sheet with next departures
/// - Optional route polyline overlay when a line is selected
/// - User location button (when geolocation is enabled)
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

    /// Current map zoom level tracked from camera changes (higher = more zoomed in).
    @State private var mapZoomLevel: Double = 10.0

    /// Zoom threshold below which cluster mode activates (zoom < threshold → clusters).
    private let clusterZoomThreshold: Double = 12.0

    // MARK: Rendered annotations (computed async off-main, never in body)
    @State private var renderedClusters: [StopCluster] = []
    @State private var renderedStops: [ResolvedStop] = []
    private let maxIndividualAnnotations = 250

    /// Background task for annotation filtering/clustering — cancelled on each camera/data change.
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

            // MARK: Compact controls — two independent ZStack layers so controls
            // sit at vertical center-right and the line chip stays bottom-left.
            if !isExpanded {
                // Controls — vertically centered on the right
                HStack {
                    Spacer()
                    mapControls
                }

                // Line selector / active line chip — bottom-left
                VStack {
                    Spacer()
                    HStack {
                        if let route = selectedRoute {
                            // Active line chip with X button
                            Button {
                                selectedRoute = nil
                                selectedDirectionId = nil
                                isFollowingVehicle = false
                                routeSelectedByVehicle = false
                            } label: {
                                HStack(spacing: 8) {
                                    LineBadge(route: route, size: .medium)
                                    Text(route.longName ?? route.name)
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(.primary)
                                        .lineLimit(1)
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
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(.regularMaterial)
                                .clipShape(Capsule())
                                .overlay(Capsule().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                                .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
                            }
                            .buttonStyle(.plain)
                        } else {
                            // "Scegli linea" button
                            Button {
                                showLinePicker = true
                            } label: {
                                HStack(spacing: 6) {
                                    LucideIcon.radio.sized(13)
                                    Text("Linee")
                                        .font(.caption.weight(.semibold))
                                }
                                .foregroundStyle(.primary)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(.regularMaterial)
                                .clipShape(Capsule())
                                .overlay(Capsule().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                                .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
                            }
                            .buttonStyle(.plain)
                        }
                        Spacer()
                    }
                    .padding(.leading, 16)
                    .padding(.bottom, 16)
                }
            }

            // MARK: Expanded controls (right + close bottom-center)
            if isExpanded {
                expandedControls
                VStack {
                    Spacer()
                    HStack {
                        if let route = selectedRoute {
                            // Active line chip with X button
                            Button {
                                selectedRoute = nil
                                selectedDirectionId = nil
                                isFollowingVehicle = false
                                routeSelectedByVehicle = false
                            } label: {
                                HStack(spacing: 8) {
                                    LineBadge(route: route, size: .medium)
                                    Text(route.longName ?? route.name)
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(.primary)
                                        .lineLimit(1)
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
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(.regularMaterial)
                                .clipShape(Capsule())
                                .overlay(Capsule().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                                .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
                            }
                            .buttonStyle(.plain)
                        } else {
                            // "Scegli linea" button
                            Button {
                                showLinePicker = true
                            } label: {
                                HStack(spacing: 6) {
                                    LucideIcon.radio.sized(13)
                                    Text("Linee")
                                        .font(.caption.weight(.semibold))
                                }
                                .foregroundStyle(.primary)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(.regularMaterial)
                                .clipShape(Capsule())
                                .overlay(Capsule().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                                .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
                            }
                            .buttonStyle(.plain)
                        }
                        Spacer()
                    }
                    .padding(.leading, 16)
                    .padding(.bottom, 100)
                }
            }
            // MARK: Vehicle card overlay
            if let selection = selectedVehicle {
                VStack {
                    Spacer()
                    VehicleDetailSheet(
                        vehicle: selection.vehicle,
                        route: selection.route,
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

            // MARK: Stop preview card overlay
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
        }
        .accessibilityIdentifier("mappa_tab")
    }

    // MARK: - Map Content

    // MARK: - Map Content (UIViewRepresentable wrapping MKMapView)
    //
    // Uses a native MKMapView via TransitMapView so we can set MKAnnotationView
    // zPriority/displayPriority (not exposed on SwiftUI's Annotation). Vehicles
    // are pinned to .max zPriority so they always render above stops and polylines.

    private var mapContent: some View {
        TransitMapView(
            region: $mapRegion,
            clusters: mapZoomLevel < clusterZoomThreshold ? renderedClusters : [],
            stops: mapZoomLevel < clusterZoomThreshold ? [] : renderedStops,
            vehicles: displayedVehicles,
            polylines: cachedRoutePolylines,
            zoomLevel: zoomLevel,
            selectedStopId: selectedStop?.id,
            selectedRouteColor: selectedRoute?.color,
            showsUserLocation: config.features.enableGeolocation,
            routeIdByTripId: store.routeIdByTripId,
            routeLookup: { store.route(forId: $0) },
            transitTypeForRoute: { route in
                route.map { TransitType(gtfsRouteType: $0.transitType) } ?? .bus
            },
            onClusterTap: { cluster in
                zoomToFit(stops: cluster.stops)
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
                    let span = region.span.latitudeDelta
                    mapZoomLevel = log2(360.0 / max(span, 0.000001))
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
            .accessibilityLabel("Espandi mappa")
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
                .accessibilityLabel("Chiudi mappa")
                .accessibilityIdentifier("btn_map_collapse")
                .padding(.bottom, 44)
            }
        }
    }

    // MARK: - Annotation update

    /// Schedules an async annotation update, cancelling any in-flight computation.
    ///
    /// Captures all required state on the main thread, then dispatches the heavy
    /// filter + cluster work to a background thread, writing results back on main.
    /// Eliminates the synchronous O(n) blocking that previously ran every 150 ms
    /// during continuous camera movement.
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
        let zoom = mapZoomLevel
        let threshold = clusterZoomThreshold
        let maxAnnot = maxIndividualAnnotations
        let hasRoute = selectedRoute != nil

        annotationUpdateTask?.cancel()
        annotationUpdateTask = Task {
            let (clusters, stops) = await Task.detached(priority: .userInitiated) {
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

                if zoom < threshold {
                    return (MappaTab.buildClusters(from: visible, zoom: zoom), [ResolvedStop]())
                } else {
                    return ([StopCluster](), Array(visible.prefix(maxAnnot)))
                }
            }.value

            guard !Task.isCancelled else { return }
            renderedClusters = clusters
            renderedStops = stops
        }
    }

    // MARK: - Clustering

    /// Bins stops into coordinate-grid clusters.
    ///
    /// Static and nonisolated so it can be called from `Task.detached` without
    /// an actor hop. Uses a coarser bin at very far zoom (precision 1 ≈ ~11 km)
    /// and a finer bin at medium-far zoom (precision 2 ≈ ~1 km).
    nonisolated private static func buildClusters(from stops: [ResolvedStop], zoom: Double) -> [StopCluster] {
        let precision: Double = zoom < 10 ? 1.0 : 2.0
        let scale = pow(10.0, precision)
        var bins: [String: [ResolvedStop]] = [:]
        for stop in stops {
            let latKey = (stop.lat * scale).rounded() / scale
            let lngKey = (stop.lng * scale).rounded() / scale
            let key = "\(latKey),\(lngKey)"
            bins[key, default: []].append(stop)
        }
        return bins.map { key, members in
            let avgLat = members.map(\.lat).reduce(0, +) / Double(members.count)
            let avgLng = members.map(\.lng).reduce(0, +) / Double(members.count)
            return StopCluster(
                id: "cluster_\(key)",
                centerLat: avgLat,
                centerLng: avgLng,
                count: members.count,
                stops: members
            )
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

    /// Animates the map to fit all stops in a cluster, making them individually tappable.
    private func zoomToFit(stops: [ResolvedStop]) {
        guard !stops.isEmpty else { return }
        let lats = stops.map(\.lat)
        let lngs = stops.map(\.lng)
        let minLat = lats.min()!
        let maxLat = lats.max()!
        let minLng = lngs.min()!
        let maxLng = lngs.max()!
        let center = CLLocationCoordinate2D(
            latitude: (minLat + maxLat) / 2,
            longitude: (minLng + maxLng) / 2
        )
        let span = MKCoordinateSpan(
            latitudeDelta: (maxLat - minLat) * 1.5 + 0.005,
            longitudeDelta: (maxLng - minLng) * 1.5 + 0.005
        )
        mapRegion = MKCoordinateRegion(center: center, span: span)
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
    /// - animated: true only on feed refresh (smooth coordinate transitions);
    ///   false on camera change (instant, avoids chained animations that inflate duration).
    private func refreshDisplayedVehicles() {
        let updated: [GtfsRtVehicle]
        if let route = selectedRoute {
            updated = vehicleStore.vehicles(forRouteId: route.id)
        } else if mapZoomLevel >= clusterZoomThreshold, let region = visibleRegion {
            let margin = region.span.latitudeDelta * 0.5
            let minLat = region.center.latitude - region.span.latitudeDelta / 2 - margin
            let maxLat = region.center.latitude + region.span.latitudeDelta / 2 + margin
            let minLng = region.center.longitude - region.span.longitudeDelta / 2 - margin
            let maxLng = region.center.longitude + region.span.longitudeDelta / 2 + margin
            updated = vehicleStore.vehicles.filter {
                let lat = Double($0.latitude), lng = Double($0.longitude)
                return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng
            }
        } else {
            updated = []
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
}
