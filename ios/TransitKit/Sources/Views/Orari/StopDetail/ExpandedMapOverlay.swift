import SwiftUI
import MapKit

/// Full-screen immersive map overlay shown when the user expands the compact map header
/// on `StopDetailView`. Usa il componente condiviso `MapExpandedControls` per i FAB.
struct ExpandedMapOverlay: View {
    let stop: ResolvedStop
    @Binding var expandedMapPosition: MapCameraPosition
    @Binding var mapExpanded: Bool

    @State private var is3D: Bool = true
    @State private var currentHeading: Double = 0

    private var stopCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng)
    }

    private var pitch: CGFloat { is3D ? 65 : 0 }

    /// Reset bearing compare solo se la mappa è davvero ruotata.
    /// Soglia 1° per evitare jitter da micro-movimenti di camera.
    private var hasBearingToReset: Bool { abs(currentHeading) > 1.0 }

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .top) {
                Map(position: $expandedMapPosition) {
                    if stop.docks.isEmpty {
                        Annotation(stop.name, coordinate: stopCoordinate) {
                            ZStack {
                                Circle()
                                    .fill(AppTheme.accent)
                                    .frame(width: 32, height: 32)
                                LucideIcon.signpost.sized(15)
                                    .foregroundStyle(.white)
                            }
                            .shadow(color: .black.opacity(0.3), radius: 4, y: 2)
                        }
                    }
                }
                .mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
                .ignoresSafeArea(.all)
                .onMapCameraChange(frequency: .continuous) { ctx in
                    currentHeading = ctx.camera.heading
                }

                // Drag handle strip — swipe down per chiudere.
                Color.clear
                    .frame(maxWidth: .infinity)
                    .frame(height: geo.safeAreaInsets.top + 72)
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 20)
                            .onEnded { value in
                                if value.translation.height > 60 {
                                    collapse()
                                }
                            }
                    )

                MapExpandedControls(
                    is3D: is3D,
                    onToggle3D: toggle3D,
                    showsRecenter: true,
                    onRecenter: recenter,
                    showsResetBearing: hasBearingToReset,
                    onResetBearing: resetBearing,
                    onCollapse: collapse
                )
            }
        }
        .ignoresSafeArea(.all)
        .onAppear { recenter() }
    }

    // MARK: - Actions

    private func recenter() {
        withAnimation(.spring(response: 0.42, dampingFraction: 0.85)) {
            expandedMapPosition = .camera(MapCamera(
                centerCoordinate: stopCoordinate,
                distance: 350,
                heading: 0,
                pitch: pitch
            ))
        }
    }

    private func toggle3D() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        is3D.toggle()
        withAnimation(.spring(response: 0.42, dampingFraction: 0.85)) {
            expandedMapPosition = .camera(MapCamera(
                centerCoordinate: stopCoordinate,
                distance: 350,
                heading: 0,
                pitch: pitch
            ))
        }
    }

    private func resetBearing() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        withAnimation(.spring(response: 0.42, dampingFraction: 0.85)) {
            expandedMapPosition = .camera(MapCamera(
                centerCoordinate: stopCoordinate,
                distance: 350,
                heading: 0,
                pitch: pitch
            ))
        }
    }

    private func collapse() {
        withAnimation(.spring(response: 0.42, dampingFraction: 0.82)) {
            mapExpanded = false
        }
    }
}
