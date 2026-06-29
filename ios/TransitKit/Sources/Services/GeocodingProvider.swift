import Foundation
import MapKit
import CoreLocation

// MARK: - GeocodeResult

/// Result kind — stops (GTFS) vs. free addresses/POI.
enum GeocodeResultKind: String {
    case stop
    case place
}

/// Unified geocoding result returned by `GeocodingProvider` (MKLocalSearch) and
/// `ScheduleStore` stop filter. Identifiable for use in SwiftUI ForEach.
struct GeocodeResult: Identifiable, Hashable {
    /// Unique ID: stop_id for `.stop`, lat/lng string for `.place`.
    let id: String
    let kind: GeocodeResultKind
    let name: String
    /// Locality / region subtitle for place results (e.g. "Boone, NC"). Nil for stops.
    let subtitle: String?
    let coordinate: CLLocationCoordinate2D
    /// GTFS stop_id, present only when `kind == .stop`.
    let stopGtfsId: String?

    static func == (lhs: GeocodeResult, rhs: GeocodeResult) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

// MARK: - GeocodingProvider

/// Forward geocoding via `MKLocalSearch` (native MapKit, no token required).
/// Biases results toward the operator's map center when no user location is available.
///
/// The store layer (`ScheduleStore.stops`) provides GTFS stop results separately;
/// this provider handles addresses and POI only (kind `.place`).
///
/// Errors are silently swallowed → callers always get `[]` on failure.
actor GeocodingProvider {

    // MARK: - Search

    /// Forward geocoding: free-text → list of `.place` GeocodeResults.
    /// - Parameters:
    ///   - text: Natural-language query (address, POI name, etc.)
    ///   - near: Bias coordinate. If nil, falls back to `operatorCenter`.
    ///   - operatorCenter: Operator map center loaded from `OperatorConfig`.
    func searchAddresses(
        _ text: String,
        near: CLLocationCoordinate2D?,
        operatorCenter: CLLocationCoordinate2D?
    ) async -> [GeocodeResult] {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else { return [] }

        let biasCoord = near ?? operatorCenter
        let request = MKLocalSearch.Request()
        request.naturalLanguageQuery = trimmed
        request.resultTypes = [.address, .pointOfInterest]
        if let bias = biasCoord {
            // Region span ~35km — tight enough to rank local results first
            // while covering the operator service area.
            request.region = MKCoordinateRegion(
                center: bias,
                latitudinalMeters: 35_000,
                longitudinalMeters: 35_000
            )
        }

        do {
            let search = MKLocalSearch(request: request)
            let response = try await search.start()
            return response.mapItems.compactMap { mapResult($0) }
        } catch {
            #if DEBUG
            print("[GeocodingProvider] MKLocalSearch error: \(error)")
            #endif
            return []
        }
    }

    // MARK: - Mapping

    private func mapResult(_ item: MKMapItem) -> GeocodeResult? {
        let coord = item.placemark.coordinate
        // Reject items with invalid coordinates
        guard coord.latitude.isFinite, coord.longitude.isFinite,
              coord.latitude != 0 || coord.longitude != 0 else { return nil }

        let name = item.name
            ?? [item.placemark.thoroughfare, item.placemark.locality]
                .compactMap { $0 }
                .filter { !$0.isEmpty }
                .joined(separator: ", ")
        guard !name.isEmpty else { return nil }

        // Build locality subtitle: "Boone, NC" or "Boone" or "NC" etc.
        let p = item.placemark
        let subtitle: String? = {
            let parts = [p.locality, p.administrativeArea]
                .compactMap { $0 }
                .filter { !$0.isEmpty }
            return parts.isEmpty ? p.country : parts.joined(separator: ", ")
        }()

        let id = String(format: "%.6f,%.6f", coord.latitude, coord.longitude)
        return GeocodeResult(id: id, kind: .place, name: name, subtitle: subtitle, coordinate: coord, stopGtfsId: nil)
    }
}
