import SwiftUI
import MapKit

// MARK: - Route Map Overlay

/// Displays a route's polyline shape on the map.
/// Uses `Route.directions.shape` coordinates and the route's GTFS color.
///
/// Usage inside a `Map { }` content builder:
/// ```swift
/// if let route = selectedRoute {
///     RouteOverlay(route: route, directionId: 0)
/// }
/// ```
struct RouteOverlay: MapContent {
    let route: APIRoute
    var directionId: Int? = nil

    /// Directions to render. If directionId is specified, show only that one;
    /// otherwise show all directions.
    private var directionsToRender: [APIRouteDirection] {
        if let directionId {
            return route.directions.filter { $0.directionId == directionId }
        }
        return route.directions
    }

    private var strokeColor: Color {
        Color(hex: route.color ?? "#000000")
    }

    /// shapePolyline is an encoded polyline string — not yet decoded.
    /// For now, no shape overlays are drawn; stop annotations provide fallback visualization.
    private var polylines: [(id: Int, coordinates: [CLLocationCoordinate2D])] {
        return []
    }

    var body: some MapContent {
        ForEach(polylines, id: \.id) { polyline in
            MapPolyline(coordinates: polyline.coordinates)
                .stroke(strokeColor, lineWidth: 4)
        }
    }
}

// MARK: - Route Overlay Toggle

/// A button to toggle route overlay visibility on the map.
struct RouteOverlayToggle: View {
    @Binding var isVisible: Bool
    let routeName: String
    let routeColor: String

    var body: some View {
        Button {
            withAnimation(.easeInOut(duration: 0.2)) {
                isVisible.toggle()
            }
        } label: {
            HStack(spacing: 6) {
                Circle()
                    .fill(Color(hex: routeColor))
                    .frame(width: 10, height: 10)

                Text(routeName)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(isVisible ? Color(.label) : AppTheme.textTertiary)

                (isVisible ? LucideIcon.eye : LucideIcon.eyeOff).sized(11)
                    .foregroundStyle(isVisible ? AppTheme.accent : AppTheme.textTertiary)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(.regularMaterial)
            .clipShape(Capsule())
            .shadow(color: .black.opacity(0.1), radius: 2, y: 1)
        }
        .accessibilityLabel(String(format: NSLocalizedString("toggle_route_overlay", comment: ""), routeName))
        .accessibilityIdentifier("btn_route_overlay_\(routeName)")
    }
}
