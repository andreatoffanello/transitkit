import SwiftUI
import MapKit

/// Non-interactive inline minimap for a line direction.
/// Shows a polyline connecting the stops and markers for terminal/intermediate stops.
/// Used in LineDetailView to give a quick visual overview of the route.
struct LineStopsMap: View {
    let stops: [ResolvedStop]
    let lineColor: Color

    private var stopCoordinates: [CLLocationCoordinate2D] {
        stops.map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng) }
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
        let minLat = lats.min()!
        let maxLat = lats.max()!
        let minLon = lons.min()!
        let maxLon = lons.max()!
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
            // Polyline connecting stops in order
            if stopCoordinates.count >= 2 {
                MapPolyline(coordinates: stopCoordinates)
                    .stroke(lineColor, style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
            }

            // Stop markers: terminals bigger with hollow center, intermediates small
            ForEach(Array(stops.enumerated()), id: \.element.id) { idx, stop in
                let isTerminal = idx == 0 || idx == stops.count - 1
                let coord = CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng)
                Annotation(isTerminal ? stop.name : "", coordinate: coord) {
                    Circle()
                        .fill(lineColor)
                        .frame(width: isTerminal ? 12 : 7, height: isTerminal ? 12 : 7)
                        .overlay(
                            Circle()
                                .fill(.white)
                                .frame(width: isTerminal ? 5 : 0, height: isTerminal ? 5 : 0)
                        )
                        .shadow(color: lineColor.opacity(0.3), radius: 2)
                }
            }
        }
        .mapStyle(.standard(pointsOfInterest: .excludingAll))
        .allowsHitTesting(false)
    }
}
