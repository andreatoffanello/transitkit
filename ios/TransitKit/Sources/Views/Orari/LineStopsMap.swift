import SwiftUI
import MapKit

/// Non-interactive inline minimap for a line direction.
/// Draws the real route shape when `shapeCoordinates` are provided (GTFS shapes.txt),
/// falling back to a straight polyline connecting stops when shape is absent.
struct LineStopsMap: View {
    let stops: [ResolvedStop]
    let lineColor: Color
    /// [[lat, lon]] pairs from `APIRouteDirection.shape` — the actual GTFS route geometry.
    var shapeCoordinates: [[Double]]? = nil

    private var stopCoordinates: [CLLocationCoordinate2D] {
        stops.map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng) }
    }

    /// Uses the real shape when available, stops as fallback.
    private var polylineCoordinates: [CLLocationCoordinate2D] {
        guard let shape = shapeCoordinates, shape.count >= 2 else { return stopCoordinates }
        return shape.compactMap { pair in
            guard pair.count >= 2 else { return nil }
            return CLLocationCoordinate2D(latitude: pair[0], longitude: pair[1])
        }
    }

    private var region: MKCoordinateRegion {
        guard !stops.isEmpty else {
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 45.0, longitude: 10.0),
                span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
            )
        }
        let lats = stops.map(\.lat)
        let lons = stops.map(\.lng)
        let minLat = lats.min()!; let maxLat = lats.max()!
        let minLon = lons.min()!; let maxLon = lons.max()!
        return MKCoordinateRegion(
            center: CLLocationCoordinate2D(
                latitude: (minLat + maxLat) / 2,
                longitude: (minLon + maxLon) / 2
            ),
            span: MKCoordinateSpan(
                latitudeDelta: max((maxLat - minLat) * 1.3, 0.005),
                longitudeDelta: max((maxLon - minLon) * 1.3, 0.005)
            )
        )
    }

    var body: some View {
        Map(initialPosition: .region(region)) {
            if polylineCoordinates.count >= 2 {
                MapPolyline(coordinates: polylineCoordinates)
                    .stroke(lineColor, style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
            }
            ForEach(Array(stops.enumerated()), id: \.element.id) { idx, stop in
                let isTerminal = idx == 0 || idx == stops.count - 1
                let coord = CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng)
                Annotation(isTerminal ? stop.name : "", coordinate: coord) {
                    Circle()
                        .fill(lineColor)
                        .frame(width: isTerminal ? 12 : 7, height: isTerminal ? 12 : 7)
                        .overlay(Circle().fill(.white).frame(width: isTerminal ? 5 : 0, height: isTerminal ? 5 : 0))
                        .shadow(color: lineColor.opacity(0.3), radius: 2)
                }
            }
        }
        .mapStyle(.standard(pointsOfInterest: .excludingAll))
        .allowsHitTesting(false)
    }
}
