import SwiftUI

// MARK: - Full Schedule Sheet

struct FullScheduleSheet: View {
    let stop: ResolvedStop
    @Environment(ScheduleStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var selectedDayGroup: DayGroup?
    @State private var filterLine: String?
    @State private var isReady = false

    private var allGroups: [DayGroup: [Departure]] {
        store.fullScheduleDepartures(forStopId: stop.id)
    }

    private var sortedDayGroups: [DayGroup] {
        allGroups.keys.sorted { $0.id < $1.id }
    }

    private var currentDepartures: [Departure] {
        guard let group = selectedDayGroup else { return [] }
        return allGroups[group] ?? []
    }

    private var filteredDepartures: [Departure] {
        let deps = currentDepartures
        guard let line = filterLine else { return deps }
        return deps.filter { $0.lineName == line }
    }

    private var availableLines: [String] {
        var seen = Set<String>()
        return currentDepartures.map(\.lineName).filter { seen.insert($0).inserted }
    }

    init(stop: ResolvedStop) {
        self.stop = stop
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    daySelector
                    lineFilter
                    if isReady {
                        departuresBoard
                            .transition(.opacity)
                    } else {
                        ProgressView()
                            .tint(AppTheme.accent)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 40)
                    }
                }
            }
            .navigationTitle(stop.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    // SheetCloseButton canonico: `circleX` (icona col cerchio
                    // inciso) creava un doppio-cerchio rispetto agli altri
                    // close di sheet.
                    SheetCloseButton { dismiss() }
                        .accessibilityIdentifier("btn_close_schedule")
                }
            }
            .onAppear {
                // Select today's day group or first available
                if selectedDayGroup == nil {
                    selectedDayGroup = sortedDayGroups.first
                }
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 150_000_000)
                    withAnimation(.easeIn(duration: 0.2)) { isReady = true }
                }
            }
            .onChange(of: selectedDayGroup) {
                isReady = false
                filterLine = nil
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 50_000_000)
                    withAnimation(.easeIn(duration: 0.15)) { isReady = true }
                }
            }
        }
    }

    // MARK: - Day Selector

    @ViewBuilder
    private var daySelector: some View {
        if sortedDayGroups.count > 1 {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(sortedDayGroups) { group in
                        let isSelected = selectedDayGroup == group
                        Button {
                            withAnimation(.smooth(duration: 0.2)) {
                                selectedDayGroup = group
                            }
                        } label: {
                            Text(label(for: group))
                                .font(.system(size: 15, weight: isSelected ? .semibold : .medium))
                                .foregroundStyle(isSelected ? .white : AppTheme.textPrimary)
                                .padding(.horizontal, 16)
                                .frame(height: 44)
                                .background {
                                    if isSelected {
                                        Capsule().fill(AppTheme.accent)
                                    } else {
                                        Capsule().fill(AppTheme.glassFill)
                                            .overlay(Capsule().strokeBorder(AppTheme.textPrimary.opacity(0.12), lineWidth: 1))
                                    }
                                }
                        }
                        .accessibilityIdentifier("btn_day_\(group.id)")
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
            }
        }
    }

    /// Disambiguates duplicate labels (typically "Limited service") with a
    /// numeric suffix. Without this, an operator with 3 different
    /// limited-service patterns would show 3 identical chips.
    private func label(for group: DayGroup) -> String {
        let base = group.displayLabel
        let duplicates = sortedDayGroups.filter { $0.displayLabel == base }
        guard duplicates.count > 1 else { return base }
        let position = (duplicates.firstIndex(of: group) ?? 0) + 1
        return "\(base) · \(position)"
    }

    // MARK: - Line Filter

    @ViewBuilder
    private var lineFilter: some View {
        if availableLines.count > 1 {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    FilterChip(
                        label: String(localized: "filter_all_lines"),
                        isSelected: filterLine == nil,
                        action: {
                            withAnimation(.smooth(duration: 0.2)) { filterLine = nil }
                        }
                    )
                    .opacity(filterLine != nil ? 0.35 : 1.0)
                    .animation(.smooth(duration: 0.2), value: filterLine)
                    .accessibilityIdentifier("btn_filter_all_lines_schedule")

                    ForEach(availableLines, id: \.self) { line in
                        let route = store.routes.first { $0.name == line }
                        let isSelected = filterLine == line
                        LineFilterChip(
                            lineName: line,
                            routeColor: route?.color ?? "#666666",
                            isSelected: isSelected
                        ) {
                            withAnimation(.smooth(duration: 0.2)) {
                                filterLine = (filterLine == line) ? nil : line
                            }
                        }
                        .opacity(filterLine != nil && !isSelected ? 0.35 : 1.0)
                        .animation(.smooth(duration: 0.2), value: filterLine)
                        .accessibilityIdentifier("btn_filter_line_\(line)")
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 10)
            }
        }
    }

    // MARK: - Departures Board

    private var departuresBoard: some View {
        let departures = filteredDepartures
        let hasDocks = !stop.docks.isEmpty

        return VStack(spacing: 0) {
            if departures.isEmpty {
                VStack(spacing: 8) {
                    LucideIcon.clock.sized(28)
                        .foregroundStyle(AppTheme.textTertiary)
                    Text(String(localized: "no_departures"))
                        .font(.system(size: 14))
                        .foregroundStyle(AppTheme.textSecondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
            } else {
                // Group by hour
                let grouped = Dictionary(grouping: departures) { dep in
                    String(dep.time.prefix(2))
                }
                let hours = grouped.keys.sorted()

                ForEach(hours, id: \.self) { hour in
                    if let hourDeps = grouped[hour] {
                        // Hour separator
                        HStack(spacing: 6) {
                            ClockTime.styledText(
                                ClockTime.hourHeader(gtfsHour: hour, timeZone: store.operatorTimezone),
                                size: 13, weight: .semibold, color: AppTheme.textTertiary
                            )
                            .frame(width: 46, alignment: .trailing)
                            Rectangle()
                                .fill(AppTheme.separatorLine)
                                .frame(height: 0.5)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 10)
                        .padding(.bottom, 2)

                        // Departure rows
                        ForEach(hourDeps) { dep in
                            HStack(spacing: 10) {
                                ClockTime.styledText(
                                    ClockTime.clock(gtfs: dep.time, timeZone: store.operatorTimezone),
                                    size: 15, weight: .medium, color: AppTheme.textPrimary
                                )
                                .frame(width: 74, alignment: .leading)

                                LineBadge(departure: dep, size: .large)

                                Text(dep.headsign)
                                    .font(.system(size: 14))
                                    .foregroundStyle(AppTheme.textSecondary)
                                    .lineLimit(1)

                                Spacer()

                                if hasDocks && !dep.dock.isEmpty {
                                    DockBadgeView(letter: dep.dock)
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 6)
                            .accessibilityIdentifier("schedule_dep_\(dep.lineName)_\(dep.time)")
                        }
                    }
                }

                Color.clear.frame(height: 40)
            }
        }
    }
}
