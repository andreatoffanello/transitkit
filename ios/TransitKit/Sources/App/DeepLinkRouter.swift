import SwiftUI

// MARK: - Deep Link Router

/// Holds pending deeplink navigation requests.
/// Parsed by TransitKitApp.onOpenURL, consumed by the relevant tab/view.
///
/// Supported URLs:
///   transitkit://line/<routeId>                    → LineDetailView
///   transitkit://line/<routeId>/map                → LineDetailView → LineMapView (direction 0)
///   transitkit://line/<routeId>/map/<directionId>  → LineDetailView → LineMapView (specific direction)
///   transitkit://stop/<stopId>                     → StopDetailView
///   transitkit://stop/<stopId>/schedule            → StopDetailView → FullScheduleSheet
///   transitkit://trip/<stopId>/<routeId>/<time>    → TripDetailView (e.g. time = "08:30")
@Observable
final class DeepLinkRouter {
    var pendingRoute: APIRoute? = nil
    var pendingStop: ResolvedStop? = nil
    var pendingTrip: TripTarget? = nil
    /// When true, LineDetailView auto-opens its map fullScreenCover.
    var autoOpenMap = false
    /// Direction to pre-select when auto-opening map.
    var pendingDirectionId: Int? = nil
    /// StopId for which to auto-open the FullScheduleSheet.
    var openScheduleForStop: String? = nil
    /// Raw URL queued while store is still loading (cold start).
    var pendingUrl: URL? = nil
}

// MARK: - Trip Target

/// Bundles the two values TripDetailView needs so they can be pushed
/// as a single NavigationPath entry.
struct TripTarget: Hashable {
    let departure: Departure
    let fromStop: ResolvedStop
}
