import SwiftUI
import MapKit

/// Line picker bottom sheet.
///
/// Movete-style: custom search field at top, sectioned list by transit type
/// (METRO / TRAM / BUS / FERRY / …), each row shows route color badge + name +
/// live vehicle count. Tapping a row selects the route on the map.
struct LinePickerSheet: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(\.dismiss) private var dismiss

    let onSelect: (APIRoute) -> Void

    @State private var searchText = ""
    @FocusState private var searchFocused: Bool

    /// Section ordering — matches transit hierarchy (metro first, smaller modes last).
    private let sectionOrder: [TransitType] = [
        .metro, .rail, .tram, .monorail, .trolleybus, .bus, .ferry,
        .cable_tram, .gondola, .funicular
    ]

    private var filtered: [APIRoute] {
        guard !searchText.isEmpty else { return store.routes }
        let q = searchText.lowercased()
        return store.routes.filter {
            $0.name.lowercased().contains(q) ||
            ($0.longName ?? "").lowercased().contains(q)
        }
    }

    /// Group by transit type and sort: live count desc, then name asc.
    private var grouped: [(TransitType, [APIRoute])] {
        let byType = Dictionary(grouping: filtered) { $0.resolvedTransitType }
        return sectionOrder.compactMap { type -> (TransitType, [APIRoute])? in
            guard let routes = byType[type], !routes.isEmpty else { return nil }
            let sorted = routes.sorted { a, b in
                let aCount = vehicleStore.liveCount(forRouteId: a.id)
                let bCount = vehicleStore.liveCount(forRouteId: b.id)
                if aCount != bCount { return aCount > bCount }
                return a.name.localizedStandardCompare(b.name) == .orderedAscending
            }
            return (type, sorted)
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                searchField
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 12)

                Divider()

                if grouped.isEmpty {
                    emptyState
                } else {
                    ScrollView {
                        LazyVStack(spacing: 0, pinnedViews: .sectionHeaders) {
                            ForEach(grouped, id: \.0) { type, routes in
                                Section {
                                    ForEach(Array(routes.enumerated()), id: \.element.id) { idx, route in
                                        Button {
                                            onSelect(route)
                                            dismiss()
                                        } label: {
                                            LinePickerRow(
                                                route: route,
                                                vehicleCount: vehicleStore.liveCount(forRouteId: route.id)
                                            )
                                        }
                                        .buttonStyle(.plain)
                                        .accessibilityIdentifier("line_row_\(route.id)")

                                        if idx < routes.count - 1 {
                                            Divider().padding(.leading, 64)
                                        }
                                    }
                                } header: {
                                    sectionHeader(type: type, count: routes.count)
                                }
                            }
                        }
                    }
                }
            }
            .background(Color(.systemBackground))
            .navigationTitle(Text(String(localized: "lines_title")))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "dismiss")) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        // Search field stays unfocused on open — keeps the section browse
        // experience clean; tap to focus when needed.
    }

    // MARK: - Search Field

    private var searchField: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .medium))
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
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 16))
                        .foregroundStyle(.tertiary)
                }
                .accessibilityLabel(Text(String(localized: "a11y_clear_search")))
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Section Header

    private func sectionHeader(type: TransitType, count: Int) -> some View {
        HStack(spacing: 6) {
            type.icon.sized(11)
                .foregroundStyle(Color(hex: colorForTransitType(type)))
            Text(sectionTitle(type))
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
                .textCase(.uppercase)
                .kerning(0.5)
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color(.systemBackground))
    }

    private func sectionTitle(_ type: TransitType) -> String {
        switch type {
        case .metro: return "Metro"
        case .tram: return "Tram"
        case .rail: return "Ferrovie"
        case .bus: return "Bus"
        case .ferry: return "Traghetti"
        case .trolleybus: return "Filobus"
        case .monorail: return "Monorail"
        case .cable_tram: return "Cable Tram"
        case .gondola: return "Funivia"
        case .funicular: return "Funicolare"
        }
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 28))
                .foregroundStyle(.tertiary)
            Text(String(localized: "no_matching_line"))
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.vertical, 48)
    }
}

// MARK: - Row

private struct LinePickerRow: View {
    let route: APIRoute
    let vehicleCount: Int

    var body: some View {
        HStack(spacing: 12) {
            LineBadge(route: route, size: .medium)

            VStack(alignment: .leading, spacing: 2) {
                Text(route.longName ?? route.name)
                    .font(.system(size: 15))
                    .foregroundStyle(.primary)
                    .lineLimit(1)
            }

            Spacer()

            if vehicleCount > 0 {
                HStack(spacing: 4) {
                    Image(systemName: "circle.fill")
                        .font(.system(size: 6))
                        .foregroundStyle(AppTheme.realtimeGreen)
                    Text("\(vehicleCount)")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            // 0 live vehicles: render nothing — cleaner than a placeholder dash.
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(minHeight: 48)
        .contentShape(Rectangle())
    }
}
