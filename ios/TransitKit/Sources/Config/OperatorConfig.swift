import Foundation
import SwiftUI

// MARK: - Operator Configuration

/// Configuration loaded from the operator's config.json.
/// Controls branding, features, and map defaults.
struct OperatorConfig: Codable {
    let id: String
    let name: String
    let fullName: String
    let url: String
    let region: String
    let country: String
    let timezone: String
    let locale: [String]
    let theme: ThemeConfig
    let store: StoreConfig
    let map: MapConfig
    let features: FeaturesConfig
    let contact: ContactConfig?
    let fares: FareInfo?
    let pointsOfSale: [PointOfSale]?
    let privacyUrl: String?
    let gtfsRt: GtfsRtConfig?
    let headsignMap: [String: String]?

    struct ThemeConfig: Codable {
        let primaryColor: String
        let accentColor: String
        let textOnPrimary: String
        let secondaryColor: String?
    }

    struct StoreConfig: Codable {
        let title: String
        let subtitle: String
        let keywords: String
    }

    struct MapConfig: Codable {
        let centerLat: Double
        let centerLng: Double
        let defaultZoom: Double

        enum CodingKeys: String, CodingKey {
            case centerLat, centerLng, defaultZoom
        }

        init(from decoder: Decoder) throws {
            let c = try decoder.container(keyedBy: CodingKeys.self)
            centerLat = try c.decodeIfPresent(Double.self, forKey: .centerLat) ?? 0.0
            centerLng = try c.decodeIfPresent(Double.self, forKey: .centerLng) ?? 0.0
            defaultZoom = try c.decodeIfPresent(Double.self, forKey: .defaultZoom) ?? 12.0
        }
    }

    struct FeaturesConfig: Codable {
        let enableMap: Bool
        let enableGeolocation: Bool
        let enableFavorites: Bool
        let enableNotifications: Bool

        enum CodingKeys: String, CodingKey {
            case enableMap, enableGeolocation, enableFavorites, enableNotifications
        }

        init(from decoder: Decoder) throws {
            let c = try decoder.container(keyedBy: CodingKeys.self)
            enableMap = try c.decodeIfPresent(Bool.self, forKey: .enableMap) ?? true
            enableGeolocation = try c.decodeIfPresent(Bool.self, forKey: .enableGeolocation) ?? false
            enableFavorites = try c.decodeIfPresent(Bool.self, forKey: .enableFavorites) ?? true
            enableNotifications = try c.decodeIfPresent(Bool.self, forKey: .enableNotifications) ?? false
        }
    }

    struct ContactConfig: Codable {
        let phone: String?
        let email: String?
    }

    struct GtfsRtConfig: Codable {
        let vehiclePositionsUrl: String?
        let tripUpdatesUrl: String?
        let serviceAlertsUrl: String?

        enum CodingKeys: String, CodingKey {
            case vehiclePositionsUrl = "vehicle_positions"
            case tripUpdatesUrl = "trip_updates"
            case serviceAlertsUrl = "service_alerts"
        }
    }
}

// MARK: - Fare Info

/// Fare information loaded from the operator's config.json under the "fares" key.
struct FareInfo: Codable {
    let types: [FareType]
    let purchaseUrl: String?
    let notes: String?
}

/// A single fare type (e.g. single ride, day pass).
struct FareType: Codable, Identifiable {
    var id: String { name }
    let name: String   // "Single ride", "Day pass", etc.
    let price: String  // "$1.50", "Free", etc.
    let notes: String? // "Valid 2 hours", etc.
}

// MARK: - Points of Sale

/// A physical location where tickets can be purchased.
struct PointOfSale: Codable, Identifiable {
    var id: String { name }
    let name: String
    let address: String?
    let hours: String?
}

// MARK: - Config Loader

enum ConfigLoader {
    static func load() throws -> OperatorConfig {
        guard let url = Bundle.main.url(forResource: "config", withExtension: "json") else {
            throw ConfigError.fileNotFound
        }
        let data = try Data(contentsOf: url)
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return try decoder.decode(OperatorConfig.self, from: data)
    }

    enum ConfigError: LocalizedError {
        case fileNotFound

        var errorDescription: String? {
            switch self {
            case .fileNotFound: "Operator config.json not found in bundle"
            }
        }
    }
}

// MARK: - Theme

/// App-wide theme derived from operator config + sensible defaults.
/// Uses the operator's accent color and builds a complete dark/light palette.
@MainActor
enum AppTheme {
    // These are set once at app launch from the operator config
    nonisolated(unsafe) static var accentHex = "#0066CC"
    nonisolated(unsafe) static var primaryHex = "#003366"
    nonisolated(unsafe) static var secondaryHex: String? = nil

    private static func isValidHex(_ hex: String) -> Bool {
        let clean = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        return (clean.count == 6 || clean.count == 8) &&
               clean.allSatisfy({ "0123456789abcdefABCDEF".contains($0) })
    }

    static func configure(from config: OperatorConfig) {
        if isValidHex(config.theme.accentColor) { accentHex = config.theme.accentColor }
        if isValidHex(config.theme.primaryColor) { primaryHex = config.theme.primaryColor }
        secondaryHex = config.theme.secondaryColor.flatMap { isValidHex($0) ? $0 : nil }
    }

    // MARK: Backgrounds
    static let background = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(hex: "#080C18") : UIColor(hex: "#F5F7FA")
    })
    static let bgSecondary = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(white: 1, alpha: 0.05) : .white
    })

    // MARK: Glass morphism
    static let glassFill = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark
            ? UIColor(white: 1, alpha: 0x14 / 255)
            : UIColor(white: 1, alpha: 0xCC / 255)
    })
    static let glassBorder = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark
            ? UIColor(white: 1, alpha: 0.06)
            : UIColor(white: 0, alpha: 0.05)
    })
    static let separatorLine = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark
            ? UIColor(white: 1, alpha: 0.04)
            : UIColor(white: 0, alpha: 0.04)
    })

    // MARK: Accent (dynamic from config)
    static var accent: Color { Color(hex: accentHex) }
    static var primary: Color { Color(hex: primaryHex) }
    static var secondary: Color { secondaryHex.map { Color(hex: $0) } ?? accent.opacity(0.8) }

    // MARK: Text
    static let textPrimary = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(hex: "#F0F0F0") : UIColor(hex: "#111827")
    })
    static let textSecondary = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(white: 1, alpha: 0.5) : UIColor(hex: "#666666")
    })
    static let textTertiary = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(white: 1, alpha: 0.35) : UIColor(hex: "#999999")
    })

    // MARK: Status
    static let realtimeGreen = Color(hex: "#22C55E")
    static let realtimeRed = Color(hex: "#EF4444")

    // MARK: Tab bar
    static let tabBarBg = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(hex: "#131729E6") : UIColor(white: 1, alpha: 0xE6 / 255)
    })
    static let tabInactive = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark ? UIColor(hex: "#555B6E") : UIColor(hex: "#9CA3AF")
    })
}

// MARK: - Hex Color Extensions

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3:  (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6:  (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:  (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default: (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(.sRGB, red: Double(r)/255, green: Double(g)/255, blue: Double(b)/255, opacity: Double(a)/255)
    }
}

extension UIColor {
    convenience init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6:  (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:  (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default: (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(red: CGFloat(r)/255, green: CGFloat(g)/255, blue: CGFloat(b)/255, alpha: CGFloat(a)/255)
    }
}
