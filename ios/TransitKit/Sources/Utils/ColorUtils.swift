import SwiftUI

/// Computes WCAG 2.1 relative luminance for a hex color and returns
/// the text color (#FFFFFF or #000000) that achieves the highest contrast ratio.
func contrastingTextColor(for backgroundHex: String) -> String {
    // linearize sRGB component
    func linearize(_ c: Double) -> Double {
        c <= 0.04045 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4)
    }
    let hex = backgroundHex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
    var int: UInt64 = 0
    Scanner(string: hex).scanHexInt64(&int)
    let r, g, b: Double
    switch hex.count {
    case 6:
        r = Double((int >> 16) & 0xFF) / 255
        g = Double((int >> 8) & 0xFF) / 255
        b = Double(int & 0xFF) / 255
    default: return "#FFFFFF"
    }
    let L = 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b)
    // Contrast with white: (1 + 0.05) / (L + 0.05)
    // Contrast with black: (L + 0.05) / (0 + 0.05)
    let contrastWhite = 1.05 / (L + 0.05)
    let contrastBlack = (L + 0.05) / 0.05
    return contrastWhite >= contrastBlack ? "#FFFFFF" : "#000000"
}
