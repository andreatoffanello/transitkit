import SwiftUI

// MARK: - Departure Time State

/// The visual state of a departure time relative to now.
enum DepartureTimeState {
    /// Absolute time display (e.g. "17:04") — beyond the relative threshold.
    case absolute(String)
    /// Minutes countdown (e.g. "3'") — within 60 minutes.
    case minutes(Int)
    /// Hours + minutes (e.g. "1h 23'") — between 60 min and the threshold.
    case hoursMinutes(Int, Int)
    /// Departing right now (0 minutes).
    case departing
    /// Already departed — should be dimmed.
    case passed(String)
}

// MARK: - TimeDisplay

/// Formats and displays departure time relative to now.
///
/// States:
/// - **absolute**: shows "17:04" for departures > 60 min away
/// - **minutes**: shows "3'" countdown for departures within 60 min
/// - **departing**: shows "→" arrow for departures happening now
/// - **passed**: shows dimmed time for departed services
struct TimeDisplay: View {
    let state: DepartureTimeState
    /// Quando true, mostra il pallino live a sinistra del countdown/hh:mm,
    /// dentro lo stesso frame così resta inline col numero senza gap.
    var liveDot: Bool = false

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var pulsing = false

    var body: some View {
        Group {
            switch state {
            case .departing:
                HStack(spacing: 3) {
                    Circle()
                        .fill(AppTheme.realtimeGreen)
                        .frame(width: 6, height: 6)
                        .opacity(pulsing ? 1.0 : 0.3)
                        .animation(
                            reduceMotion
                                ? nil
                                : .easeInOut(duration: 1.5).repeatForever(autoreverses: true),
                            value: pulsing
                        )
                        .onAppear { pulsing = true }
                    Text("\u{2192}")
                        .font(.system(.headline, weight: .bold))
                        .foregroundStyle(AppTheme.realtimeGreen)
                }

            case .minutes(let mins):
                HStack(alignment: .lastTextBaseline, spacing: 1) {
                    if liveDot {
                        LiveBadge()
                            .alignmentGuide(.lastTextBaseline) { d in d[VerticalAlignment.center] }
                            .padding(.trailing, 4)
                    }
                    Text("\(mins)")
                        .font(.system(.title3, weight: .bold).monospacedDigit())
                        .foregroundStyle(mins <= 5 ? AppTheme.realtimeGreen : AppTheme.textPrimary)
                        .contentTransition(.numericText())
                    Text("'")
                        .font(.system(.subheadline, weight: .bold))
                        .foregroundStyle(mins <= 5 ? AppTheme.realtimeGreen : AppTheme.textSecondary)
                }

            case .hoursMinutes(let h, let m):
                HStack(alignment: .lastTextBaseline, spacing: 2) {
                    Text("\(h)h")
                        .font(.system(.subheadline, weight: .bold).monospacedDigit())
                        .foregroundStyle(AppTheme.textPrimary)
                    if m > 0 {
                        Text("\(m)'")
                            .font(.system(.footnote, weight: .semibold).monospacedDigit())
                            .foregroundStyle(AppTheme.textSecondary)
                    }
                }

            case .absolute(let time):
                HStack(alignment: .lastTextBaseline, spacing: 1) {
                    if liveDot {
                        LiveBadge()
                            .alignmentGuide(.lastTextBaseline) { d in d[VerticalAlignment.center] }
                            .padding(.trailing, 4)
                    }
                    Text(time)
                        .font(.system(.headline, weight: .semibold).monospacedDigit())
                        .foregroundStyle(AppTheme.textPrimary)
                        .contentTransition(.numericText())
                }

            case .passed(let time):
                Text(time)
                    .font(.system(size: 13, weight: .medium, design: .monospaced))
                    .foregroundStyle(AppTheme.textTertiary)
            }
        }
        .frame(minWidth: 52, alignment: .trailing)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
    }

    private var accessibilityLabel: String {
        switch state {
        case .departing:
            String(localized: "time_departing_a11y")
        case .minutes(let mins):
            String(format: NSLocalizedString("time_minutes_a11y", comment: ""), mins)
        case .hoursMinutes(let h, let m):
            m > 0
                ? "\(h) ore \(m) minuti"
                : "\(h) ore"
        case .absolute(let time):
            String(format: NSLocalizedString("time_at_a11y", comment: ""), time)
        case .passed(let time):
            String(format: NSLocalizedString("time_passed_a11y", comment: ""), time)
        }
    }
}

// MARK: - Departure Time Computation

extension TimeDisplay {
    /// Creates a `TimeDisplay` from a `Departure` and the current time.
    /// - Parameters:
    ///   - departure: The departure to display.
    ///   - now: Current date (defaults to `.now`).
    ///   - relativeThreshold: Minutes up to which relative format is used.
    ///     Default 60 (show minutes up to 1h, absolute beyond).
    ///     Pass e.g. 180 for home screen cards to show "1h 23'" up to 3h.
    init(departure: Departure, now: Date = .now, relativeThreshold: Int = 60) {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.hour, .minute], from: now)
        let nowMinutes = (components.hour ?? 0) * 60 + (components.minute ?? 0)
        var diff = departure.minutesFromMidnight - nowMinutes

        // Wrap-around: se la partenza è "passata" da più di 1h, assumiamo che
        // sia la prima occorrenza del giorno successivo (lo store restituisce
        // upcoming departures rispetto a now, quindi non dovrebbero esserci
        // partenze veramente passate). Diff < -60 → +24h.
        if diff < -60 {
            diff += 1440
        }

        if diff < 0 {
            self.init(state: .passed(departure.time))
        } else if diff == 0 {
            self.init(state: .departing)
        } else if diff <= 60 {
            self.init(state: .minutes(diff))
        } else if diff <= relativeThreshold {
            self.init(state: .hoursMinutes(diff / 60, diff % 60))
        } else {
            self.init(state: .absolute(departure.time))
        }
    }
}
