import SwiftUI

/// Lucide icon names mapped to asset catalog names.
/// All icons are stroke-based SVGs (24x24, stroke-width 2, round caps/joins)
/// stored as template images in the asset catalog for tinting via `.foregroundStyle()`.
enum LucideIcon: String, CaseIterable {
    // Transport
    case bus = "lucide-bus-front"
    case train = "lucide-train"
    case ship = "lucide-ship"
    case cableCar = "lucide-cable-car"

    // Navigation / Tabs
    case home = "lucide-layout-dashboard"
    case search = "lucide-search"
    case star = "lucide-star"
    case starOff = "lucide-star-off"
    case settings = "lucide-settings"
    case clock = "lucide-clock"
    case mapPin = "lucide-map-pin"
    case mapPinOff = "lucide-map-pin-off"
    case map = "lucide-map"
    case navigation = "lucide-navigation"
    case crosshair = "lucide-crosshair"
    case globe = "lucide-globe"
    case externalLink = "lucide-external-link"

    // Actions
    case chevronRight = "lucide-chevron-right"
    case chevronDown = "lucide-chevron-down"
    case x = "lucide-x"
    case circleX = "lucide-circle-x"
    case filter = "lucide-filter"
    case share = "lucide-share"
    case refreshCw = "lucide-refresh-cw"
    case copy = "lucide-copy"
    case info = "lucide-info"
    case list = "lucide-list"

    // Status / Indicators
    case wifiOff = "lucide-wifi-off"
    case alertTriangle = "lucide-alert-triangle"
    case check = "lucide-check"
    case radio = "lucide-radio"
    case eye = "lucide-eye"
    case eyeOff = "lucide-eye-off"

    // Info / Settings
    case ticket = "lucide-ticket"
    case bell = "lucide-bell"
    case shield = "lucide-shield"
    case table = "lucide-table"
    case phone = "lucide-phone"
    case mail = "lucide-mail"

    /// SwiftUI Image view with template rendering mode for tinting.
    var image: Image {
        Image(rawValue, bundle: .main)
            .renderingMode(.template)
    }
}

// MARK: - SwiftUI Convenience

extension Image {
    /// Create an Image from a LucideIcon asset.
    init(lucide: LucideIcon) {
        self.init(lucide.rawValue, bundle: .main)
    }
}
