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
///   transitkit://map/stop/<stopId>                 → Mappa tab, mostra preview card fermata
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
    /// Stop to show as preview card on the Mappa tab (consumed by MappaTab).
    var pendingMapPreviewStop: ResolvedStop? = nil
    /// Bare request to switch to the Mappa tab (no pre-selected stop/line).
    /// Uses a UUID so repeated requests from the same view trigger observers.
    var pendingMapOpen: UUID? = nil
    /// Vehicle id to show as preview card on the Mappa tab. Consumed by MappaTab.
    var pendingMapPreviewVehicleId: String? = nil
    /// Route id to focus on the Mappa tab (camera fit on polyline, line filter).
    /// Consumed by MappaTab. Used by LineDetail toolbar "open map" button.
    var pendingMapPreviewRouteId: String? = nil
    /// Direction id to apply when focusing a route on the Mappa tab.
    var pendingMapPreviewDirectionId: Int? = nil
    /// Shader playground (dev tool). Set true to present the fullscreen shader view
    /// for visual iteration. Trigger via `transitkit://shader`.
    var showShaderPlayground = false
}

// MARK: - Trip Target

/// Bundles the two values TripDetailView needs so they can be pushed
/// as a single NavigationPath entry.
struct TripTarget: Hashable, Identifiable {
    var id: String { departure.routeId + "_" + (departure.tripId ?? "") + "_" + fromStop.id }
    let departure: Departure
    let fromStop: ResolvedStop
}
