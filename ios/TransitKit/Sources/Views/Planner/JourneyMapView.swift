import SwiftUI
import MapKit
import CoreLocation

/// Mappa overview del viaggio. Pattern portato da Movete:
/// - una polyline per ogni transit leg col colore della linea, passando per
///   gli intermediate stops (segmenti retti — MOTIS oggi non passa shape GTFS)
/// - walking leg dashed gray
/// - intermediate stops via `StopAnnotationView` (stessi componenti + zoom
///   tier della mappa principale)
/// - pin endpoint start/end (verde/rosso) sopra le polilinee
/// - posizione utente via `UserAnnotation()`
///
/// Z-order: polilinee → user location → intermediate stops → endpoint pins.
/// SwiftUI Map renderizza gli annotations in ordine, quindi gli ultimi
/// stanno sopra. Endpoint pins ultimi = sempre in cima.
struct JourneyMapView: View {
    let journey: Journey
    /// Override del tier calcolato. Usato dalla preview compatta (200pt) per
    /// forzare `.far`: a parità di lat-span, la preview ha un rapporto
    /// pixel/grado molto basso e i marker tier `.neighborhood` apparirebbero
    /// troppo grossi rispetto alla mappa "vera" zoomata uguale.
    var fixedTier: MapZoomTier? = nil

    @Environment(ScheduleStore.self) private var store

    @State private var cameraPosition: MapCameraPosition = .automatic
    @State private var currentLatSpan: Double = 0.02

    private var zoomLevel: MapZoomLevel {
        // tier .city corrisponde visivamente a level .far
        if fixedTier == .city { return .far }
        return MapZoomLevel(latitudeDelta: currentLatSpan)
    }
    private var zoomTier: MapZoomTier {
        fixedTier ?? MapZoomTier(latitudeDelta: currentLatSpan)
    }

    var body: some View {
        Map(position: $cameraPosition) {
            // 1) Polilinee per leg
            ForEach(identifiedLegs) { entry in
                if entry.coords.count >= 2 {
                    let poly = MapPolyline(coordinates: entry.coords)
                    switch entry.leg {
                    case .transit(let t):
                        poly.stroke(
                            polylineColor(for: t),
                            style: StrokeStyle(lineWidth: 5, lineCap: .round, lineJoin: .round)
                        )
                    case .walking:
                        poly.stroke(
                            Color(.secondaryLabel),
                            style: StrokeStyle(
                                lineWidth: 3, lineCap: .round, lineJoin: .round,
                                dash: [3, 5]
                            )
                        )
                    }
                }
            }

            // 2) User location (semi-trasparente sotto i marker)
            UserAnnotation()

            // 3) Fermate intermedie — stessi componenti della mappa principale
            ForEach(intermediateAnnotations) { entry in
                Annotation(
                    "",
                    coordinate: CLLocationCoordinate2D(latitude: entry.stop.lat, longitude: entry.stop.lng),
                    anchor: zoomTier == .street ? .bottom : .center
                ) {
                    StopAnnotationView(
                        stop: entry.stop,
                        tier: zoomTier,
                        zoomLevel: zoomLevel,
                        isSelected: false,
                        routeColor: entry.routeColorHex
                    )
                }
                .annotationTitles(.hidden)
            }

            // 4) Endpoint pins (start/end) — sempre in cima
            if let start = startCoord {
                Annotation("", coordinate: start) { EndpointPin(role: .start) }
                    .annotationTitles(.hidden)
            }
            if let end = endCoord {
                Annotation("", coordinate: end) { EndpointPin(role: .end) }
                    .annotationTitles(.hidden)
            }
        }
        .mapStyle(.standard(elevation: .flat, pointsOfInterest: .excludingAll))
        .tint(.blue) // forza user location pallino blu (vedi LocationPickerMap)
        .onMapCameraChange { ctx in
            currentLatSpan = ctx.region.span.latitudeDelta
        }
        .onAppear { fitToContent() }
    }

    // MARK: - Data

    private struct IdentifiedLegEntry: Identifiable {
        let id: Int
        let leg: Leg
        let coords: [CLLocationCoordinate2D]
    }

    private var decodedLegs: [(leg: Leg, coords: [CLLocationCoordinate2D])] {
        journey.legs.map { leg in (leg: leg, coords: coordsForLeg(leg)) }
    }

    private var identifiedLegs: [IdentifiedLegEntry] {
        stitchedDecodedLegs.enumerated().map { i, e in
            IdentifiedLegEntry(id: i, leg: e.leg, coords: e.coords)
        }
    }

    /// Salda gli endpoint di leg consecutivi per evitare buchi sub-meter
    /// tra polyline decodificata e fermate canonical.
    private var stitchedDecodedLegs: [(leg: Leg, coords: [CLLocationCoordinate2D])] {
        let raw = decodedLegs
        return raw.enumerated().map { (idx, entry) in
            guard idx > 0, !entry.coords.isEmpty,
                  let prevLast = raw[idx - 1].coords.last else { return entry }
            let first = entry.coords[0]
            if abs(prevLast.latitude - first.latitude) < 1e-6 &&
               abs(prevLast.longitude - first.longitude) < 1e-6 {
                return entry
            }
            return (leg: entry.leg, coords: [prevLast] + entry.coords)
        }
    }

    private func coordsForLeg(_ leg: Leg) -> [CLLocationCoordinate2D] {
        switch leg {
        case .transit(let t):
            var pts: [CLLocationCoordinate2D] = [
                CLLocationCoordinate2D(latitude: t.boardStop.lat, longitude: t.boardStop.lng)
            ]
            pts.append(contentsOf: t.intermediateStops.map {
                CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng)
            })
            pts.append(CLLocationCoordinate2D(latitude: t.alightStop.lat, longitude: t.alightStop.lng))
            return pts
        case .walking(let w):
            return [
                CLLocationCoordinate2D(latitude: w.fromStop.lat, longitude: w.fromStop.lng),
                CLLocationCoordinate2D(latitude: w.toStop.lat, longitude: w.toStop.lng)
            ]
        }
    }

    // MARK: - Intermediate stops resolution

    private struct IntermediateAnnotation: Identifiable {
        let id: String
        let stop: ResolvedStop
        let routeColorHex: String
    }

    /// Risolve ogni `IntermediateStop` MOTIS a un `ResolvedStop` del catalogo
    /// (così `StopAnnotationView` può applicare lo stile transit-type +
    /// route color). Se non trovato nel catalogo, costruisce un ResolvedStop
    /// ad-hoc per non perdere il marker sulla mappa.
    private var intermediateAnnotations: [IntermediateAnnotation] {
        var out: [IntermediateAnnotation] = []
        for leg in journey.legs {
            guard case .transit(let t) = leg else { continue }
            let routeHex = t.routeColor
            for inter in t.intermediateStops {
                let resolved = resolveIntermediateStop(inter)
                out.append(IntermediateAnnotation(
                    id: "\(t.id)_\(inter.id)",
                    stop: resolved,
                    routeColorHex: routeHex
                ))
            }
        }
        return out
    }

    private func resolveIntermediateStop(_ inter: IntermediateStop) -> ResolvedStop {
        // Try lookup canonical id or any gtfs_stop_id match
        if let match = store.stops.first(where: {
            $0.id == inter.id || $0.gtfsStopIds.contains(inter.id)
        }) {
            return match
        }
        // Ad-hoc fallback con i dati MOTIS (lat/lng)
        return ResolvedStop(
            id: inter.id,
            name: inter.name,
            lat: inter.lat,
            lng: inter.lng,
            lineNames: [],
            transitTypes: [.bus],
            docks: [],
            gtfsStopIds: [inter.id]
        )
    }

    // MARK: - Endpoints

    private var startCoord: CLLocationCoordinate2D? {
        identifiedLegs.first?.coords.first
    }

    private var endCoord: CLLocationCoordinate2D? {
        identifiedLegs.last?.coords.last
    }

    // MARK: - Camera fit

    private func fitToContent() {
        let allCoords = identifiedLegs.flatMap { $0.coords }
        guard !allCoords.isEmpty else { return }
        let lats = allCoords.map(\.latitude)
        let lngs = allCoords.map(\.longitude)
        let center = CLLocationCoordinate2D(
            latitude: ((lats.min() ?? 0) + (lats.max() ?? 0)) / 2,
            longitude: ((lngs.min() ?? 0) + (lngs.max() ?? 0)) / 2
        )
        let span = MKCoordinateSpan(
            latitudeDelta: max(0.005, ((lats.max() ?? 0) - (lats.min() ?? 0)) * 1.4),
            longitudeDelta: max(0.005, ((lngs.max() ?? 0) - (lngs.min() ?? 0)) * 1.4)
        )
        currentLatSpan = span.latitudeDelta
        cameraPosition = .region(MKCoordinateRegion(center: center, span: span))
    }

    // MARK: - Polyline color

    private func polylineColor(for leg: TransitLeg) -> Color {
        let hex = leg.routeColor
        if hex.uppercased() == "FFFFFF" { return Color(.secondaryLabel) }
        return Color(hex: "#\(hex)")
    }
}

// MARK: - EndpointPin

private struct EndpointPin: View {
    enum Role { case start, end }
    let role: Role

    var body: some View {
        ZStack {
            Circle()
                .fill(role == .start ? Color.green : Color.red)
                .frame(width: 18, height: 18)
            Circle()
                .strokeBorder(.white, lineWidth: 2.5)
                .frame(width: 18, height: 18)
        }
        .shadow(color: .black.opacity(0.25), radius: 3, y: 1)
    }
}
