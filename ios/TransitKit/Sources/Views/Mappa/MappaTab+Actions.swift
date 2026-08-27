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
    /// Usa `visibleRegion` (fresca dal `onMapCameraChange`) con fallback a `mapRegion`.
    var currentTier: MapZoomTier {
        MapZoomTier(latitudeDelta: (visibleRegion ?? mapRegion).span.latitudeDelta)
    }

    /// Mappa SwiftUI (base unica, pattern Movete/DoVe): fermate come `Annotation`
    /// dentro la `Map`; mezzi e puck come overlay via `proxy.convert` → sempre
    /// sopra le fermate e billboard (in piedi) anche in 3D. Sostituisce la vecchia
    /// `TransitMapView` UIKit (`MKMapView`) e il suo z-order via `zPriority`.
    var mapContent: some View {
        MapReader { proxy in
            Map(position: $cameraPosition) {
                stopMapAnnotations
                if selectedRoute != nil {
                    RouteOverlay(polylines: cachedRoutePolylines, color: selectedRoute?.color)
                }
            }
            .mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
            .overlay(alignment: .topLeading) { vehicleMapOverlay(proxy: proxy) }
            .overlay(alignment: .topLeading) { userPuckMapOverlay(proxy: proxy) }
            .onMapCameraChange(frequency: .continuous) { ctx in
                liveRegion = ctx.region   // re-render overlay a ogni frame
                cameraHeading = ctx.camera.heading
            }
            .onMapCameraChange(frequency: .onEnd) { ctx in
                handleRegionChange(
                    ctx.region,
                    cameraDistance: ctx.camera.distance,
                    cameraPitch: ctx.camera.pitch
                )
            }
        }
        // mapRegion/is3D sono lo stato "comando": qui pilotiamo la camera della
        // Map. MKCoordinateRegion non è Equatable → chiave come array di Double.
        .onChange(of: [
            mapRegion.center.latitude, mapRegion.center.longitude,
            mapRegion.span.latitudeDelta, mapRegion.span.longitudeDelta,
            is3D ? 1.0 : 0.0
        ]) { _, _ in
            driveCamera()
        }
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

    /// Fermate come `Annotation`. iOS 26: il `ForEach` deve stare in una
    /// `@MapContentBuilder` dedicata, non inline nella `Map { }`.
    @MapContentBuilder
    var stopMapAnnotations: some MapContent {
        let stops = currentTier == .city ? [] : renderedStops
        ForEach(stops, id: \.id) { s in
            Annotation(
                "",
                coordinate: CLLocationCoordinate2D(latitude: s.lat, longitude: s.lng),
                anchor: currentTier == .street ? .bottom : .center
            ) {
                StopAnnotationView(
                    stop: s,
                    tier: currentTier,
                    zoomLevel: zoomLevel,
                    isSelected: selectedStop?.id == s.id,
                    routeColor: selectedRoute?.color
                )
                .onTapGesture {
                    Haptics.navigate()
                    handleStopTap(s)
                }
            }
        }
    }

    /// Mezzi live come overlay SwiftUI (sopra le fermate, glide morbido).
    @ViewBuilder
    func vehicleMapOverlay(proxy: MapProxy) -> some View {
        let _ = liveRegion   // dipendenza → re-render a ogni frame camera
        ZStack(alignment: .topLeading) {
            Color.clear
            ForEach(displayedVehicles) { v in
                let resolvedRouteId = v.routeId.isEmpty
                    ? (store.routeIdByTripId[v.tripId] ?? "")
                    : v.routeId
                let route = store.route(forId: resolvedRouteId)
                AnimatedVehicleMarker(
                    vehicle: v,
                    proxy: proxy,
                    routeColor: route?.color,
                    transitType: route.map { TransitType(gtfsRouteType: $0.transitType) } ?? .bus,
                    route: route,
                    tier: currentTier,
                    isSelected: selectedVehicle?.vehicle.id == v.id,
                    onTap: {
                        Haptics.navigate()
                        handleVehicleTap(v)
                    }
                )
            }
        }
    }

    /// Puck posizione utente come overlay SwiftUI (sopra tutto, billboard in 3D).
    @ViewBuilder
    func userPuckMapOverlay(proxy: MapProxy) -> some View {
        let _ = liveRegion
        if config.features.enableGeolocation,
           let loc = locationManager.location,
           let point = proxy.convert(loc.coordinate, to: .local),
           point.x.isFinite, point.y.isFinite {
            UserLocationDot()
                .position(point)
                .allowsHitTesting(false)
        }
    }

    // MARK: - Camera driver + region change

    /// Stato 3D EFFETTIVO: intent dell'utente ∧ zoom che lo consente. È questo
    /// che va mostrato nei controlli e usato come punto di partenza del toggle.
    var is3DEffective: Bool {
        is3D && MapZoomLevels.allows3D(latitudeDelta: cameraLatDelta)
    }

    /// Mappa ruotata a sufficienza da meritare la bussola. La soglia di 1° tiene
    /// fuori il rumore del gesto, e la distanza angolare da nord evita che un
    /// heading di 359° (mappa di fatto dritta) la lasci accesa.
    var hasBearingToReset: Bool {
        let angle = abs(cameraHeading.truncatingRemainder(dividingBy: 360))
        return min(angle, 360 - angle) > 1
    }

    /// Pilota la camera SwiftUI dalla region "comando" `mapRegion` + `is3D`.
    /// Unico punto in cui si scrive il pitch per le mosse programmatiche
    /// (recenter, fit linea, tap fermata/mezzo, follow, deep link).
    ///
    /// Lo heading di default è quello CORRENTE: forzare 0 qui buttava via la
    /// rotazione dell'utente a ogni tap fermata, fit linea, tick del follow o
    /// deep link. Solo il reset bussola passa un valore esplicito.
    func driveCamera(heading: Double? = nil) {
        let region = mapRegion
        let pitch = MapZoomLevels.pitch(for: region.span.latitudeDelta, intent3D: is3D)
        lastDrivenPitch = pitch
        // Sincrono, non dal debounce: la pill 3D/2D deve cambiare col tap.
        cameraLatDelta = region.span.latitudeDelta
        let cam = MapCamera(
            centerCoordinate: region.center,
            distance: MappaTab.distanceForSpan(region.span),
            heading: heading ?? cameraHeading,
            pitch: CGFloat(pitch)
        )
        withAnimation(.easeInOut(duration: 0.35)) {
            cameraPosition = .camera(cam)
        }
        // iOS NON fa fire affidabilmente `onMapCameraChange` per le mosse
        // programmatiche (gotcha noto, parità Movete) → senza questo le fermate
        // visibili non verrebbero ricalcolate dopo recenter/fit/tap/follow.
        handleRegionChange(
            region,
            cameraDistance: MappaTab.distanceForSpan(region.span),
            cameraPitch: pitch
        )
    }

    /// Toggle 3D dei controlli mappa. Parte dallo stato EFFETTIVO, così il tap
    /// fa sempre l'opposto di quello che la pill mostra. Chiedere il 3D da
    /// lontano avvicina anche la camera: inclinare una vista d'insieme darebbe
    /// una mappa piatta storta, e il bottone sembrerebbe rotto. Parità Android.
    func toggle3D() {
        Haptics.selection()
        // Region LIVE come base, non `mapRegion`: quest'ultima è ferma
        // all'ultima mossa programmatica e ripartire da lì butterebbe via il pan.
        let center = (visibleRegion ?? mapRegion).center
        let willBe3D = !is3DEffective
        // Lo zoom di partenza è `cameraLatDelta`, non lo span riportato: se la
        // camera è inclinata quello è gonfiato e riusarlo allargherebbe la vista.
        let latDelta = (willBe3D && !MapZoomLevels.allows3D(latitudeDelta: cameraLatDelta))
            ? MapZoomLevels.pitch3DEntryDelta
            : cameraLatDelta
        mapRegion = MKCoordinateRegion(
            center: center,
            span: MKCoordinateSpan(latitudeDelta: latDelta, longitudeDelta: latDelta)
        )
        is3D = willBe3D
    }

    /// Debounce 150ms a fine gesto: zoom tier, fermate visibili, mezzi, pitch.
    func handleRegionChange(
        _ region: MKCoordinateRegion,
        cameraDistance: CLLocationDistance,
        cameraPitch: Double
    ) {
        cameraTask?.cancel()
        cameraTask = Task {
            try? await Task.sleep(for: .milliseconds(150))
            guard !Task.isCancelled else { return }
            let newZoom = MapZoomLevel(latitudeDelta: region.span.latitudeDelta)
            if newZoom != zoomLevel { zoomLevel = newZoom }
            visibleRegion = region
            cameraLatDelta = MappaTab.spanLatitudeDelta(forDistance: cameraDistance)
            updateAnnotations()
            refreshDisplayedVehicles()
            reconcilePitch(center: region.center, cameraPitch: cameraPitch)
        }
    }

    /// Riallinea camera e intent a fine gesto. Il pitch è l'unico pezzo di
    /// stato che l'utente può muovere senza passare dai controlli (gesto a due
    /// dita) e che lo zoom può rendere non più consentito.
    ///
    /// Il `guard` finale è quello che impedisce il loop: la correzione ri-scrive
    /// `mapRegion`, che ri-chiama `driveCamera` → `handleRegionChange`, e senza
    /// il confronto con `lastDrivenPitch` la mappa vibrerebbe sulla soglia.
    private func reconcilePitch(center: CLLocationCoordinate2D, cameraPitch: Double) {
        let latDelta = cameraLatDelta
        let tilted = cameraPitch > 1
        // Riallineare `mapRegion` prima di toccare intent/camera è obbligatorio:
        // è ferma all'ultima mossa programmatica, e ri-driverebbe la camera
        // buttando via il pan che l'utente ha appena fatto.
        let live = MKCoordinateRegion(
            center: center,
            span: MKCoordinateSpan(latitudeDelta: latDelta, longitudeDelta: latDelta)
        )
        // Inclinazione cambiata a mano dove il 3D è consentito → è l'INTENT a
        // seguire il dito, o la pill mentirebbe sullo stato della mappa.
        if MapZoomLevels.allows3D(latitudeDelta: latDelta),
           abs(cameraPitch - lastDrivenPitch) > 2,
           tilted != is3D {
            mapRegion = live
            is3D = tilted
            return
        }
        // Zoom uscito (o rientrato) dalla banda del 3D → la camera si adegua da
        // sola. L'intent resta dov'è: è una preferenza, non uno stato di camera.
        let desired = MapZoomLevels.pitch(for: latDelta, intent3D: is3D)
        // `desired != lastDrivenPitch` serve solo contro il clamping di MapKit
        // (chiediamo 45, ne concede 30, e ri-chiederemmo 45 all'infinito):
        // vale quindi solo per le richieste di TILT. L'appiattimento non viene
        // mai clampato, e sommarci quel confronto lo bloccherebbe proprio nel
        // caso per cui esiste — camera già portata a 0 da noi, utente che
        // inclina a due dita fuori dalla banda 3D.
        guard abs(cameraPitch - desired) > 2,
              desired == 0 || desired != lastDrivenPitch else { return }
        mapRegion = live
        // `driveCamera` esplicito: un tilt a due dita non muove né centro né
        // distanza, quindi `live` può essere identica a `mapRegion` e
        // l'`onChange` non scatterebbe — la camera resterebbe storta.
        driveCamera()
    }

    // MARK: - Tap handlers (ex onStopTap / onVehicleTap)

    func handleStopTap(_ stop: ResolvedStop) {
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
        // La preview card occupa ~36% bottom: centriamo la fermata sopra la card
        // spostando il center a SUD (~22% dello span lat).
        let span = 0.005
        let cardOffsetLat = span * 0.22
        mapRegion = MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: stop.lat - cardOffsetLat, longitude: stop.lng),
            span: MKCoordinateSpan(latitudeDelta: span, longitudeDelta: span)
        )
        if isExpanded {
            withAnimation { isExpanded = false }
        }
    }

    func handleVehicleTap(_ vehicle: GtfsRtVehicle) {
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
        // Solo il CENTRO: lo span fisso di prima risucchiava dentro l'utente
        // che aveva allargato per capire dove stesse andando il mezzo, a ogni
        // refresh del feed. L'avvicinamento resta al primo aggancio (tap sul
        // marker / deep link), dove è l'utente ad averlo chiesto.
        mapRegion = MKCoordinateRegion(
            center: CLLocationCoordinate2D(
                latitude: Double(updated.latitude),
                longitude: Double(updated.longitude)
            ),
            span: MKCoordinateSpan(latitudeDelta: cameraLatDelta, longitudeDelta: cameraLatDelta)
        )
    }
}
