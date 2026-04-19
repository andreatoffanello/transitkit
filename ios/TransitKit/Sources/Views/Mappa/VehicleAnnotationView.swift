import SwiftUI

// MARK: - Vehicle Annotation View
//
// Porting 1:1 da movete (ios/Movete/Features/Map/VehicleAnnotation.swift):
// - pallino colore GTFS con ring bianco + halo pulsante (sempre)
// - badge rettangolare col nome linea sopra (solo tier .street)
// - tail triangolo che collega badge al dot
//
// La pulse alpha è ancorata a `Date` via `TimelineView` così tutti i pin del
// frame corrente condividono lo stesso valore senza creare N animazioni
// parallele.

struct VehicleAnnotationView: View {
    let vehicle: GtfsRtVehicle
    let routeColor: String?
    let transitType: TransitType
    let tier: MapZoomTier
    let route: APIRoute?
    var isSelected: Bool = false

    // MARK: Colors

    private var lineColor: Color {
        let hex = routeColor ?? colorForTransitType(transitType)
        return Color(hex: hex.hasPrefix("#") ? hex : "#\(hex)")
    }

    // MARK: Badge content

    /// Nome breve della linea per il badge. Fallback: primi 3 char di longName.
    private var routeName: String {
        guard let route else { return "" }
        if !route.name.isEmpty {
            return route.name.count <= 4 ? route.name : String(route.name.prefix(3))
        }
        if let long = route.longName, !long.isEmpty {
            return String(long.prefix(3))
        }
        return ""
    }

    // MARK: Tier sizing (1:1 con movete)

    private var dotSize: CGFloat {
        switch tier {
        case .city:         return 8
        case .neighborhood: return 11
        case .street:       return 13
        }
    }

    private var haloSize: CGFloat {
        switch tier {
        case .city:         return 18
        case .neighborhood: return 24
        case .street:       return 30
        }
    }

    private var showBadge: Bool { tier == .street && !routeName.isEmpty }

    var body: some View {
        TimelineView(.animation) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            // Pulse periodo 1.8s, ease in/out tra 0.16 e 0.42
            let phase = (sin(t * .pi / 0.9) + 1) / 2
            let alpha = 0.16 + (0.42 - 0.16) * phase

            VStack(spacing: 0) {
                if showBadge {
                    // Badge pill
                    Text(routeName)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                        .padding(.horizontal, 9)
                        .padding(.vertical, 4)
                        .background(
                            RoundedRectangle(cornerRadius: 11, style: .continuous)
                                .fill(lineColor)
                        )
                        .shadow(color: .black.opacity(0.25), radius: 4, y: 2)

                    // Tail triangle
                    DownwardBadgeTail()
                        .fill(lineColor)
                        .frame(width: 8, height: 5)

                    Spacer().frame(height: 1)
                }

                // Halo + ring + dot in un unico Canvas per depth/stabilità
                Canvas { ctx, size in
                    let center = CGPoint(x: size.width / 2, y: size.height / 2)
                    let haloR = size.width / 2
                    let ringR = (dotSize + 3) / 2
                    let dotR = dotSize / 2

                    // Halo pulsante
                    ctx.fill(
                        Path(ellipseIn: CGRect(
                            x: center.x - haloR, y: center.y - haloR,
                            width: haloR * 2, height: haloR * 2
                        )),
                        with: .color(lineColor.opacity(alpha))
                    )
                    // Shadow del ring (cerchio nero translucido offset)
                    ctx.fill(
                        Path(ellipseIn: CGRect(
                            x: center.x - ringR - 0.5,
                            y: center.y - ringR + 0.5,
                            width: (ringR + 0.5) * 2,
                            height: (ringR + 0.5) * 2
                        )),
                        with: .color(.black.opacity(0.22))
                    )
                    // Ring bianco
                    ctx.fill(
                        Path(ellipseIn: CGRect(
                            x: center.x - ringR, y: center.y - ringR,
                            width: ringR * 2, height: ringR * 2
                        )),
                        with: .color(.white)
                    )
                    // Dot colorato
                    ctx.fill(
                        Path(ellipseIn: CGRect(
                            x: center.x - dotR, y: center.y - dotR,
                            width: dotR * 2, height: dotR * 2
                        )),
                        with: .color(lineColor)
                    )
                }
                .frame(width: haloSize, height: haloSize)

                // Spacer invisibile che bilancia badge+tail+gap sopra, così il
                // centro visivo del composable coincide col centro del pallino
                // (MapKit Annotation piazza il centro del View sulla coordinata).
                if showBadge {
                    Spacer().frame(height: 26)
                }
            }
        }
        .accessibilityLabel("\(transitType.displayName) \(vehicle.id)")
    }
}

// MARK: - Badge tail

private struct DownwardBadgeTail: Shape {
    func path(in rect: CGRect) -> Path {
        Path { p in
            p.move(to: CGPoint(x: rect.minX, y: rect.minY))
            p.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
            p.addLine(to: CGPoint(x: rect.midX, y: rect.maxY))
            p.closeSubpath()
        }
    }
}
