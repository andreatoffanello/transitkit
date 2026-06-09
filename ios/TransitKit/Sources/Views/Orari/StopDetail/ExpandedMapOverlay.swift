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

    @State private var is3D: Bool = true
    @State private var currentHeading: Double = 0
    /// Span camera corrente — pilota i tier dot→pin delle fermate linea
    /// (stesse soglie della mappa principale, `MapZoomLevels`).
    @State private var currentLatDelta: Double = 0.005

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
        .onAppear { recenter() }
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
                Map(position: $expandedMapPosition) {
                    UserAnnotation()

                    if let route = selectedRoute {
                        // Vista linea — il marker base della fermata è
                        // sostituito dalle fermate della linea (la corrente
                        // resta evidenziata via highlightedStopId).
                        MapLineFocusContent(
                            route: route,
                            polylines: linePolylines,
                            stops: lineStops,
                            vehicles: lineVehicles,
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
                .onMapCameraChange(frequency: .continuous) { ctx in
                    currentHeading = ctx.camera.heading
                    currentLatDelta = ctx.region.span.latitudeDelta
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
                    onRecenter: recenter,
                    showsResetBearing: hasBearingToReset,
                    onResetBearing: resetBearing,
                    onCollapse: collapse
                )
            }
        }
        .ignoresSafeArea(.all)
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
