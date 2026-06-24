import SwiftUI
import MapKit

// MARK: - Map Line Focus Content

/// Contenuto riusabile per la "vista linea" dentro una SwiftUI `Map { }`:
/// polilinea (tutte le direzioni) + fermate tiered. I **mezzi live NON sono
/// qui**: vanno resi come overlay SwiftUI via `MapProxy.convert` (vedi
/// `AnimatedVehicleMarker`), perché SwiftUI `Map` non garantisce lo z-order tra
/// `Annotation` e in 3D li ordina per profondità → un mezzo finirebbe sotto una
/// fermata. Pattern Movete/DoVe.
///
/// iOS 26: un `ForEach` direttamente nella closure di `Map { }` NON itera —
/// il rendering vive qui dentro come `MapContent` dedicato (stesso pattern
/// di `RouteOverlay`).
struct MapLineFocusContent: MapContent {
    let route: APIRoute
    let polylines: [CachedPolyline]
    let stops: [ResolvedStop]
    let tier: MapZoomTier
    let zoomLevel: MapZoomLevel
    /// Fermata evidenziata (es. la fermata del dettaglio corrente).
    var highlightedStopId: String? = nil

    var body: some MapContent {
        RouteOverlay(polylines: polylines, color: route.color)
        stopAnnotations
    }

    @MapContentBuilder
    private var stopAnnotations: some MapContent {
        ForEach(stops, id: \.id) { s in
            Annotation(
                "",
                coordinate: CLLocationCoordinate2D(latitude: s.lat, longitude: s.lng),
                anchor: tier == .street ? .bottom : .center
            ) {
                StopAnnotationView(
                    stop: s,
                    tier: tier,
                    zoomLevel: zoomLevel,
                    isSelected: s.id == highlightedStopId,
                    routeColor: route.color
                )
            }
        }
    }
}

// MARK: - Polyline loader

/// Decodifica le polilinee di TUTTE le direzioni di una route fuori dal main
/// thread: `shapePolyline` (Google encoded) → `shape` ([[lat,lng]]) → fallback
/// coordinate fermate ordinate. Mirror di `MappaTab.recomputeRoutePolylines`
/// senza il filtro direzione — la vista linea mostra l'intera linea.
@MainActor
func decodeAllRoutePolylines(for route: APIRoute, store: ScheduleStore) async -> [CachedPolyline] {
    // Fallback fermate risolte sul main (accesso allo store).
    let fallbackCoords: [Int: [CLLocationCoordinate2D]] = Dictionary(
        route.directions.map { d in
            (d.directionId, store.stopsForRoute(route.id, directionId: d.directionId)
                .map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng) })
        },
        uniquingKeysWith: { first, _ in first }
    )
    let directions = route.directions

    return await Task.detached(priority: .utility) {
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
}
