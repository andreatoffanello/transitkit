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
                // Trattamento canonico dei dischi su mappa: stesso materiale e
                // stesso hairline del pill qui accanto. L'ombra resta la sua
                // (8/4): è il controllo più grande e sta su tile non sfocate.
                MapCircleButton(
                    icon: .x,
                    size: 56,
                    iconSize: 20,
                    shadowRadius: 8,
                    shadowY: 4,
                    action: onCollapse
                )
                    .accessibilityLabel(Text(L("a11y_close_map")))
                    .accessibilityIdentifier("btn_map_collapse")
                    .padding(.bottom, 44)
            }
        }
    }

    private var pill: some View {
        VStack(spacing: 0) {
            // Label reflects CURRENT state, not the action. Users glance at the
            // pill to know which mode they're in; the action (toggle) is the
            // tap itself.
            textCell(label: is3D ? "3D" : "2D", action: onToggle3D)
                .accessibilityLabel(L(is3D ? "map_switch_to_2d" : "map_switch_to_3d"))
                .accessibilityIdentifier("btn_map_toggle_3d")

            if showsRecenter {
                divider
                iconCell(icon: .navigation, action: onRecenter)
                    .accessibilityLabel(L("center_on_location"))
                    .accessibilityIdentifier("btn_map_recenter")
            }

            if showsResetBearing {
                divider
                iconCell(icon: .compass, action: onResetBearing)
                    .accessibilityLabel(L("reset_map_view"))
                    .accessibilityIdentifier("btn_map_reset_bearing")
            }
        }
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(MapChrome.hairline, lineWidth: MapChrome.hairlineWidth)
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
