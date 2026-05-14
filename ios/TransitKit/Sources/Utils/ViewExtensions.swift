import SwiftUI

private struct AdaptiveNavBarBackground: ViewModifier {
    func body(content: Content) -> some View {
        // Two different toolbarBackground overloads (Visibility vs ShapeStyle) produce
        // different concrete types — AnyView erases both so the compiler can unify them.
        if #available(iOS 26, *) {
            return AnyView(content.toolbarBackground(.hidden, for: .navigationBar))
        }
        return AnyView(content.toolbarBackground(.regularMaterial, for: .navigationBar))
    }
}

extension View {
    /// Transparent on iOS 26+ (liquid glass), regularMaterial glass on iOS 18.
    func adaptiveNavBarBackground() -> some View {
        modifier(AdaptiveNavBarBackground())
    }
}
