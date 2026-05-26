import SwiftUI

/// Tab container for Orari (Schedules) — fermate only.
/// Lines are now in the dedicated LineeTab.
struct OrariTab: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(SearchHistoryStore.self) private var searchHistoryStore
    @Environment(DeepLinkRouter.self) private var router
    @State private var searchQuery = ""
    @State private var selectedTransitType: TransitType?
    @State private var path = NavigationPath()

    // MARK: - Available transit types (auto-populated from data)

    private var availableTransitTypes: [TransitType] {
        let types = Set(store.stops.flatMap { $0.transitTypes })
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

                // Stops list
                StopsListView(
                    searchQuery: searchQuery,
                    transitTypeFilter: selectedTransitType,
                    recentIds: searchHistoryStore.recentStopIds
                )
                .frame(maxHeight: .infinity)
            }
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle(String(localized: "tab_schedules"))
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: ResolvedStop.self) { stop in
                let _ = searchHistoryStore.recordStop(stop.id)
                StopDetailView(stop: stop)
            }
            .navigationDestination(for: TripTarget.self) { target in
                TripDetailView(departure: target.departure, fromStop: target.fromStop)
            }
            .task {
                if store.stops.isEmpty && !store.isLoading {
                    await store.load()
                }
            }
            .onAppear { consumePending() }
            .onChange(of: router.pendingStop) { _, _ in consumePending() }
            .onChange(of: router.pendingTrip) { _, _ in consumePending() }
            .onChange(of: router.pendingSearchQuery) { _, _ in consumeSearch() }
            .onChange(of: router.pendingSearchScope) { _, _ in consumeSearch() }
        }
    }

    // MARK: - Deep Link

    private func consumePending() {
        if let stop = router.pendingStop {
            router.pendingStop = nil
            path = NavigationPath()
            path.append(stop)
        } else if let trip = router.pendingTrip {
            router.pendingTrip = nil
            path = NavigationPath()
            path.append(trip.fromStop)
            path.append(trip)
        }
        consumeSearch()
    }

    private func consumeSearch() {
        guard router.pendingSearchScope == .stops,
              let query = router.pendingSearchQuery else { return }
        router.pendingSearchQuery = nil
        path = NavigationPath()
        searchQuery = query
    }

    // MARK: - Search Bar

    private var searchBar: some View {
        HStack(spacing: 8) {
            LucideIcon.search.sized(15)
                .foregroundStyle(AppTheme.textTertiary)

            TextField(
                String(localized: "search_stop_placeholder"),
                text: $searchQuery
            )
            .font(.system(.subheadline))
            .foregroundStyle(AppTheme.textPrimary)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)
            .accessibilityIdentifier("search_schedules")

            if !searchQuery.isEmpty {
                Button {
                    searchQuery = ""
                } label: {
                    LucideIcon.circleX.sized(14)
                        .foregroundStyle(AppTheme.textTertiary)
                }
                .accessibilityIdentifier("btn_clear_search")
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

    // MARK: - Filter Chips

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
                .accessibilityIdentifier("filter_all")

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
                    .accessibilityIdentifier("filter_\(type.rawValue)")
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 8)
        }
    }
}
