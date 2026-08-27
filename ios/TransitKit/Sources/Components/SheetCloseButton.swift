import SwiftUI

/// Close canonico per toolbar di sheet/fullScreenCover.
///
/// **Il disco grigio esiste solo su iOS 18.** Da iOS 26 SwiftUI avvolge ogni
/// toolbar item in una capsula di Liquid Glass per conto suo: disegnarci dentro
/// un `Circle` pieno dà un cerchio dentro una capsula — due contenitori per una
/// sola icona, il difetto più visibile nelle nav bar dell'app. Su iOS 18 invece
/// il toolbar item non riceve nessun contenitore, quindi il disco è l'unica cosa
/// che stacca la X dal contenuto sotto e va tenuto.
///
/// Regola generale: dentro un elemento di glass l'icona sta nuda. Il vetro È il
/// contenitore.
struct SheetCloseButton: View {
    let action: () -> Void

    var body: some View {
        Button {
            Haptics.navigate()
            action()
        } label: {
            LucideIcon.x.sized(18)
                .foregroundStyle(AppTheme.textPrimary)
                .frame(width: 32, height: 32)
                .background { legacyDisc }
                .frame(width: 44, height: 44)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L("action_close"))
    }

    @ViewBuilder
    private var legacyDisc: some View {
        if #unavailable(iOS 26) {
            Circle().fill(AppTheme.bgSecondary)
        }
    }
}
