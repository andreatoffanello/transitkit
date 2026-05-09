import SwiftUI

// MARK: - AlertListView

/// Full list of currently-active service alerts. Opened from the Home hero banner
/// and from the dedicated Alert sections on stop/line detail screens.
/// Sorted by severity (severe → warning → info → unknown), then alphabetically.
struct AlertListView: View {
    @Environment(AlertStore.self) private var alertStore
    @Environment(\.dismiss) private var dismiss

    private var sortedAlerts: [GtfsRtAlert] {
        alertStore.activeAlerts.sorted { lhs, rhs in
            if lhs.severity.rawValue != rhs.severity.rawValue {
                return lhs.severity.rawValue > rhs.severity.rawValue
            }
            return lhs.headerText.resolved().localizedCompare(rhs.headerText.resolved()) == .orderedAscending
        }
    }

    var body: some View {
        Group {
            if sortedAlerts.isEmpty {
                emptyState
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(sortedAlerts) { alert in
                            NavigationLink {
                                AlertDetailView(alert: alert)
                            } label: {
                                alertRow(alert)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 16)
                }
            }
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle(String(localized: "alerts_title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(String(localized: "action_close")) { dismiss() }
                    .foregroundStyle(AppTheme.textPrimary)
            }
        }
    }

    // MARK: Row

    private func alertRow(_ alert: GtfsRtAlert) -> some View {
        HStack(alignment: .top, spacing: 12) {
            severityDot(for: alert.severity)
                .padding(.top, 6)

            VStack(alignment: .leading, spacing: 4) {
                Text(alert.headerText.resolved())
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                if let sub = alertSubtitle(alert), !sub.isEmpty {
                    Text(sub)
                        .font(.caption)
                        .foregroundStyle(AppTheme.textTertiary)
                        .lineLimit(1)
                }
            }

            Spacer(minLength: 0)

            LucideIcon.chevronRight.sized(14)
                .foregroundStyle(AppTheme.textTertiary)
                .padding(.top, 4)
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(AppTheme.bgSecondary)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(AppTheme.glassBorder, lineWidth: 1)
        )
    }

    private var emptyState: some View {
        EmptyStateView(
            icon: .check,
            title: String(localized: "alerts_empty_title"),
            subtitle: String(localized: "alerts_empty_subtitle")
        )
    }

    // MARK: Helpers

    /// A short subtitle — affected routes/stops count or effect label — to give the row context.
    private func alertSubtitle(_ alert: GtfsRtAlert) -> String? {
        let routes = alert.affectedRouteIds.count
        let stops = alert.affectedStopIds.count
        var parts: [String] = []
        if routes > 0 { parts.append(String(format: String(localized: "alerts_affected_routes_count"), routes)) }
        if stops > 0 { parts.append(String(format: String(localized: "alerts_affected_stops_count"), stops)) }
        if parts.isEmpty, let effect = effectLabel(alert.effect) { return effect }
        return parts.joined(separator: " · ")
    }

    private func effectLabel(_ effect: AlertEffect) -> String? {
        switch effect {
        case .noService:          return String(localized: "alert_effect_no_service")
        case .reducedService:     return String(localized: "alert_effect_reduced_service")
        case .significantDelays:  return String(localized: "alert_effect_delays")
        case .detour:             return String(localized: "alert_effect_detour")
        case .stopMoved:          return String(localized: "alert_effect_stop_moved")
        case .additionalService:  return String(localized: "alert_effect_additional")
        case .modifiedService:    return String(localized: "alert_effect_modified")
        case .accessibilityIssue: return String(localized: "alert_effect_accessibility")
        case .noEffect, .unknownEffect, .otherEffect: return nil
        }
    }

    @ViewBuilder
    private func severityDot(for severity: AlertSeverity) -> some View {
        Circle()
            .fill(severityColor(severity))
            .frame(width: 8, height: 8)
    }

    private func severityColor(_ severity: AlertSeverity) -> Color {
        switch severity {
        case .severe:  return .red
        case .warning: return .orange
        case .info:    return AppTheme.accent
        case .unknown: return AppTheme.textTertiary
        }
    }
}
