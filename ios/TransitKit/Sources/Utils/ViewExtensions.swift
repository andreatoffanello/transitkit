import SwiftUI

private struct AdaptiveNavBarBackground: ViewModifier {
    func body(content: Content) -> some View {
        // Deployment target 26.0: la nav bar resta trasparente sul Liquid Glass.
        content.toolbarBackground(.hidden, for: .navigationBar)
    }
}

extension View {
    /// Transparent nav bar over Liquid Glass.
    func adaptiveNavBarBackground() -> some View {
        modifier(AdaptiveNavBarBackground())
    }
}
