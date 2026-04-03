import SwiftUI

/// List of all routes/lines, grouped by transit type with search and filter.
/// Each row shows a LineBadge with GTFS color, long name, transit type icon, and direction count.
struct LinesListView: View {
    let searchQuery: String
    let transitTypeFilter: TransitType?
    let recentIds: [String]
    @Environment(ScheduleStore.self) private var store
    @State private var collapsedTypes: Set<TransitType> = []

    init(searchQuery: String, transitTypeFilter: TransitType?, recentIds: [String] = []) {
        self.searchQuery = searchQuery
        self.transitTypeFilter = transitTypeFilter
        self.recentIds = recentIds
    }

    // MARK: - Fuzzy scoring

    /// Returns a score 0-100 for how well `text` matches `query` (subsequence match).
    private func fuzzyScore(_ text: String, query: String) -> Int {
        let t = text.lowercased()
        let q = query.lowercased()
        // Exact prefix = highest score
        if t.hasPrefix(q) { return 100 }
        // Contains = high score
        if t.contains(q) { return 80 }
        // Subsequence check
        var qi = q.startIndex
        for char in t {
            if qi < q.endIndex && char == q[qi] {
                qi = q.index(after: qi)
            }
        }
        return qi == q.endIndex ? 50 : 0
    }

    // MARK: - Deduplication

    /// Deduplicates routes by name, keeping the one with the most directions.
    /// Preserves the original order of first occurrence.
    private func deduplicated(_ routes: [Route]) -> [Route] {
        var seen: [String: Route] = [:]
        for route in routes {
            if let existing = seen[route.name] {
                if route.directions.count > existing.directions.count {
                    seen[route.name] = route
                }
            } else {
                seen[route.name] = route
            }
        }
        var result: [Route] = []
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

    private var filteredRoutes: [Route] {
        var result = store.routes

        // Filter by transit type
        if let type = transitTypeFilter {
            result = result.filter { $0.transitType == type }
        }

        // Filter and sort by search query
        if !searchQuery.isEmpty {
            if searchQuery.count >= 2 {
                // Fuzzy-scored filter: compute best score across name, longName and id
                let scored = result.compactMap { route -> (route: Route, score: Int)? in
                    let score = max(
                        fuzzyScore(route.name, query: searchQuery),
                        max(
                            fuzzyScore(route.longName, query: searchQuery),
                            fuzzyScore(route.id, query: searchQuery)
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
                    || route.longName.lowercased().contains(query)
                }
                return deduplicated(result.sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending })
            }
        }

        return deduplicated(result.sorted {
            $0.name.localizedStandardCompare($1.name) == .orderedAscending
        })
    }

    /// Recent routes resolved from the store, preserving recency order.
    private var recentRoutes: [Route] {
        guard searchQuery.isEmpty else { return [] }
        return recentIds.compactMap { id in store.routes.first { $0.id == id } }
    }

    /// Group routes by transit type for sectioned display.
    private var groupedRoutes: [(type: TransitType, routes: [Route])] {
        var groups: [(TransitType, [Route])] = []
        for type in TransitType.allCases {
            let matching = filteredRoutes.filter { $0.transitType == type }
            if !matching.isEmpty {
                groups.append((type, matching))
            }
        }
        return groups
    }

    private var hasMultipleTransitTypes: Bool { groupedRoutes.count > 1 }

    // MARK: - Body

    var body: some View {
        if store.isLoading && store.routes.isEmpty {
            loadingState
        } else if filteredRoutes.isEmpty && recentRoutes.isEmpty {
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
        VStack(spacing: 12) {
            Spacer()
            LucideIcon.train.sized(28)
                .foregroundStyle(AppTheme.textTertiary)
            Text(String(localized: "lines_no_result"))
                .font(.headline)
                .foregroundStyle(AppTheme.textPrimary)
            Text(String(localized: "lines_no_result_hint"))
                .font(.subheadline)
                .foregroundStyle(AppTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Routes List

    // MARK: - Recent Section

    @ViewBuilder
    private var recentSection: some View {
        if !recentRoutes.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text(String(localized: "recent_searches"))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textTertiary)
                    .padding(.horizontal, 4)

                VStack(spacing: 8) {
                    ForEach(recentRoutes) { route in
                        NavigationLink(value: route) {
                            LineRowContent(route: route, hasMultipleTypes: hasMultipleTransitTypes, store: store)
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
                    Text(String(format: NSLocalizedString("lines_result_count", comment: ""), filteredRoutes.count))
                        .font(.caption)
                        .foregroundStyle(AppTheme.textTertiary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 4)
                }

                ForEach(groupedRoutes, id: \.type) { group in
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

                        // Route rows — always visible when only one type; collapsible otherwise
                        if !hasMultipleTransitTypes || !collapsedTypes.contains(group.type) {
                            VStack(spacing: 8) {
                                ForEach(group.routes) { route in
                                    NavigationLink(value: route) {
                                        LineRowContent(route: route, hasMultipleTypes: hasMultipleTransitTypes, store: store)
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

// MARK: - Line Row Content (glass card)

private struct LineRowContent: View {
    let route: Route
    let hasMultipleTypes: Bool
    let store: ScheduleStore

    private var lineColor: Color {
        // Use luminance to detect very light colors that would be invisible against the card background
        let textOnColor = contrastingTextColor(for: route.color)
        // If white text is needed on this color, the color is dark enough to use as-is
        // If black text is needed, the color is very light — fall back to accent
        return textOnColor == "#FFFFFF" ? Color(hex: route.color) : AppTheme.accent
    }

    var body: some View {
        HStack(spacing: 12) {
            // Line badge
            LineBadge(
                lineName: route.name,
                color: route.color,
                textColor: route.textColor,
                transitType: route.transitType,
                size: .big
            )

            VStack(alignment: .leading, spacing: 2) {
                // Route long name
                Text(route.longName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)

                // Stop sequence marquee — reads from pre-cached store dictionary (O(1))
                if let sequence = store.routeStopSequences[route.id], !sequence.isEmpty {
                    MarqueeText(
                        text: sequence,
                        font: .system(size: 11),
                        foregroundStyle: AppTheme.textSecondary,
                        speed: 28
                    )
                    .frame(maxWidth: .infinity)
                }

                // Subtitle: transit type (only when multiple types) + direction count (only when >1)
                let showTransitType = hasMultipleTypes
                let showDirections = route.directions.count > 1
                if showTransitType || showDirections {
                    HStack(spacing: 6) {
                        if showTransitType {
                            route.transitType.icon.sized(10)
                                .foregroundStyle(AppTheme.textTertiary)
                            Text(route.transitType.displayName)
                                .font(.caption2.weight(.medium))
                                .foregroundStyle(AppTheme.textTertiary)
                        }

                        if showDirections {
                            if showTransitType {
                                Text("·")
                                    .font(.caption2)
                                    .foregroundStyle(AppTheme.textTertiary)
                            }
                            Text("↔ \(route.directions.count)")
                                .font(.caption2.weight(.medium))
                                .foregroundStyle(AppTheme.textTertiary)
                        }
                    }
                }
            }

            Spacer(minLength: 8)

            LucideIcon.chevronRight.image
                .font(.caption.weight(.semibold))
                .foregroundStyle(AppTheme.textTertiary)
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
        .accessibilityLabel(String(format: NSLocalizedString("line_badge_a11y", comment: ""), route.name))
        .accessibilityHint(String(localized: "a11y_hint_show_line_stops"))
        .accessibilityAddTraits(.isButton)
    }
}
