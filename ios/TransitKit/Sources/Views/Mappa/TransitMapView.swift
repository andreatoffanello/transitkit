import SwiftUI
import MapKit

// MARK: - Transit Map View
//
// UIViewRepresentable wrapping a native MKMapView. Provides full control over
// annotation z-ordering (MKAnnotationView.zPriority + displayPriority) which
// SwiftUI's `Map` + `Annotation` APIs don't expose. Vehicles are pinned above
// stops regardless of MapKit's default latitude-based annotation ordering.

struct TransitMapView: UIViewRepresentable {

    // MARK: Camera
    @Binding var region: MKCoordinateRegion

    // MARK: Data
    let clusters: [StopCluster]
    let stops: [ResolvedStop]
    let vehicles: [GtfsRtVehicle]
    let polylines: [CachedPolyline]

    // MARK: Config
    let zoomLevel: MapZoomLevel
    let selectedStopId: String?
    let selectedRouteColor: String?
    let showsUserLocation: Bool
    let routeIdByTripId: [String: String]
    let routeLookup: (String) -> APIRoute?
    let transitTypeForRoute: (APIRoute?) -> TransitType

    // MARK: Callbacks
    var onClusterTap: (StopCluster) -> Void
    var onStopTap: (ResolvedStop) -> Void
    var onVehicleTap: (GtfsRtVehicle) -> Void
    var onRegionChange: (MKCoordinateRegion) -> Void

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.delegate = context.coordinator
        map.showsUserLocation = showsUserLocation
        map.pointOfInterestFilter = .excludingAll
        map.showsCompass = false
        map.setRegion(region, animated: false)
        context.coordinator.parent = self
        return map
    }

    func updateUIView(_ uiView: MKMapView, context: Context) {
        let coord = context.coordinator
        coord.parent = self

        // Apply region from binding only when the change originated from the parent
        // (not from user gestures routed back through the binding). Small tolerance
        // prevents redundant animations for sub-degree rounding noise.
        if !coord.isUpdatingFromMap,
           !Self.regionsApproximatelyEqual(uiView.region, region) {
            coord.isUpdatingFromBinding = true
            uiView.setRegion(region, animated: true)
            coord.isUpdatingFromBinding = false
        }

        // Sync annotations (disable implicit CA animations so coordinate updates snap).
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        coord.syncAnnotations(mapView: uiView, clusters: clusters, stops: stops, vehicles: vehicles)
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

        var clusterAnnotations: [String: ClusterMKAnnotation] = [:]
        var stopAnnotations: [String: StopMKAnnotation] = [:]
        var vehicleAnnotations: [String: VehicleMKAnnotation] = [:]
        var polylineOverlays: [Int: MKPolyline] = [:]

        init(parent: TransitMapView) {
            self.parent = parent
        }

        // MARK: Annotation sync

        func syncAnnotations(mapView: MKMapView,
                              clusters: [StopCluster],
                              stops: [ResolvedStop],
                              vehicles: [GtfsRtVehicle]) {
            // Clusters
            let clusterIds = Set(clusters.map(\.id))
            for (id, ann) in clusterAnnotations where !clusterIds.contains(id) {
                mapView.removeAnnotation(ann)
                clusterAnnotations.removeValue(forKey: id)
            }
            for cluster in clusters {
                if let ann = clusterAnnotations[cluster.id] {
                    let newCoord = CLLocationCoordinate2D(latitude: cluster.centerLat, longitude: cluster.centerLng)
                    if ann.coordinate.latitude != newCoord.latitude || ann.coordinate.longitude != newCoord.longitude {
                        ann.coordinate = newCoord
                    }
                    ann.cluster = cluster
                } else {
                    let ann = ClusterMKAnnotation(cluster: cluster)
                    clusterAnnotations[cluster.id] = ann
                    mapView.addAnnotation(ann)
                }
            }

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
                        ann.coordinate = newCoord
                    }
                    ann.vehicle = vehicle
                } else {
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
                                   transitType: transitType)
                }
            }
        }

        // MARK: MKMapViewDelegate — annotation views

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            if annotation is MKUserLocation { return nil }

            if let cluster = annotation as? ClusterMKAnnotation {
                let view = (mapView.dequeueReusableAnnotationView(withIdentifier: "cluster") as? ClusterAnnotationHost)
                    ?? ClusterAnnotationHost(annotation: cluster, reuseIdentifier: "cluster")
                view.annotation = cluster
                view.configure(with: cluster.cluster)
                view.displayPriority = .required
                view.zPriority = MKAnnotationViewZPriority(rawValue: 500)
                return view
            }

            if let stopAnn = annotation as? StopMKAnnotation {
                let view = (mapView.dequeueReusableAnnotationView(withIdentifier: "stop") as? StopAnnotationHost)
                    ?? StopAnnotationHost(annotation: stopAnn, reuseIdentifier: "stop")
                view.annotation = stopAnn
                view.configure(with: stopAnn.stop,
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
                               transitType: transitType)
                view.displayPriority = .required
                view.zPriority = .max
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
            if let cluster = view.annotation as? ClusterMKAnnotation {
                parent.onClusterTap(cluster.cluster)
            } else if let stopAnn = view.annotation as? StopMKAnnotation {
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
            isUpdatingFromMap = false
        }
    }
}

// MARK: - Custom MKAnnotation classes

final class ClusterMKAnnotation: NSObject, MKAnnotation {
    var cluster: StopCluster
    @objc dynamic var coordinate: CLLocationCoordinate2D
    init(cluster: StopCluster) {
        self.cluster = cluster
        self.coordinate = CLLocationCoordinate2D(latitude: cluster.centerLat, longitude: cluster.centerLng)
    }
}

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
    init(vehicle: GtfsRtVehicle) {
        self.vehicle = vehicle
        self.coordinate = CLLocationCoordinate2D(
            latitude: Double(vehicle.latitude),
            longitude: Double(vehicle.longitude)
        )
    }
}

// MARK: - Annotation host views (UIKit wrapping SwiftUI)

final class ClusterAnnotationHost: MKAnnotationView {
    private var hosting: UIHostingController<ClusterAnnotationView>?

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        frame = CGRect(x: 0, y: 0, width: 44, height: 44)
        backgroundColor = .clear
        centerOffset = .zero
    }

    required init?(coder: NSCoder) { fatalError() }

    func configure(with cluster: StopCluster) {
        let view = ClusterAnnotationView(count: cluster.count)
        if let hc = hosting {
            hc.rootView = view
        } else {
            let hc = UIHostingController(rootView: view)
            hc.view.backgroundColor = .clear
            hc.view.frame = bounds
            hc.view.isUserInteractionEnabled = false
            addSubview(hc.view)
            hosting = hc
        }
    }
}

final class StopAnnotationHost: MKAnnotationView {
    private var hosting: UIHostingController<StopAnnotationView>?

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        frame = CGRect(x: 0, y: 0, width: 60, height: 60)
        backgroundColor = .clear
    }

    required init?(coder: NSCoder) { fatalError() }

    func configure(with stop: ResolvedStop,
                   zoomLevel: MapZoomLevel,
                   isSelected: Bool,
                   routeColor: String?) {
        let view = StopAnnotationView(
            stop: stop,
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
            addSubview(hc.view)
            hosting = hc
        }
        // Anchor: .far uses center; otherwise bottom (triangle tip at coord).
        centerOffset = zoomLevel == .far ? .zero : CGPoint(x: 0, y: -22)
    }
}

final class VehicleAnnotationHost: MKAnnotationView {
    private var hosting: UIHostingController<VehicleAnnotationView>?

    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        frame = CGRect(x: 0, y: 0, width: 60, height: 60)
        backgroundColor = .clear
        centerOffset = .zero
    }

    required init?(coder: NSCoder) { fatalError() }

    func configure(with vehicle: GtfsRtVehicle,
                   routeColor: String?,
                   transitType: TransitType) {
        let view = VehicleAnnotationView(
            vehicle: vehicle,
            routeColor: routeColor,
            transitType: transitType
        )
        if let hc = hosting {
            hc.rootView = view
        } else {
            let hc = UIHostingController(rootView: view)
            hc.view.backgroundColor = .clear
            hc.view.frame = bounds
            hc.view.isUserInteractionEnabled = false
            addSubview(hc.view)
            hosting = hc
        }
    }
}
