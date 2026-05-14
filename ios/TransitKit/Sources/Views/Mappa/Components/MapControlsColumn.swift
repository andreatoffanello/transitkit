import SwiftUI

// MARK: - Map Controls Column
//
// Pill verticale singolo (right edge, mappa compatta) con: 3D/2D, recenter,
// reset bearing, expand. Il reset bearing compare solo quando il map ha
// un heading != 0.

struct MapControlsColumn: View {
    let is3D: Bool
    let onToggle3D: () -> Void
    let showsRecenter: Bool
    let onRecenter: () -> Void
    let showsResetBearing: Bool
    let onResetBearing: () -> Void
    let onExpand: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            textCell(label: is3D ? "2D" : "3D", action: onToggle3D)
                .accessibilityLabel(String(localized: is3D ? "map_switch_to_2d" : "map_switch_to_3d"))
                .accessibilityIdentifier("btn_map_toggle_3d")

            if showsRecenter {
                divider
                iconCell(icon: .navigation, action: onRecenter)
                    .accessibilityLabel(String(localized: "center_on_location"))
                    .accessibilityIdentifier("btn_map_recenter")
            }

            if showsResetBearing {
                divider
                iconCell(icon: .compass, action: onResetBearing)
                    .accessibilityLabel(String(localized: "reset_map_view"))
                    .accessibilityIdentifier("btn_map_reset_bearing")
            }

            divider
            iconCell(icon: .maximize2, action: onExpand)
                .accessibilityLabel(Text(String(localized: "a11y_expand_map")))
                .accessibilityIdentifier("btn_map_expand")
        }
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.5)
        )
        .shadow(color: .black.opacity(0.18), radius: 6, y: 2)
        .animation(.spring(response: 0.3, dampingFraction: 0.85), value: showsResetBearing)
        .animation(.spring(response: 0.3, dampingFraction: 0.85), value: showsRecenter)
        .padding(.trailing, 16)
    }

    private var divider: some View {
        Rectangle()
            .fill(Color.primary.opacity(0.10))
            .frame(width: 28, height: 0.5)
    }

    private func iconCell(icon: LucideIcon, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            icon.sized(16)
                .foregroundStyle(.primary)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func textCell(label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(.primary)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
