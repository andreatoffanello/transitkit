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
    /// Zoom come latDelta PIATTO-equivalente, derivato dalla distanza camera.
    /// `ctx.region` si allarga quando la camera è inclinata: deciderci sopra il
    /// 3D farebbe oscillare la soglia. La distanza del pitch non sa nulla.
    @State private var cameraLatDelta: Double = ExpandedMapOverlay.focusLatDelta
    /// Region camera corrente — letta dagli overlay (mezzi/puck) per forzare
    /// il re-render via `proxy.convert` a ogni frame di pan/zoom.
    @State private var mapRegion: MKCoordinateRegion?
    /// Ultimo pitch scritto DA NOI sulla camera. Senza, un appiattimento
    /// programmatico (fit linea) è indistinguibile da un gesto a due dita e
    /// l'intent 3D verrebbe azzerato da solo. Stesso ruolo di
    /// `MappaTab.lastDrivenPitch`.
    @State private var lastDrivenPitch: Double = MapZoomLevels.pitch3D

    @State private var selectedRouteId: String? = nil
    @State private var linePolylines: [CachedPolyline] = []

    private var stopCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng)
    }

    /// Distanza della camera di focus sulla fermata. Lo span equivalente serve
    /// a `MapZoomLevels`, che decide il 3D sullo zoom e non sulla distanza.
    private static let focusDistance: CLLocationDistance = 350
    private static let focusLatDelta = MappaTab.spanLatitudeDelta(forDistance: focusDistance)

    /// Pitch delle camere di focus sulla fermata: 350 m è dentro la banda in cui
    /// il 3D è consentito, quindi qui decide solo l'intent. Stesso angolo della
    /// mappa principale — i 65° di prima erano un terzo valore senza ragione.
    private var pitch: CGFloat {
        CGFloat(MapZoomLevels.pitch(for: Self.focusLatDelta, intent3D: is3D))
    }

    /// Stato 3D EFFETTIVO (intent ∧ zoom): è questo che va nei controlli, o la
    /// pill direbbe "3D" sull'overview piatto di una linea intera.
    private var effective3D: Bool {
        is3D && MapZoomLevels.allows3D(latitudeDelta: cameraLatDelta)
    }

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
                        cameraLatDelta = MappaTab.spanLatitudeDelta(forDistance: ctx.camera.distance)
                        mapRegion = ctx.region
                    }
                    .onMapCameraChange(frequency: .onEnd) { ctx in
                        reconcilePitch(ctx.camera)
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
                    is3D: effective3D,
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
        Haptics.selection()
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
        // `.region` implica pitch 0: registrarlo evita che il rientro in zoom
        // stretto legga quello zero come "l'utente ha appiattito a mano".
        lastDrivenPitch = 0
        withAnimation(.spring(response: 0.5, dampingFraction: 0.85)) {
            expandedMapPosition = .region(region)
        }
    }

    /// Unico punto in cui si scrive la camera: registra il pitch che abbiamo
    /// chiesto, così `reconcilePitch` sa distinguerlo da un gesto dell'utente.
    private func driveCamera(_ camera: MapCamera) {
        lastDrivenPitch = Double(camera.pitch)
        withAnimation(.spring(response: 0.42, dampingFraction: 0.85)) {
            expandedMapPosition = .camera(camera)
        }
    }

    /// Camera iniziale / al deselect linea: inquadra la FERMATA del dettaglio.
    private func recenter() {
        driveCamera(MapCamera(
            centerCoordinate: stopCoordinate,
            distance: Self.focusDistance,
            heading: 0,
            pitch: pitch
        ))
    }

    /// Bottone "centra posizione": va sulla posizione REALE dell'utente.
    /// Fallback alla fermata se la posizione non è ancora disponibile.
    private func centerOnUser() {
        Haptics.selection()
        guard let coord = locationManager.location?.coordinate else {
            recenter()
            return
        }
        driveCamera(MapCamera(
            centerCoordinate: coord,
            distance: Self.focusDistance,
            heading: 0,
            pitch: pitch
        ))
    }

    /// Cambia SOLO l'inclinazione: la versione precedente ricostruiva la camera
    /// sulla fermata a 350 m, quindi il toggle teletrasportava la mappa e
    /// buttava via il pan dell'utente. Parte dallo stato EFFETTIVO, così il tap
    /// fa sempre l'opposto di quello che la pill mostra; da lontano avvicina,
    /// perché inclinare una vista d'insieme darebbe una mappa piatta storta.
    private func toggle3D() {
        Haptics.selection()
        let willBe3D = !effective3D
        is3D = willBe3D
        let latDelta = (willBe3D && !MapZoomLevels.allows3D(latitudeDelta: cameraLatDelta))
            ? MapZoomLevels.pitch3DEntryDelta
            : cameraLatDelta
        let span = MKCoordinateSpan(latitudeDelta: latDelta, longitudeDelta: latDelta)
        driveCamera(MapCamera(
            centerCoordinate: mapRegion?.center ?? stopCoordinate,
            distance: MappaTab.distanceForSpan(span),
            heading: currentHeading,
            pitch: CGFloat(MapZoomLevels.pitch(for: latDelta, intent3D: willBe3D))
        ))
    }

    /// Riallinea intent e camera a fine gesto — stessa logica di
    /// `MappaTab.reconcilePitch`, che qui mancava del tutto: la pill leggeva lo
    /// stato effettivo ma nessuno riportava la camera a piatto uscendo dalla
    /// banda 3D, e restava una mappa piatta storta con scritto "2D".
    private func reconcilePitch(_ camera: MapCamera) {
        // Lo zoom si legge dalla distanza, non da `ctx.region`: inclinata,
        // la region si allarga e la soglia oscillerebbe.
        let latDelta = MappaTab.spanLatitudeDelta(forDistance: camera.distance)
        let cameraPitch = Double(camera.pitch)
        let tilted = cameraPitch > 1
        // Solo uno scostamento dal pitch che abbiamo chiesto NOI è un gesto a
        // due dita; senza questo confronto un fit-linea appiattito farebbe
        // perdere l'intent 3D al primo rientro in zoom stretto.
        if MapZoomLevels.allows3D(latitudeDelta: latDelta),
           abs(cameraPitch - lastDrivenPitch) > 2,
           tilted != is3D {
            is3D = tilted
            lastDrivenPitch = cameraPitch
            return
        }
        // Zoom uscito (o rientrato) dalla banda del 3D → la camera si adegua.
        // L'intent resta dov'è: è una preferenza, non uno stato di camera.
        let desired = MapZoomLevels.pitch(for: latDelta, intent3D: is3D)
        // `desired != lastDrivenPitch` protegge dal clamping di MapKit su una
        // richiesta di tilt; l'appiattimento non viene mai clampato e va sempre
        // lasciato passare, o è proprio il caso storto a non correggersi mai.
        guard abs(cameraPitch - desired) > 2,
              desired == 0 || desired != lastDrivenPitch else { return }
        driveCamera(MapCamera(
            centerCoordinate: camera.centerCoordinate,
            distance: camera.distance,
            heading: camera.heading,
            pitch: CGFloat(desired)
        ))
    }

    private func resetBearing() {
        Haptics.selection()
        driveCamera(MapCamera(
            centerCoordinate: stopCoordinate,
            distance: Self.focusDistance,
            heading: 0,
            pitch: pitch
        ))
    }

    private func collapse() {
        withAnimation(.spring(response: 0.42, dampingFraction: 0.82)) {
            mapExpanded = false
        }
    }
}
