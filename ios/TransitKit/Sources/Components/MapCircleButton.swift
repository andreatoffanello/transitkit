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

    /// Hairline dei pill/dischi su mappa. Un bordo solo, mai sommato a un altro.
    static let hairline = Color.primary.opacity(0.08)
    static let hairlineWidth: CGFloat = 0.5
}

/// Bottone circolare canonico per azioni overlay su mappa (expand hero,
/// close fullscreen, recenter standalone). Sostituisce le reimplementazioni
/// sparse (36pt ultraThin+black, 32pt circle, SF symbol ad-hoc): stesso
/// ruolo → stesso trattamento.
///
/// Il materiale È il contenitore: un solo hairline (`MapChrome.hairline`, lo
/// stesso dei pill), mai un secondo cerchio dentro per l'icona.
///
/// `action == nil` → variante decorativa (affordance expand su una preview
/// interamente tappabile): nessun hit-testing.
struct MapCircleButton: View {
    let icon: LucideIcon
    var size: CGFloat = MapChrome.buttonSize
    var iconSize: CGFloat = MapChrome.iconSize
    var tint: Color = .primary
    /// L'ombra scala col disco: il close 56pt galleggia su tile di mappa non
    /// sfocate e con l'ombra dei 44pt sembrerebbe incollato.
    var shadowRadius: CGFloat = MapChrome.shadowRadius
    var shadowY: CGFloat = MapChrome.shadowY
    var action: (() -> Void)? = nil

    var body: some View {
        if let action {
            Button {
                Haptics.navigate()
                action()
            } label: { label }
                .buttonStyle(.plain)
        } else {
            label.allowsHitTesting(false)
        }
    }

    private var label: some View {
        icon.sized(iconSize)
            .foregroundStyle(tint)
            .frame(width: size, height: size)
            .background(.regularMaterial, in: Circle())
            .overlay(Circle().strokeBorder(MapChrome.hairline, lineWidth: MapChrome.hairlineWidth))
            .shadow(color: MapChrome.shadowColor, radius: shadowRadius, y: shadowY)
            .contentShape(Circle())
    }
}
