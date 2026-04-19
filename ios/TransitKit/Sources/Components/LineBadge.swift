import SwiftUI

// MARK: - LineBadge
//
// Single source of truth for line badges across the app. Every place that
// renders a GTFS line identifier — departure rows, line lists, stop
// coincidences, headers, filter chips, vehicle cards, trip details —
// goes through this component.
//
// Map annotations (VehicleAnnotationView, StopAnnotationView) are NOT in
// scope: those use custom shapes (dots, pins with halos) that aren't
// badges. Keep them as-is.
//
// Design is mono-operator. The "operator logo circle" variant from
// movete is intentionally omitted — every TransitKit install serves a
// single agency, so it would always be redundant. The API keeps
// `showTransitIcon` so multi-modal operators (bus + rail + ferry) can
// opt in to an icon chip.

/// Size preset — drives font, padding, min width, corner radius and icon size.
enum LineBadgeSize {
    case small   // coincidences, filter chips, dense lists
    case medium  // cards, sheet headers, secondary rows
    case large   // departure rows, line lists, trip headers

    var fontSize: CGFloat {
        switch self {
        case .small:  11
        case .medium: 13
        case .large:  15
        }
    }

    var iconSize: CGFloat {
        switch self {
        case .small:  12
        case .medium: 14
        case .large:  16
        }
    }

    var hPadding: CGFloat {
        switch self {
        case .small:  6
        case .medium: 8
        case .large:  10
        }
    }

    var vPadding: CGFloat {
        switch self {
        case .small:  3
        case .medium: 4
        case .large:  5
        }
    }

    var minWidth: CGFloat {
        switch self {
        case .small:  24
        case .medium: 32
        case .large:  40
        }
    }

    var cornerRadius: CGFloat {
        switch self {
        case .small:  4
        case .medium: 6
        case .large:  6
        }
    }

    var spacing: CGFloat {
        switch self {
        case .small:  3
        case .medium: 5
        case .large:  6
        }
    }
}

/// Pill-shaped badge showing a transit line name with GTFS route color.
///
/// ```swift
/// // Departure / route convenience — pulls color + text + transitType
/// LineBadge(departure: dep, size: .large)
/// LineBadge(route: apiRoute, size: .medium)
///
/// // Low-level — explicit color strings (no Route/Departure handy)
/// LineBadge(name: "R", color: "#AD2B3C", textColor: nil, transitType: .bus)
///
/// // Opt-in transit icon (bus/tram/rail/ferry)
/// LineBadge(route: apiRoute, size: .large, showTransitIcon: true)
/// ```
struct LineBadge: View {
    let name: String
    /// GTFS background color as hex (e.g. "#AD2B3C").
    let color: String
    /// GTFS text color as hex, or nil to auto-derive by WCAG luminance.
    let textColor: String?
    /// Optional transit type for the icon chip. Ignored when `showTransitIcon=false`.
    let transitType: TransitType?
    var size: LineBadgeSize = .medium
    var showTransitIcon: Bool = false

    // MARK: - Inits

    /// Convenience from a `Departure`. Defaults to `.large` (primary context).
    init(departure: Departure, size: LineBadgeSize = .large, showTransitIcon: Bool = false) {
        self.name = departure.lineName
        self.color = departure.color
        self.textColor = departure.textColor
        self.transitType = departure.transitType
        self.size = size
        self.showTransitIcon = showTransitIcon
    }

    /// Convenience from an `APIRoute`. Defaults to `.large`.
    init(route: APIRoute, size: LineBadgeSize = .large, showTransitIcon: Bool = false) {
        self.name = route.name
        self.color = route.color ?? "#000000"
        self.textColor = route.textColor
        self.transitType = route.resolvedTransitType
        self.size = size
        self.showTransitIcon = showTransitIcon
    }

    /// Low-level init for contexts without a Route/Departure model.
    /// Pass `textColor: nil` to auto-derive by WCAG luminance.
    init(
        name: String,
        color: String,
        textColor: String? = nil,
        transitType: TransitType? = nil,
        size: LineBadgeSize = .medium,
        showTransitIcon: Bool = false
    ) {
        self.name = name
        self.color = color
        self.textColor = textColor
        self.transitType = transitType
        self.size = size
        self.showTransitIcon = showTransitIcon
    }

    // MARK: - Body

    var body: some View {
        HStack(spacing: size.spacing) {
            if showTransitIcon, let type = transitType {
                type.icon.sized(size.iconSize)
                    .foregroundStyle(fgColor)
            }
            Text(name)
                .font(.system(size: size.fontSize, weight: .bold))
                .foregroundStyle(fgColor)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
        }
        .padding(.horizontal, size.hPadding)
        .padding(.vertical, size.vPadding)
        .frame(minWidth: size.minWidth)
        .background(bgColor, in: RoundedRectangle(cornerRadius: size.cornerRadius, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(String(format: NSLocalizedString("line_badge_a11y", comment: ""), name))
    }

    // MARK: - Color resolution

    private var bgColor: Color { Color(hex: color) }
    private var fgColor: Color { Color(hex: Self.resolvedTextColor(textColor, background: color)) }

    /// Text color rules:
    /// - `nil`, empty, or WCAG-sentinel values ("000000"/"FFFFFF") → always
    ///   compute by luminance (GTFS feeds often fill these as placeholders).
    /// - Any other provided value is kept only if it passes WCAG 4.5:1 against
    ///   the background. Otherwise fall back to the computed contrast color.
    private static func resolvedTextColor(_ textColor: String?, background: String) -> String {
        let sentinels: Set<String> = ["", "#", "000000", "FFFFFF", "#000000", "#FFFFFF"]
        guard let textColor, !sentinels.contains(textColor) else {
            return contrastingTextColor(for: background)
        }
        let candidate = textColor.hasPrefix("#") ? textColor : "#\(textColor)"
        let bg = background.hasPrefix("#") ? background : "#\(background)"
        func lum(_ hex: String) -> Double {
            func lin(_ c: Double) -> Double { c <= 0.04045 ? c/12.92 : pow((c+0.055)/1.055, 2.4) }
            let h = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
            var v: UInt64 = 0; Scanner(string: h).scanHexInt64(&v)
            guard h.count == 6 else { return 0 }
            return 0.2126*lin(Double((v>>16)&0xFF)/255)
                 + 0.7152*lin(Double((v>>8)&0xFF)/255)
                 + 0.0722*lin(Double(v&0xFF)/255)
        }
        let Lbg = lum(bg); let Lfg = lum(candidate)
        let ratio = Lbg > Lfg ? (Lbg+0.05)/(Lfg+0.05) : (Lfg+0.05)/(Lbg+0.05)
        return ratio >= 4.5 ? candidate : contrastingTextColor(for: background)
    }
}

