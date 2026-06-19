import SwiftUI
import MapKit

// MARK: - Transit Map View
//
// UIViewRepresentable wrapping a native MKMapView. Provides full control over
// annotation z-ordering (MKAnnotationView.zPriority + displayPriority) which
// SwiftUI's `Map` + `Annotation` APIs don't expose. Vehicles are pinned above
// stops regardless of MapKit's default latitude-based annotation ordering.
//
// Clustering removed (2026-04-17): movete parity. At `.city` tier the map
// renders nothing; the user sees clean tiles until they zoom in.

struct TransitMapView: UIViewRepresentable {

    // MARK: Camera
    @Binding var region: MKCoordinateRegion

    // MARK: Data
    let stops: [ResolvedStop]
    let vehicles: [GtfsRtVehicle]
    let polylines: [CachedPolyline]

    // MARK: Config
    let pitch: Double
    let zoomLevel: MapZoomLevel
    let tier: MapZoomTier
    let selectedStopId: String?
    let selectedVehicleId: String?
    let selectedRouteColor: String?
    let showsUserLocation: Bool
    let routeIdByTripId: [String: String]
    let routeLookup: (String) -> APIRoute?
    let transitTypeForRoute: (APIRoute?) -> TransitType

    // MARK: Callbacks
    var onStopTap: (ResolvedStop) -> Void
    var onVehicleTap: (GtfsRtVehicle) -> Void
    var onRegionChange: (MKCoordinateRegion) -> Void

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.delegate = context.coordinator
        map.showsUserLocation = showsUserLocation
        // Forza il pallino utente blu di sistema indipendentemente dall'accent
        // dell'operatore (altrimenti veniva tintato col verde AppalCART e
        // sembrava nero/scuro su sfondo chiaro).
        map.tintColor = UIColor.systemBlue
        map.pointOfInterestFilter = .excludingAll
        map.showsCompass = false
        // Cap di zoom-out (app cittadina): oltre l'overview regionale la
        // mappa è solo rumore. Allineato a minUserZoom Android (zoom 8).
        map.cameraZoomRange = MKMapView.CameraZoomRange(
            maxCenterCoordinateDistance: MapZoomLevels.maxCameraDistance
        )
        map.setRegion(region, animated: false)
        if pitch > 0 {
            // Derive the camera distance from the requested span instead of
            // reading `camera.altitude` post-`setRegion`. That value used to be
            // the freshly-constructed MKMapView default (≈ 0/very small) on the
            // first pass, collapsing the camera to a metre-level zoom on top of
            // the user dot. `Self.distance(forSpan:)` is the authoritative path.
            let camera = MKMapCamera(
                lookingAtCenter: region.center,
                fromDistance: Self.distance(forSpan: region.span),
                pitch: CGFloat(pitch),
                heading: 0
            )
            map.setCamera(camera, animated: false)
        }
        context.coordinator.parent = self
        context.coordinator.lastBindingRegion = region
        context.coordinator.lastAppliedPitch = pitch
        return map
    }

    /// Converts an `MKCoordinateSpan` to the line-of-sight distance used by
    /// `MKMapCamera(fromDistance:)`. Approximation: viewport height in metres
    /// divided by twice the tangent of half MapKit's nominal vertical FOV
    /// (~30°). Works for the pitches we care about (0–60°).
    private static func distance(forSpan span: MKCoordinateSpan) -> CLLocationDistance {
        let metersPerDegLat = 111_000.0
        let viewportHeight = span.latitudeDelta * metersPerDegLat
        // tan(15°) ≈ 0.2679
        return viewportHeight / (2.0 * 0.2679)
    }

    func updateUIView(_ uiView: MKMapView, context: Context) {
        let coord = context.coordinator
        coord.parent = self

        // Re-assert il pallino utente blu: SwiftUI re-applica l'accent
        // dell'environment come tintColor sulle UIView ad ogni layout pass,
        // sovrascrivendo il systemBlue di makeUIView col verde operator.
        if uiView.tintColor != UIColor.systemBlue {
            uiView.tintColor = UIColor.systemBlue
        }

        // Apply region from binding only when the binding value actually changed.
        // Comparing against lastBindingRegion (not uiView.region) prevents pitch
        // changes from spuriously triggering a setRegion that would flatten 3D view.
        let bindingChanged = coord.lastBindingRegion.map {
            !Self.regionsApproximatelyEqual($0, region)
        } ?? true
        if !coord.isUpdatingFromMap {
            if bindingChanged {
                coord.lastBindingRegion = region
                coord.isUpdatingFromBinding = true
                uiView.setRegion(region, animated: false)
                // setRegion resets pitch; re-apply immediately.
                if pitch > 0 {
                    coord.lastAppliedPitch = pitch
                    uiView.setCamera(MKMapCamera(
                        lookingAtCenter: region.center,
                        fromDistance: Self.distance(forSpan: region.span),
                        pitch: CGFloat(pitch),
                        heading: uiView.camera.heading
                    ), animated: false)
                }
                coord.isUpdatingFromBinding = false
            } else if pitch != coord.lastAppliedPitch {
                // Pitch-only change (3D toggle) — animate directly without
                // region change. Reuse the live altitude from MapKit since the
                // span hasn't moved.
                coord.lastAppliedPitch = pitch
                let camera = MKMapCamera(
                    lookingAtCenter: uiView.region.center,
                    fromDistance: uiView.camera.altitude > 0
                        ? uiView.camera.altitude
                        : Self.distance(forSpan: uiView.region.span),
                    pitch: CGFloat(pitch),
                    heading: uiView.camera.heading
                )
                coord.isUpdatingFromBinding = true
                uiView.setCamera(camera, animated: true)
                coord.isUpdatingFromBinding = false
            }
        }

        // Sync annotations (disable implicit CA animations so coordinate updates snap).
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        coord.syncAnnotations(mapView: uiView, stops: stops, vehicles: vehicles)
        coord.syncOverlays(mapView: uiView, polylines: polylines)
        CATransaction.commit()

        // Force annotation view re-config when route/zoom/selection changes
        // (delegate's viewFor isn't re-called automatically for unchanged annotations).
        coord.reconfigureExistingAnnotations(mapView: uiView)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    private static func regionsApproximatelyEqual(_ a: MKCoordinateRegion, _ b: MKCoordinateRegion) -> Bool {
        abs(a.center.latitude - b.center.latitude) < 1e-6 &&
        abs(a.center.longitude - b.center.longitude) < 1e-6 &&
        abs(a.span.latitudeDelta - b.span.latitudeDelta) < 1e-6 &&
        abs(a.span.longitudeDelta - b.span.longitudeDelta) < 1e-6
    }

    // MARK: - Coordinator

    final class Coordinator: NSObject, MKMapViewDelegate {
        var parent: TransitMapView
        var isUpdatingFromBinding = false
        var isUpdatingFromMap = false
        // Tracks the last region value the binding held, so programmatic pitch
        // changes (which alter uiView.region) don't trigger a spurious setRegion.
        var lastBindingRegion: MKCoordinateRegion?
        var lastAppliedPitch: Double = 0

        var stopAnnotations: [String: StopMKAnnotation] = [:]
        var vehicleAnnotations: [String: VehicleMKAnnotation] = [:]
        var polylineOverlays: [Int: MKPolyline] = [:]

        // Display link unico che anima tutti i veicoli con animazione pendente.
        // Si pausa automaticamente quando nessun veicolo è in transito.
        private nonisolated(unsafe) var displayLink: CADisplayLink?

        init(parent: TransitMapView) {
            self.parent = parent
            super.init()
            let link = CADisplayLink(target: self, selector: #selector(tickVehicleAnimations))
            link.preferredFrameRateRange = CAFrameRateRange(minimum: 30, maximum: 60, preferred: 60)
            link.add(to: .main, forMode: .common)
            link.isPaused = true
            self.displayLink = link
        }

        deinit {
            displayLink?.invalidate()
        }

        // MARK: Vehicle tween

        /// Programma un tween lineare da `current` a `target`. Se un'animazione è
        /// già in corso, la ripiglia dal valore corrente (no salti) e azzera il
        /// timer verso il nuovo target.
        private func scheduleVehicleTween(_ ann: VehicleMKAnnotation,
                                          to target: CLLocationCoordinate2D) {
            let fromCoord = ann.coordinate
            // Skip se il delta è sotto il rumore GPS (~1m) — evita animazioni pigre.
            let dLat = abs(fromCoord.latitude - target.latitude)
            let dLng = abs(fromCoord.longitude - target.longitude)
            if dLat < 0.00001 && dLng < 0.00001 {
                ann.coordinate = target
                ann.animationFrom = nil
                ann.animationTo = nil
                return
            }
            ann.animationFrom = fromCoord
            ann.animationTo = target
            ann.animationStart = CACurrentMediaTime()
            // Durata base 1.0s; per salti grandi (GPS ricalibration, veicolo
            // appena entrato in viewport) mantieni una cadenza coerente fino a 2s
            // così distanze lunghe non sembrano teleport accelerati.
            let metersPerDegLat = 111_000.0
            let distance = sqrt((dLat * metersPerDegLat) * (dLat * metersPerDegLat)
                              + (dLng * metersPerDegLat * 0.8) * (dLng * metersPerDegLat * 0.8))
            ann.animationDuration = min(1.0, max(0.3, distance / 120.0))
            displayLink?.isPaused = false
        }

        @objc private func tickVehicleAnimations() {
            let now = CACurrentMediaTime()
            var anyActive = false
            for ann in vehicleAnnotations.values {
                guard let from = ann.animationFrom, let to = ann.animationTo else { continue }
                let t = (now - ann.animationStart) / ann.animationDuration
                if t >= 1.0 {
                    ann.coordinate = to
                    ann.animationFrom = nil
                    ann.animationTo = nil
                    continue
                }
                anyActive = true
                // Interpolazione lineare su lat/lng — stile movete "breve movimento
                // lineare". Ease-in-out sembrerebbe più sofisticato ma falsa la
                // percezione di velocità del veicolo reale.
                let lat = from.latitude + (to.latitude - from.latitude) * t
                let lng = from.longitude + (to.longitude - from.longitude) * t
                ann.coordinate = CLLocationCoordinate2D(latitude: lat, longitude: lng)
            }
            if !anyActive { displayLink?.isPaused = true }
        }

        // MARK: Annotation sync

        func syncAnnotations(mapView: MKMapView,
                              stops: [ResolvedStop],
                              vehicles: [GtfsRtVehicle]) {
            // Stops
            let stopIds = Set(stops.map(\.id))
            for (id, ann) in stopAnnotations where !stopIds.contains(id) {
                mapView.removeAnnotation(ann)
                stopAnnotations.removeValue(forKey: id)
            }
            for stop in stops {
                if let ann = stopAnnotations[stop.id] {
                    let newCoord = CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng)
                    if ann.coordinate.latitude != newCoord.latitude || ann.coordinate.longitude != newCoord.longitude {
                        ann.coordinate = newCoord
                    }
                    ann.stop = stop
                } else {
                    let ann = StopMKAnnotation(stop: stop)
                    stopAnnotations[stop.id] = ann
                    mapView.addAnnotation(ann)
                }
            }

            // Vehicles
            let vehicleIds = Set(vehicles.map(\.id))
            for (id, ann) in vehicleAnnotations where !vehicleIds.contains(id) {
                mapView.removeAnnotation(ann)
                vehicleAnnotations.removeValue(forKey: id)
            }
            for vehicle in vehicles {
                let newCoord = CLLocationCoordinate2D(
                    latitude: Double(vehicle.latitude),
                    longitude: Double(vehicle.longitude)
                )
                if let ann = vehicleAnnotations[vehicle.id] {
                    if ann.coordinate.latitude != newCoord.latitude || ann.coordinate.longitude != newCoord.longitude {
                        scheduleVehicleTween(ann, to: newCoord)
                    }
                    ann.vehicle = vehicle
                } else {
                    // Primo spawn in viewport: nessuna animazione, il pin appare
                    // direttamente sul coordinate corrente (evita un wipe-in da un
                    // punto arbitrario).
                    let ann = VehicleMKAnnotation(vehicle: vehicle)
                    vehicleAnnotations[vehicle.id] = ann
                    mapView.addAnnotation(ann)
                }
            }
        }

        func syncOverlays(mapView: MKMapView, polylines: [CachedPolyline]) {
            let newIds = Set(polylines.map(\.id))
            for (id, poly) in polylineOverlays where !newIds.contains(id) {
                mapView.removeOverlay(poly)
                polylineOverlays.removeValue(forKey: id)
            }
            for p in polylines where polylineOverlays[p.id] == nil {
                var coords = p.coordinates
                let mk = MKPolyline(coordinates: &coords, count: coords.count)
                mk.title = String(p.id)
                polylineOverlays[p.id] = mk
                mapView.addOverlay(mk, level: .aboveRoads)
            }
        }

        func reconfigureExistingAnnotations(mapView: MKMapView) {
            for (_, ann) in stopAnnotations {
                if let view = mapView.view(for: ann) as? StopAnnotationHost {
                    view.configure(with: ann.stop,
                                   tier: parent.tier,
                                   zoomLevel: parent.zoomLevel,
                                   isSelected: parent.selectedStopId == ann.stop.id,
                                   routeColor: parent.selectedRouteColor)
                }
            }
            for (_, ann) in vehicleAnnotations {
                if let view = mapView.view(for: ann) as? VehicleAnnotationHost {
                    let resolvedRouteId = ann.vehicle.routeId.isEmpty
                        ? (parent.routeIdByTripId[ann.vehicle.tripId] ?? "")
                        : ann.vehicle.routeId
                    let route = parent.routeLookup(resolvedRouteId)
                    let transitType = parent.transitTypeForRoute(route)
                    let effectiveColor = route?.color ?? parent.selectedRouteColor
                    view.configure(with: ann.vehicle,
                                   routeColor: effectiveColor,
                                   transitType: transitType,
                                   tier: parent.tier,
                                   route: route,
                                   isSelected: parent.selectedVehicleId == ann.vehicle.id)
                    // Keep a11y properties current — must be re-asserted here because
                    // viewFor is only called once (on first add or dequeue); subsequent
                    // updates go through reconfigureExistingAnnotations exclusively.
                    // Without this, dequeued views may carry a stale identifier from
                    // the previous vehicle that used the same view object.
                    view.isAccessibilityElement = true
                    view.accessibilityIdentifier = "map_vehicle_\(ann.vehicle.id)"
                    let routeName = route?.name ?? transitType.displayName
                    view.accessibilityLabel = "\(routeName) \(transitType.displayName)"
                }
            }
        }

        // MARK: MKMapViewDelegate — annotation views

        // Garantisce che il pallino utente (MKUserLocationView, default blu di
        // sistema) renda SEMPRE sopra stop e veicoli. Senza questo, i veicoli
        // con zPriority .max lo coprivano.
        func mapView(_ mapView: MKMapView, didAdd views: [MKAnnotationView]) {
            for v in views where v.annotation is MKUserLocation {
                v.zPriority = .max
                v.displayPriority = .required
            }
        }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            if annotation is MKUserLocation { return nil }

            if let stopAnn = annotation as? StopMKAnnotation {
                let view = (mapView.dequeueReusableAnnotationView(withIdentifier: "stop") as? StopAnnotationHost)
                    ?? StopAnnotationHost(annotation: stopAnn, reuseIdentifier: "stop")
                view.annotation = stopAnn
                view.configure(with: stopAnn.stop,
                               tier: parent.tier,
                               zoomLevel: parent.zoomLevel,
                               isSelected: parent.selectedStopId == stopAnn.stop.id,
                               routeColor: parent.selectedRouteColor)
                view.displayPriority = .required
                view.zPriority = MKAnnotationViewZPriority(rawValue: 100)
                return view
            }

            if let vehicleAnn = annotation as? VehicleMKAnnotation {
                let view = (mapView.dequeueReusableAnnotationView(withIdentifier: "vehicle") as? VehicleAnnotationHost)
                    ?? VehicleAnnotationHost(annotation: vehicleAnn, reuseIdentifier: "vehicle")
                view.annotation = vehicleAnn
                let resolvedRouteId = vehicleAnn.vehicle.routeId.isEmpty
                    ? (parent.routeIdByTripId[vehicleAnn.vehicle.tripId] ?? "")
                    : vehicleAnn.vehicle.routeId
                let route = parent.routeLookup(resolvedRouteId)
                let transitType = parent.transitTypeForRoute(route)
                let effectiveColor = route?.color ?? parent.selectedRouteColor
                view.configure(with: vehicleAnn.vehicle,
                               routeColor: effectiveColor,
                               transitType: transitType,
                               tier: parent.tier,
                               route: route,
                               isSelected: parent.selectedVehicleId == vehicleAnn.vehicle.id)
                view.displayPriority = .required
                // Sotto al pallino utente (.max) ma sopra alle stop (100).
                view.zPriority = MKAnnotationViewZPriority(rawValue: 900)
                // Accessibility — vehicle markers must be reachable via VoiceOver
                // and idb/Maestro automation. The SwiftUI host view (isUserInteractionEnabled=false)
                // cannot itself carry a11y — set it on the MKAnnotationView wrapper.
                view.isAccessibilityElement = true
                view.accessibilityIdentifier = "map_vehicle_\(vehicleAnn.vehicle.id)"
                let routeName = route?.name ?? transitType.displayName
                view.accessibilityLabel = "\(routeName) \(transitType.displayName)"
                return view
            }
            return nil
        }

        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let polyline = overlay as? MKPolyline {
                let renderer = MKPolylineRenderer(polyline: polyline)
                let hex = parent.selectedRouteColor ?? "#3388FF"
                renderer.strokeColor = UIColor(Color(hex: hex))
                renderer.lineWidth = 4
                renderer.lineCap = .round
                renderer.lineJoin = .round
                return renderer
            }
            return MKOverlayRenderer(overlay: overlay)
        }

        func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView) {
            mapView.deselectAnnotation(view.annotation, animated: false)
            if let stopAnn = view.annotation as? StopMKAnnotation {
                parent.onStopTap(stopAnn.stop)
            } else if let vehicleAnn = view.annotation as? VehicleMKAnnotation {
                parent.onVehicleTap(vehicleAnn.vehicle)
            }
        }

        func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
            if isUpdatingFromBinding { return }
            isUpdatingFromMap = true
            parent.region = mapView.region
            parent.onRegionChange(mapView.region)
            // Keep lastBindingRegion in sync so pitch-induced perspective span changes
            // don't look like a binding change and trigger a spurious setRegion.
            lastBindingRegion = mapView.region
            isUpdatingFromMap = false
        }
    }
}

// MARK: - Custom MKAnnotation classes

final class StopMKAnnotation: NSObject, MKAnnotation {
    var stop: ResolvedStop
    @objc dynamic var coordinate: CLLocationCoordinate2D
    init(stop: ResolvedStop) {
        self.stop = stop
        self.coordinate = CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng)
    }
}

final class VehicleMKAnnotation: NSObject, MKAnnotation {
    var vehicle: GtfsRtVehicle
    @objc dynamic var coordinate: CLLocationCoordinate2D

    // Tween lineare tra due update GTFS-RT. Il Coordinator's CADisplayLink
    // interpola `coordinate` frame-by-frame da `animationFrom` → `animationTo`
    // sulla durata `animationDuration`, così il marker scivola invece di saltare.
    var animationFrom: CLLocationCoordinate2D?
    var animationTo: CLLocationCoordinate2D?
    var animationStart: CFTimeInterval = 0
    var animationDuration: CFTimeInterval = 1.0

    init(vehicle: GtfsRtVehicle) {
        self.vehicle = vehicle
        self.coordinate = CLLocationCoordinate2D(
            latitude: Double(vehicle.latitude),
            longitude: Double(vehicle.longitude)
        )
    }

    var isAnimating: Bool { animationTo != nil }
}

// MARK: - Annotation host views (UIKit wrapping SwiftUI)

final class StopAnnotationHost: MKAnnotationView {
    private var hosting: UIHostingController<StopAnnotationView>?

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        frame = CGRect(x: 0, y: 0, width: 60, height: 60)
        backgroundColor = .clear
        clipsToBounds = false
    }

    required init?(coder: NSCoder) { fatalError() }

    func configure(with stop: ResolvedStop,
                   tier: MapZoomTier,
                   zoomLevel: MapZoomLevel,
                   isSelected: Bool,
                   routeColor: String?) {
        let view = StopAnnotationView(
            stop: stop,
            tier: tier,
            zoomLevel: zoomLevel,
            isSelected: isSelected,
            routeColor: routeColor
        )
        if let hc = hosting {
            hc.rootView = view
        } else {
            let hc = UIHostingController(rootView: view)
            hc.view.backgroundColor = .clear
            hc.view.frame = bounds
            hc.view.isUserInteractionEnabled = false
            hc.view.clipsToBounds = false
            addSubview(hc.view)
            hosting = hc
        }
        // Anchor mapping:
        // - .city / .neighborhood: square marker is centered inside the host,
        //   so host-center at coordinate is correct → zero offset.
        // - .street: the SwiftUI content bottom-aligns inside a 60pt-tall host
        //   (triangle tip at host's bottom edge). We shift the host up by half
        //   its height so the bottom edge — and therefore the tip — lands on the
        //   coordinate regardless of label visibility.
        switch tier {
        case .city, .neighborhood:
            centerOffset = .zero
        case .street:
            centerOffset = CGPoint(x: 0, y: -bounds.height / 2)
        }
    }
}

final class VehicleAnnotationHost: MKAnnotationView {
    private var hosting: UIHostingController<VehicleAnnotationView>?

    // Tracked for point(inside:with:) and centerOffset computation.
    private var currentTier: MapZoomTier = .neighborhood

    // MARK: Tier-dependent geometry (mirrors VehicleAnnotationView constants)

    private var haloSize: CGFloat {
        switch currentTier {
        case .city:         return 18
        case .neighborhood: return 24
        case .street:       return 30
        }
    }

    private var dotSize: CGFloat {
        switch currentTier {
        case .city:         return 8
        case .neighborhood: return 11
        case .street:       return 13
        }
    }

    /// Approximate height of badge pill + tail above the halo at .street tier.
    /// Used to bias centerOffset so the dot (not the badge top) lands on the coordinate.
    private static let badgePlusTailHeight: CGFloat = 31  // ~26pt pill + 5pt tail
    /// Spacer below the halo at .street tier (balance element in VehicleAnnotationView).
    private static let spacerHeight: CGFloat = 11

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        // Oversized host so the badge + halo aren't clipped. centerOffset is
        // applied in configure() based on tier so the dot lands on the coordinate.
        frame = CGRect(x: 0, y: 0, width: 100, height: 100)
        backgroundColor = .clear
        clipsToBounds = false
    }

    required init?(coder: NSCoder) { fatalError() }

    // MARK: Hit-testing
    //
    // The annotation view frame is 100×100 (larger than the visible marker) so
    // the badge+halo aren't clipped. We narrow the tappable area to the actual
    // visible content — a circle whose radius equals haloSize/2 centred on the
    // coordinate's screen point (centre of the annotation view after the
    // centerOffset is applied). At .street tier the dot sits below badge+tail,
    // so we include the badge rect above the halo as well.
    // This prevents the large transparent dead-zone of the 100×100 frame from
    // stealing taps that should reach nearby stop annotations beneath it.
    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        let center = CGPoint(x: bounds.midX, y: bounds.midY)
        let hR = haloSize / 2 + 4  // +4pt finger-slop beyond visible halo edge

        // Halo circle hit area (always present)
        let dx = point.x - center.x
        let dy = point.y - center.y
        if dx * dx + dy * dy <= hR * hR { return true }

        // Badge rect hit area — only at .street tier
        if currentTier == .street {
            // Badge sits directly above the halo; the halo top is at
            // (center.y - haloSize/2). Badge height ≈ badgePlusTailHeight.
            let badgeTop    = center.y - haloSize / 2 - Self.badgePlusTailHeight - 4
            let badgeBottom = center.y - haloSize / 2 + 4
            let badgeHalfW: CGFloat = 32   // typical badge pill half-width
            if point.y >= badgeTop && point.y <= badgeBottom
               && abs(point.x - center.x) <= badgeHalfW {
                return true
            }
        }
        return false
    }

    func configure(with vehicle: GtfsRtVehicle,
                   routeColor: String?,
                   transitType: TransitType,
                   tier: MapZoomTier,
                   route: APIRoute?,
                   isSelected: Bool) {
        currentTier = tier

        // Center the dot (not the badge or spacer) on the coordinate.
        // At .neighborhood/.city: content is just the halo — already centred → zero offset.
        // At .street: VStack = badge(~26) + tail(5) + halo(30) + spacer(11).
        // With the VStack centred in the 100pt frame the halo's dot-centre lands
        // at frame.midY + ~3pt. Apply a small downward shift so the map coordinate
        // (placed at frame.midY by MapKit) aligns with the dot.
        // Net: dot is at (badgePlusTailHeight - spacerHeight)/2 ≈ 10pt above midY.
        // So shift the frame 10pt up (positive y = down in UIKit, so offset.y = -10).
        switch tier {
        case .city, .neighborhood:
            centerOffset = .zero
        case .street:
            let shift = (Self.badgePlusTailHeight - Self.spacerHeight) / 2
            centerOffset = CGPoint(x: 0, y: -shift)
        }

        let view = VehicleAnnotationView(
            vehicle: vehicle,
            routeColor: routeColor,
            transitType: transitType,
            tier: tier,
            route: route,
            isSelected: isSelected
        )
        if let hc = hosting {
            hc.rootView = view
        } else {
            let hc = UIHostingController(rootView: view)
            hc.view.backgroundColor = .clear
            hc.view.frame = bounds
            hc.view.isUserInteractionEnabled = false
            hc.view.clipsToBounds = false
            addSubview(hc.view)
            hosting = hc
        }
    }
}
