import Foundation

/// Normalizes GTFS trip_headsign strings for display.
/// Strips common meaningless prefixes and applies operator-specific mappings.
enum HeadsignNormalizer {

    /// Generic direction words that convey no useful destination information.
    private static let genericDirections: Set<String> = [
        "inbound", "outbound", "northbound", "southbound", "eastbound", "westbound",
        "north", "south", "east", "west", "inward", "outward", "loop", "circular"
    ]

    /// Resolves a headsign for display. When the raw headsign is a generic direction
    /// word ("Inbound", "Outbound", etc.), falls back to the route's long name or
    /// first non-generic direction headsign.
    static func resolve(_ raw: String, route: APIRoute?) -> String {
        guard let route else { return normalize(raw) }
        let lower = raw.lowercased().trimmingCharacters(in: .whitespaces)
        guard genericDirections.contains(lower) else { return normalize(raw) }

        // Prefer route longName if it's not also generic
        if let longName = route.longName, !longName.isEmpty,
           !genericDirections.contains(longName.lowercased()) {
            return normalize(longName)
        }

        // Fall back to first non-generic direction headsign
        for dir in route.directions {
            if let h = dir.headsign, !h.isEmpty,
               !genericDirections.contains(h.lowercased()) {
                return normalize(h)
            }
        }

        // Nothing better — keep the raw value normalized
        return normalize(raw)
    }

    /// Normalize a raw headsign string.
    /// - Parameters:
    ///   - raw: The raw GTFS trip_headsign value
    ///   - map: Optional operator-provided mapping (e.g. "Inbound" → "City Center")
    /// - Returns: A cleaned, human-readable destination string
    static func normalize(_ raw: String, map: [String: String]? = nil) -> String {
        guard !raw.isEmpty else { return raw }

        // 1. Exact match in operator map (case-insensitive)
        if let map {
            let lower = raw.lowercased()
            for (key, value) in map {
                if key.lowercased() == lower { return value }
            }
        }

        // 2. Strip common meaningless prefixes
        var result = raw
        let prefixes = ["Towards ", "Toward ", "To ", "Direction ", "Dir. ", "Via "]
        for prefix in prefixes {
            if result.lowercased().hasPrefix(prefix.lowercased()) {
                result = String(result.dropFirst(prefix.count)).trimmingCharacters(in: .whitespaces)
                break
            }
        }

        // 3. Strip numeric stop-code prefixes produced by some GTFS feeds.
        //    e.g. "101 - Central Station" → "Central Station"
        //         "Stop 42 - Main St"     → "Main St"
        //    Pattern: optional "Stop " + digits + " - " (or " – ")
        let numericPrefixPattern = #"^(?:Stop\s+)?\d+\s*[-–]\s*"#
        if let range = result.range(of: numericPrefixPattern, options: .regularExpression) {
            // Only strip if what remains is non-empty
            let remainder = String(result[range.upperBound...]).trimmingCharacters(in: .whitespaces)
            if !remainder.isEmpty {
                result = remainder
            }
        }

        // 4. Title-case if all-caps (e.g. "INBOUND" → "Inbound")
        if result == result.uppercased() && result.count > 1 {
            result = result.capitalized
        }

        return result
    }
}
