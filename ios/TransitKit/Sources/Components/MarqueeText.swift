import SwiftUI

private struct TextWidthKey: PreferenceKey {
    nonisolated(unsafe) static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = max(value, nextValue()) }
}

struct MarqueeText: View {
    let text: String
    var font: Font = .system(size: 11)
    var foregroundStyle: Color = .secondary
    var speed: Double = 28 // pts per second

    @State private var textWidth: CGFloat = 0
    @State private var animOffset: CGFloat = 0
    @State private var isAnimating = false

    private var separator: String { "   ·   " }
    private var loopText: String { text + separator + text }
    private var loopWidth: CGFloat { textWidth + separatorWidth }

    // Approximate separator width (used for loop offset)
    private var separatorWidth: CGFloat { CGFloat(separator.count) * (font == .system(size: 11) ? 5.5 : 6.0) }

    var body: some View {
        GeometryReader { geo in
            let containerWidth = geo.size.width
            let needsScroll = textWidth > containerWidth

            ZStack(alignment: .leading) {
                // Hidden text for measurement
                Text(text)
                    .font(font)
                    .fixedSize()
                    .hidden()
                    .background(
                        GeometryReader { textGeo in
                            Color.clear.preference(key: TextWidthKey.self, value: textGeo.size.width)
                        }
                    )

                if needsScroll {
                    Text(loopText)
                        .font(font)
                        .foregroundStyle(foregroundStyle)
                        .fixedSize()
                        .offset(x: -animOffset)
                        .onAppear {
                            guard !isAnimating else { return }
                            isAnimating = true
                            animOffset = 0
                            let duration = loopWidth / speed
                            withAnimation(.linear(duration: duration).repeatForever(autoreverses: false)) {
                                animOffset = loopWidth
                            }
                        }
                        .onDisappear {
                            isAnimating = false
                            animOffset = 0
                        }
                } else {
                    Text(text)
                        .font(font)
                        .foregroundStyle(foregroundStyle)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .clipped()
            .mask(
                needsScroll
                    ? AnyView(LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: .black, location: 0.08),
                            .init(color: .black, location: 0.92),
                            .init(color: .clear, location: 1)
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    ))
                    : AnyView(Color.black)
            )
        }
        .frame(height: font == .system(size: 11) ? 14 : 16)
        .onPreferenceChange(TextWidthKey.self) { textWidth = $0 }
    }
}
