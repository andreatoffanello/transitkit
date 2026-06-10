import SwiftUI

/// Token unici del chrome flottante su mappa — allineati ai pill canonici
/// (`MapControlsColumn`/`MapExpandedControls`: .regularMaterial, celle 44pt).
/// Ogni bottone overlay su mappa legge da qui, niente valori magici sparsi.
enum MapChrome {
    static let buttonSize: CGFloat = 44
    static let iconSize: CGFloat = 16
    static let shadowColor = Color.black.opacity(0.15)
    static let shadowRadius: CGFloat = 6
    static let shadowY: CGFloat = 2
}

/// Bottone circolare canonico per azioni overlay su mappa (expand hero,
/// close fullscreen, recenter standalone). Sostituisce le reimplementazioni
/// sparse (36pt ultraThin+black, 32pt circle, SF symbol ad-hoc): stesso
/// ruolo → stesso trattamento.
///
/// `action == nil` → variante decorativa (affordance expand su una preview
/// interamente tappabile): nessun hit-testing.
struct MapCircleButton: View {
    let icon: LucideIcon
    var tint: Color = .primary
    var action: (() -> Void)? = nil

    var body: some View {
        if let action {
            Button(action: action) { label }
                .buttonStyle(.plain)
        } else {
            label.allowsHitTesting(false)
        }
    }

    private var label: some View {
        icon.sized(MapChrome.iconSize)
            .foregroundStyle(tint)
            .frame(width: MapChrome.buttonSize, height: MapChrome.buttonSize)
            .background(.regularMaterial, in: Circle())
            .shadow(color: MapChrome.shadowColor, radius: MapChrome.shadowRadius, y: MapChrome.shadowY)
    }
}
