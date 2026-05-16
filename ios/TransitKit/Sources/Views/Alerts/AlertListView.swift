import SwiftUI

// MARK: - AlertListView

/// List of currently-active service alerts. Two filter pills with counters:
/// "My lines" (default when the user has favourites) and "All".
/// Cards in the "All" view are reordered so alerts relevant to the user's
/// favourite routes float to the top, each carrying a "YOUR LINE" overline.
struct AlertListView: View {
    @Environment(AlertStore.self) private var alertStore
    @Environment(ScheduleStore.self) private var store
    @Environment(FavoritesManager.self) private var favoritesManager
    @Environment(\.dismiss) private var dismiss
    @Environment(\.isPresented) private var isPresented

    enum Filter: Equatable { case mine, all }

    @State private var filter: Filter = .mine

    private var myLineIds: Set<String> {
        Set(favoritesManager.favoriteRouteIds)
    }

    private var activeAlerts: [GtfsRtAlert] {
        alertStore.activeAlerts
    }

    private var myAlerts: [GtfsRtAlert] {
        let favs = myLineIds
        guard !favs.isEmpty else { return [] }
        return activeAlerts.filter { $0.isRelevant(forRoutes: favs) }
    }

    /// In the "All" view: relevant-first, then most-recent first.
    private var orderedAllAlerts: [GtfsRtAlert] {
        let favs = myLineIds
        return activeAlerts.sorted { lhs, rhs in
            let lhsRelevant = lhs.isRelevant(forRoutes: favs)
            let rhsRelevant = rhs.isRelevant(forRoutes: favs)
            if lhsRelevant != rhsRelevant { return lhsRelevant }
            return (lhs.firstActiveStart ?? 0) > (rhs.firstActiveStart ?? 0)
        }
    }

    private var visibleAlerts: [GtfsRtAlert] {
        filter == .mine ? myAlerts : orderedAllAlerts
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
        .onAppear {
            if myLineIds.isEmpty { filter = .all }
        }
    }

    // MARK: Filter bar

    @ViewBuilder
    private var filterBar: some View {
        HStack(spacing: 8) {
            if !myLineIds.isEmpty {
                filterPill(
                    title: String(localized: "alerts_filter_mine"),
                    count: myAlerts.count,
                    isActive: filter == .mine
                ) {
                    filter = .mine
                }
                .accessibilityIdentifier("filter_mine")
            }
            filterPill(
                title: String(localized: "alerts_filter_all"),
                count: activeAlerts.count,
                isActive: filter == .all
            ) {
                filter = .all
            }
            .accessibilityIdentifier("filter_all")
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

    private func filterPill(
        title: String,
        count: Int,
        isActive: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(title)
                    .font(.system(size: 13, weight: isActive ? .semibold : .medium))
                    .foregroundStyle(isActive ? AppTheme.textPrimary : AppTheme.textSecondary)
                Text("\(count)")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(isActive ? AppTheme.textPrimary : AppTheme.textTertiary)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 1)
                    .background(
                        Capsule().fill(
                            isActive
                                ? AppTheme.accent.opacity(0.28)
                                : AppTheme.bgSecondary
                        )
                    )
            }
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

    // MARK: Content

    @ViewBuilder
    private var content: some View {
        if filter == .mine && myLineIds.isEmpty {
            EmptyStateView(
                icon: .star,
                title: String(localized: "alerts_empty_no_favorites_title"),
                subtitle: String(localized: "alerts_empty_no_favorites_subtitle")
            )
        } else if visibleAlerts.isEmpty {
            emptyState
        } else {
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(visibleAlerts) { alert in
                        VStack(alignment: .leading, spacing: 4) {
                            if filter == .all
                                && !myLineIds.isEmpty
                                && alert.isRelevant(forRoutes: myLineIds) {
                                Text(String(localized: "alerts_your_line_badge"))
                                    .font(.caption2.weight(.bold))
                                    .kerning(0.5)
                                    .foregroundStyle(.orange)
                                    .padding(.horizontal, 4)
                            }
                            NavigationLink {
                                AlertDetailView(alert: alert)
                            } label: {
                                AlertCard(alert: alert)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            }
        }
    }

    @ViewBuilder
    private var emptyState: some View {
        EmptyStateView(
            icon: .check,
            title: String(
                localized: filter == .mine
                    ? "alerts_empty_favorites_title"
                    : "alerts_empty_title"
            ),
            subtitle: String(
                localized: filter == .mine
                    ? "alerts_empty_favorites_subtitle"
                    : "alerts_empty_subtitle"
            )
        )
    }
}
