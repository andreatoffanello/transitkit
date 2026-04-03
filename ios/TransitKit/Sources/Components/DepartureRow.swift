import SwiftUI

// MARK: - DepartureRow

/// A single departure row showing line badge, headsign, countdown, and dock indicator.
/// Used in stop detail views, favorite stop cards, and search results.
///
/// Layout: `[LineBadge] Headsign [DockBadge]  TimeDisplay  HH:MM`
struct DepartureRow: View {
    let departure: Departure
    var isFirst: Bool = false

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(VehicleStore.self) private var vehicleStore

    /// Mirrors the threshold logic in TimeDisplay.init(departure:now:).
    /// ≤ 60 min → .minutes countdown; > 60 min → .absolute clock time.
    private var timeState: DepartureTimeState {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.hour, .minute], from: .now)
        let nowMinutes = (components.hour ?? 0) * 60 + (components.minute ?? 0)
        let diff = departure.minutesFromMidnight - nowMinutes

        if diff < 0 {
            return .passed(departure.time)
        } else if diff == 0 {
            return .departing
        } else if diff <= 60 {
            return .minutes(diff)
        } else {
            return .absolute(departure.time)
        }
    }

    private var isDeparted: Bool {
        if case .passed = timeState { return true }
        return false
    }

    private var isSoon: Bool {
        switch timeState {
        case .departing: true
        case .minutes(let m): m <= 5
        default: false
        }
    }

    var body: some View {
        HStack(spacing: 8) {
            // Line badge
            LineBadge(departure: departure, size: .big)

            // Headsign
            Text(departure.headsign)
                .font(.system(size: isFirst ? 14 : 13, weight: isFirst ? .semibold : .regular))
                .foregroundStyle(isDeparted ? AppTheme.textTertiary : AppTheme.textPrimary)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)

            // Dock indicator (if present)
            if !departure.dock.isEmpty {
                DockBadge(letter: departure.dock)
            }

            // Live indicator
            if vehicleStore.isLive(tripId: departure.tripId) {
                LiveBadge()
            }

            // Countdown
            TimeDisplay(state: timeState)

            // Absolute time (always shown alongside countdown when within 60 min)
            if case .minutes = timeState {
                Text(departure.time)
                    .font(.system(size: isFirst ? 13 : 12, weight: .medium, design: .monospaced))
                    .foregroundStyle(AppTheme.textSecondary)
                    .fixedSize(horizontal: true, vertical: false)
            } else if case .departing = timeState {
                Text(departure.time)
                    .font(.system(size: isFirst ? 13 : 12, weight: .medium, design: .monospaced))
                    .foregroundStyle(AppTheme.textSecondary)
                    .fixedSize(horizontal: true, vertical: false)
            }
        }
        .padding(.vertical, isFirst ? 14 : 10)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(AppTheme.separatorLine)
                .frame(height: 0.5)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(
            "\(departure.lineName), \(departure.headsign), \(countdownAccessibilityLabel)\(departure.dock.isEmpty ? "" : ", dock \(departure.dock)")"
        )
    }

    private var countdownAccessibilityLabel: String {
        switch timeState {
        case .departing: String(localized: "time_departing_a11y")
        case .minutes(let m): String(format: NSLocalizedString("time_minutes_a11y", comment: ""), m)
        case .absolute(let t): String(format: NSLocalizedString("time_at_a11y", comment: ""), t)
        case .passed(let t): String(format: NSLocalizedString("time_passed_a11y", comment: ""), t)
        }
    }
}

// MARK: - DockBadge

/// Small badge for dock/platform indicators (e.g. "B4", "A").
struct DockBadge: View {
    let letter: String

    var body: some View {
        Text(letter)
            .font(.system(size: 10, weight: .heavy, design: .rounded))
            .foregroundStyle(AppTheme.textPrimary)
            .padding(.horizontal, 5)
            .padding(.vertical, 2)
            .background(
                RoundedRectangle(cornerRadius: 4)
                    .fill(AppTheme.bgSecondary)
                    .overlay(
                        RoundedRectangle(cornerRadius: 4)
                            .strokeBorder(AppTheme.glassBorder, lineWidth: 1)
                    )
            )
            .accessibilityLabel(String(format: NSLocalizedString("dock_a11y", comment: ""), letter))
    }
}
