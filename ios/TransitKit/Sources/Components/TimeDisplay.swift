import SwiftUI

// MARK: - Departure Time State

/// The visual state of a departure time relative to now.
enum DepartureTimeState {
    /// Absolute time display (e.g. "17:04") — more than 60 minutes away.
    case absolute(String)
    /// Minutes countdown (e.g. "3'") — within 60 minutes.
    case minutes(Int)
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
                    Text("\(mins)")
                        .font(.system(.title3, weight: .bold).monospacedDigit())
                        .foregroundStyle(mins <= 5 ? AppTheme.realtimeGreen : AppTheme.textPrimary)
                        .contentTransition(.numericText())
                    Text("'")
                        .font(.system(.subheadline, weight: .bold))
                        .foregroundStyle(mins <= 5 ? AppTheme.realtimeGreen : AppTheme.textSecondary)
                }

            case .absolute(let time):
                Text(time)
                    .font(.system(.headline, weight: .semibold).monospacedDigit())
                    .foregroundStyle(AppTheme.textPrimary)
                    .contentTransition(.numericText())

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
    init(departure: Departure, now: Date = .now) {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.hour, .minute], from: now)
        let nowMinutes = (components.hour ?? 0) * 60 + (components.minute ?? 0)
        let diff = departure.minutesFromMidnight - nowMinutes

        if diff < 0 {
            self.init(state: .passed(departure.time))
        } else if diff == 0 {
            self.init(state: .departing)
        } else if diff <= 60 {
            self.init(state: .minutes(diff))
        } else {
            self.init(state: .absolute(departure.time))
        }
    }
}
