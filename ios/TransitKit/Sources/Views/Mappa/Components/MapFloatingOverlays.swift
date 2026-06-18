import SwiftUI

// MARK: - Vehicle Card Overlay
//
// Floating card anchored to the bottom of the map, showing the selected
// vehicle's details (route, live position, follow toggle). Split from
// `MappaTab.swift` — behavior-preserving, same paddings/transitions.

struct VehicleCardOverlay: View {
    let vehicle: GtfsRtVehicle
    let route: APIRoute?
    let isFollowing: Bool
    let onToggleFollow: () -> Void
    let onOpenLine: (() -> Void)?
    let onOpenTrip: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        VStack {
            Spacer()
            VehicleDetailSheet(
                vehicle: vehicle,
                route: route,
                isFollowing: isFollowing,
                onToggleFollow: onToggleFollow,
                onOpenLine: onOpenLine,
                onOpenTrip: onOpenTrip,
                onDismiss: onDismiss
            )
            .padding(.horizontal, 12)
            .padding(.bottom, 90) // above tab bar
            .transition(.move(edge: .bottom).combined(with: .opacity))
        }
        .allowsHitTesting(true)
        .ignoresSafeArea(edges: .bottom)
    }
}

// MARK: - Stop Preview Overlay
//
// Floating card anchored to the bottom of the map, previewing a tapped
// stop with next departures. Tapping opens the full StopDetailView.
// Split from `MappaTab.swift` — behavior-preserving.

struct StopPreviewOverlay: View {
    let stop: ResolvedStop
    let onDismiss: () -> Void
    let onOpenStop: () -> Void

    var body: some View {
        VStack {
            Spacer()
            StopPreviewCard(
                stop: stop,
                onDismiss: onDismiss,
                onOpenStop: onOpenStop
            )
            .padding(.horizontal, 12)
            .padding(.bottom, 90)
            .transition(.move(edge: .bottom).combined(with: .opacity))
        }
        .allowsHitTesting(true)
        .ignoresSafeArea(edges: .bottom)
    }
}
