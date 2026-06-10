import SwiftUI

/// Etichetta nome fermata sotto al pin a tier .street — porting 1:1 da
/// movete (ios/Movete/Features/Map/StopMapLabel.swift).
///
/// Parità con Android (`StopSymbolLayer` textField a zoom ≥ street): testo
/// con stroke (halo) — no pill background. Theme-aware: in light testo
/// near-nero + halo bianco; in dark testo near-bianco + halo nero. Stroke
/// via 8 shadow direzionali a raggio 1 (cardinali + diagonali) — SwiftUI
/// non ha `Text.stroke` nativo.
///
/// Posizionamento: il padding top 10 crea il gap dal tip del pin quando la
/// label è agganciata al coordinate (qui: overlay sotto il frame 60pt dello
/// StopAnnotationView, il cui bottom coincide col tip).
struct StopMapLabel: View {
    let name: String
    @Environment(\.colorScheme) private var colorScheme

    private var textColor: Color {
        colorScheme == .dark
            ? Color(red: 0.95, green: 0.95, blue: 0.97)
            : Color(red: 0.10, green: 0.10, blue: 0.12)
    }

    private var haloColor: Color {
        colorScheme == .dark
            ? Color.black.opacity(0.92)
            : Color.white.opacity(0.95)
    }

    var body: some View {
        ZStack {
            // 8-direction halo "stroke": 4 cardinali + 4 diagonali a 1pt.
            ForEach(haloOffsets, id: \.id) { offset in
                Text(name)
                    .font(.system(size: 11.5, weight: .medium))
                    .foregroundStyle(haloColor)
                    .offset(x: offset.dx, y: offset.dy)
            }
            Text(name)
                .font(.system(size: 11.5, weight: .medium))
                .foregroundStyle(textColor)
        }
        .multilineTextAlignment(.center)
        .lineLimit(2)
        .fixedSize(horizontal: false, vertical: true)
        .frame(maxWidth: 92)
        .padding(.top, 10)
        .allowsHitTesting(false)
    }

    private struct HaloOffset: Identifiable {
        let id: String
        let dx: CGFloat
        let dy: CGFloat
    }

    private let haloOffsets: [HaloOffset] = [
        .init(id: "n",  dx: 0,  dy: -1),
        .init(id: "s",  dx: 0,  dy: 1),
        .init(id: "w",  dx: -1, dy: 0),
        .init(id: "e",  dx: 1,  dy: 0),
        .init(id: "nw", dx: -1, dy: -1),
        .init(id: "ne", dx: 1,  dy: -1),
        .init(id: "sw", dx: -1, dy: 1),
        .init(id: "se", dx: 1,  dy: 1),
    ]
}
