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

    // MARK: Map state
    @State private var mapPosition: MapCameraPosition
    @State private var zoomLevel: MapZoomLevel = .far
    @State private var visibleRegion: MKCoordinateRegion?
    @State private var mapReady = false
    @State private var cameraTask: Task<Void, Never>?

    /// Current map zoom level tracked from camera changes (higher = more zoomed in).
    @State private var mapZoomLevel: Double = 10.0

    /// Zoom threshold below which cluster mode activates (zoom < threshold → clusters).
    private let clusterZoomThreshold: Double = 12.0

    // MARK: Rendered annotations (computed async in cameraTask, never in body)
    @State private var renderedClusters: [StopCluster] = []
    @State private var renderedStops: [ResolvedStop] = []
    private let maxIndividualAnnotations = 250

    // MARK: Selection
    /// Used only for the highlighted-annotation visual state (isSelected).
    @State private var selectedStop: ResolvedStop?
    /// Drives direct push navigation to StopDetailView (Batch C — 1-tap flow).
    @State private var navigationDestinationStop: ResolvedStop? = nil

    // MARK: Fullscreen expand
    @State private var isExpanded = false

    // MARK: Route overlay
    @State private var selectedRoute: Route?
    @State private var selectedDirectionId: Int?
    @State private var showRouteOverlay = false

    // MARK: Init

    init(config: OperatorConfig) {
        self.config = config
        let center = CLLocationCoordinate2D(
            latitude: config.map.centerLat,
            longitude: config.map.centerLng
        )
        let span = Self.spanForZoom(config.map.defaultZoom)
        _mapPosition = State(initialValue: .region(MKCoordinateRegion(center: center, span: span)))
    }

    // MARK: - Visible stops (performance filter)

    /// Only render stops within the visible region (+ margin) to keep 500+ stops performant.
    private var visibleStops: [ResolvedStop] {
        guard mapReady else { return [] }
        guard let region = visibleRegion else { return store.stops }
        let margin = region.span.latitudeDelta * 0.3
        let minLat = region.center.latitude - region.span.latitudeDelta / 2 - margin
        let maxLat = region.center.latitude + region.span.latitudeDelta / 2 + margin
        let minLng = region.center.longitude - region.span.longitudeDelta / 2 - margin
        let maxLng = region.center.longitude + region.span.longitudeDelta / 2 + margin
        return store.stops.filter { stop in
            stop.lat >= minLat && stop.lat <= maxLat &&
            stop.lng >= minLng && stop.lng <= maxLng
        }
    }

    // MARK: - Body

    var body: some View {
        ZStack {
            // Full-bleed map — extends under status bar and lateral edges
            mapContent
                .mapStyle(.standard(pointsOfInterest: .excludingAll))
                .onMapCameraChange(frequency: .continuous) { context in
                    cameraTask?.cancel()
                    cameraTask = Task {
                        try? await Task.sleep(for: .milliseconds(150))
                        guard !Task.isCancelled else { return }
                        let newZoom = MapZoomLevel(latitudeDelta: context.region.span.latitudeDelta)
                        if newZoom != zoomLevel {
                            zoomLevel = newZoom
                        }
                        visibleRegion = context.region
                        let span = context.region.span.latitudeDelta
                        mapZoomLevel = log2(360.0 / max(span, 0.000001))
                        updateAnnotations()
                    }
                }
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

            // MARK: Compact controls (top-right, respects safe area)
            if !isExpanded {
                VStack {
                    HStack {
                        Spacer()
                        mapControls
                    }
                    Spacer()
                    // Route overlay toggle
                    if let route = selectedRoute {
                        HStack {
                            RouteOverlayToggle(
                                isVisible: $showRouteOverlay,
                                routeName: route.name,
                                routeColor: route.color
                            )
                            Spacer()
                        }
                        .padding(.leading, 16)
                        .padding(.bottom, 16)
                    }
                }
            }

            // MARK: Expanded controls (right + close bottom-center)
            if isExpanded {
                expandedControls
                if let route = selectedRoute {
                    VStack {
                        Spacer()
                        HStack {
                            RouteOverlayToggle(
                                isVisible: $showRouteOverlay,
                                routeName: route.name,
                                routeColor: route.color
                            )
                            Spacer()
                        }
                        .padding(.leading, 16)
                        .padding(.bottom, 100)
                    }
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .toolbar(isExpanded ? .hidden : .visible, for: .tabBar)
        .navigationDestination(item: $navigationDestinationStop) { stop in
            StopDetailView(stop: stop)
        }
        .onChange(of: mapReady) { _, ready in
            guard ready else { return }
            updateAnnotations()
        }
        .onChange(of: store.stops.count) { _, _ in
            guard mapReady else { return }
            updateAnnotations()
        }
        .accessibilityIdentifier("mappa_tab")
    }

    // MARK: - Map Content

    private var mapContent: some View {
        Map(position: $mapPosition) {
            // MARK: User location
            if config.features.enableGeolocation {
                UserAnnotation()
            }

            // MARK: Stop annotations (cluster mode at far zoom, individual at close zoom)
            // renderedClusters / renderedStops are pre-computed in cameraTask (debounced 150ms)
            // and never recalculated during body rendering for zero per-frame cost.
            if mapZoomLevel < clusterZoomThreshold {
                ForEach(renderedClusters) { cluster in
                    Annotation(
                        "",
                        coordinate: CLLocationCoordinate2D(
                            latitude: cluster.centerLat,
                            longitude: cluster.centerLng
                        )
                    ) {
                        ClusterAnnotationView(count: cluster.count)
                            .onTapGesture {
                                zoomToFit(stops: cluster.stops)
                            }
                    }
                }
            } else {
                ForEach(renderedStops) { stop in
                    Annotation(
                        "",
                        coordinate: CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng),
                        anchor: zoomLevel == .far ? .center : .bottom
                    ) {
                        StopAnnotationView(
                            stop: stop,
                            zoomLevel: zoomLevel,
                            isSelected: selectedStop?.id == stop.id
                        )
                        .onTapGesture {
                            // Batch C: 1-tap flow — push StopDetailView directly.
                            selectedStop = stop   // keeps annotation highlighted briefly
                            navigationDestinationStop = stop
                            if isExpanded {
                                withAnimation { isExpanded = false }
                            }
                        }
                    }
                }
            }

            // MARK: Route polyline overlay
            if showRouteOverlay, let route = selectedRoute {
                RouteOverlay(route: route, directionId: selectedDirectionId)
            }
        }
    }

    // MARK: - Map Controls

    private var mapControls: some View {
        VStack(spacing: 8) {
            if config.features.enableGeolocation {
                Button {
                    mapPosition = .userLocation(
                        fallback: .region(MKCoordinateRegion(
                            center: CLLocationCoordinate2D(
                                latitude: config.map.centerLat,
                                longitude: config.map.centerLng
                            ),
                            span: MKCoordinateSpan(latitudeDelta: 0.015, longitudeDelta: 0.015)
                        ))
                    )
                } label: {
                    LucideIcon.navigation.sized(16)
                        .foregroundStyle(.primary)
                        .frame(width: 44, height: 44)
                        .background(.regularMaterial)
                        .clipShape(Circle())
                        .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
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
                withAnimation {
                    mapPosition = .region(MKCoordinateRegion(center: center, span: span))
                }
            } label: {
                LucideIcon.map.sized(16)
                    .foregroundStyle(.primary)
                    .frame(width: 44, height: 44)
                    .background(.regularMaterial)
                    .clipShape(Circle())
                    .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
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
                    .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
            }
            .accessibilityLabel("Espandi mappa")
            .accessibilityIdentifier("btn_map_expand")
        }
        .padding(.top, 12)
        .padding(.trailing, 16)
    }

    // MARK: - Expanded Overlay Controls

    private var expandedControls: some View {
        ZStack {
            // Controls on the right side
            VStack {
                HStack {
                    Spacer()
                    VStack(spacing: 8) {
                        if config.features.enableGeolocation {
                            Button {
                                mapPosition = .userLocation(
                                    fallback: .region(MKCoordinateRegion(
                                        center: CLLocationCoordinate2D(
                                            latitude: config.map.centerLat,
                                            longitude: config.map.centerLng
                                        ),
                                        span: MKCoordinateSpan(latitudeDelta: 0.015, longitudeDelta: 0.015)
                                    ))
                                )
                            } label: {
                                LucideIcon.navigation.sized(16)
                                    .foregroundStyle(.primary)
                                    .frame(width: 44, height: 44)
                                    .background(.regularMaterial)
                                    .clipShape(Circle())
                                    .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
                            }
                            .accessibilityLabel(String(localized: "center_on_location"))
                        }

                        Button {
                            let center = CLLocationCoordinate2D(
                                latitude: config.map.centerLat,
                                longitude: config.map.centerLng
                            )
                            let span = Self.spanForZoom(config.map.defaultZoom)
                            withAnimation {
                                mapPosition = .region(MKCoordinateRegion(center: center, span: span))
                            }
                        } label: {
                            LucideIcon.map.sized(16)
                                .foregroundStyle(.primary)
                                .frame(width: 44, height: 44)
                                .background(.regularMaterial)
                                .clipShape(Circle())
                                .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
                        }
                        .accessibilityLabel(String(localized: "reset_map_view"))
                    }
                    .padding(.top, 12)
                    .padding(.trailing, 16)
                }
                Spacer()
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

    /// Computes and atomically sets renderedClusters / renderedStops based on current zoom.
    /// Called from cameraTask (debounced) and on initial load — never from body.
    private func updateAnnotations() {
        let stops = visibleStops
        if mapZoomLevel < clusterZoomThreshold {
            renderedClusters = clusteredStops(from: stops)
            renderedStops = []
        } else {
            renderedClusters = []
            renderedStops = Array(stops.prefix(maxIndividualAnnotations))
        }
    }

    // MARK: - Clustering

    /// Bins visible stops into coordinate-grid clusters.
    ///
    /// Uses a coarser bin at very far zoom (precision 1 decimal place ≈ ~11 km) and
    /// a finer bin at medium-far zoom (precision 2 decimal places ≈ ~1 km).
    private func clusteredStops(from stops: [ResolvedStop]) -> [StopCluster] {
        let precision: Double = mapZoomLevel < 10 ? 1.0 : 2.0
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
        withAnimation(.easeInOut(duration: 0.4)) {
            mapPosition = .region(MKCoordinateRegion(center: center, span: span))
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

    /// Call this to show a route overlay on the map (e.g. from a line detail view).
    func selectRoute(_ route: Route, directionId: Int? = nil) {
        selectedRoute = route
        selectedDirectionId = directionId
        showRouteOverlay = true

        // Zoom to fit the route shape
        if let direction = route.directions.first(where: { directionId == nil || $0.id == directionId }),
           !direction.shape.isEmpty {
            let coords = direction.shape.compactMap { pair -> CLLocationCoordinate2D? in
                guard pair.count >= 2 else { return nil }
                return CLLocationCoordinate2D(latitude: pair[0], longitude: pair[1])
            }
            if !coords.isEmpty {
                let lats = coords.map(\.latitude)
                let lngs = coords.map(\.longitude)
                let center = CLLocationCoordinate2D(
                    latitude: (lats.min()! + lats.max()!) / 2,
                    longitude: (lngs.min()! + lngs.max()!) / 2
                )
                let span = MKCoordinateSpan(
                    latitudeDelta: (lats.max()! - lats.min()!) * 1.3,
                    longitudeDelta: (lngs.max()! - lngs.min()!) * 1.3
                )
                withAnimation {
                    mapPosition = .region(MKCoordinateRegion(center: center, span: span))
                }
            }
        }
    }
}
