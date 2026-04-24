import SwiftUI

// MARK: - Map Active Route Chip
//
// Top-left chip shown on the map when a route overlay is active. Tap clears
// the overlay via `onDismiss`. Live vehicle count comes from the parent
// (VehicleStore lookup) so this view stays free of environment dependencies.
//
// Split from `MappaTab.swift` — behavior-preserving.

struct MapActiveRouteChip: View {
    let route: APIRoute
    let liveCount: Int
    let onDismiss: () -> Void

    var body: some View {
        Button(action: onDismiss) {
            HStack(spacing: 8) {
                LineBadge(route: route, size: .medium)
                if liveCount > 0 {
                    HStack(spacing: 3) {
                        Circle().fill(AppTheme.realtimeGreen).frame(width: 6, height: 6)
                        Text("\(liveCount)")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(.secondary)
                    }
                }
                Image(systemName: "xmark")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(.secondary)
                    .frame(width: 18, height: 18)
                    .background(Color.primary.opacity(0.08), in: Circle())
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(.regularMaterial, in: Capsule())
            .overlay(Capsule().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
            .shadow(color: .black.opacity(0.18), radius: 6, y: 3)
            // Hit-slop: expand tappable surface around the chip without
            // enlarging the visual pill. Guarantees a >=44x44pt target.
            .padding(8)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("btn_map_clear_route")
        .accessibilityLabel(Text(String(localized: "a11y_remove_selected_line")))
    }
}
