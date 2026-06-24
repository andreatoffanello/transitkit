import SwiftUI
import MapKit

/// Full-screen immersive map overlay shown when the user expands the compact map header
/// on `StopDetailView`. Usa il componente condiviso `MapExpandedControls` per i FAB.
///
/// Vista linea: track di badge in alto (`ExpandedMapLineTrack`) — tap su un
/// badge attiva polilinea + fermate tiered + mezzi live della linea (stessi
/// primitivi della mappa principale via `MapLineFocusContent`) con fit camera
/// sull'intera linea; ri-tap deseleziona e riporta la camera sulla fermata.
/// Alla chiusura dell'overlay lo stato di selezione (@State) muore con la view.
struct ExpandedMapOverlay: View {
    let stop: ResolvedStop
    @Binding var expandedMapPosition: MapCameraPosition
    @Binding var mapExpanded: Bool

    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(LocationManager.self) private var locationManager

    @State private var is3D: Bool = true
    @State private var currentHeading: Double = 0
    /// Span camera corrente — pilota i tier dot→pin delle fermate linea
    /// (stesse soglie della mappa principale, `MapZoomLevels`).
    @State private var currentLatDelta: Double = 0.005
    /// Region camera corrente — letta dagli overlay (mezzi/puck) per forzare
    /// il re-render via `proxy.convert` a ogni frame di pan/zoom.
    @State private var mapRegion: MKCoordinateRegion?

    @State private var selectedRouteId: String? = nil
    @State private var linePolylines: [CachedPolyline] = []

    private var stopCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng)
    }

    private var pitch: CGFloat { is3D ? 65 : 0 }

    /// Reset bearing compare solo se la mappa è davvero ruotata.
    /// Soglia 1° per evitare jitter da micro-movimenti di camera.
    private var hasBearingToReset: Bool { abs(currentHeading) > 1.0 }

    // MARK: - Line focus

    /// Route che servono questa fermata, nell'ordine dei badge del dettaglio.
    private var servingRoutes: [APIRoute] {
        stop.lineNames.compactMap { name in
            store.routes.first { $0.name == name }
        }
    }

    private var selectedRoute: APIRoute? {
        guard let id = selectedRouteId else { return nil }
        return store.routes.first { $0.id == id }
    }

    /// Tutte le fermate della linea selezionata (unione delle direzioni).
    private var lineStops: [ResolvedStop] {
        guard let route = selectedRoute else { return [] }
        var seen = Set<String>()
        var result: [ResolvedStop] = []
        for dir in route.directions {
            for s in store.stopsForRoute(route.id, directionId: dir.directionId)
            where seen.insert(s.id).inserted {
                result.append(s)
            }
        }
        return result
    }

    /// Mezzi live della linea — stesso filtro per routeId della mappa principale.
    private var lineVehicles: [GtfsRtVehicle] {
        guard let route = selectedRoute else { return [] }
        return vehicleStore.vehicles(forRouteId: route.id)
    }

    private var tier: MapZoomTier { MapZoomTier(latitudeDelta: currentLatDelta) }
    private var zoomLevel: MapZoomLevel { MapZoomLevel(latitudeDelta: currentLatDelta) }

    var body: some View {
        ZStack(alignment: .top) {
            mapLayer

            // Vignetta theme-aware in alto — sfuma verso il colore di
            // sfondo dell'app (scuro in dark, chiaro in light), solo per
            // leggibilità del track. Niente banda fissa.
            LinearGradient(
                colors: [AppTheme.background.opacity(0.88), .clear],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 130)
            .frame(maxWidth: .infinity)
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            // Track badge linee — trasparente, edge-to-edge sopra la mappa.
            // Fuori dallo scope ignoresSafeArea: parte sotto la status bar.
            if !servingRoutes.isEmpty {
                ExpandedMapLineTrack(
                    routes: servingRoutes,
                    selectedRouteId: selectedRouteId,
                    onTap: toggleLine
                )
                .padding(.top, 8)
            }
        }
        .onAppear {
            // Garantisce un fix posizione per il puck overlay (LocationManager
            // si ferma dopo il primo fix → se non già disponibile lo richiede).
            locationManager.requestPermissionAndStart()
            recenter()
        }
        .task(id: selectedRouteId) {
            // Fetch + decode shape della linea selezionata, poi fit camera.
            guard let route = selectedRoute else { return }
            let polys = await decodeAllRoutePolylines(for: route, store: store)
            guard !Task.isCancelled, selectedRouteId == route.id else { return }
            linePolylines = polys
            fitToLine(polys)
        }
    }

    private var mapLayer: some View {
        GeometryReader { geo in
            ZStack(alignment: .top) {
                MapReader { proxy in
                    Map(
                        position: $expandedMapPosition,
                        bounds: MapCameraBounds(maximumDistance: MapZoomLevels.maxCameraDistance)
                    ) {
                        if let route = selectedRoute {
                            // Vista linea — il marker base della fermata è
                            // sostituito dalle fermate della linea (la corrente
                            // resta evidenziata via highlightedStopId). I mezzi
                            // NON sono qui: sono overlay (vedi sotto).
                            MapLineFocusContent(
                                route: route,
                                polylines: linePolylines,
                                stops: lineStops,
                                tier: tier,
                                zoomLevel: zoomLevel,
                                highlightedStopId: stop.id
                            )
                        } else if stop.docks.isEmpty {
                            Annotation(stop.name, coordinate: stopCoordinate) {
                                ZStack {
                                    Circle()
                                        .fill(AppTheme.accent)
                                        .frame(width: 32, height: 32)
                                    LucideIcon.signpost.sized(15)
                                        .foregroundStyle(.white)
                                }
                                .shadow(color: .black.opacity(0.3), radius: 4, y: 2)
                            }
                        }
                    }
                    .mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
                    .ignoresSafeArea(.all)
                    // Mezzi live + puck come overlay SwiftUI via proxy.convert:
                    // sempre sopra le fermate e billboard (in piedi) anche in 3D.
                    // Pattern Movete/DoVe (SwiftUI Map non ha zPriority).
                    .overlay(alignment: .topLeading) { vehicleOverlay(proxy: proxy) }
                    .overlay(alignment: .topLeading) { userPuckOverlay(proxy: proxy) }
                    .onMapCameraChange(frequency: .continuous) { ctx in
                        currentHeading = ctx.camera.heading
                        currentLatDelta = ctx.region.span.latitudeDelta
                        mapRegion = ctx.region
                    }
                }

                // Drag handle strip — swipe down per chiudere.
                Color.clear
                    .frame(maxWidth: .infinity)
                    .frame(height: geo.safeAreaInsets.top + 72)
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 20)
                            .onEnded { value in
                                if value.translation.height > 60 {
                                    collapse()
                                }
                            }
                    )

                MapExpandedControls(
                    is3D: is3D,
                    onToggle3D: toggle3D,
                    showsRecenter: true,
                    onRecenter: centerOnUser,
                    showsResetBearing: hasBearingToReset,
                    onResetBearing: resetBearing,
                    onCollapse: collapse
                )
            }
        }
        .ignoresSafeArea(.all)
    }

    // MARK: - Overlay layers (mezzi + puck via proxy.convert)

    /// Mezzi live della linea come overlay SwiftUI (non interattivi qui).
    @ViewBuilder
    private func vehicleOverlay(proxy: MapProxy) -> some View {
        let _ = mapRegion   // dipendenza esplicita → re-render a ogni frame camera
        if let route = selectedRoute {
            ZStack(alignment: .topLeading) {
                Color.clear
                ForEach(lineVehicles) { v in
                    AnimatedVehicleMarker(
                        vehicle: v,
                        proxy: proxy,
                        routeColor: route.color,
                        transitType: route.resolvedTransitType,
                        route: route,
                        tier: tier
                    )
                }
            }
            .allowsHitTesting(false)
        }
    }

    /// Puck posizione utente come overlay SwiftUI (sopra tutto, billboard in 3D).
    @ViewBuilder
    private func userPuckOverlay(proxy: MapProxy) -> some View {
        let _ = mapRegion
        if let loc = locationManager.location,
           let point = proxy.convert(loc.coordinate, to: .local),
           point.x.isFinite, point.y.isFinite {
            UserLocationDot()
                .position(point)
                .allowsHitTesting(false)
        }
    }

    // MARK: - Actions

    private func toggleLine(_ route: APIRoute) {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        if selectedRouteId == route.id {
            // Deselect: pulisci i layer linea e riporta la camera sulla fermata.
            selectedRouteId = nil
            linePolylines = []
            recenter()
        } else {
            selectedRouteId = route.id
            linePolylines = []
        }
    }

    /// Inquadra l'intera linea (bbox di tutte le direzioni). Overview → 2D.
    private func fitToLine(_ polylines: [CachedPolyline]) {
        let coords = polylines.flatMap(\.coordinates)
        guard coords.count >= 2 else { return }
        let lats = coords.map(\.latitude)
        let lngs = coords.map(\.longitude)
        let minLat = lats.min()!, maxLat = lats.max()!
        let minLng = lngs.min()!, maxLng = lngs.max()!
        let region = MKCoordinateRegion(
            center: CLLocationCoordinate2D(
                latitude: (minLat + maxLat) / 2,
                longitude: (minLng + maxLng) / 2
            ),
            span: MKCoordinateSpan(
                latitudeDelta: max((maxLat - minLat) * 1.4, 0.01),
                longitudeDelta: max((maxLng - minLng) * 1.4, 0.01)
            )
        )
        withAnimation(.spring(response: 0.5, dampingFraction: 0.85)) {
            expandedMapPosition = .region(region)
        }
    }

    /// Camera iniziale / al deselect linea: inquadra la FERMATA del dettaglio.
    private func recenter() {
        withAnimation(.spring(response: 0.42, dampingFraction: 0.85)) {
            expandedMapPosition = .camera(MapCamera(
                centerCoordinate: stopCoordinate,
                distance: 350,
                heading: 0,
                pitch: pitch
            ))
        }
    }

    /// Bottone "centra posizione": va sulla posizione REALE dell'utente.
    /// Fallback alla fermata se la posizione non è ancora disponibile.
    private func centerOnUser() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        guard let coord = locationManager.location?.coordinate else {
            recenter()
            return
        }
        withAnimation(.spring(response: 0.42, dampingFraction: 0.85)) {
            expandedMapPosition = .camera(MapCamera(
                centerCoordinate: coord,
                distance: 350,
                heading: 0,
                pitch: pitch
            ))
        }
    }

    private func toggle3D() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        is3D.toggle()
        withAnimation(.spring(response: 0.42, dampingFraction: 0.85)) {
            expandedMapPosition = .camera(MapCamera(
                centerCoordinate: stopCoordinate,
                distance: 350,
                heading: 0,
                pitch: pitch
            ))
        }
    }

    private func resetBearing() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        withAnimation(.spring(response: 0.42, dampingFraction: 0.85)) {
            expandedMapPosition = .camera(MapCamera(
                centerCoordinate: stopCoordinate,
                distance: 350,
                heading: 0,
                pitch: pitch
            ))
        }
    }

    private func collapse() {
        withAnimation(.spring(response: 0.42, dampingFraction: 0.82)) {
            mapExpanded = false
        }
    }
}
