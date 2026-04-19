import SwiftUI
import UIKit

// MARK: - AlertDetailView

/// Full-screen detail for a single GTFS-RT alert.
/// Layout: hero (severity badge + title) → description card → affected routes/stops chips
/// → optional "Read more" link. Consumers drill in from AlertListView or from the
/// alert rows embedded in StopDetailView / LineDetailView.
struct AlertDetailView: View {
    let alert: GtfsRtAlert
    @Environment(ScheduleStore.self) private var store

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                hero

                descriptionCard

                if !affectedRoutes.isEmpty {
                    routesCard
                }

                if !affectedStops.isEmpty {
                    stopsCard
                }

                if let url = alert.url, let parsed = URL(string: url) {
                    Link(destination: parsed) {
                        readMoreRow
                    }
                }

                Spacer(minLength: 32)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) { EmptyView() }
        }
        .toolbar(.hidden, for: .tabBar)
    }

    // MARK: Hero

    private var hero: some View {
        VStack(alignment: .leading, spacing: 12) {
            severityPill
            Text(alert.headerText.resolved())
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(AppTheme.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.top, 4)
    }

    private var severityPill: some View {
        HStack(spacing: 8) {
            Circle()
                .fill(severityColor)
                .frame(width: 8, height: 8)
            Text(severityLabel.uppercased())
                .font(.caption.weight(.bold))
                .kerning(0.6)
                .foregroundStyle(severityColor)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(
            Capsule(style: .continuous)
                .fill(severityColor.opacity(0.12))
        )
    }

    // MARK: Cards

    private var descriptionCard: some View {
        let body = alert.descriptionText.resolved()
        return GlassCard(cornerRadius: 16) {
            Text(body.isEmpty ? String(localized: "alert_no_description") : body)
                .font(.subheadline)
                .foregroundStyle(AppTheme.textPrimary)
                .lineSpacing(4)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
        }
    }

    private var routesCard: some View {
        GlassCard(cornerRadius: 16) {
            VStack(alignment: .leading, spacing: 12) {
                sectionHeader(icon: .route, label: String(localized: "alerts_affected_routes"))
                FlowLayout(spacing: 8) {
                    ForEach(affectedRoutes, id: \.id) { route in
                        LineBadgePill(route: route)
                    }
                }
            }
            .padding(16)
        }
    }

    private var stopsCard: some View {
        GlassCard(cornerRadius: 16) {
            VStack(alignment: .leading, spacing: 0) {
                sectionHeader(icon: .mapPin, label: String(localized: "alerts_affected_stops"))
                    .padding(.bottom, 8)
                ForEach(Array(affectedStops.enumerated()), id: \.element.id) { idx, stop in
                    HStack(spacing: 10) {
                        LucideIcon.mapPin.sized(14)
                            .foregroundStyle(AppTheme.textTertiary)
                        Text(stop.name)
                            .font(.subheadline)
                            .foregroundStyle(AppTheme.textPrimary)
                            .lineLimit(1)
                        Spacer()
                    }
                    .padding(.vertical, 10)

                    if idx < affectedStops.count - 1 {
                        Rectangle()
                            .fill(AppTheme.separatorLine)
                            .frame(height: 0.5)
                    }
                }
            }
            .padding(16)
        }
    }

    private var readMoreRow: some View {
        HStack(spacing: 12) {
            LucideIcon.externalLink.sized(18)
                .foregroundStyle(AppTheme.accent)
            Text(String(localized: "alerts_read_more"))
                .font(.headline)
                .foregroundStyle(AppTheme.textPrimary)
            Spacer()
            LucideIcon.chevronRight.sized(14)
                .foregroundStyle(AppTheme.textTertiary)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(AppTheme.bgSecondary)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(AppTheme.glassBorder, lineWidth: 1)
        )
    }

    // MARK: Helpers

    private func sectionHeader(icon: LucideIcon, label: String) -> some View {
        HStack(spacing: 8) {
            icon.image
                .font(.subheadline)
                .foregroundStyle(AppTheme.accent)
            Text(label.uppercased())
                .font(.caption.weight(.semibold))
                .kerning(0.6)
                .foregroundStyle(AppTheme.textTertiary)
        }
    }

    private var affectedRoutes: [APIRoute] {
        alert.affectedRouteIds
            .compactMap { store.route(forId: $0) }
            .sorted { $0.name.localizedCompare($1.name) == .orderedAscending }
    }

    private var affectedStops: [ResolvedStop] {
        alert.affectedStopIds
            .compactMap { id in store.stops.first(where: { $0.id == id }) }
            .sorted { $0.name.localizedCompare($1.name) == .orderedAscending }
    }

    private var severityColor: Color {
        switch alert.severity {
        case .severe:  return .red
        case .warning: return .orange
        case .info:    return AppTheme.accent
        case .unknown: return AppTheme.textSecondary
        }
    }

    private var severityLabel: String {
        switch alert.severity {
        case .severe:  return String(localized: "alert_severity_severe")
        case .warning: return String(localized: "alert_severity_warning")
        case .info:    return String(localized: "alert_severity_info")
        case .unknown: return String(localized: "alert_severity_advisory")
        }
    }
}

// MARK: - LineBadgePill

/// Affected-route chip shown in alert detail. Thin wrapper around the design
/// system `LineBadge` — kept as a private struct only to keep callsite code
/// tidy (alert detail already destructures `route` inline).
private struct LineBadgePill: View {
    let route: APIRoute

    var body: some View {
        LineBadge(route: route, size: .medium)
    }
}
