import SwiftUI

// MARK: - AlertListView

/// Full list of currently-active service alerts. Top filter bar: All / Favorites / Specific line.
/// Sorted by severity (severe → warning → info → unknown), then alphabetically.
struct AlertListView: View {
    @Environment(AlertStore.self) private var alertStore
    @Environment(ScheduleStore.self) private var store
    @Environment(FavoritesManager.self) private var favoritesManager
    @Environment(\.dismiss) private var dismiss
    @Environment(\.isPresented) private var isPresented

    enum Filter: Equatable {
        case all
        case favorites
        case route(String) // routeId
    }

    @State private var filter: Filter = .all

    private var filteredAlerts: [GtfsRtAlert] {
        let base: [GtfsRtAlert]
        switch filter {
        case .all:
            base = alertStore.activeAlerts
        case .favorites:
            let favs = Set(favoritesManager.favoriteRouteIds)
            guard !favs.isEmpty else { return [] }
            base = alertStore.activeAlerts.filter { !$0.affectedRouteIds.isDisjoint(with: favs) }
        case .route(let rid):
            base = alertStore.activeAlerts.filter { $0.affectedRouteIds.contains(rid) }
        }
        return base.sorted { lhs, rhs in
            if lhs.severity.rawValue != rhs.severity.rawValue {
                return lhs.severity.rawValue > rhs.severity.rawValue
            }
            return lhs.headerText.resolved().localizedCompare(rhs.headerText.resolved()) == .orderedAscending
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            filterBar
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle(String(localized: "alerts_title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if isPresented {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(String(localized: "action_close")) { dismiss() }
                        .foregroundStyle(AppTheme.textPrimary)
                }
            }
        }
    }

    // MARK: Content

    @ViewBuilder
    private var content: some View {
        if filteredAlerts.isEmpty {
            emptyState
        } else {
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(filteredAlerts) { alert in
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

    // MARK: Filter bar

    @ViewBuilder
    private var filterBar: some View {
        HStack(spacing: 8) {
            filterPill(label: String(localized: "alerts_filter_all"), isActive: filter == .all) {
                filter = .all
            }
            filterPill(label: String(localized: "alerts_filter_favorites"), isActive: filter == .favorites) {
                filter = .favorites
            }
            routePicker
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(AppTheme.background)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(AppTheme.separatorLine)
                .frame(height: 0.5)
        }
        .sensoryFeedback(.selection, trigger: filter)
    }

    private func filterPill(label: String, isActive: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 13, weight: isActive ? .semibold : .medium))
                .foregroundStyle(isActive ? AppTheme.textPrimary : AppTheme.textSecondary)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(
                    Capsule().fill(isActive ? AppTheme.accent.opacity(0.18) : AppTheme.bgSecondary.opacity(0.5))
                )
                .overlay(
                    Capsule().strokeBorder(
                        isActive ? AppTheme.accent.opacity(0.5) : AppTheme.glassBorder,
                        lineWidth: 1
                    )
                )
        }
        .buttonStyle(.plain)
    }

    private var routePicker: some View {
        Menu {
            ForEach(store.routes, id: \.id) { route in
                Button {
                    filter = .route(route.id)
                } label: {
                    Label(route.longName ?? route.name, systemImage: filter == .route(route.id) ? "checkmark" : "")
                }
            }
        } label: {
            HStack(spacing: 6) {
                Text(routePickerLabel)
                    .font(.system(size: 13, weight: routePickerIsActive ? .semibold : .medium))
                    .foregroundStyle(routePickerIsActive ? AppTheme.textPrimary : AppTheme.textSecondary)
                LucideIcon.chevronDown.sized(12)
                    .foregroundStyle(routePickerIsActive ? AppTheme.textPrimary : AppTheme.textSecondary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(
                Capsule().fill(routePickerIsActive ? AppTheme.accent.opacity(0.18) : AppTheme.bgSecondary.opacity(0.5))
            )
            .overlay(
                Capsule().strokeBorder(
                    routePickerIsActive ? AppTheme.accent.opacity(0.5) : AppTheme.glassBorder,
                    lineWidth: 1
                )
            )
        }
        .accessibilityIdentifier("alerts_filter_route_menu")
    }

    private var routePickerIsActive: Bool {
        if case .route = filter { return true }
        return false
    }

    private var routePickerLabel: String {
        if case .route(let rid) = filter, let route = store.route(forId: rid) {
            return route.name
        }
        return String(localized: "alerts_filter_route")
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

    @ViewBuilder
    private var emptyState: some View {
        switch filter {
        case .favorites where favoritesManager.favoriteRouteIds.isEmpty:
            EmptyStateView(
                icon: .star,
                title: String(localized: "alerts_empty_no_favorites_title"),
                subtitle: String(localized: "alerts_empty_no_favorites_subtitle")
            )
        case .favorites:
            EmptyStateView(
                icon: .check,
                title: String(localized: "alerts_empty_favorites_title"),
                subtitle: String(localized: "alerts_empty_favorites_subtitle")
            )
        case .route:
            EmptyStateView(
                icon: .check,
                title: String(localized: "alerts_empty_route_title"),
                subtitle: String(localized: "alerts_empty_route_subtitle")
            )
        case .all:
            EmptyStateView(
                icon: .check,
                title: String(localized: "alerts_empty_title"),
                subtitle: String(localized: "alerts_empty_subtitle")
            )
        }
    }

    // MARK: Helpers

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
