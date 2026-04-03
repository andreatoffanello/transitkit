import SwiftUI

// MARK: - Badge Size

/// Size presets for the line badge.
/// - `big`: primary contexts — departure rows and lines list (13pt, shows transit icon)
/// - `medium`: secondary contexts — stop badge lists, coincidences, headers (11pt, no icon)
enum BadgeSize {
    case big    // departure rows, lines list, trip headers
    case medium // stop badge lists, coincidences, stop headers

    var fontSize: CGFloat {
        switch self {
        case .big:    13
        case .medium: 11
        }
    }

    var iconSize: CGFloat {
        switch self {
        case .big:    16
        case .medium: 12
        }
    }

    var hPadding: CGFloat {
        switch self {
        case .big:    10
        case .medium: 6
        }
    }

    var vPadding: CGFloat {
        switch self {
        case .big:    5
        case .medium: 3
        }
    }

    var spacing: CGFloat {
        switch self {
        case .big:    4
        case .medium: 3
        }
    }

    /// Show transit type icon only on big size; medium relies on color alone.
    var showIcon: Bool { self == .big }
}

// MARK: - LineBadge

/// Pill-shaped badge showing a transit line name with GTFS route color.
/// Handles GTFS color theming and WCAG 4.5:1 text legibility automatically.
///
/// Usage:
/// ```swift
/// LineBadge(departure: dep, size: .big)
/// LineBadge(lineName: "BRT", color: "#c1cd23", textColor: "#000000", transitType: .bus, size: .medium)
/// ```
struct LineBadge: View {
    let lineName: String
    let color: String      // hex background, e.g. "#FF6600"
    let textColor: String  // hex foreground, e.g. "#FFFFFF"
    let transitType: TransitType
    var size: BadgeSize = .medium

    /// Convenience init from a Departure model.
    init(departure: Departure, size: BadgeSize = .big) {
        self.lineName = departure.lineName
        self.color = departure.color
        self.textColor = LineBadge.resolvedTextColor(departure.textColor, background: departure.color)
        self.transitType = departure.transitType
        self.size = size
    }

    /// Convenience init from a Route model.
    init(route: Route, size: BadgeSize = .big) {
        self.lineName = route.name
        self.color = route.color
        self.textColor = LineBadge.resolvedTextColor(route.textColor, background: route.color)
        self.transitType = route.transitType
        self.size = size
    }

    init(
        lineName: String,
        color: String,
        textColor: String,
        transitType: TransitType,
        size: BadgeSize = .medium
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
        let sentinels: Set<String> = ["", "#", "000000", "FFFFFF", "#000000", "#FFFFFF"]
        // For sentinel/generic values, always compute WCAG contrast
        if sentinels.contains(textColor) {
            return contrastingTextColor(for: background)
        }
        // For custom colors, verify WCAG 4.5:1 — if it fails, override with computed contrast
        let candidate = textColor.hasPrefix("#") ? textColor : "#\(textColor)"
        let bg = background.hasPrefix("#") ? background : "#\(background)"
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
