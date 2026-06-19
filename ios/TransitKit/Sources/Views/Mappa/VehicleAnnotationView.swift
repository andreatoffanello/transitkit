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

    // MARK: Geometry helpers

    private var ringR: CGFloat { (dotSize + 3) / 2 }
    private var haloR: CGFloat { haloSize / 2 }

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

                    // Tail triangle — tip tocca il top del ring, nessun gap
                    DownwardBadgeTail()
                        .fill(lineColor)
                        .frame(width: 8, height: 5)
                }

                // Ring + dot + halo in ZStack:
                //   - halo layer (dietro): Circle con opacity pulsante, dimensionato
                //     haloSize e posizionato a center così può overflow verso l'alto
                //     senza che il Canvas di ring/dot lo clippi. SwiftUI non clippa
                //     i figli di uno ZStack a meno di .clipped() esplicito.
                //   - ring/dot layer (davanti): Canvas con centerY = ringR (con badge)
                //     o height/2 (senza badge). Frame = haloSize×haloSize.
                //
                // Con badge: ringR è il top del Canvas quindi il ring top coincide
                // col Canvas top, direttamente connesso al tail. Il halo sovrasta
                // il Canvas di (haloR - ringR) px verso l'alto, ma essendo in ZStack
                // e non in Canvas, quel tratto è visibile (no clip).
                // Senza badge: center.y = size.height/2 — comportamento invariato.
                ZStack {
                    // Halo pulsante — disegnato come Circle SwiftUI, non in Canvas,
                    // così non è soggetto al clipping del Canvas sottostante.
                    // Allineamento: il centro del halo deve coincidere col centro
                    // del ring, non col centro dello ZStack frame.
                    // Con badge: ring center è a ringR dal top dello ZStack (= haloSize/2
                    // centrato in ZStack, ma ring è a ringR = haloSize/2 - (haloR-ringR)
                    // dal top → ring center.y relativo a ZStack center = -(haloR-ringR)).
                    let ringOffsetY: CGFloat = showBadge ? -(haloR - ringR) : 0
                    Circle()
                        .fill(lineColor.opacity(alpha))
                        .frame(width: haloSize, height: haloSize)
                        .offset(y: ringOffsetY)
                        .allowsHitTesting(false)

                    // Ring + dot nel Canvas originale (non modificato rispetto
                    // al comportamento pre-fix — ring al top con badge, centrato senza).
                    Canvas { ctx, size in
                        let centerY: CGFloat = showBadge ? ringR : size.height / 2
                        let center = CGPoint(x: size.width / 2, y: centerY)
                        let rR = ringR
                        let dR = dotSize / 2

                        // Shadow del ring (cerchio nero translucido offset)
                        ctx.fill(
                            Path(ellipseIn: CGRect(
                                x: center.x - rR - 0.5,
                                y: center.y - rR + 0.5,
                                width: (rR + 0.5) * 2,
                                height: (rR + 0.5) * 2
                            )),
                            with: .color(.black.opacity(0.22))
                        )
                        // Ring bianco
                        ctx.fill(
                            Path(ellipseIn: CGRect(
                                x: center.x - rR, y: center.y - rR,
                                width: rR * 2, height: rR * 2
                            )),
                            with: .color(.white)
                        )
                        // Dot colorato
                        ctx.fill(
                            Path(ellipseIn: CGRect(
                                x: center.x - dR, y: center.y - dR,
                                width: dR * 2, height: dR * 2
                            )),
                            with: .color(lineColor)
                        )
                    }
                    .frame(width: haloSize, height: haloSize)
                }
                .frame(width: haloSize, height: haloSize)

                // Spacer invisibile che bilancia badge+tail sopra, così il
                // centro visivo del composable coincide col centro del pallino
                // (MapKit Annotation piazza il centro del View sulla coordinata).
                if showBadge {
                    Spacer().frame(height: 11)
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
