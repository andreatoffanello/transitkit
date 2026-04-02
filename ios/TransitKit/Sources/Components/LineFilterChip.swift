import SwiftUI

/// Filter chip that uses the line's GTFS color when selected.
/// Unselected: glass morphism with colored text.
/// Selected: solid GTFS-color background with contrasting text.
struct LineFilterChip: View {
    let lineName: String
    let routeColor: String   // GTFS hex
    let isSelected: Bool
    let action: () -> Void

    private var resolvedTextColor: Color {
        Color(hex: contrastingTextColor(for: routeColor))
    }

    var body: some View {
        Button(action: action) {
            Text(lineName)
                .font(.footnote.weight(isSelected ? .bold : .semibold))
                .foregroundStyle(isSelected ? resolvedTextColor : Color(hex: routeColor))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background {
                    if isSelected {
                        Capsule().fill(Color(hex: routeColor))
                    } else {
                        Capsule().fill(AppTheme.glassFill)
                            .overlay(Capsule().strokeBorder(Color(hex: routeColor).opacity(0.4), lineWidth: 1))
                    }
                }
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.25, dampingFraction: 0.8), value: isSelected)
    }
}
