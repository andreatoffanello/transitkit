import SwiftUI

/// Movete-style alert card — used in the global alert list and in the
/// contextual alert sections inside stop / line detail.
///
/// Layout: cause icon + actionable title (cause · effect), header text as
/// secondary detail (2 lines), horizontal line badges for affected routes.
/// The bureaucratic `headerText` from the feed is intentionally demoted to
/// the body — the title row uses a human-readable cause/effect combination.
struct AlertCard: View {
    let alert: GtfsRtAlert
    @Environment(ScheduleStore.self) private var store

    private var affectedRoutes: [APIRoute] {
        alert.affectedRouteIds
            .compactMap { store.route(forId: $0) }
            .sorted { $0.name.localizedCompare($1.name) == .orderedAscending }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                AlertCauseIcon.icon(for: alert.cause).sized(14)
                    .foregroundStyle(severityColor)
                Text(alert.displayTitle)
                    .font(.callout.weight(.semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(1)
            }

            let header = alert.headerText.resolved()
            if !header.isEmpty {
                Text(header)
                    .font(.footnote)
                    .foregroundStyle(AppTheme.textSecondary)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
            }

            if !affectedRoutes.isEmpty {
                // Single-line row: up to 6 badges + "+N" as plain text.
                // Horizontal scroll inside the card competed with the alert
                // list scroll and hid badges past the viewport. The full list
                // lives in AlertDetail.
                let maxVisible = 6
                let visible = Array(affectedRoutes.prefix(maxVisible))
                let overflow = affectedRoutes.count - visible.count
                HStack(spacing: 4) {
                    ForEach(visible, id: \.id) { route in
                        LineBadge(route: route, size: .small)
                    }
                    if overflow > 0 {
                        Text("+\(overflow)")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(AppTheme.textSecondary)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(AppTheme.bgSecondary)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(severityColor.opacity(0.35), lineWidth: 1)
        )
    }

    private var severityColor: Color {
        switch alert.severity {
        case .severe:  return .red
        case .warning: return .orange
        case .info:    return AppTheme.accent
        case .unknown: return .orange
        }
    }
}
