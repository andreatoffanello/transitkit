import SwiftUI

// MARK: - Map Search Pill
//
// Floating search pill shown at the top of the map when no route is active.
// Tap opens the line picker sheet (wired by the parent).
//
// Split from `MappaTab.swift` — behavior-preserving.

struct MapSearchPill: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(.secondary)
                Text(String(localized: "map_search_placeholder"))
                    .font(.system(size: 15))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: 360)
            .background(.regularMaterial, in: Capsule())
            .overlay(Capsule().stroke(Color.primary.opacity(0.08), lineWidth: 0.5))
            .shadow(color: .black.opacity(0.18), radius: 8, y: 3)
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 24)
        .accessibilityIdentifier("btn_map_search_pill")
        .accessibilityLabel(Text(String(localized: "a11y_search_line_or_stop")))
    }
}
