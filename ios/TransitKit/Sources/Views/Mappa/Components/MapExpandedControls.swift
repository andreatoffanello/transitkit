import SwiftUI

// MARK: - Map Expanded Controls
//
// Pill verticale singolo (right edge) con: 3D/2D, recenter, reset bearing.
// Il reset bearing compare solo quando il map ha un heading != 0.
// Close button: cerchio separato al centro-bottom.

struct MapExpandedControls: View {
    let is3D: Bool
    let onToggle3D: () -> Void
    let showsRecenter: Bool
    let onRecenter: () -> Void
    let showsResetBearing: Bool
    let onResetBearing: () -> Void
    let onCollapse: () -> Void

    var body: some View {
        ZStack {
            HStack {
                Spacer()
                pill
                    .padding(.trailing, 16)
            }

            VStack {
                Spacer()
                Button(action: onCollapse) {
                    LucideIcon.x.sized(20)
                        .foregroundStyle(.primary)
                        .frame(width: 56, height: 56)
                        .background(.regularMaterial, in: Circle())
                        .overlay(Circle().strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.5))
                        .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text(String(localized: "a11y_close_map")))
                .accessibilityIdentifier("btn_map_collapse")
                .padding(.bottom, 44)
            }
        }
    }

    private var pill: some View {
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
        }
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.5)
        )
        .shadow(color: .black.opacity(0.18), radius: 6, y: 2)
        .animation(.spring(response: 0.3, dampingFraction: 0.85), value: showsResetBearing)
        .animation(.spring(response: 0.3, dampingFraction: 0.85), value: showsRecenter)
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
