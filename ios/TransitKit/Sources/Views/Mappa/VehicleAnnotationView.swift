import SwiftUI

// MARK: - Bearing Cone Shape

/// A pie-slice sector centered on the frame, pointing in a given compass direction.
/// Used for the directional heading cone on vehicle annotations, à la Google Maps.
private struct BearingConeShape: Shape {
    let bearing: Float  // degrees clockwise from True North (GTFS-RT convention)

    private let arcAngle: Double = 65   // total sweep in degrees
    private let radius: CGFloat = 28    // pixels from center to arc outer edge

    func path(in rect: CGRect) -> Path {
        let center = CGPoint(x: rect.midX, y: rect.midY)
        // GTFS bearing: 0=N, 90=E, 180=S, 270=W (clockwise from North)
        // SwiftUI angles: 0=right(East), increasing clockwise
        // → SwiftUI angle = bearing − 90
        let mid   = Double(bearing) - 90
        let start = Angle(degrees: mid - arcAngle / 2)
        let end   = Angle(degrees: mid + arcAngle / 2)

        var path = Path()
        path.move(to: center)
        path.addArc(center: center, radius: radius, startAngle: start, endAngle: end, clockwise: false)
        path.closeSubpath()
        return path
    }
}

// MARK: - Vehicle Annotation View

/// Map annotation for a live GTFS-RT vehicle.
/// Shows a colored circle with the transit type icon.
/// When bearing is known, a directional cone (Google Maps–style) extends behind the circle.
struct VehicleAnnotationView: View {
    let vehicle: GtfsRtVehicle
    let routeColor: String?
    let transitType: TransitType

    private var bgColor: Color {
        Color(hex: routeColor ?? colorForTransitType(transitType))
    }

    var body: some View {
        ZStack {
            // Direction cone — rendered behind vehicle circle, radial gradient fill (solid near circle, transparent outward)
            if vehicle.bearing != 0 {
                BearingConeShape(bearing: vehicle.bearing)
                    .fill(
                        RadialGradient(
                            colors: [bgColor.opacity(0.65), bgColor.opacity(0)],
                            center: .center,
                            startRadius: 12,
                            endRadius: 28
                        )
                    )
            }

            // Outer white ring
            Circle()
                .fill(.white)
                .frame(width: 26, height: 26)

            // Colored fill
            Circle()
                .fill(bgColor)
                .frame(width: 22, height: 22)

            // Transit type icon
            transitType.icon.sized(10)
                .foregroundStyle(.white)
        }
        .shadow(color: .black.opacity(0.25), radius: 3, y: 1.5)
        .frame(width: 44, height: 44)
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Vehicle \(vehicle.label.isEmpty ? vehicle.id : vehicle.label)")
    }
}
