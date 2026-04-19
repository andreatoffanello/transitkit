import SwiftUI
import UIKit

/// SwiftUI wrapper around MarqueeLabel (UILabel subclass).
/// Handles all edge cases for scrolling text reliably via Core Animation.
struct MarqueeText: View {
    let text: String
    var fontSize: CGFloat = 11
    var fontWeight: UIFont.Weight = .regular
    var foregroundStyle: Color = .secondary
    var speed: Double = 28 // pts per second

    var body: some View {
        _MarqueeTextRepresentable(text: text, fontSize: fontSize, fontWeight: fontWeight, foregroundStyle: foregroundStyle, speed: speed)
            .frame(height: fontSize > 11 ? 16 : 14)
    }
}

private struct _MarqueeTextRepresentable: UIViewRepresentable {
    let text: String
    let fontSize: CGFloat
    let fontWeight: UIFont.Weight
    let foregroundStyle: Color
    let speed: Double

    func makeUIView(context: Context) -> MarqueeLabel {
        let label = MarqueeLabel(frame: .zero, rate: CGFloat(speed), fadeLength: 8)
        label.type = .continuous
        label.animationDelay = 0
        label.font = UIFont.systemFont(ofSize: fontSize, weight: fontWeight)
        label.textColor = UIColor(foregroundStyle)
        label.text = text
        label.setContentHuggingPriority(.defaultLow, for: .horizontal)
        label.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        return label
    }

    func updateUIView(_ label: MarqueeLabel, context: Context) {
        label.font = UIFont.systemFont(ofSize: fontSize, weight: fontWeight)
        label.textColor = UIColor(foregroundStyle)
        if label.text != text {
            label.text = text
        }
    }
}
