import SwiftUI

/// Filter chip that always shows a solid GTFS-color background for readability.
/// Selected: solid fill + white ring border.
/// Unselected: solid fill, no border.
/// Disabled state (opacity) is applied externally by the caller.
struct LineFilterChip: View {
    let lineName: String
    let routeColor: String   // GTFS hex
    let isSelected: Bool
    let action: () -> Void

    private var textColor: Color {
        Color(hex: contrastingTextColor(for: routeColor))
    }

    var body: some View {
        Button(action: action) {
            Text(lineName)
                .font(.footnote.weight(.semibold))
                .foregroundStyle(textColor)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Capsule().fill(Color(hex: routeColor)))
                .overlay {
                    if isSelected {
                        Capsule()
                            .strokeBorder(.white.opacity(0.6), lineWidth: 2)
                    }
                }
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.25, dampingFraction: 0.8), value: isSelected)
    }
}
