import SwiftUI

/// Vertical list of direction pills for LineDetailView.
/// Active pill: bg lineColor.opacity(0.15), border lineColor.opacity(0.5).
/// Replaces the old segmented control to be more readable for long headsigns
/// and to tint with the line's color, matching the Movete pattern.
struct DirectionPills: View {
    let directions: [APIRouteDirection]
    let lineColor: Color
    @Binding var selectedDirectionId: Int

    var body: some View {
        VStack(spacing: 8) {
            ForEach(directions) { direction in
                pill(for: direction)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .sensoryFeedback(.selection, trigger: selectedDirectionId)
    }

    @ViewBuilder
    private func pill(for direction: APIRouteDirection) -> some View {
        let isActive = direction.directionId == selectedDirectionId
        Button {
            withAnimation(.spring(response: 0.32, dampingFraction: 0.82)) {
                selectedDirectionId = direction.directionId
            }
        } label: {
            HStack(spacing: 10) {
                LucideIcon.repeat2.sized(14)
                    .foregroundStyle(isActive ? lineColor : AppTheme.textTertiary)
                Text(direction.headsign ?? "")
                    .font(.system(size: 14, weight: isActive ? .semibold : .regular))
                    .foregroundStyle(isActive ? AppTheme.textPrimary : AppTheme.textSecondary)
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(isActive ? lineColor.opacity(0.12) : AppTheme.bgSecondary.opacity(0.6))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(
                        isActive ? lineColor.opacity(0.5) : AppTheme.glassBorder,
                        lineWidth: isActive ? 1.5 : 1
                    )
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("direction_pill_\(direction.directionId)")
        .accessibilityAddTraits(isActive ? [.isSelected] : [])
    }
}
