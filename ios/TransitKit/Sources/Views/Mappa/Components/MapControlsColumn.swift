import SwiftUI

// MARK: - Map Controls Column
//
// Vertical column of circular glass controls on the right edge of the map:
// recenter (optional — driven by `showsRecenter`), reset default view, and
// expand-to-fullscreen. Positioned vertically-centered in the map area by
// the parent's ZStack so it never collides with nav bar or tab bar.
//
// Split from `MappaTab.swift` — behavior-preserving.

struct MapControlsColumn: View {
    let showsRecenter: Bool
    let onRecenter: () -> Void
    let onReset: () -> Void
    let onExpand: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            if showsRecenter {
                Button(action: onRecenter) {
                    LucideIcon.navigation.sized(16)
                        .foregroundStyle(.primary)
                        .frame(width: 44, height: 44)
                        .background(.regularMaterial)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                        .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
                }
                .accessibilityLabel(String(localized: "center_on_location"))
                .accessibilityIdentifier("btn_map_recenter")
            }

            // Reset to default view
            Button(action: onReset) {
                LucideIcon.map.sized(16)
                    .foregroundStyle(.primary)
                    .frame(width: 44, height: 44)
                    .background(.regularMaterial)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                    .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
            }
            .accessibilityLabel(String(localized: "reset_map_view"))
            .accessibilityIdentifier("btn_map_reset")

            // Expand to fullscreen
            Button(action: onExpand) {
                LucideIcon.maximize2.sized(16)
                    .foregroundStyle(.primary)
                    .frame(width: 44, height: 44)
                    .background(.regularMaterial)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
                    .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
            }
            .accessibilityLabel(Text(String(localized: "a11y_expand_map")))
            .accessibilityIdentifier("btn_map_expand")
        }
        .padding(.trailing, 16)
    }
}
