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
    // Access level intentionally internal (not `private`): helper methods in
    // `MappaTab+Actions.swift` read @State and stores from this type.
    @Environment(ScheduleStore.self) var store
    @Environment(VehicleStore.self) var vehicleStore
    @Environment(DeepLinkRouter.self) var router
    @Environment(LocationManager.self) var locationManager

    // MARK: Map state
    /// Region "comando": le azioni (tap, fit linea, follow, recenter, deeplink)
    /// scrivono qui. Un `onChange` pilota `cameraPosition` (SwiftUI `Map`).
    @State var mapRegion: MKCoordinateRegion
    /// Camera della SwiftUI `Map`. Pilotata da `mapRegion`+`is3D`.
    @State var cameraPosition: MapCameraPosition = .automatic
    /// Region live (continuous) — letta dagli overlay mezzi/puck per forzare il
    /// re-render via `proxy.convert` a ogni frame di pan/zoom.
    @State var liveRegion: MKCoordinateRegion?
    @State var zoomLevel: MapZoomLevel = .far
    @State var visibleRegion: MKCoordinateRegion?
    @State var mapReady = false
    @State var cameraTask: Task<Void, Never>?

    // MARK: Rendered annotations (computed async off-main, never in body)
    @State var renderedStops: [ResolvedStop] = []
    let maxIndividualAnnotations = 250

    /// Background task for annotation filtering — cancelled on each camera/data change.
    @State var annotationUpdateTask: Task<Void, Never>?

    // MARK: Route polyline cache
    /// Pre-decoded polylines for the selected route, computed once on background thread.
    /// Avoids running `decodeGooglePolyline` on every Map body re-render (60 fps during pan).
    @State var cachedRoutePolylines: [CachedPolyline] = []
    @State var routePolylineTask: Task<Void, Never>?

    // MARK: Selection
    /// Used only for the highlighted-annotation visual state (isSelected).
    @State var selectedStop: ResolvedStop?
    /// Drives direct push navigation to StopDetailView (Batch C — 1-tap flow).
    @State var navigationDestinationStop: ResolvedStop? = nil

    // MARK: Fullscreen expand
    @State var isExpanded = false

    // MARK: Route overlay
    @State var selectedRoute: APIRoute?
    @State var selectedDirectionId: Int?

    // MARK: Line picker
    @State var showLinePicker = false

    // MARK: Vehicle selection
    struct VehicleSelection: Identifiable {
        let vehicle: GtfsRtVehicle
        let route: APIRoute?
        var id: String { vehicle.id }
    }
    @State var selectedVehicle: VehicleSelection?
    /// When non-nil, presents TripDetailView as a fullscreen cover (opened via the vehicle card).
    @State var tripSheetTarget: TripTarget?
    /// When non-nil, presents LineDetailView as a fullscreen cover (opened via the "Linea" button).
    struct LineTarget: Identifiable {
        let route: APIRoute
        let directionId: Int
        var id: String { route.id + "_\(directionId)" }
    }
    @State var lineSheetTarget: LineTarget?

    // MARK: Vehicle display
    /// Smoothly-animated vehicle list — updated with withAnimation on each feed refresh
    /// and on camera-end so new vehicles pan into view without jumping.
    @State var displayedVehicles: [GtfsRtVehicle] = []

    // MARK: Initial centering
    @State var didCenterOnDevice = false

    // MARK: Vehicle follow
    /// True while the map camera is locked to the selected vehicle's live position.
    @State var isFollowingVehicle = false

    // MARK: 3D toggle
    @State var is3D: Bool = true
    /// True when the active route overlay was auto-selected by tapping a vehicle
    /// (vs. manually via line picker). Determines whether closing the vehicle card
    /// should also tear down the route overlay.
    @State var routeSelectedByVehicle = false

    // MARK: Init

    init(config: OperatorConfig) {
        self.config = config
        let center = CLLocationCoordinate2D(
            latitude: config.map.centerLat,
            longitude: config.map.centerLng
        )
        let span = Self.spanForZoom(config.map.defaultZoom)
        _mapRegion = State(initialValue: MKCoordinateRegion(center: center, span: span))
        // is3D default true → entry 3D (pitch 45). La camera segue mapRegion via onChange.
        _cameraPosition = State(initialValue: .camera(MapCamera(
            centerCoordinate: center,
            distance: Self.distanceForSpan(span),
            heading: 0,
            pitch: 45
        )))
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
                    MapSearchPill { showLinePicker = true }
                        .padding(.top, 8)
                    Spacer()
                }
            }

            // Active-line chip (top-left) when a route is selected
            if !isExpanded, let route = selectedRoute {
                VStack {
                    HStack {
                        // Outer paddings reduced by 8pt to compensate for the
                        // 8pt hit-slop added inside `MapActiveRouteChip` — the
                        // visible chip stays in the same screen position.
                        MapActiveRouteChip(
                            route: route,
                            liveCount: vehicleStore.liveCount(forRouteId: route.id),
                            onDismiss: {
                                selectedRoute = nil
                                selectedDirectionId = nil
                                isFollowingVehicle = false
                                routeSelectedByVehicle = false
                            }
                        )
                        .padding(.leading, 8)
                        .padding(.top, 0)
                        Spacer()
                    }
                    Spacer()
                }
            }

            // MARK: Compact controls — centered vertically on the *visible* map
            // area (i.e. excluding the search pill at the top and the tab bar at
            // the bottom). Without the bottom offset, default ZStack centering
            // lands ~30pt above the visual center because the tab bar eats
            // ~85pt of the bottom edge.
            if !isExpanded {
                HStack {
                    Spacer()
                    MapControlsColumn(
                        is3D: is3D,
                        onToggle3D: { is3D.toggle() },
                        showsRecenter: config.features.enableGeolocation,
                        onRecenter: recenterOnDefault,
                        showsResetBearing: false,
                        onResetBearing: resetToDefaultView,
                        onExpand: {
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                                isExpanded = true
                            }
                        }
                    )
                }
                .padding(.top, 60)
            }

            // MARK: Expanded controls (right + close bottom-center)
            if isExpanded {
                MapExpandedControls(
                    is3D: is3D,
                    onToggle3D: { is3D.toggle() },
                    showsRecenter: config.features.enableGeolocation,
                    onRecenter: recenterOnDefault,
                    showsResetBearing: false,
                    onResetBearing: resetToDefaultView,
                    onCollapse: {
                        withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                            isExpanded = false
                        }
                    }
                )
            }

            // MARK: Vehicle card overlay — floating (margins + shadow)
            if let selection = selectedVehicle {
                VehicleCardOverlay(
                    vehicle: selection.vehicle,
                    route: selection.route,
                    isFollowing: isFollowingVehicle,
                    onToggleFollow: {
                        isFollowingVehicle.toggle()
                        if isFollowingVehicle { followSelectedVehicleIfNeeded() }
                    },
                    onOpenLine: selection.route.map { route in
                        {
                            // Navigation action (DoVe parity): push the line-detail
                            // screen for this vehicle's route, mirroring how the
                            // "Corsa" button opens TripDetailView via tripSheetTarget.
                            lineSheetTarget = LineTarget(
                                route: route,
                                directionId: selectedDirectionId ?? route.directions.first?.directionId ?? 0
                            )
                        }
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
            }

            // MARK: Stop preview card overlay — floating
            if let stop = selectedStop, selectedVehicle == nil {
                StopPreviewOverlay(
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
            }
        }
        .onAppear {
            // Garantisce un fix posizione per il puck overlay (idempotente).
            if config.features.enableGeolocation {
                locationManager.requestPermissionAndStart()
            }
            guard !didCenterOnDevice,
                  let loc = locationManager.location,
                  config.features.enableGeolocation else { return }
            didCenterOnDevice = true
            // Re-centre on the user but keep the operator-configured zoom level —
            // hardcoding a tight span here used to zoom past the level where
            // Apple Maps streams tiles for sparser areas (e.g. Boone NC), leaving
            // the map empty/beige.
            mapRegion = MKCoordinateRegion(
                center: loc.coordinate,
                span: Self.spanForZoom(config.map.defaultZoom)
            )
        }
        .toolbar(.hidden, for: .navigationBar)
        .toolbar(isExpanded ? .hidden : .visible, for: .tabBar)
        .navigationDestination(item: $navigationDestinationStop) { stop in
            StopDetailView(stop: stop)
        }
        .fullScreenCover(isPresented: $showLinePicker) {
            LinePickerSheet(
                onSelect: { route in
                    // Manual line selection — stop any active vehicle follow.
                    isFollowingVehicle = false
                    routeSelectedByVehicle = false
                    selectedVehicle = nil
                    selectRoute(route, directionId: route.directions.first?.directionId)
                    fitMapToRoute(route)
                },
                onSelectStop: { stop in
                    // Stop picked from search — focus it and show the preview card.
                    handleStopTap(stop)
                }
            )
        }
        .onChange(of: router.pendingMapOpen) { _, newId in
            // Bare `transitkit://map` arrived — wipe any leftover selection so
            // the user gets a clean map, not the previous deeplink's route
            // filter / stop preview / vehicle follow. Using `.onChange` rather
            // than `.task(id:)` so the closure runs synchronously on the same
            // tick the id changes, before any peer observer can null it out.
            guard newId != nil else { return }
            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                selectedRoute = nil
                selectedDirectionId = nil
                selectedStop = nil
                selectedVehicle = nil
                isFollowingVehicle = false
                routeSelectedByVehicle = false
            }
            router.pendingMapOpen = nil
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
        .task(id: router.pendingMapPreviewRouteId) {
            guard let rid = router.pendingMapPreviewRouteId else { return }
            let pendingDir = router.pendingMapPreviewDirectionId
            router.pendingMapPreviewRouteId = nil
            router.pendingMapPreviewDirectionId = nil
            // Wait up to 3s for the route catalog to load on cold start.
            for _ in 0..<30 {
                if store.route(forId: rid) != nil { break }
                try? await Task.sleep(for: .milliseconds(100))
            }
            guard let route = store.route(forId: rid) else { return }
            isFollowingVehicle = false
            routeSelectedByVehicle = false
            selectedVehicle = nil
            selectedStop = nil
            selectRoute(route, directionId: pendingDir ?? route.directions.first?.directionId)
            fitMapToRoute(route)
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
            // Arrivo da "Vedi su mappa" / card vettura / deep link map/vehicle →
            // parti GIÀ in FOLLOW (come il tap diretto sul marker), così la camera
            // resta agganciata al mezzo. followSelectedVehicleIfNeeded() gira a
            // ogni refresh feed e ricentra alla stessa span. Parità Android.
            isFollowingVehicle = true
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
        .fullScreenCover(item: $tripSheetTarget) { target in
            NavigationStack {
                TripDetailView(departure: target.departure, fromStop: target.fromStop)
                    .navigationBarBackButtonHidden(true)
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button(String(localized: "action_chiudi")) {
                                tripSheetTarget = nil
                            }
                        }
                    }
            }
        }
        .fullScreenCover(item: $lineSheetTarget) { target in
            NavigationStack {
                LineDetailView(route: target.route)
                    .navigationBarBackButtonHidden(true)
                    .toolbar {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button(String(localized: "action_chiudi")) {
                                lineSheetTarget = nil
                            }
                        }
                    }
                    .onAppear {
                        // Pre-select the direction the vehicle was travelling in.
                        router.pendingDirectionId = target.directionId
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
        // O(1) lookup via the pre-built `apiDepartureByTripId` index in
        // ScheduleStore — replaces the previous `stops.lazy.flatMap.first`
        // scan that ran on the main thread at every vehicle tap.
        guard let apiDep = store.apiDepartureByTripId[tripId]?.departure else { return nil }
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

    // MARK: - Map Controls actions

    /// Bottone "centra posizione": va sulla posizione REALE dell'utente.
    /// Fallback al centro operatore quando la posizione non è disponibile (o
    /// la geolocalizzazione è disattivata).
    private func recenterOnDefault() {
        let center = locationManager.location?.coordinate
            ?? CLLocationCoordinate2D(
                latitude: config.map.centerLat,
                longitude: config.map.centerLng
            )
        mapRegion = MKCoordinateRegion(
            center: center,
            span: MKCoordinateSpan(latitudeDelta: 0.015, longitudeDelta: 0.015)
        )
    }

    /// Reset camera to the operator's default center and zoom.
    private func resetToDefaultView() {
        let center = CLLocationCoordinate2D(
            latitude: config.map.centerLat,
            longitude: config.map.centerLng
        )
        let span = Self.spanForZoom(config.map.defaultZoom)
        mapRegion = MKCoordinateRegion(center: center, span: span)
    }

    // MARK: - Helpers

    /// Converts a GTFS-style zoom level (roughly Google Maps zoom 0-20) to a MapKit span.
    static func spanForZoom(_ zoom: Double) -> MKCoordinateSpan {
        // zoom 12 ≈ 0.04 lat delta (city), zoom 14 ≈ 0.01 (neighborhood)
        let latDelta = 360.0 / pow(2.0, zoom)
        return MKCoordinateSpan(latitudeDelta: latDelta, longitudeDelta: latDelta)
    }

    /// `MKCoordinateSpan` → distanza camera per `MapCamera(distance:)`.
    /// Altezza viewport in metri / (2·tan(15°)). Mirror del vecchio
    /// `TransitMapView.distance(forSpan:)`.
    static func distanceForSpan(_ span: MKCoordinateSpan) -> CLLocationDistance {
        let viewportHeight = span.latitudeDelta * 111_000.0
        return viewportHeight / (2.0 * 0.2679)   // tan(15°) ≈ 0.2679
    }

    /// Rebinds the open vehicle card to the latest GtfsRtVehicle snapshot after
    /// a feed refresh so position, currentStopId, status and timestamp stay
    /// current. If the vehicle dropped out of the feed (trip finished, block
    /// transition) the selection is cleared.
    ///
    /// Kept here (not in `MappaTab+Actions.swift`) because it constructs a new
    /// `VehicleSelection`, a type nested under `MappaTab`.
    func refreshSelectedVehicle() {
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
