import SwiftUI

// MARK: - Live Badge

/// A small green pulsing dot indicating real-time tracked status.
/// Used in DepartureRow, LineDetailView header, HomeTab stop cards.
///
/// Variants:
///   LiveBadge()                    → dot only
///   LiveBadge(count: 3)            → "● 3 live"
///   LiveBadge(label: "live")       → "● live"
struct LiveBadge: View {
    enum Style {
        case dot                  // just the circle, smallest footprint
        case chip(String)         // "● text" capsule pill
    }

    let style: Style
    @State private var pulsing = false

    init() { self.style = .dot }
    init(count: Int) { self.style = .chip("\(count) live") }
    init(label: String) { self.style = .chip(label) }

    var body: some View {
        switch style {
        case .dot:
            dot
        case .chip(let text):
            HStack(spacing: 4) {
                dot
                Text(text)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(.primary)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(.regularMaterial, in: Capsule())
        }
    }

    private var dot: some View {
        ZStack {
            Circle()
                .fill(Color.green.opacity(0.3))
                .frame(width: 10, height: 10)
                .scaleEffect(pulsing ? 1.6 : 1.0)
                .opacity(pulsing ? 0 : 1)

            Circle()
                .fill(Color.green)
                .frame(width: 6, height: 6)
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 1.2).repeatForever(autoreverses: false)) {
                pulsing = true
            }
        }
    }
}
