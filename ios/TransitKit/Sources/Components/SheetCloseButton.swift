import SwiftUI

/// Close canonico per toolbar di sheet/fullScreenCover — idioma iOS della
/// X in cerchio grigio (32pt visivi, hit-area 44pt). Unifica i tre stili
/// che convivevano: X nuda, `circleX` (doppio cerchio), cerchio senza
/// hit-area minima.
struct SheetCloseButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            LucideIcon.x.sized(18)
                .foregroundStyle(AppTheme.textSecondary)
                .frame(width: 32, height: 32)
                .background(Circle().fill(AppTheme.bgSecondary))
                .frame(width: 44, height: 44)
                .contentShape(Circle())
        }
        .accessibilityLabel(String(localized: "action_close"))
    }
}
