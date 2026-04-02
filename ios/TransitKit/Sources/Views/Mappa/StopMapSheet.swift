import SwiftUI

// MARK: - Stop Map Sheet

/// Bottom sheet displayed when a stop annotation is tapped on the map.
/// Shows stop name, transit types, next departures, and line badges.
struct StopMapSheet: View {
    let stop: ResolvedStop
    @Environment(ScheduleStore.self) private var store

    /// Callback to navigate to the full stop detail view.
    var onShowAllDepartures: ((ResolvedStop) -> Void)?

    private var upcomingDepartures: [Departure] {
        store.upcomingDepartures(forStopId: stop.id, limit: 5)
    }

    /// Unique routes serving this stop (for line badges).
    private var routesAtStop: [Route] {
        stop.lineNames.compactMap { lineName in
            store.routes.first { $0.name == lineName }
        }
        .uniqued(by: \.id)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // MARK: Header
            header

            Divider()
                .foregroundStyle(AppTheme.separatorLine)

            // MARK: Upcoming Departures
            if upcomingDepartures.isEmpty {
                noDeparturesView
            } else {
                departuresList
            }

            // MARK: Line Badges
            if !routesAtStop.isEmpty {
                lineBadgesRow
            }

            // MARK: See All Button
            if let onShowAllDepartures {
                Button {
                    onShowAllDepartures(stop)
                } label: {
                    HStack {
                        Text(String(localized: "see_all_departures"))
                            .font(.system(size: 14, weight: .semibold))
                        Spacer()
                        LucideIcon.chevronRight.image
                            .font(.system(size: 12, weight: .semibold))
                    }
                    .foregroundStyle(AppTheme.accent)
                    .padding(.vertical, 10)
                    .padding(.horizontal, 14)
                    .background(AppTheme.accent.opacity(0.1), in: RoundedRectangle(cornerRadius: 10))
                }
                .accessibilityIdentifier("btn_see_all_departures")
            }
        }
        .padding(16)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("stop_map_sheet")
    }

    // MARK: - Subviews

    private var header: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text(stop.name)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(2)

                // Transit type icons
                HStack(spacing: 6) {
                    ForEach(Array(stop.transitTypes).sorted(by: { $0.rawValue < $1.rawValue }), id: \.self) { type in
                        HStack(spacing: 3) {
                            type.icon.image
                                .font(.system(size: 11))
                            Text(type.displayName)
                                .font(.system(size: 11, weight: .medium))
                        }
                        .foregroundStyle(AppTheme.textSecondary)
                    }
                }
            }

            Spacer()

            // Drag indicator visual cue
            RoundedRectangle(cornerRadius: 2.5)
                .fill(AppTheme.textTertiary)
                .frame(width: 36, height: 5)
                .padding(.top, 2)
        }
    }

    private var noDeparturesView: some View {
        HStack {
            LucideIcon.clock.image
                .foregroundStyle(AppTheme.textTertiary)
            Text(String(localized: "no_departures_today"))
                .font(.system(size: 13))
                .foregroundStyle(AppTheme.textSecondary)
        }
        .padding(.vertical, 8)
    }

    private var departuresList: some View {
        VStack(spacing: 6) {
            ForEach(upcomingDepartures) { departure in
                HStack(spacing: 8) {
                    LineBadge(departure: departure, size: .small)

                    Text(departure.headsign)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)

                    Spacer()

                    TimeDisplay(departure: departure)
                }
                .padding(.vertical, 4)
                .accessibilityIdentifier("sheet_dep_\(departure.lineName)_\(departure.time)")
            }
        }
    }

    private var lineBadgesRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                ForEach(routesAtStop) { route in
                    LineBadge(
                        lineName: route.name,
                        color: route.color,
                        textColor: route.textColor,
                        transitType: route.transitType,
                        size: .small
                    )
                }
            }
        }
    }
}

// MARK: - Array Uniqued Helper

private extension Array {
    func uniqued<T: Hashable>(by keyPath: KeyPath<Element, T>) -> [Element] {
        var seen = Set<T>()
        return filter { seen.insert($0[keyPath: keyPath]).inserted }
    }
}
