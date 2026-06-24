import SwiftUI

// MARK: - DepartureRow

/// A single departure row: line badge | destination + description (marquee) | time.
///
/// Layout:
/// ```
/// [LineBadge]  [Destination marquee     ]  [DockBadge] [Live]  [5'    ]
///              [Description marquee     ]               [10:23  ]
/// ```
///
/// All rows have identical layout — `isFirst` is kept in the interface for call-site
/// compatibility but no longer drives font/padding differences.
struct DepartureRow: View {
    let departure: Departure
    var isFirst: Bool = false
    var hideBadge: Bool = false

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(ScheduleStore.self) private var scheduleStore

    /// Plausibility-filtered RT delay in minutes for this departure's trip.
    /// Nil when untracked or outside the −5…+30 min plausibility window.
    private var rtDelay: Int? {
        guard let tripId = departure.tripId else { return nil }
        return vehicleStore.reliableDelayMinutes(forTripId: tripId)
    }

    /// LIVE = info realtime presente: o un delay trip-update affidabile, O un
    /// veicolo reale tracciato nel positions feed (ciò che la Home conta come
    /// "N live"). Prima era gated solo sul delay → le partenze con un mezzo reale
    /// ma senza trip-update non mostravano mai il LIVE. Parità con Android
    /// (StopDetailDepartureRow: `isRealtime || delay != null`).
    private var isLive: Bool {
        if rtDelay != nil { return true }
        guard let tripId = departure.tripId else { return false }
        return vehicleStore.isLive(tripId: tripId)
    }

    private var timeState: DepartureTimeState {
        scheduleStore.timeState(for: departure, delayMinutes: rtDelay)
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
        HStack(spacing: 10) {
            // Line badge — hidden when a line filter is active (all rows are same line)
            if !hideBadge {
                LineBadge(departure: departure, size: .large)
            }

            // Destination + description stack
            destinationStack
                .frame(maxWidth: .infinity, alignment: .leading)
                .layoutPriority(1)

            // Dock (compact, right of text)
            if !departure.dock.isEmpty {
                DockBadge(letter: departure.dock)
            }

            // Time stack: live dot inline col countdown + assoluto sotto
            timeStack
                .fixedSize(horizontal: true, vertical: false)
        }
        .padding(.vertical, 12)
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

    // MARK: - Destination stack

    @ViewBuilder
    private var destinationStack: some View {
        let sequence: String? = {
            guard let s = scheduleStore.routeStopSequences[departure.routeId], !s.isEmpty else { return nil }
            return s
        }()

        VStack(alignment: .leading, spacing: 2) {
            // Primary: headsign / destination
            Text(departure.headsign)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(isDeparted ? AppTheme.textTertiary : AppTheme.textPrimary)
                .lineLimit(1)
                .truncationMode(.tail)

            // Secondary: stop sequence / route description
            if let seq = sequence, seq != departure.headsign {
                Text(seq)
                    .font(.system(size: 11))
                    .foregroundStyle(AppTheme.textTertiary)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
        }
    }

    // MARK: - Time stack

    @ViewBuilder
    private var timeStack: some View {
        VStack(alignment: .trailing, spacing: 1) {
            // LIVE pill = delay trip-update plausibile (rtDelay != nil dopo clamp
            // −5…+30 min) OPPURE veicolo reale presente nel positions feed. Vedi
            // `isLive` — il countdown/clock sotto resta keyed sul solo rtDelay.
            TimeDisplay(state: timeState, liveDot: isLive)

            // Absolute clock beneath the countdown. Shifted forward by the
            // plausibility-filtered RT delay when present; byte-for-byte the
            // scheduled time otherwise — zero regression for non-live rows.
            switch timeState {
            case .minutes, .departing:
                Text(departure.shiftedTime(byMinutes: rtDelay ?? 0))
                    .font(.system(size: 11, weight: .medium, design: .monospaced))
                    .foregroundStyle(isSoon ? AppTheme.realtimeGreen.opacity(0.8) : AppTheme.textSecondary)
            default:
                EmptyView()
            }
        }
    }

    // MARK: - Accessibility

    private var countdownAccessibilityLabel: String {
        switch timeState {
        case .departing: String(localized: "time_departing_a11y")
        case .minutes(let m): String(format: NSLocalizedString("time_minutes_a11y", comment: ""), m)
        case .hoursMinutes(let h, let m): m > 0 ? "\(h) ore \(m) minuti" : "\(h) ore"
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

