import SwiftUI

/// Service alerts affecting a specific stop, rendered inline within `StopDetailView`.
/// Self-contained: reads alerts from `AlertStore` via environment.
struct StopAlertsSection: View {
    let stop: ResolvedStop
    @Environment(AlertStore.self) private var alertStore

    var body: some View {
        let alerts = alertStore.alerts(forStopId: stop.id)
        if !alerts.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    LucideIcon.alertTriangle.sized(14)
                        .foregroundStyle(AppTheme.accent)
                    Text(String(localized: "stop_detail_alerts_section").uppercased())
                        .font(.caption.weight(.semibold))
                        .kerning(0.6)
                        .foregroundStyle(AppTheme.textTertiary)
                    Spacer()
                }
                .padding(.horizontal, 20)

                VStack(spacing: 8) {
                    ForEach(alerts) { alert in
                        NavigationLink {
                            AlertDetailView(alert: alert)
                        } label: {
                            inlineAlertRow(alert)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 20)
            }
            .padding(.top, 16)
            .padding(.bottom, 4)
        }
    }

    private func inlineAlertRow(_ alert: GtfsRtAlert) -> some View {
        HStack(spacing: 10) {
            Circle()
                .fill(alertRowDotColor(alert.severity))
                .frame(width: 8, height: 8)
            Text(alert.headerText.resolved())
                .font(.subheadline.weight(.medium))
                .foregroundStyle(AppTheme.textPrimary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)
            Spacer(minLength: 0)
            LucideIcon.chevronRight.sized(14)
                .foregroundStyle(AppTheme.textTertiary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(AppTheme.bgSecondary)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(AppTheme.glassBorder, lineWidth: 1)
        )
    }

    private func alertRowDotColor(_ severity: AlertSeverity) -> Color {
        switch severity {
        case .severe:  return .red
        case .warning: return .orange
        case .info:    return AppTheme.accent
        case .unknown: return AppTheme.textTertiary
        }
    }
}
