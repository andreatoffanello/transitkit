import SwiftUI

/// Main tab container for the Orari (Schedules) tab.
/// Provides a segmented control (Stops / Lines), search bar, and transit type filter chips.
struct OrariTab: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(SearchHistoryStore.self) private var searchHistoryStore
    @Environment(DeepLinkRouter.self) private var router
    @State private var segment: OrariSegment = .stops
    @State private var searchQuery = ""
    @State private var selectedTransitType: TransitType?
    @State private var path = NavigationPath()

    enum OrariSegment: String, CaseIterable {
        case stops
        case lines

        var label: String {
            switch self {
            case .stops: String(localized: "segment_stops")
            case .lines: String(localized: "segment_lines")
            }
        }
    }

    // MARK: - Available transit types (auto-populated from data)

    private var availableTransitTypes: [TransitType] {
        let types = Set(store.routes.map(\.transitType))
        return TransitType.allCases.filter { types.contains($0) }
    }

    var body: some View {
        NavigationStack(path: $path) {
            VStack(spacing: 0) {
                // Segmented picker
                segmentedBar

                // Search bar
                searchBar

                // Transit type filter chips — only meaningful when multiple types are present
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

                // Content
                Group {
                    switch segment {
                    case .stops:
                        StopsListView(
                            searchQuery: searchQuery,
                            transitTypeFilter: selectedTransitType,
                            recentIds: searchHistoryStore.recentStopIds
                        )
                    case .lines:
                        LinesListView(
                            searchQuery: searchQuery,
                            transitTypeFilter: selectedTransitType,
                            recentIds: searchHistoryStore.recentLineIds
                        )
                    }
                }
                .frame(maxHeight: .infinity)
            }
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle(String(localized: "tab_schedules"))
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: ResolvedStop.self) { stop in
                let _ = searchHistoryStore.recordStop(stop.id)
                StopDetailView(stop: stop)
            }
            .navigationDestination(for: Route.self) { route in
                let _ = searchHistoryStore.recordLine(route.id)
                LineDetailView(route: route)
            }
            .navigationDestination(for: TripTarget.self) { target in
                TripDetailView(departure: target.departure, fromStop: target.fromStop, isRoot: true)
            }
            .task {
                if store.stops.isEmpty && !store.isLoading {
                    await store.load()
                }
            }
            .onAppear { consumePending() }
            .onChange(of: router.pendingRoute) { _, _ in consumePending() }
            .onChange(of: router.pendingStop) { _, _ in consumePending() }
            .onChange(of: router.pendingTrip) { _, _ in consumePending() }
        }
    }

    // MARK: - Segmented Bar

    private var segmentedBar: some View {
        Picker(String(localized: "tab_schedules"), selection: $segment) {
            ForEach(OrariSegment.allCases, id: \.self) { seg in
                Text(seg.label).tag(seg)
            }
        }
        .pickerStyle(.segmented)
        .accessibilityIdentifier("segment_picker")
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(.ultraThinMaterial)
        .background(AppTheme.glassFill)
    }

    // MARK: - Search Bar

    private var searchBar: some View {
        HStack(spacing: 8) {
            LucideIcon.search.sized(15)
                .foregroundStyle(AppTheme.textTertiary)

            TextField(
                segment == .stops
                    ? String(localized: "search_stop_placeholder")
                    : String(localized: "search_line_placeholder"),
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
        .padding(.bottom, 8)
    }

    // MARK: - Filter Chips

    // MARK: - Deep Link

    private func consumePending() {
        if let route = router.pendingRoute {
            router.pendingRoute = nil
            path = NavigationPath()
            segment = .lines
            path.append(route)
        } else if let stop = router.pendingStop {
            router.pendingStop = nil
            path = NavigationPath()
            segment = .stops
            path.append(stop)
        } else if let trip = router.pendingTrip {
            router.pendingTrip = nil
            path = NavigationPath()
            segment = .stops
            path.append(trip.fromStop)
            path.append(trip)
        }
    }

    // MARK: - Filter Chips

    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // "All" chip
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

