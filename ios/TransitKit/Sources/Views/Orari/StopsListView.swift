import SwiftUI

/// List of all stops, searchable and filterable by transit type.
/// Each row shows the stop name, transit type icon, line badges, and a chevron.
struct StopsListView: View {
    let searchQuery: String
    let transitTypeFilter: TransitType?
    let recentIds: [String]
    @Environment(ScheduleStore.self) private var store
    @State private var appeared = false

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

    // MARK: - Filtering

    private var filteredStops: [ResolvedStop] {
        var result = store.stops

        // Filter by transit type
        if let type = transitTypeFilter {
            result = result.filter { $0.transitTypes.contains(type) }
        }

        // Filter and sort by search query
        if !searchQuery.isEmpty {
            if searchQuery.count >= 2 {
                // Fuzzy-scored filter: compute best score across name and id, drop zero-score items
                let scored = result.compactMap { stop -> (stop: ResolvedStop, score: Int)? in
                    let score = max(
                        fuzzyScore(stop.name, query: searchQuery),
                        fuzzyScore(stop.id, query: searchQuery)
                    )
                    return score > 0 ? (stop, score) : nil
                }
                return scored
                    .sorted { $0.score != $1.score ? $0.score > $1.score : $0.stop.name.localizedStandardCompare($1.stop.name) == .orderedAscending }
                    .map(\.stop)
            } else {
                let query = searchQuery.lowercased()
                result = result.filter { $0.name.lowercased().contains(query) }
                return result.sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
            }
        }

        return result.sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
    }

    /// Recent stops resolved from the store, preserving recency order.
    private var recentStops: [ResolvedStop] {
        guard searchQuery.isEmpty else { return [] }
        return recentIds.compactMap { id in store.stops.first { $0.id == id } }
    }

    /// Group stops by their primary transit type for sectioned display.
    private var groupedStops: [(type: TransitType, stops: [ResolvedStop])] {
        let stops = filteredStops
        // When filtering by a specific type or searching, show flat list
        if transitTypeFilter != nil || !searchQuery.isEmpty {
            return []
        }

        var groups: [(TransitType, [ResolvedStop])] = []
        for type in TransitType.allCases {
            let matching = stops.filter { $0.transitTypes.contains(type) }
            if !matching.isEmpty {
                groups.append((type, matching))
            }
        }
        return groups
    }

    private var showGrouped: Bool {
        transitTypeFilter == nil && searchQuery.isEmpty && groupedStops.count > 1
    }

    // MARK: - Body

    var body: some View {
        if store.isLoading && store.stops.isEmpty {
            loadingState
        } else if filteredStops.isEmpty && recentStops.isEmpty {
            emptyState
        } else {
            stopsScrollView
        }
    }

    // MARK: - Loading

    private var loadingState: some View {
        VStack(spacing: 14) {
            Spacer()
            ProgressView()
                .tint(AppTheme.accent)
            Text(String(localized: "stops_loading"))
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
            LucideIcon.search.image
                .font(.system(size: 28))
                .foregroundStyle(AppTheme.textTertiary)
            Text(String(localized: "stops_no_result"))
                .font(.headline)
                .foregroundStyle(AppTheme.textPrimary)
            Text(String(localized: "stops_no_result_hint"))
                .font(.subheadline)
                .foregroundStyle(AppTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Stops List

    private var stopsScrollView: some View {
        ScrollView(showsIndicators: false) {
            if showGrouped {
                groupedContent
            } else {
                flatContent
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .onAppear {
            withAnimation(.easeOut(duration: 0.3)) { appeared = true }
        }
    }

    // MARK: - Recent Section

    @ViewBuilder
    private var recentSection: some View {
        if !recentStops.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text(String(localized: "recent_searches"))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textTertiary)
                    .padding(.horizontal, 4)

                ForEach(Array(recentStops.enumerated()), id: \.element.id) { index, stop in
                    stopRow(stop: stop, index: index)
                }
            }
            .padding(.bottom, 4)
        }
    }

    private var groupedContent: some View {
        LazyVStack(spacing: 16, pinnedViews: .sectionHeaders) {
            // Recent section shown above grouped content when query is empty
            if !recentStops.isEmpty {
                Section {
                    recentSection
                } header: {
                    EmptyView()
                }
            }

            ForEach(groupedStops, id: \.type) { group in
                Section {
                    ForEach(Array(group.stops.enumerated()), id: \.element.id) { index, stop in
                        stopRow(stop: stop, index: index)
                    }
                } header: {
                    sectionHeader(type: group.type, count: group.stops.count)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .padding(.bottom, 80)
    }

    private var flatContent: some View {
        LazyVStack(spacing: 8) {
            // Recent section — only visible when not actively searching
            recentSection

            // Result count
            if !searchQuery.isEmpty {
                Text(String(localized: "stops_result_count \(filteredStops.count)"))
                    .font(.caption)
                    .foregroundStyle(AppTheme.textTertiary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 4)
                    .padding(.bottom, 4)
            }

            ForEach(Array(filteredStops.enumerated()), id: \.element.id) { index, stop in
                stopRow(stop: stop, index: index)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .padding(.bottom, 80)
    }

    // MARK: - Section Header

    private func sectionHeader(type: TransitType, count: Int) -> some View {
        HStack(spacing: 8) {
            type.icon.image
                .font(.footnote.weight(.semibold))
                .foregroundStyle(AppTheme.textSecondary)
            Text(type.displayName)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(AppTheme.textSecondary)
            Text("\(count)")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(AppTheme.textTertiary)
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(AppTheme.textTertiary.opacity(0.1), in: Capsule())
            Spacer()
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 8)
        .background(AppTheme.background)
    }

    // MARK: - Stop Row

    private func stopRow(stop: ResolvedStop, index: Int) -> some View {
        NavigationLink(value: stop) {
            StopRowContent(stop: stop, store: store)
        }
        .buttonStyle(.plain)
        .opacity(appeared ? 1 : 0)
        .offset(y: appeared ? 0 : 10)
        .animation(
            .timingCurve(0.16, 1, 0.3, 1, duration: 0.3).delay(Double(min(index, 15)) * 0.03),
            value: appeared
        )
        .accessibilityIdentifier("stop_row_\(stop.id)")
    }
}

// MARK: - Stop Row Content (glass card)

private struct StopRowContent: View {
    let stop: ResolvedStop
    let store: ScheduleStore

    /// Primary transit type (for the leading icon).
    private var primaryType: TransitType {
        let priority: [TransitType] = [.ferry, .tram, .metro, .bus]
        for type in priority {
            if stop.transitTypes.contains(type) { return type }
        }
        return stop.transitTypes.first ?? .bus
    }

    /// Line badges: resolve route colors for each line at this stop.
    private var lineBadges: [(name: String, route: Route?)] {
        stop.lineNames.prefix(6).map { name in
            let route = store.routes.first { $0.name == name }
            return (name, route)
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            // Transit type icon
            primaryType.icon.image
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(AppTheme.accent)
                .frame(width: 28, height: 28)
                .background(AppTheme.accent.opacity(0.12), in: RoundedRectangle(cornerRadius: 7))

            VStack(alignment: .leading, spacing: 5) {
                // Stop name
                HStack(spacing: 6) {
                    Text(stop.name)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)

                    // Multi-type indicator
                    if stop.transitTypes.count > 1 {
                        HStack(spacing: 3) {
                            ForEach(Array(stop.transitTypes).sorted(by: { $0.rawValue < $1.rawValue }), id: \.self) { type in
                                type.icon.image
                                    .font(.system(size: 9, weight: .semibold))
                                    .foregroundStyle(AppTheme.textTertiary)
                            }
                        }
                    }
                }

                // Line badges (first 6)
                if !stop.lineNames.isEmpty {
                    HStack(spacing: 4) {
                        ForEach(lineBadges, id: \.name) { badge in
                            LineBadge(
                                lineName: badge.name,
                                color: badge.route?.color ?? "#666666",
                                textColor: badge.route?.textColor ?? "#FFFFFF",
                                transitType: badge.route?.transitType ?? primaryType,
                                size: .small
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
        .accessibilityLabel(String(format: NSLocalizedString("a11y_stop_lines_count", comment: "Accessibility label: stop name and line count"), stop.name, stop.lineNames.count))
        .accessibilityAddTraits(.isButton)
    }
}
