import SwiftUI
import MapKit

// MARK: - MappaTab actions / logic
//
// Split from `MappaTab.swift` — behavior-preserving.
// Groups the non-View orchestration helpers (annotation filter,
// route polyline decode, vehicle viewport filter, camera fit, etc.)
// that would otherwise inflate the main body file.
//
// All helpers read/write @State on the MappaTab view instance, so
// they stay methods on MappaTab (not free functions).

extension MappaTab {
    // MARK: - Map Content

    /// Current zoom tier derived from the live region span. `.city` renders nothing
    /// (clean high-zoom view — no clusters, no pins, no vehicles). `.neighborhood`
    /// shows stop +-square markers. `.street` shows full pin stops + live vehicles.
    var currentTier: MapZoomTier {
        MapZoomTier(latitudeDelta: mapRegion.span.latitudeDelta)
    }

    var mapContent: some View {
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
                // La preview card occupa ~36% dello schermo bottom; centriamo
                // la fermata sopra la card spostando il center mappa a SUD
                // (~22% dello span lat). Senza, la fermata finisce sotto la
                // card subito dopo il tap.
                let span = 0.005
                let cardOffsetLat = span * 0.22
                mapRegion = MKCoordinateRegion(
                    center: CLLocationCoordinate2D(
                        latitude: stop.lat - cardOffsetLat,
                        longitude: stop.lng
                    ),
                    span: MKCoordinateSpan(latitudeDelta: span, longitudeDelta: span)
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
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 300_000_000)
                    withAnimation(.easeIn(duration: 0.2)) {
                        mapReady = true
                    }
                }
            }
        }
        .ignoresSafeArea()
    }

    // MARK: - Annotation update

    /// Schedules an async annotation update, cancelling any in-flight computation.
    ///
    /// Captures all required state on the main thread, then dispatches the heavy
    /// filter work to a background thread, writing results back on main.
    func updateAnnotations() {
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
    func recomputeRoutePolylines() {
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

    // MARK: - Camera helpers

    /// Animates the map camera to fit all stops of a route.
    func fitMapToRoute(_ route: APIRoute) {
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
    func refreshDisplayedVehicles() {
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
    func followSelectedVehicleIfNeeded() {
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
