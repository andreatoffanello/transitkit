import SwiftUI

// MARK: - Deep Link Router

/// Holds pending deeplink navigation requests.
/// Parsed by `TransitKitApp.resolve(url:store:)`, consumed by the relevant tab/view.
///
/// Supported URLs:
///   transitkit://home                              → switch to Home tab
///   transitkit://schedules (alias: orari)          → switch to Stops tab
///   transitkit://lines (alias: linee)              → switch to Lines tab
///   transitkit://map                               → switch to Map tab
///   transitkit://alerts (alias: avvisi)            → switch to Alerts tab
///   transitkit://favorites                         → Home tab, focus favorites section
///   transitkit://settings[/notifications]          → present Settings sheet (optional anchor)
///   transitkit://servizi[/<serviceId>]             → present Servizi sheet (optional service detail)
///   transitkit://planner[?from=&to=&when=]         → present Planner (optional prefilled origin/dest/time)
///   transitkit://search?q=<text>[&scope=stops|lines] → switch tab + prefill search
///   transitkit://line/<routeId>                    → push LineDetailView
///   transitkit://line/<routeId>/map[/<directionId>] → push LineDetail then focus polyline on Map tab
///   transitkit://stop/<stopId>                     → push StopDetailView
///   transitkit://stop/<stopId>/schedule            → StopDetailView with FullScheduleSheet open
///   transitkit://trip/<stopId>/<routeId>/<HH:MM>   → push TripDetailView
///   transitkit://map/stop/<stopId>                 → Map tab + stop preview card
///   transitkit://map/vehicle/<vehicleId>           → Map tab + vehicle preview card
///   transitkit://map/route/<routeId>[/<directionId>] → Map tab + focus polyline
///   transitkit://alert/<alertId>                   → Alerts tab + push AlertDetailView
///   transitkit://shader                            → ShaderPlaygroundView (dev only)
@Observable
final class DeepLinkRouter {
    // MARK: Navigation (existing)
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

    // MARK: Tab switching
    /// Request a specific tab to be selected. UUID makes repeated requests distinct.
    var pendingTabSwitch: TabSwitch? = nil

    // MARK: Alerts
    /// Push AlertDetailView for this alert id. Consumed by Tab 4 (NavigationStack with AlertListView).
    var pendingAlertId: String? = nil

    // MARK: Home secondary sheets
    /// Trigger the Settings sheet from Home. UUID for repeated trigger.
    var pendingSettingsOpen: UUID? = nil
    /// Optional anchor within Settings (e.g. "notifications") for the consumer to scroll/focus.
    var pendingSettingsAnchor: String? = nil
    /// Trigger the Servizi sheet from Home.
    var pendingServiziOpen: UUID? = nil
    /// Optional service id to push inside Servizi.
    var pendingServiziId: String? = nil
    /// Trigger Onboarding fullScreenCover from Home (dev aid).
    var pendingOnboardingOpen: UUID? = nil

    // MARK: Planner
    /// Open the Planner with optional prefilled origin/destination/when.
    var pendingPlannerLaunch: PendingPlannerLaunch? = nil
    /// Bare open Planner (no prefill).
    var pendingPlannerOpen: UUID? = nil

    // MARK: Search prefill
    /// Search text to prefill into the active tab's search field.
    var pendingSearchQuery: String? = nil
    /// Whether the search applies to stops (Tab 1) or lines (Tab 2).
    var pendingSearchScope: SearchScope = .stops

    // MARK: Focus
    /// Scroll-to-anchor request for the Home favorites section.
    var pendingFocusFavorites: UUID? = nil
}

// MARK: - Tab Switch

/// Request to switch to a specific tab. UUID ensures repeated identical switches
/// (e.g. user invokes the same deeplink twice) still trigger observers.
struct TabSwitch: Equatable, Hashable {
    let index: Int
    let id: UUID
    init(index: Int) {
        self.index = index
        self.id = UUID()
    }
}

// MARK: - Search Scope

enum SearchScope: Equatable, Hashable {
    case stops
    case lines
}

// MARK: - Trip Target

/// Bundles the two values TripDetailView needs so they can be pushed
/// as a single NavigationPath entry.
struct TripTarget: Hashable, Identifiable {
    var id: String { departure.routeId + "_" + (departure.tripId ?? "") + "_" + fromStop.id }
    let departure: Departure
    let fromStop: ResolvedStop
}

// MARK: - Planner Launch

/// Carries optional prefill values for a deep-linked Planner open.
/// Not the same as `PlannerLaunch` (HomeTab's local nav model): a deeplink
/// may have only one of origin/destination set, while `PlannerLaunch` requires both.
struct PendingPlannerLaunch: Identifiable, Hashable {
    let id = UUID()
    let origin: PlannerLocation?
    let destination: PlannerLocation?
    let when: WhenSelection
}
