import SwiftUI

// MARK: - FilterChip

/// Selectable chip for filtering by transit type or category.
/// Active state uses accent color fill; inactive uses glass morphism.
///
/// Usage:
/// ```swift
/// FilterChip(label: "Bus", icon: "bus", isSelected: true) { toggleBus() }
/// FilterChip(label: "All", isSelected: false) { showAll() }
/// ```
struct FilterChip: View {
    let label: String
    let isSelected: Bool
    var icon: LucideIcon? = nil
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                if let icon {
                    icon.image
                        .font(.caption.weight(.semibold))
                }
                Text(label)
                    .font(.footnote.weight(isSelected ? .bold : .semibold))
            }
            .foregroundStyle(isSelected ? .white : AppTheme.textSecondary)
            .padding(.horizontal, 14)
            .padding(.vertical, 7)
            .background {
                if isSelected {
                    Capsule().fill(AppTheme.accent)
                } else {
                    Capsule().fill(AppTheme.glassFill)
                        .overlay(Capsule().strokeBorder(AppTheme.glassBorder, lineWidth: 1))
                }
            }
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.25, dampingFraction: 0.8), value: isSelected)
        .accessibilityLabel(label)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}

// MARK: - TransitTypeFilterChip

/// Convenience chip for filtering by `TransitType`, auto-populating icon and label.
struct TransitTypeFilterChip: View {
    let transitType: TransitType
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        FilterChip(
            label: transitType.displayName,
            isSelected: isSelected,
            icon: transitType.icon,
            action: action
        )
    }
}

// MARK: - ScaleButtonStyle

/// Subtle scale-down press effect, respects Reduce Motion.
struct ScaleButtonStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(reduceMotion ? 1.0 : (configuration.isPressed ? 0.96 : 1.0))
            .opacity(configuration.isPressed ? 0.85 : 1.0)
            .animation(
                reduceMotion ? nil : .spring(response: 0.22, dampingFraction: 0.65),
                value: configuration.isPressed
            )
    }
}
