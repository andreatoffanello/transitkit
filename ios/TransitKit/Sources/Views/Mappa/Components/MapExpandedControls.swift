import SwiftUI

// MARK: - Map Expanded Controls
//
// Overlay shown while the map is in fullscreen (isExpanded) mode:
// right-edge recenter + reset column, plus a large close button
// bottom-center. Mirrors the compact controls column but without the
// expand button (since we're already expanded).
//
// Split from `MappaTab.swift` — behavior-preserving.

struct MapExpandedControls: View {
    let showsRecenter: Bool
    let onRecenter: () -> Void
    let onReset: () -> Void
    let onCollapse: () -> Void

    var body: some View {
        ZStack {
            // Controls — vertically centered on the right (HStack inside ZStack centers naturally)
            HStack {
                Spacer()
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
                    }

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
                }
                .padding(.trailing, 16)
            }

            // Close button — bottom center
            VStack {
                Spacer()
                Button(action: onCollapse) {
                    LucideIcon.x.sized(20)
                        .foregroundStyle(.primary)
                        .frame(width: 56, height: 56)
                        .background(.regularMaterial)
                        .clipShape(Circle())
                        .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
                }
                .accessibilityLabel(Text(String(localized: "a11y_close_map")))
                .accessibilityIdentifier("btn_map_collapse")
                .padding(.bottom, 44)
            }
        }
    }
}
