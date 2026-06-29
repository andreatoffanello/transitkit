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
    /// Quando true, mostra il badge "LIVE" (pill capsule) a sinistra del
    /// countdown. True ≡ feed RT ha un delay plausibile per questo trip
    /// (VehicleStore.reliableDelayMinutes != nil). Parità Movete.
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
                    Text("m")
                        .font(.system(.subheadline, weight: .bold))
                        .foregroundStyle(mins <= 5 ? AppTheme.realtimeGreen : AppTheme.textSecondary)
                }

            case .hoursMinutes(let h, let m):
                HStack(alignment: .lastTextBaseline, spacing: 2) {
                    Text("\(h)h")
                        .font(.system(.subheadline, weight: .bold).monospacedDigit())
                        .foregroundStyle(AppTheme.textPrimary)
                    if m > 0 {
                        Text("\(m)m")
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
                    ClockTime.styledText(time, size: 17, weight: .semibold, design: .default,
                                         color: AppTheme.textPrimary)
                        .monospacedDigit()
                        .contentTransition(.numericText())
                }

            case .passed(let time):
                ClockTime.styledText(time, size: 13, weight: .medium,
                                     color: AppTheme.textTertiary)
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

