import SwiftUI

/// Formats transit clock times respecting the device's 12-/24-hour setting.
///
/// US riders default to 12-hour ("4:45 PM"); a rider who turned on
/// Settings → General → Date & Time → "24-Hour Time" sees "16:45". We never
/// hardcode the hour cycle: the `"j"` skeleton resolves the locale-preferred
/// hour, so the same code is correct in every region — matching the web
/// `formatClockTime` behavior.
///
/// GTFS schedule strings are wall-clock in the *operator's* timezone, so every
/// entry point takes the operator `TimeZone` and formats against it (never the
/// device tz) — countdowns and clocks stay consistent wherever the rider is.
@MainActor
enum ClockTime {
    /// Cached formatters keyed by "tzIdentifier|template" — `setLocalizedDate…`
    /// is non-trivial and a schedule board reformats dozens of rows on scroll.
    private static var cache: [String: DateFormatter] = [:]

    private static func formatter(template: String, timeZone: TimeZone) -> DateFormatter {
        let key = "\(timeZone.identifier)|\(template)"
        if let cached = cache[key] { return cached }
        let f = DateFormatter()
        f.locale = .autoupdatingCurrent
        f.timeZone = timeZone
        f.setLocalizedDateFormatFromTemplate(template)
        cache[key] = f
        return f
    }

    /// A real instant → "4:45 PM" / "16:45" in the operator timezone.
    static func clock(_ date: Date, timeZone: TimeZone) -> String {
        formatter(template: "jmm", timeZone: timeZone).string(from: date)
    }

    /// GTFS "HH:mm" / "HH:mm:ss" wall-clock → localized clock. Hours ≥ 24 wrap
    /// (GTFS "25:10" = next-day "1:10 AM"). Returns the input unchanged if it
    /// can't be parsed.
    static func clock(gtfs hhmm: String, timeZone: TimeZone) -> String {
        guard let date = gtfsDate(hhmm, timeZone: timeZone) else { return hhmm }
        return formatter(template: "jmm", timeZone: timeZone).string(from: date)
    }

    /// Hour-only header for the schedule rail: the GTFS 2-digit hour ("16") →
    /// "4 PM" (12h) or "16" (24h).
    static func hourHeader(gtfsHour twoDigit: String, timeZone: TimeZone) -> String {
        guard let h = Int(twoDigit), let date = gtfsDate("\(h):00", timeZone: timeZone) else {
            return twoDigit
        }
        return formatter(template: "j", timeZone: timeZone).string(from: date)
    }

    // MARK: - Styled rendering

    /// Renders a formatted clock string ("4:45 PM", or the hour rail "4 PM")
    /// with the AM/PM meridiem set smaller and more muted than the numerals —
    /// the Apple Clock / Citymapper treatment. On 24-hour locales (no meridiem)
    /// it renders the plain string. Returns a single concatenated `Text` so it
    /// lays out and truncates as one run.
    static func styledText(
        _ formatted: String,
        size: CGFloat,
        weight: Font.Weight = .regular,
        design: Font.Design = .monospaced,
        color: Color
    ) -> Text {
        let numeralFont = Font.system(size: size, weight: weight, design: design)
        guard let (numerals, meridiem) = splitMeridiem(formatted) else {
            return Text(formatted).font(numeralFont).foregroundStyle(color)
        }
        let meridiemFont = Font.system(size: size * 0.7, weight: weight, design: design)
        return Text(numerals).font(numeralFont).foregroundStyle(color)
            + Text("\u{2009}\(meridiem)").font(meridiemFont).foregroundStyle(color.opacity(0.55))
    }

    private static let amPmSymbols: [String] = {
        let f = DateFormatter()
        f.locale = .autoupdatingCurrent
        return [f.amSymbol, f.pmSymbol].compactMap { $0 }.filter { !$0.isEmpty }
    }()

    /// Splits a formatted time into (numerals, meridiem) when it ends with the
    /// locale's AM/PM symbol; nil on 24h strings or unrecognized formats.
    private static func splitMeridiem(_ s: String) -> (numerals: String, meridiem: String)? {
        for sym in amPmSymbols where s.hasSuffix(sym) {
            let numerals = String(s.dropLast(sym.count)).trimmingCharacters(in: .whitespaces)
            if !numerals.isEmpty { return (numerals, sym) }
        }
        return nil
    }

    private static func gtfsDate(_ hhmm: String, timeZone: TimeZone) -> Date? {
        let parts = hhmm.split(separator: ":")
        guard let rawHour = parts.first.flatMap({ Int($0) }),
              parts.count >= 2, let minute = Int(parts[1]) else { return nil }
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        var comps = DateComponents()
        comps.year = 2001; comps.month = 1; comps.day = 1
        comps.hour = rawHour % 24
        comps.minute = minute
        return cal.date(from: comps)
    }
}
