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
        role == .origin ? "From" : "To"
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
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search stops")
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
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

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(stop.name)
                .font(.system(.body, weight: .semibold))
                .foregroundStyle(.primary)

            HStack(spacing: 4) {
                ForEach(stop.lineNames.prefix(5), id: \.self) { line in
                    Text(line)
                        .font(.system(size: 11, weight: .bold))
                        .padding(.horizontal, 5)
                        .padding(.vertical, 2)
                        .background(Color(.secondarySystemFill))
                        .clipShape(RoundedRectangle(cornerRadius: 3, style: .continuous))
                        .foregroundStyle(.secondary)
                }

                if let loc = location {
                    Spacer()
                    Text(distanceLabel(lat: stop.lat, lng: stop.lng, from: loc))
                        .font(.system(size: 12))
                        .foregroundStyle(.tertiary)
                }
            }
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
    }

    private func distanceLabel(lat: Double, lng: Double, from loc: CLLocation) -> String {
        let d = loc.distance(from: CLLocation(latitude: lat, longitude: lng))
        return d < 1000 ? "\(Int(d)) m" : String(format: "%.1f km", d / 1000)
    }
}
