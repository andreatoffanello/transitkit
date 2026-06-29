import SwiftUI
import MapKit

/// Line + stop picker, opened from the map.
///
/// Reuses the exact card rows of the Lines and Stops tabs (`LineRowContent` /
/// `StopRowContent`) and the shared `FuzzySearch` scorer, so the search here is
/// identical to the rest of the app — no per-screen drift.
///
/// - Empty query → browse all lines (live-first, then alphabetical).
/// - Typing → fuzzy results: **lines first** (they have precedence on the map),
///   then matching **stops** under a "Fermate" header. Stops only ever surface
///   once the user starts typing.
struct LinePickerSheet: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(\.dismiss) private var dismiss

    let onSelect: (APIRoute) -> Void
    let onSelectStop: (ResolvedStop) -> Void

    @State private var searchText = ""
    @FocusState private var searchFocused: Bool

    private var hasMultipleTransitTypes: Bool {
        Set(store.routes.map { $0.resolvedTransitType }).count > 1
    }

    private var isSearching: Bool { !searchText.isEmpty }

    // MARK: - Filtering

    /// Lines — empty query: live-first then alphabetical. Typing: fuzzy-scored.
    private var lineResults: [APIRoute] {
        guard isSearching else {
            return store.routes.sorted { a, b in
                let la = vehicleStore.liveCount(forRouteId: a.id)
                let lb = vehicleStore.liveCount(forRouteId: b.id)
                if la != lb { return la > lb }
                return a.name.localizedStandardCompare(b.name) == .orderedAscending
            }
        }
        let scored = store.routes.compactMap { route -> (route: APIRoute, score: Int)? in
            let score = max(
                FuzzySearch.score(route.name, query: searchText),
                max(
                    FuzzySearch.score(route.longName ?? "", query: searchText),
                    FuzzySearch.score(route.id, query: searchText)
                )
            )
            return score > 0 ? (route, score) : nil
        }
        return scored
            .sorted { $0.score != $1.score ? $0.score > $1.score : $0.route.name.localizedStandardCompare($1.route.name) == .orderedAscending }
            .map(\.route)
    }

    /// Stops — only surfaced while typing; lines take precedence on the map.
    private var stopResults: [ResolvedStop] {
        guard isSearching else { return [] }
        let scored = store.stops.compactMap { stop -> (stop: ResolvedStop, score: Int)? in
            let score = max(
                FuzzySearch.score(stop.name, query: searchText),
                FuzzySearch.score(stop.id, query: searchText)
            )
            return score > 0 ? (stop, score) : nil
        }
        return scored
            .sorted { $0.score != $1.score ? $0.score > $1.score : $0.stop.name.localizedStandardCompare($1.stop.name) == .orderedAscending }
            .map(\.stop)
    }

    private var hasResults: Bool { !lineResults.isEmpty || !stopResults.isEmpty }

    // MARK: - Body

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                searchField
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 12)

                Divider()

                if hasResults {
                    resultsList
                } else {
                    emptyState
                }
            }
            .background(AppTheme.background)
            .navigationTitle(Text(String(localized: "map_search_title")))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "dismiss")) { dismiss() }
                }
            }
        }
        .presentationDragIndicator(.visible)
    }

    // MARK: - Results

    private var resultsList: some View {
        ScrollView(showsIndicators: false) {
            LazyVStack(spacing: 8) {
                // Lines — when searching alongside stops, label the section.
                if isSearching && !lineResults.isEmpty && !stopResults.isEmpty {
                    sectionHeader(String(localized: "lines_title"))
                }
                ForEach(lineResults) { route in
                    Button {
                        onSelect(route)
                        dismiss()
                    } label: {
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

                // Stops — only present while typing.
                if !stopResults.isEmpty {
                    sectionHeader(String(localized: "stops"))
                        .padding(.top, lineResults.isEmpty ? 0 : 8)
                    ForEach(stopResults) { stop in
                        Button {
                            onSelectStop(stop)
                            dismiss()
                        } label: {
                            StopRowContent(stop: stop, store: store)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("stop_row_\(stop.id)")
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .padding(.bottom, 40)
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.caption.weight(.semibold))
            .foregroundStyle(AppTheme.textTertiary)
            .textCase(.uppercase)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 4)
            .padding(.bottom, 2)
    }

    // MARK: - Search Field

    private var searchField: some View {
        HStack(spacing: 10) {
            LucideIcon.search.sized(15)
                .foregroundStyle(.secondary)
            TextField(String(localized: "map_search_placeholder"), text: $searchText)
                .font(.system(size: 15))
                .focused($searchFocused)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                .accessibilityIdentifier("line_picker_search_field")
            if !searchText.isEmpty {
                Button {
                    searchText = ""
                } label: {
                    LucideIcon.circleX.sized(16)
                        .foregroundStyle(.tertiary)
                }
                .accessibilityLabel(Text(String(localized: "a11y_clear_search")))
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Empty

    private var emptyState: some View {
        EmptyStateView(
            icon: .search,
            title: String(localized: "no_matching_line"),
            subtitle: String(localized: "empty_no_results_subtitle")
        )
    }
}
