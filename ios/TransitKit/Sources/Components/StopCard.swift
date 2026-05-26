import SwiftUI

// MARK: - StopCard

/// Compact card showing stop name, transit type icons, line badges, and next departure.
/// Used in stop lists, nearby views, and search results.
/// Glass morphism style with adaptive light/dark mode.
///
/// The next-departure time is passed pre-computed (via `ScheduleStore.timeState(for:)`)
/// so the component stays pure and the operator-timezone calculation lives in one
/// place across the app.
struct StopCard: View {
    let name: String
    let transitTypes: [TransitType]
    let lines: [(name: String, color: String, textColor: String)]
    var nextDepartureState: DepartureTimeState? = nil
    var nextDepartureA11y: (lineName: String, time: String)? = nil
    var distance: String? = nil

    var body: some View {
        HStack(spacing: 8) {
            // Stop info: two rows
            VStack(alignment: .leading, spacing: 5) {
                // Row 1: name + distance  ·  Spacer  ·  modal type icons
                HStack(spacing: 0) {
                    Text(name)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)

                    if let distance {
                        Text("  \(distance)")
                            .font(.caption2.weight(.medium))
                            .foregroundStyle(AppTheme.textTertiary)
                    }

                    Spacer(minLength: 6)

                    // Modal type icons — small, no background box
                    HStack(spacing: 4) {
                        ForEach(transitTypes.sorted(by: { $0.rawValue < $1.rawValue }), id: \.rawValue) { type in
                            type.icon.sized(12)
                                .foregroundStyle(AppTheme.textSecondary)
                        }
                    }
                }

                // Row 2: line badges
                if !lines.isEmpty {
                    HStack(spacing: 4) {
                        ForEach(lines.prefix(6), id: \.name) { line in
                            LineBadge(
                                name: line.name,
                                color: line.color,
                                textColor: line.textColor,
                                transitType: transitTypes.first ?? .bus,
                                size: .medium
                            )
                        }
                        if lines.count > 6 {
                            OverflowBadge(count: lines.count - 6)
                        }
                    }
                }
            }

            // Next departure time
            if let state = nextDepartureState {
                TimeDisplay(state: state)
            }

            LucideIcon.chevronRight.sized(11)
                .foregroundStyle(AppTheme.textTertiary)
        }
        .padding(12)
        .adaptiveGlass(in: RoundedRectangle(cornerRadius: 12), withShadow: true)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityAddTraits(.isButton)
    }

    // MARK: - Private

    private var accessibilityLabel: String {
        var parts = [name]
        let typeNames = transitTypes.map(\.displayName).joined(separator: ", ")
        if !typeNames.isEmpty { parts.append(typeNames) }
        let lineNames = lines.map(\.name).joined(separator: ", ")
        if !lineNames.isEmpty { parts.append(String(format: NSLocalizedString("stop_lines_a11y", comment: ""), lineNames)) }
        if let a11y = nextDepartureA11y {
            parts.append(String(format: NSLocalizedString("stop_next_a11y", comment: ""), a11y.lineName, a11y.time))
        }
        return parts.joined(separator: ", ")
    }
}
