import SwiftUI
import MapKit

/// Full-screen immersive map overlay shown when the user expands the compact map header
/// on `StopDetailView`. State is owned by the parent; passed in via bindings/closures.
struct ExpandedMapOverlay: View {
    let stop: ResolvedStop
    @Binding var expandedMapPosition: MapCameraPosition
    @Binding var mapExpanded: Bool
    @Binding var showMapAppPicker: Bool
    let openInAppleMaps: () -> Void
    let openInGoogleMaps: () -> Void
    let openInWaze: () -> Void

    private var stopCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng)
    }

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .top) {
                // Full-screen map
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

                // Drag handle strip — sits above the map, captures swipe-down
                Color.clear
                    .frame(maxWidth: .infinity)
                    .frame(height: geo.safeAreaInsets.top + 72)
                    .contentShape(Rectangle())
                    .gesture(
                        DragGesture(minimumDistance: 20)
                            .onEnded { value in
                                if value.translation.height > 60 {
                                    withAnimation(.spring(response: 0.42, dampingFraction: 0.82)) {
                                        mapExpanded = false
                                    }
                                }
                            }
                    )

                // Close button — anchored to safe area top
                HStack {
                    Spacer()
                    Button {
                        withAnimation(.spring(response: 0.42, dampingFraction: 0.82)) {
                            mapExpanded = false
                        }
                    } label: {
                        LucideIcon.x.sized(14)
                            .foregroundStyle(.primary)
                            .frame(width: 44, height: 44)
                            .background(.regularMaterial, in: Capsule())
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text(String(localized: "a11y_close_map")))
                    .accessibilityIdentifier("btn_close_map")
                }
                .padding(.top, geo.safeAreaInsets.top + 16)
                .padding(.trailing, 16)

                // Bottom controls row
                HStack(alignment: .bottom) {
                    // "Apri in mappe" button — bottom leading
                    Button(String(localized: "open_in_maps")) {
                        showMapAppPicker = true
                    }
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.primary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(.regularMaterial, in: Capsule())
                    .padding(.bottom, geo.safeAreaInsets.bottom + 16)
                    .padding(.leading, 16)

                    Spacer()

                    // Map controls — bottom trailing
                    VStack(spacing: 8) {
                        // Reset north / bearing
                        Button {
                            withAnimation {
                                expandedMapPosition = .camera(MapCamera(
                                    centerCoordinate: stopCoordinate,
                                    distance: 350,
                                    heading: 0,
                                    pitch: 65
                                ))
                            }
                        } label: {
                            Image(systemName: "location.north.fill")
                                .font(.system(size: 14))
                                .frame(width: 40, height: 40)
                                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 8))
                        }
                        .accessibilityLabel(String(localized: "reset_map_view"))

                        // Re-center on stop
                        Button {
                            withAnimation {
                                expandedMapPosition = .camera(MapCamera(
                                    centerCoordinate: stopCoordinate,
                                    distance: 350,
                                    heading: 0,
                                    pitch: 65
                                ))
                            }
                        } label: {
                            Image(systemName: "scope")
                                .font(.system(size: 14))
                                .frame(width: 40, height: 40)
                                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 8))
                        }
                        .accessibilityLabel(String(localized: "center_on_location"))
                    }
                    .padding(.bottom, geo.safeAreaInsets.bottom + 16)
                    .padding(.trailing, 16)
                }
                .frame(maxHeight: .infinity, alignment: .bottom)
            }
        }
        .ignoresSafeArea(.all)
        .confirmationDialog(Text(String(localized: "open_in_prompt")), isPresented: $showMapAppPicker) {
            Button("Apple Maps") { openInAppleMaps() }
            if UIApplication.shared.canOpenURL(URL(string: "comgooglemaps://")!) {
                Button("Google Maps") { openInGoogleMaps() }
            }
            if UIApplication.shared.canOpenURL(URL(string: "waze://")!) {
                Button("Waze") { openInWaze() }
            }
            Button(String(localized: "cancel"), role: .cancel) { }
        }
        .onAppear {
            expandedMapPosition = .camera(MapCamera(
                centerCoordinate: stopCoordinate,
                distance: 350,
                heading: 0,
                pitch: 65
            ))
        }
    }
}
