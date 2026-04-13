import SwiftUI
import MapKit

// MARK: - Cached Polyline

/// A pre-decoded polyline for one route direction.
/// Computed once off-main-thread and passed into `RouteOverlay` to avoid
/// re-decoding on every Map body evaluation (which fires at 60 fps during pan).
struct CachedPolyline: Identifiable, Sendable {
    let id: Int                                 // direction ID
    let coordinates: [CLLocationCoordinate2D]
}

// MARK: - Route Map Overlay

/// Displays pre-decoded route polylines on the map.
///
/// Polylines are computed asynchronously in `MappaTab.recomputeRoutePolylines()`
/// when the selected route or direction changes, and passed in as `CachedPolyline`
/// values. This eliminates per-frame polyline decoding work.
///
/// Usage inside a `Map { }` content builder:
/// ```swift
/// RouteOverlay(polylines: cachedRoutePolylines, color: selectedRoute?.color)
/// ```
struct RouteOverlay: MapContent {
    let polylines: [CachedPolyline]
    let color: String?

    private var strokeColor: Color {
        Color(hex: color ?? "#3388FF")
    }

    var body: some MapContent {
        ForEach(polylines) { polyline in
            MapPolyline(coordinates: polyline.coordinates)
                .stroke(strokeColor, style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round))
        }
    }
}

// MARK: - Google Encoded Polyline Decoder

/// Decodes a Google-encoded polyline string into an array of coordinates.
/// Spec: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
///
/// Called from `MappaTab.recomputeRoutePolylines()` on a background thread.
func decodeGooglePolyline(_ encoded: String) -> [CLLocationCoordinate2D] {
    var coordinates: [CLLocationCoordinate2D] = []
    let bytes = Array(encoded.utf8)
    var index = 0
    var lat = 0
    var lng = 0

    while index < bytes.count {
        // Decode latitude delta
        var result = 0
        var shift = 0
        var byte: Int
        repeat {
            guard index < bytes.count else { return coordinates }
            byte = Int(bytes[index]) - 63
            index += 1
            result |= (byte & 0x1F) << shift
            shift += 5
        } while byte >= 0x20
        lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1)

        // Decode longitude delta
        result = 0
        shift = 0
        repeat {
            guard index < bytes.count else { return coordinates }
            byte = Int(bytes[index]) - 63
            index += 1
            result |= (byte & 0x1F) << shift
            shift += 5
        } while byte >= 0x20
        lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1)

        coordinates.append(CLLocationCoordinate2D(
            latitude: Double(lat) / 1e5,
            longitude: Double(lng) / 1e5
        ))
    }
    return coordinates
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
