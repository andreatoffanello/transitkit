import SwiftUI

// MARK: - GlassCard

/// Glass morphism card container, adaptive to light/dark mode.
/// On iOS 26+ uses the native `.glassEffect`; on older versions falls back to
/// thin material + semi-transparent fill + subtle border.
///
/// Usage:
/// ```swift
/// GlassCard {
///     Text("Hello")
/// }
///
/// GlassCard(cornerRadius: 16, withShadow: true) {
///     VStack { ... }
/// }
/// ```
struct GlassCard<Content: View>: View {
    let cornerRadius: CGFloat
    let withShadow: Bool
    @ViewBuilder let content: () -> Content

    init(
        cornerRadius: CGFloat = 12,
        withShadow: Bool = false,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.cornerRadius = cornerRadius
        self.withShadow = withShadow
        self.content = content
    }

    var body: some View {
        content()
            .adaptiveGlass(
                in: RoundedRectangle(cornerRadius: cornerRadius),
                withShadow: withShadow
            )
    }
}

// MARK: - Adaptive Glass Modifier

extension View {
    /// Applies glass morphism background with adaptive light/dark mode support.
    /// On iOS 26+ uses native `glassEffect`; older versions use material + fill + border.
    @ViewBuilder
    func adaptiveGlass(
        in shape: some Shape = RoundedRectangle(cornerRadius: 12),
        withShadow: Bool = false
    ) -> some View {
        let base = Group {
            if #available(iOS 26, *) {
                self.glassEffect(.regular, in: shape)
            } else {
                self
                    .background(AppTheme.glassFill, in: shape)
                    .background(.thinMaterial, in: shape)
                    .overlay(shape.stroke(AppTheme.glassBorder, lineWidth: 1))
            }
        }
        if withShadow {
            base.shadow(color: .black.opacity(0.04), radius: 8, y: 4)
        } else {
            base
        }
    }
}
