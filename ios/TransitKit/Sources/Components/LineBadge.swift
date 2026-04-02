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

    /// Returns a contrast-safe text color. If the supplied `textColor` is empty or
    /// one of the bare sentinel values (no semantic override), WCAG luminance is used
    /// to pick the highest-contrast option (#FFFFFF or #000000).
    private static func resolvedTextColor(_ textColor: String, background: String) -> String {
        let sentinels: Set<String> = ["", "000000", "FFFFFF", "#000000", "#FFFFFF"]
        guard !sentinels.contains(textColor) else {
            return contrastingTextColor(for: background)
        }
        return textColor
    }

    private var bgColor: Color { Color(hex: color) }
    private var fgColor: Color { Color(hex: textColor) }

    var body: some View {
        HStack(spacing: size.spacing) {
            if size.showIcon {
                transitType.icon.image
                    .font(.system(size: size.iconSize, weight: .semibold))
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
