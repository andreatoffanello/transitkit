import SwiftUI

/// Track orizzontale di badge linea sopra la mappa espansa — selettore della
/// "vista linea". Nessuna card attorno: track trasparente edge-to-edge, fade
/// orizzontale ai bordi (mask LinearGradient) come affordance di scroll.
/// Selezionato = ring accent esterno (2pt, leggero offset); gli altri badge
/// attenuati a 0.45 quando c'è una selezione attiva.
struct ExpandedMapLineTrack: View {
    let routes: [APIRoute]
    let selectedRouteId: String?
    let onTap: (APIRoute) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(routes) { route in
                    let isSelected = selectedRouteId == route.id
                    let anySelection = selectedRouteId != nil
                    Button {
                        onTap(route)
                    } label: {
                        LineBadge(
                            name: route.name,
                            color: route.color ?? "#666666",
                            textColor: route.textColor ?? "#FFFFFF",
                            transitType: route.resolvedTransitType,
                            size: .medium
                        )
                        .padding(3)
                        .overlay(
                            RoundedRectangle(cornerRadius: 13, style: .continuous)
                                .stroke(AppTheme.accent.opacity(isSelected ? 1 : 0), lineWidth: 2)
                        )
                    }
                    .buttonStyle(.plain)
                    .opacity(!anySelection || isSelected ? 1.0 : 0.45)
                    .animation(.smooth(duration: 0.2), value: selectedRouteId)
                    .accessibilityLabel(route.name)
                    .accessibilityIdentifier("map_line_badge_\(route.id)")
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 6)
        }
        .mask(
            LinearGradient(
                stops: [
                    .init(color: .clear, location: 0.00),
                    .init(color: .black, location: 0.05),
                    .init(color: .black, location: 0.90),
                    .init(color: .clear, location: 1.00),
                ],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
    }
}
