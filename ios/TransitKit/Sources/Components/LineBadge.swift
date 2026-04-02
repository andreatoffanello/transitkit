import SwiftUI

// MARK: - Badge Size

/// Size presets for the line badge, matching transit-scale design.
enum BadgeSize {
    case tiny    // map pins, very compact
    case small   // departure rows, stop cards
    case medium  // line detail headers, filters

    var fontSize: CGFloat {
        switch self {
        case .tiny:   8
        case .small:  11
        case .medium: 13
        }
    }

    var iconSize: CGFloat {
        switch self {
        case .tiny:   8
        case .small:  12
        case .medium: 16
        }
    }

    var hPadding: CGFloat {
        switch self {
        case .tiny:   4
        case .small:  6
        case .medium: 10
        }
    }

    var vPadding: CGFloat {
        switch self {
        case .tiny:   2
        case .small:  3
        case .medium: 5
        }
    }

    var spacing: CGFloat {
        switch self {
        case .tiny:   2
        case .small:  3
        case .medium: 4
        }
    }

    /// Show transit type icon only on medium size; smaller sizes rely on color alone.
    var showIcon: Bool { self == .medium }
}

// MARK: - LineBadge

/// Pill-shaped badge showing a transit line name with GTFS route color.
/// Takes colors directly from the `Departure` model or from explicit hex strings.
///
/// Usage:
/// ```swift
/// LineBadge(lineName: "BRT", color: "#c1cd23", textColor: "#000000", transitType: .bus)
/// LineBadge(departure: dep, size: .small)
/// ```
struct LineBadge: View {
    let lineName: String
    let color: String      // hex background, e.g. "#FF6600"
    let textColor: String  // hex foreground, e.g. "#FFFFFF"
    let transitType: TransitType
    var size: BadgeSize = .small

    /// Convenience init from a Departure model.
    init(departure: Departure, size: BadgeSize = .small) {
        self.lineName = departure.lineName
        self.color = departure.color
        self.textColor = LineBadge.resolvedTextColor(departure.textColor, background: departure.color)
        self.transitType = departure.transitType
        self.size = size
    }

    init(
        lineName: String,
        color: String,
        textColor: String,
        transitType: TransitType,
        size: BadgeSize = .small
    ) {
        self.lineName = lineName
        self.color = color
        self.textColor = LineBadge.resolvedTextColor(textColor, background: color)
        self.transitType = transitType
        self.size = size
    }

    /// Returns a contrast-safe text color. Sentinel/generic values are always resolved
    /// via WCAG luminance. Custom colors are kept only when they pass the WCAG 4.5:1
    /// contrast ratio against the badge background; otherwise the computed contrast is used.
    private static func resolvedTextColor(_ textColor: String, background: String) -> String {
        let sentinels: Set<String> = ["", "000000", "FFFFFF", "#000000", "#FFFFFF"]
        // For sentinel/generic values, always compute WCAG contrast
        if sentinels.contains(textColor) {
            return contrastingTextColor(for: background)
        }
        // For custom colors, verify WCAG 4.5:1 — if it fails, override with computed contrast
        let candidate = textColor.hasPrefix("#") ? textColor : "#\(textColor)"
        let bg = background.hasPrefix("#") ? background : "#\(background)"
        // Quick luminance check inline to avoid importing ColorUtils separately
        func lum(_ hex: String) -> Double {
            func lin(_ c: Double) -> Double { c <= 0.04045 ? c/12.92 : pow((c+0.055)/1.055, 2.4) }
            let h = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
            var v: UInt64 = 0; Scanner(string: h).scanHexInt64(&v)
            guard h.count == 6 else { return 0 }
            return 0.2126*lin(Double((v>>16)&0xFF)/255) + 0.7152*lin(Double((v>>8)&0xFF)/255) + 0.0722*lin(Double(v&0xFF)/255)
        }
        let Lbg = lum(bg); let Lfg = lum(candidate)
        let ratio = Lbg > Lfg ? (Lbg+0.05)/(Lfg+0.05) : (Lfg+0.05)/(Lbg+0.05)
        return ratio >= 4.5 ? candidate : contrastingTextColor(for: background)
    }

    private var bgColor: Color { Color(hex: color) }
    private var fgColor: Color { Color(hex: textColor) }

    var body: some View {
        HStack(spacing: size.spacing) {
            if size.showIcon {
                transitType.icon.sized(size.iconSize)
                    .foregroundStyle(fgColor)
            }
            Text(lineName)
                .font(.system(size: size.fontSize, weight: .bold))
                .foregroundStyle(fgColor)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
        }
        .padding(.horizontal, size.hPadding)
        .padding(.vertical, size.vPadding)
        .background(bgColor, in: RoundedRectangle(cornerRadius: 4))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(String(localized: "line_badge_a11y \(lineName)"))
    }
}
