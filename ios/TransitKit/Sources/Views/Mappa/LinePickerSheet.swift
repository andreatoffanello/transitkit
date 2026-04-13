import SwiftUI
import MapKit

struct LinePickerSheet: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(\.dismiss) private var dismiss

    let onSelect: (APIRoute) -> Void

    @State private var searchText = ""

    private var filtered: [APIRoute] {
        guard !searchText.isEmpty else { return store.routes }
        let q = searchText.lowercased()
        return store.routes.filter {
            $0.name.lowercased().contains(q) ||
            ($0.longName ?? "").lowercased().contains(q)
        }
    }

    // Group by transit type, sorted by live vehicle count desc then name
    private var grouped: [(TransitType, [APIRoute])] {
        let byType = Dictionary(grouping: filtered) { $0.resolvedTransitType }
        return byType.sorted { a, b in
            let aOrder = TransitType.allCases.firstIndex(of: a.key) ?? 99
            let bOrder = TransitType.allCases.firstIndex(of: b.key) ?? 99
            return aOrder < bOrder
        }.map { (type, routes) in
            let sorted = routes.sorted { a, b in
                let aCount = vehicleStore.liveCount(forRouteId: a.id)
                let bCount = vehicleStore.liveCount(forRouteId: b.id)
                if aCount != bCount { return aCount > bCount }
                return a.name < b.name
            }
            return (type, sorted)
        }
    }

    var body: some View {
        NavigationStack {
            List {
                ForEach(grouped, id: \.0) { type, routes in
                    Section {
                        ForEach(routes) { route in
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
                        }
                    } header: {
                        Text(type.displayName)
                    }
                }
            }
            .listStyle(.insetGrouped)
            .searchable(text: $searchText, prompt: "Cerca linea...")
            .navigationTitle("Scegli linea")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Chiudi") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }
}

private struct LinePickerRow: View {
    let route: APIRoute
    let vehicleCount: Int

    var body: some View {
        HStack(spacing: 12) {
            LineBadge(route: route, size: .medium)

            VStack(alignment: .leading, spacing: 2) {
                Text(route.longName ?? route.name)
                    .font(.subheadline)
                    .foregroundStyle(.primary)
                    .lineLimit(1)
            }

            Spacer()

            if vehicleCount > 0 {
                HStack(spacing: 4) {
                    Circle()
                        .fill(AppTheme.realtimeGreen)
                        .frame(width: 7, height: 7)
                    Text("\(vehicleCount)")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.vertical, 2)
    }
}
