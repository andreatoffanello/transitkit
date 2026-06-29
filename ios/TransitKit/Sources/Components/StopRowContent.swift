import SwiftUI

/// Glass card row for a single stop — the canonical stop row used by both the
/// Stops tab (`StopsListView`) and the map line/stop picker (`LinePickerSheet`).
///
/// Shows the stop name, the modal-type icons, and up to six line badges
/// (+N overflow). The whole card is the tap target — no trailing chevron
/// (redundant on a full-width tappable card).
struct StopRowContent: View {
    let stop: ResolvedStop
    let store: ScheduleStore

    /// Primary transit type (used as a fallback for badge colors).
    private var primaryType: TransitType {
        let priority: [TransitType] = [.ferry, .tram, .metro, .bus]
        for type in priority {
            if stop.transitTypes.contains(type) { return type }
        }
        return stop.transitTypes.first ?? .bus
    }

    /// Line badges: resolve route colors for each line at this stop.
    private var lineBadges: [(name: String, route: APIRoute?)] {
        stop.lineNames.prefix(6).map { name in
            let route = store.routes.first { $0.name == name }
            return (name, route)
        }
    }

    var body: some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 5) {
                // Row 1: name  ·  Spacer  ·  modal type icons
                HStack(spacing: 0) {
                    Text(stop.name)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)

                    Spacer(minLength: 6)

                    HStack(spacing: 4) {
                        ForEach(Array(stop.transitTypes).sorted(by: { $0.rawValue < $1.rawValue }), id: \.self) { type in
                            type.icon.sized(12)
                                .foregroundStyle(AppTheme.textSecondary)
                        }
                    }
                }

                // Row 2: line badges
                if !stop.lineNames.isEmpty {
                    HStack(spacing: 4) {
                        ForEach(lineBadges, id: \.name) { badge in
                            LineBadge(
                                name: badge.name,
                                color: badge.route.flatMap(\.color) ?? "#666666",
                                textColor: badge.route.flatMap(\.textColor) ?? "#FFFFFF",
                                transitType: badge.route.map { TransitType(gtfsRouteType: $0.transitType) } ?? primaryType,
                                size: .medium
                            )
                        }
                        if stop.lineNames.count > 6 {
                            Text("+\(stop.lineNames.count - 6)")
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundStyle(AppTheme.textTertiary)
                                .padding(.horizontal, 5)
                                .padding(.vertical, 3)
                                .background(
                                    AppTheme.textTertiary.opacity(0.12),
                                    in: RoundedRectangle(cornerRadius: 4)
                                )
                        }
                    }
                }
            }
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 12)
        .background(AppTheme.glassFill, in: RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(AppTheme.glassBorder, lineWidth: 1)
        )
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(String(format: NSLocalizedString("a11y_stop_lines_count", comment: "Accessibility label: stop name and line count"), stop.name, stop.lineNames.count))
        .accessibilityAddTraits(.isButton)
    }
}
