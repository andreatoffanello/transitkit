import SwiftUI

/// List of all routes/lines, grouped by transit type with search and filter.
/// Each row shows a LineBadge with GTFS color, long name, transit type icon, and direction count.
struct LinesListView: View {
    let searchQuery: String
    let transitTypeFilter: TransitType?
    let recentIds: [String]
    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore
    @State private var collapsedTypes: Set<TransitType> = []

    init(searchQuery: String, transitTypeFilter: TransitType?, recentIds: [String] = []) {
        self.searchQuery = searchQuery
        self.transitTypeFilter = transitTypeFilter
        self.recentIds = recentIds
    }

    // MARK: - Deduplication

    /// Deduplicates routes by name, keeping the one with the most directions.
    /// Preserves the original order of first occurrence.
    private func deduplicated(_ routes: [APIRoute]) -> [APIRoute] {
        var seen: [String: APIRoute] = [:]
        for route in routes {
            if let existing = seen[route.name] {
                if route.directions.count > existing.directions.count {
                    seen[route.name] = route
                }
            } else {
                seen[route.name] = route
            }
        }
        var result: [APIRoute] = []
        var added: Set<String> = []
        for route in routes {
            if !added.contains(route.name) {
                result.append(seen[route.name]!)
                added.insert(route.name)
            }
        }
        return result
    }

    // MARK: - Filtering

    private var filteredAPIRoutes: [APIRoute] {
        var result = store.routes

        // Filter by transit type
        if let type = transitTypeFilter {
            result = result.filter { TransitType(gtfsRouteType: $0.transitType) == type }
        }

        // Filter and sort by search query
        if !searchQuery.isEmpty {
            if searchQuery.count >= 2 {
                // Fuzzy-scored filter: compute best score across name, longName and id
                let scored = result.compactMap { route -> (route: APIRoute, score: Int)? in
                    let score = max(
                        FuzzySearch.score(route.name, query: searchQuery),
                        max(
                            FuzzySearch.score(route.longName ?? "", query: searchQuery),
                            FuzzySearch.score(route.id, query: searchQuery)
                        )
                    )
                    return score > 0 ? (route, score) : nil
                }
                let sorted = scored
                    .sorted { $0.score != $1.score ? $0.score > $1.score : $0.route.name.localizedStandardCompare($1.route.name) == .orderedAscending }
                    .map(\.route)
                return deduplicated(sorted)
            } else {
                let query = searchQuery.lowercased()
                result = result.filter { route in
                    route.name.lowercased().contains(query)
                    || (route.longName ?? "").lowercased().contains(query)
                }
                return deduplicated(result.sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending })
            }
        }

        return deduplicated(result.sorted {
            $0.name.localizedStandardCompare($1.name) == .orderedAscending
        })
    }

    /// Recent routes resolved from the store, preserving recency order.
    private var recentAPIRoutes: [APIRoute] {
        guard searchQuery.isEmpty else { return [] }
        return recentIds.compactMap { id in store.routes.first { $0.id == id } }
    }

    /// Group routes by transit type for sectioned display.
    private var groupedAPIRoutes: [(type: TransitType, routes: [APIRoute])] {
        var groups: [(TransitType, [APIRoute])] = []
        for type in TransitType.allCases {
            let matching = filteredAPIRoutes.filter { TransitType(gtfsRouteType: $0.transitType) == type }
            if !matching.isEmpty {
                groups.append((type, matching))
            }
        }
        return groups
    }

    private var hasMultipleTransitTypes: Bool { groupedAPIRoutes.count > 1 }

    // MARK: - Body

    var body: some View {
        if store.isLoading && store.routes.isEmpty {
            loadingState
        } else if filteredAPIRoutes.isEmpty && recentAPIRoutes.isEmpty {
            emptyState
        } else {
            routesScrollView
        }
    }

    // MARK: - Loading

    private var loadingState: some View {
        VStack(spacing: 14) {
            Spacer()
            ProgressView()
                .tint(AppTheme.accent)
            Text(String(localized: "lines_loading"))
                .font(.subheadline)
                .foregroundStyle(AppTheme.textTertiary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Empty

    private var emptyState: some View {
        EmptyStateView(
            icon: .train,
            title: String(localized: "lines_no_result"),
            subtitle: String(localized: "lines_no_result_hint")
        )
    }

    // MARK: - APIRoutes List

    // MARK: - Recent Section

    @ViewBuilder
    private var recentSection: some View {
        if !recentAPIRoutes.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text(String(localized: "recent_searches"))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textTertiary)
                    .padding(.horizontal, 4)

                VStack(spacing: 8) {
                    ForEach(recentAPIRoutes) { route in
                        NavigationLink(value: route) {
                            LineRowContent(
                                route: route,
                                hasMultipleTypes: hasMultipleTransitTypes,
                                store: store,
                                liveCount: vehicleStore.liveCount(forRouteId: route.id)
                            )
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("recent_line_row_\(route.id)")
                    }
                }
            }
            .padding(.bottom, 4)
        }
    }

    private var routesScrollView: some View {
        ScrollView(showsIndicators: false) {
            LazyVStack(spacing: 16) {
                // Recent section — only visible when not actively searching
                recentSection

                // Result count when searching
                if !searchQuery.isEmpty {
                    Text(String(format: NSLocalizedString("lines_result_count", comment: ""), filteredAPIRoutes.count))
                        .font(.caption)
                        .foregroundStyle(AppTheme.textTertiary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 4)
                }

                ForEach(groupedAPIRoutes, id: \.type) { group in
                    VStack(alignment: .leading, spacing: 10) {
                        // Category header — only shown when multiple transit types are present
                        if hasMultipleTransitTypes {
                            Button {
                                withAnimation(.easeInOut(duration: 0.25)) {
                                    if collapsedTypes.contains(group.type) {
                                        collapsedTypes.remove(group.type)
                                    } else {
                                        collapsedTypes.insert(group.type)
                                    }
                                }
                            } label: {
                                HStack(spacing: 8) {
                                    group.type.icon.image
                                        .font(.footnote.weight(.semibold))
                                        .foregroundStyle(AppTheme.textSecondary)
                                    Text(group.type.displayName)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundStyle(AppTheme.textSecondary)
                                    Text("\(group.routes.count)")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundStyle(AppTheme.textTertiary)
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(AppTheme.textTertiary.opacity(0.1), in: Capsule())
                                    Spacer()
                                    (collapsedTypes.contains(group.type) ? LucideIcon.chevronRight : LucideIcon.chevronDown).image
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(AppTheme.textTertiary)
                                }
                                .padding(.horizontal, 4)
                                .frame(minHeight: 44)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                        }

                        // APIRoute rows — always visible when only one type; collapsible otherwise
                        if !hasMultipleTransitTypes || !collapsedTypes.contains(group.type) {
                            VStack(spacing: 8) {
                                ForEach(group.routes) { route in
                                    NavigationLink(value: route) {
                                        LineRowContent(
                                route: route,
                                hasMultipleTypes: hasMultipleTransitTypes,
                                store: store,
                                liveCount: vehicleStore.liveCount(forRouteId: route.id)
                            )
                                    }
                                    .buttonStyle(.plain)
                                    .accessibilityIdentifier("line_row_\(route.id)")
                                }
                            }
                            .transition(.opacity.combined(with: .move(edge: .top)))
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 4)
            .padding(.bottom, 80)
        }
        .scrollDismissesKeyboard(.interactively)
    }
}

// MARK: - LineeTab

/// Standalone tab for all transit lines with search and transit-type filter.
/// Deep-link aware: consumes `DeepLinkRouter.pendingRoute` to push line detail.
struct LineeTab: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(SearchHistoryStore.self) private var searchHistoryStore
    @Environment(DeepLinkRouter.self) private var router
    @State private var searchQuery = ""
    @State private var selectedTransitType: TransitType?
    @State private var path = NavigationPath()

    private var availableTransitTypes: [TransitType] {
        let types = Set(store.routes.map { TransitType(gtfsRouteType: $0.transitType) })
        return TransitType.allCases.filter { types.contains($0) }
    }

    var body: some View {
        NavigationStack(path: $path) {
            VStack(spacing: 0) {
                // Search bar
                searchBar

                // Transit type filter chips
                if availableTransitTypes.count > 1 {
                    filterChips
                }

                // Separator
                Rectangle()
                    .fill(AppTheme.separatorLine)
                    .frame(height: 0.5)

                // Loading indicator
                if store.isLoading {
                    ProgressView()
                        .tint(AppTheme.accent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 4)
                }

                // Lines list
                LinesListView(
                    searchQuery: searchQuery,
                    transitTypeFilter: selectedTransitType,
                    recentIds: searchHistoryStore.recentLineIds
                )
                .frame(maxHeight: .infinity)
            }
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle(String(localized: "tab_lines"))
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: APIRoute.self) { route in
                let _ = searchHistoryStore.recordLine(route.id)
                LineDetailView(route: route)
            }
            .task {
                if store.routes.isEmpty && !store.isLoading {
                    await store.load()
                }
            }
            .onAppear { consumePending() }
            .onChange(of: router.pendingRoute) { _, _ in consumePending() }
            .onChange(of: router.pendingSearchQuery) { _, _ in consumeSearch() }
            .onChange(of: router.pendingSearchScope) { _, _ in consumeSearch() }
        }
    }

    private func consumePending() {
        if let route = router.pendingRoute {
            router.pendingRoute = nil
            path = NavigationPath()
            path.append(route)
        }
        consumeSearch()
    }

    private func consumeSearch() {
        guard router.pendingSearchScope == .lines,
              let query = router.pendingSearchQuery else { return }
        router.pendingSearchQuery = nil
        path = NavigationPath()
        searchQuery = query
    }

    private var searchBar: some View {
        HStack(spacing: 8) {
            LucideIcon.search.sized(15)
                .foregroundStyle(AppTheme.textTertiary)

            TextField(
                String(localized: "search_line_placeholder"),
                text: $searchQuery
            )
            .font(.system(.subheadline))
            .foregroundStyle(AppTheme.textPrimary)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)
            .accessibilityIdentifier("search_lines")

            if !searchQuery.isEmpty {
                Button {
                    searchQuery = ""
                } label: {
                    LucideIcon.circleX.sized(14)
                        .foregroundStyle(AppTheme.textTertiary)
                }
                .accessibilityIdentifier("btn_clear_lines_search")
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(AppTheme.glassFill, in: RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(AppTheme.glassBorder, lineWidth: 1)
        )
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                FilterChip(
                    label: String(localized: "filter_all"),
                    isSelected: selectedTransitType == nil
                ) {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        selectedTransitType = nil
                    }
                }
                .accessibilityIdentifier("filter_all_lines")

                ForEach(availableTransitTypes, id: \.self) { type in
                    FilterChip(
                        label: type.displayName,
                        isSelected: selectedTransitType == type,
                        icon: type.icon
                    ) {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            selectedTransitType = selectedTransitType == type ? nil : type
                        }
                    }
                    .accessibilityIdentifier("filter_line_\(type.rawValue)")
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 8)
        }
    }
}
