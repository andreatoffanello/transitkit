import SwiftUI
import MapKit
import CoreLocation

/// Map picker fullscreen — pin centrale fisso, l'utente sposta la mappa
/// per posizionarlo. Il nome del posto viene popolato via CLGeocoder reverse
/// geocoding (debounced) ad ogni stop di pan, mostrato in una card flottante
/// compatta in basso con bottone "Conferma".
struct LocationPickerMap: View {
    let onConfirm: (PlannerLocation) -> Void

    @Environment(LocationManager.self) private var locationManager
    @Environment(\.dismiss) private var dismiss
    @Environment(\.operatorConfig) private var config

    @State private var cameraPosition: MapCameraPosition = .automatic
    @State private var currentCenter: CLLocationCoordinate2D = .init(latitude: 0, longitude: 0)
    @State private var resolvedName: String = ""
    @State private var isCameraMoving = false
    @State private var geocodeTask: Task<Void, Never>? = nil
    @State private var moveDebounce: Task<Void, Never>? = nil

    var body: some View {
        ZStack {
            mapView

            centerPin
                .allowsHitTesting(false)

            VStack {
                Spacer()
                HStack {
                    Spacer()
                    if locationManager.location != nil {
                        centerOnUserButton
                    }
                }
                .padding(.trailing, 16)
                .padding(.bottom, confirmCardHeight + 16)

                confirmCard
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
            }
        }
        .navigationTitle(String(localized: "planner_pick_on_map"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .onAppear { initCamera() }
    }

    // MARK: - Map

    private var mapView: some View {
        Map(position: $cameraPosition)
            .mapStyle(.standard(elevation: .flat, pointsOfInterest: .excludingAll))
            .tint(.blue)
            .mapControls { MapCompass() }
            .ignoresSafeArea()
            .onMapCameraChange(frequency: .continuous) { ctx in
                currentCenter = ctx.region.center
                isCameraMoving = true
                moveDebounce?.cancel()
                moveDebounce = Task { @MainActor in
                    try? await Task.sleep(for: .milliseconds(200))
                    guard !Task.isCancelled else { return }
                    isCameraMoving = false
                    UISelectionFeedbackGenerator().selectionChanged()
                    scheduleReverseGeocode(for: currentCenter)
                }
            }
    }

    // MARK: - Pin (dot + stem — Movete style)

    private var centerPin: some View {
        VStack(spacing: 0) {
            // Dot + ring. Halo come background così non influenza la layout
            // del VStack (bug: halo 48×48 dentro ZStack creava gap di 13pt
            // tra dot bottom e stem top).
            ZStack {
                Circle()
                    .fill(AppTheme.accent)
                    .frame(width: 22, height: 22)
                    .shadow(color: .black.opacity(0.25), radius: 4, y: 2)
                Circle()
                    .strokeBorder(.white, lineWidth: 3)
                    .frame(width: 22, height: 22)
            }
            .background(
                Circle()
                    .fill(AppTheme.accent.opacity(isCameraMoving ? 0.15 : 0))
                    .frame(width: 48, height: 48)
                    .blur(radius: 10)
                    .animation(.easeOut(duration: 0.18), value: isCameraMoving)
            )
            .scaleEffect(isCameraMoving ? 1.12 : 1.0)
            .offset(y: isCameraMoving ? -8 : 0)
            .animation(.spring(response: 0.32, dampingFraction: 0.62), value: isCameraMoving)

            // Stem — si allunga di 8pt quando il dot si solleva per mantenere
            // la connessione visiva dot→ground senza gap.
            Rectangle()
                .fill(AppTheme.accent.opacity(0.45))
                .frame(width: 2, height: isCameraMoving ? 20 : 12)
                .animation(.spring(response: 0.32, dampingFraction: 0.62), value: isCameraMoving)

            // Ombra sul terreno — si allarga durante il drag per dare lift
            Ellipse()
                .fill(.black.opacity(0.18))
                .frame(width: isCameraMoving ? 16 : 10, height: 3)
                .blur(radius: 1.5)
                .animation(.spring(response: 0.32, dampingFraction: 0.62), value: isCameraMoving)
        }
        // L'apex del pin (fondo dello stem) deve stare al centro schermo.
        // VStack height a riposo: 22 (dot) + 12 (stem) + 3 (shadow) = 37pt.
        // Centro a 18.5pt, apex a 34pt → offset -15.5 ≈ -16.
        .offset(y: -16)
    }

    // MARK: - Center on user FAB

    private var centerOnUserButton: some View {
        Button {
            guard let loc = locationManager.location else { return }
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            withAnimation(.easeInOut(duration: 0.38)) {
                cameraPosition = .region(MKCoordinateRegion(
                    center: loc.coordinate,
                    span: MKCoordinateSpan(latitudeDelta: 0.012, longitudeDelta: 0.012)
                ))
            }
        } label: {
            Image(systemName: "location.fill")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(AppTheme.accent)
                .frame(width: 44, height: 44)
                .background(.regularMaterial, in: Circle())
                .shadow(color: .black.opacity(0.15), radius: 6, y: 2)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(String(localized: "map_center_on_me"))
        .accessibilityIdentifier("map_picker_center_me")
    }

    // MARK: - Confirm card

    private var confirmCardHeight: CGFloat { 72 }

    private var confirmCard: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(String(localized: "planner_map_selected_location"))
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(AppTheme.textTertiary)
                    .textCase(.uppercase)
                    .kerning(0.3)

                if resolvedName.isEmpty {
                    Text(String(localized: "planner_map_drag_hint"))
                        .font(.system(size: 15))
                        .foregroundStyle(AppTheme.textSecondary)
                } else {
                    Text(resolvedName)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)
                        .transition(.opacity)
                }
            }

            Spacer(minLength: 8)

            Button {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                let displayName = resolvedName.isEmpty
                    ? String(format: "%.5f, %.5f", currentCenter.latitude, currentCenter.longitude)
                    : resolvedName
                onConfirm(.place(name: displayName, coordinate: currentCenter))
                dismiss()
            } label: {
                Text(String(localized: "planner_confirm_location"))
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 10)
                    .background(AppTheme.accent, in: Capsule())
            }
            .buttonStyle(PressableButtonStyle())
            .accessibilityIdentifier("map_picker_confirm")
        }
        .padding(16)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .shadow(color: .black.opacity(0.10), radius: 8, y: 3)
    }

    // MARK: - Init camera

    private func initCamera() {
        if let loc = locationManager.location {
            currentCenter = loc.coordinate
            cameraPosition = .region(MKCoordinateRegion(
                center: loc.coordinate,
                span: MKCoordinateSpan(latitudeDelta: 0.020, longitudeDelta: 0.020)
            ))
        } else if let cfg = config {
            let center = CLLocationCoordinate2D(latitude: cfg.map.centerLat, longitude: cfg.map.centerLng)
            currentCenter = center
            cameraPosition = .region(MKCoordinateRegion(
                center: center,
                span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
            ))
        }
        scheduleReverseGeocode(for: currentCenter)
    }

    // MARK: - Reverse geocoding

    private func scheduleReverseGeocode(for coord: CLLocationCoordinate2D) {
        geocodeTask?.cancel()
        geocodeTask = Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(350))
            guard !Task.isCancelled else { return }
            let loc = CLLocation(latitude: coord.latitude, longitude: coord.longitude)
            let placemarks = try? await CLGeocoder().reverseGeocodeLocation(loc)
            guard !Task.isCancelled else { return }
            if let p = placemarks?.first {
                withAnimation(.easeInOut(duration: 0.15)) {
                    resolvedName = [p.name, p.thoroughfare, p.locality]
                        .compactMap { $0 }
                        .filter { !$0.isEmpty }
                        .first ?? String(format: "%.5f, %.5f", coord.latitude, coord.longitude)
                }
            } else {
                resolvedName = String(format: "%.5f, %.5f", coord.latitude, coord.longitude)
            }
        }
    }
}
