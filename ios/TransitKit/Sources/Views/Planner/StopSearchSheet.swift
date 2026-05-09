import SwiftUI
import CoreLocation

// MARK: - StopSearchSheet
// Modal stop picker — shows all stops sorted by proximity (or alphabetically
// when location is unavailable), with live text search.

struct StopSearchSheet: View {
    enum Role { case origin, destination }

    let role: Role
    let excludedStopId: String?
    @Binding var selected: ResolvedStop?

    @Environment(ScheduleStore.self) private var store
    @Environment(LocationManager.self) private var locationManager
    @Environment(\.dismiss) private var dismiss

    @State private var query: String = ""

    private var title: String {
        role == .origin
            ? String(localized: "planner_search_from_title")
            : String(localized: "planner_search_to_title")
    }

    private var candidates: [ResolvedStop] {
        store.stops.filter { $0.id != excludedStopId }
    }

    private var filteredStops: [ResolvedStop] {
        let base = query.isEmpty
            ? candidates
            : candidates.filter { $0.name.localizedCaseInsensitiveContains(query) }
        guard let loc = locationManager.location else {
            return base.sorted { $0.name < $1.name }
        }
        return base.sorted { a, b in
            let da = distanceSquared(lat: a.lat, lng: a.lng, from: loc)
            let db = distanceSquared(lat: b.lat, lng: b.lng, from: loc)
            return da < db
        }
    }

    var body: some View {
        NavigationStack {
            List(filteredStops) { stop in
                Button {
                    selected = stop
                    dismiss()
                } label: {
                    StopRow(stop: stop, location: locationManager.location)
                }
                .buttonStyle(.plain)
            }
            .listStyle(.plain)
            .searchable(
                text: $query,
                placement: .navigationBarDrawer(displayMode: .always),
                prompt: Text("search_stop_placeholder")
            )
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "cancel")) { dismiss() }
                }
            }
        }
        .onAppear {
            locationManager.requestPermissionAndStart()
        }
    }

    private func distanceSquared(lat: Double, lng: Double, from loc: CLLocation) -> Double {
        let dlat = lat - loc.coordinate.latitude
        let dlng = lng - loc.coordinate.longitude
        return dlat * dlat + dlng * dlng
    }
}

// MARK: - StopRow

private struct StopRow: View {
    let stop: ResolvedStop
    let location: CLLocation?

    @Environment(ScheduleStore.self) private var store

    /// Out-of-service-area threshold. Beyond this we hide the distance label
    /// — showing "7700 km" when the user is on a different continent (or
    /// has stale GPS) is misleading rather than informative.
    private static let maxDistanceMeters: Double = 50_000

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(stop.name)
                .font(.system(.body, weight: .semibold))
                .foregroundStyle(.primary)

            HStack(spacing: 4) {
                ForEach(stop.lineNames.prefix(5), id: \.self) { name in
                    if let route = store.route(forName: name) {
                        LineBadge(route: route, size: .small)
                    } else {
                        // Fallback for the rare case where the line name
                        // can't be resolved (stale data, transient state).
                        LineBadge(name: name, color: "#8E8E93", textColor: nil, size: .small)
                    }
                }

                if let label = distanceLabel(from: location) {
                    Spacer()
                    Text(label)
                        .font(.system(size: 12))
                        .foregroundStyle(.tertiary)
                }
            }
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
    }

    private func distanceLabel(from loc: CLLocation?) -> String? {
        guard let loc else { return nil }
        let d = loc.distance(from: CLLocation(latitude: stop.lat, longitude: stop.lng))
        guard d < Self.maxDistanceMeters else { return nil }
        return d < 1000 ? "\(Int(d)) m" : String(format: "%.1f km", d / 1000)
    }
}
