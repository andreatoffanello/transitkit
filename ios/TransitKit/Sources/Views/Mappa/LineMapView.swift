import SwiftUI
import MapKit

// MARK: - LineMapView

/// Full-screen map showing a single route's shape + real-time vehicle positions.
/// Opened via fullScreenCover from LineDetailView.
struct LineMapView: View {
    let route: Route
    let directionId: Int

    @Environment(\.dismiss) private var dismiss
    @Environment(VehicleStore.self) private var vehicleStore

    private var lineColor: Color { Color(hex: route.color) }
    private var textColor: Color { Color(hex: contrastingTextColor(for: route.color)) }

    private var vehicles: [GtfsRtVehicle] {
        vehicleStore.vehicles(forRouteId: route.id)
    }

    // MARK: - Zoom tracking
    @State private var cameraPosition: MapCameraPosition = .automatic
    @State private var latitudeDelta: Double = 0.1   // degrees; drives rendering tier

    var body: some View {
        ZStack(alignment: .topLeading) {
            Map(position: $cameraPosition) {
                RouteOverlay(route: route, directionId: directionId)

                ForEach(vehicles) { vehicle in
                    Annotation("", coordinate: CLLocationCoordinate2D(
                        latitude: Double(vehicle.latitude),
                        longitude: Double(vehicle.longitude)
                    )) {
                        VehiclePinView(
                            routeName: route.name,
                            color: lineColor,
                            textColor: textColor,
                            bearing: vehicle.bearing,
                            zoomTier: zoomTier
                        )
                    }
                }
            }
            .mapStyle(.standard(pointsOfInterest: .excludingAll))
            .ignoresSafeArea()
            .onMapCameraChange { context in
                latitudeDelta = context.region.span.latitudeDelta
            }

            // Close button
            Button { dismiss() } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.primary)
                    .frame(width: 36, height: 36)
                    .background(.regularMaterial, in: Circle())
                    .shadow(color: .black.opacity(0.12), radius: 4, y: 2)
            }
            .padding(.top, 56)
            .padding(.leading, 16)
            .accessibilityLabel("Chiudi mappa")
            .accessibilityIdentifier("btn_linemap_close")

            // Live vehicle count badge (top right)
            if !vehicles.isEmpty {
                VStack {
                    HStack {
                        Spacer()
                        LiveBadge(count: vehicles.count)
                            .shadow(color: .black.opacity(0.1), radius: 3, y: 1)
                            .padding(.top, 58)
                            .padding(.trailing, 16)
                    }
                    Spacer()
                }
            }
        }
    }

    // MARK: - Zoom tier

    enum ZoomTier {
        case far      // latitudeDelta > 0.08 — tiny dot
        case medium   // 0.03…0.08 — small badge, no needle
        case close    // < 0.03 — full badge + bearing needle
    }

    private var zoomTier: ZoomTier {
        if latitudeDelta > 0.08 { return .far }
        if latitudeDelta > 0.03 { return .medium }
        return .close
    }
}

// MARK: - Vehicle Pin View

private struct VehiclePinView: View {
    let routeName: String
    let color: Color
    let textColor: Color
    let bearing: Float
    let zoomTier: LineMapView.ZoomTier

    var body: some View {
        switch zoomTier {
        case .far:
            // Tiny dot — no label, no needle
            Circle()
                .fill(color)
                .frame(width: 8, height: 8)
                .overlay(Circle().stroke(.white, lineWidth: 1))

        case .medium:
            // Compact badge, no needle — readable but not cluttered
            ZStack {
                Circle()
                    .fill(color)
                    .frame(width: 22, height: 22)
                    .overlay(Circle().stroke(.white, lineWidth: 1.2))
                Text(routeName)
                    .font(.system(size: 7, weight: .black, design: .rounded))
                    .foregroundStyle(textColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .padding(.horizontal, 2)
            }
            .shadow(color: .black.opacity(0.2), radius: 3, y: 1)
            .drawingGroup()

        case .close:
            // Full badge + bearing needle that rotates independently
            FullVehiclePin(
                routeName: routeName,
                color: color,
                textColor: textColor,
                bearing: bearing
            )
        }
    }
}

// MARK: - Full Vehicle Pin (close zoom)

/// Badge stays upright; only the directional needle rotates.
/// Both elements share a 46×46 ZStack so `rotationEffect` on the needle
/// anchors at the badge center.
private struct FullVehiclePin: View {
    let routeName: String
    let color: Color
    let textColor: Color
    let bearing: Float

    private static let size: CGFloat = 46

    var body: some View {
        ZStack {
            // ── Bearing needle ──────────────────────────────────────────
            // Canvas fills the full 46×46 frame.
            // Triangle tip drawn at (cx, cy-20), base at (cx±5, cy-12).
            // rotationEffect rotates the canvas around its center = badge center.
            // Only shown when bearing is non-zero (bearing == 0 means "unknown").
            if bearing != 0 {
                Canvas { ctx, size in
                    let cx = size.width / 2
                    let cy = size.height / 2
                    var path = Path()
                    path.move(to: CGPoint(x: cx,   y: cy - 20))   // tip
                    path.addLine(to: CGPoint(x: cx - 5, y: cy - 11)) // left base
                    path.addLine(to: CGPoint(x: cx + 5, y: cy - 11)) // right base
                    path.closeSubpath()
                    // Filled with line color
                    ctx.fill(path, with: .color(color))
                    // White outline for contrast on similar-colored backgrounds
                    ctx.stroke(path, with: .color(.white), lineWidth: 1.5)
                }
                .frame(width: Self.size, height: Self.size)
                .rotationEffect(.degrees(Double(bearing)))
            }

            // ── Badge ────────────────────────────────────────────────────
            // Centered in the ZStack. Never rotated.
            ZStack {
                Circle()
                    .fill(color)
                    .frame(width: 30, height: 30)
                    .overlay(Circle().stroke(.white, lineWidth: 1.5))
                Text(routeName)
                    .font(.system(size: 9, weight: .black, design: .rounded))
                    .foregroundStyle(textColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .padding(.horizontal, 2)
            }
            .shadow(color: .black.opacity(0.2), radius: 3, y: 1)
        }
        .frame(width: Self.size, height: Self.size)
        .drawingGroup()
        .accessibilityLabel("\(routeName) in transito")
        .accessibilityIdentifier("vehicle_\(routeName)")
    }
}


// MARK: - Environment Key

private struct VehiclePositionsUrlKey: EnvironmentKey {
    static let defaultValue: String? = nil
}

extension EnvironmentValues {
    var vehiclePositionsUrl: String? {
        get { self[VehiclePositionsUrlKey.self] }
        set { self[VehiclePositionsUrlKey.self] = newValue }
    }
}
