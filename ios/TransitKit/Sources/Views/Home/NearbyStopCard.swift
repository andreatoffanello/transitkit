import SwiftUI

/// Walking time estimate for a distance in meters, using the same heuristic as
/// the home favorites card (80 m/min average pace).
func walkingTime(meters: Double) -> String {
    let minutes = Int((meters / 80.0).rounded(.up))
    if minutes <= 1 { return String(localized: "walking_1_min") }
    if minutes > 10 { return String(localized: "walking_10_plus_min") }
    return String(format: String(localized: "walking_n_min"), minutes)
}

/// Horizontal-scroll card for a nearby stop. Shows pin icon + stop name + walking
/// time on the header row, and a horizontal strip of line badges below. Mirrors
/// the Movete pattern: fixed width, glass surface, line badges as the dominant
/// affordance under the name.
struct NearbyStopCard: View {
    let stop: ResolvedStop
    let distanceMeters: Double
    let routes: [APIRoute]
    let onTap: () -> Void

    /// Max number of badges shown before the "+N" overflow chip.
    private static let maxVisibleBadges = 4

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 10) {
                header
                badges
            }
            .padding(14)
            .frame(width: 240, alignment: .leading)
            .adaptiveGlass(in: RoundedRectangle(cornerRadius: 14), withShadow: true)
        }
        .buttonStyle(PressableCardStyle())
        .accessibilityIdentifier("home_nearby_stop_\(stop.id)")
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 8) {
            stopPinIcon(transitTypes: stop.transitTypes).image
                .resizable()
                .frame(width: 14, height: 14)
                .foregroundStyle(AppTheme.textSecondary)
            Text(stop.name)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(AppTheme.textPrimary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    // MARK: - Line badges

    @ViewBuilder
    private var badges: some View {
        if routes.isEmpty {
            Text(walkingTime(meters: distanceMeters))
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(AppTheme.textTertiary)
        } else {
            HStack(spacing: 6) {
                let visible = routes.prefix(Self.maxVisibleBadges)
                let overflow = routes.count - visible.count
                ForEach(Array(visible), id: \.id) { route in
                    LineBadge(route: route, size: .small)
                }
                if overflow > 0 {
                    overflowChip(overflow)
                }
                Spacer(minLength: 0)
                Text(walkingTime(meters: distanceMeters))
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(AppTheme.textTertiary)
                    .lineLimit(1)
            }
        }
    }

    private func overflowChip(_ n: Int) -> some View {
        Text("+\(n)")
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(AppTheme.textSecondary)
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(
                Capsule().fill(AppTheme.textPrimary.opacity(0.08))
            )
    }
}
