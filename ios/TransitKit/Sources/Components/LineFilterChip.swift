import SwiftUI

/// Filter chip that always shows a solid GTFS-color background for readability.
/// Dimming when inactive is applied externally by the caller via `.opacity()`.
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
                // Same RoundedRectangle shape as LineBadge for visual coherence —
                // every line code in the app shares one shape language.
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color(hex: routeColor))
                )
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.25, dampingFraction: 0.8), value: isSelected)
    }
}
