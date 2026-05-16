import SwiftUI

/// "AVVISI" section pinned to the bottom of `StopDetailView`. Renders the
/// rich `AlertCard` shared with the global alert list. The list is computed
/// by the parent (so the chip at the top and this section stay in sync) —
/// this view assumes the caller already filtered by stop / line scope.
struct StopAlertsSection: View {
    let alerts: [GtfsRtAlert]

    var body: some View {
        if !alerts.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 8) {
                    LucideIcon.alertTriangle.sized(14)
                        .foregroundStyle(.orange)
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
                            AlertCard(alert: alert)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 24)
            .padding(.bottom, 16)
        }
    }
}
