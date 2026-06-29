import SwiftUI
import CoreLocation
import MapKit

/// Full-screen location picker for the journey planner (origin or destination).
/// Pushed via NavigationDestination — no sheets.
///
/// Two modes:
/// - `.select` (default): chosen location is returned to the planner as origin/destination.
/// - `.assign(key)`: chosen location is ONLY saved as home/work shortcut; the
///   current trip is not touched. Set/Use are fully decoupled.
///
/// Results are partitioned in two sections:
/// - "Stops" — GTFS stops filtered from `ScheduleStore`
/// - "Places" — addresses/POI from MKLocalSearch (`GeocodingProvider`)
struct LocationPickerView: View {

    // MARK: - Mode

    enum Mode: Equatable {
        /// Choose a location for the current trip.
        case select
        /// Save the chosen location as a home/work shortcut, then pop back.
        case assign(SavedPlaceKey)
    }

    // MARK: - Init

    let isOrigin: Bool
    let excludedStopId: String?
    var mode: Mode = .select
    /// Pre-fills the search field — used by the "Modifica" flow to seed the existing place name.
    var initialQuery: String = ""
    var onSelect: (PlannerLocation) -> Void = { _ in }

    // MARK: - Environment

    @Environment(ScheduleStore.self) private var store
    @Environment(LocationManager.self) private var locationManager
    @Environment(SavedPlacesStore.self) private var savedPlacesStore
    @Environment(\.dismiss) private var dismiss
    @Environment(\.operatorConfig) private var config

    // MARK: - State

    @State private var query = ""
    @State private var addressResults: [GeocodeResult] = []
    @State private var isSearching = false
    @State private var searchTask: Task<Void, Never>? = nil
    @State private var showMapPicker = false
    /// Stores the LocationPickerMap result across the pop animation boundary.
    /// Consumed in `.onChange(of: showMapPicker)` once the map screen has dismissed.
    @State private var mapPickedLocation: PlannerLocation? = nil
    @FocusState private var fieldFocused: Bool
    /// When non-nil, a dedicated assign sub-screen is pushed for this key.
    @State private var assignKey: SavedPlaceKey? = nil

    private static let geocodingProvider = GeocodingProvider()

    // MARK: - Computed

    private var isAssigning: Bool {
        if case .assign = mode { return true }
        return false
    }

    private var navigationTitle: String {
        switch mode {
        case .assign(let key):
            return key == .home
                ? String(localized: "planner_picker_assign_home_title")
                : String(localized: "planner_picker_assign_work_title")
        case .select:
            return isOrigin
                ? String(localized: "planner_search_from_title")
                : String(localized: "planner_search_to_title")
        }
    }

    private var candidates: [ResolvedStop] {
        store.stops.filter { $0.id != excludedStopId }
    }

    private var filteredStops: [ResolvedStop] {
        let q = query.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return [] }
        let base = candidates.filter { $0.name.localizedCaseInsensitiveContains(q) }
        guard let loc = locationManager.location else {
            return base.sorted { $0.name < $1.name }
        }
        return base.sorted { a, b in
            hypot(a.lat - loc.coordinate.latitude, a.lng - loc.coordinate.longitude) <
            hypot(b.lat - loc.coordinate.latitude, b.lng - loc.coordinate.longitude)
        }
    }

    private var nearbyStops: [ResolvedStop] {
        guard let loc = locationManager.location else { return [] }
        return candidates
            .map { ($0, hypot($0.lat - loc.coordinate.latitude, $0.lng - loc.coordinate.longitude)) }
            .sorted { $0.1 < $1.1 }
            .prefix(5)
            .map { $0.0 }
    }

    // MARK: - Body

    var body: some View {
        VStack(spacing: 0) {
            searchField
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 10)

            Divider()

            if query.trimmingCharacters(in: .whitespaces).isEmpty {
                quickChoicesSection
            } else {
                searchResultsSection
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle(navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .onAppear {
            if !initialQuery.isEmpty && query.isEmpty {
                query = initialQuery
                scheduleSearch(for: initialQuery)
            }
        }
        .navigationDestination(isPresented: $showMapPicker) {
            LocationPickerMap { picked in
                mapPickedLocation = picked
            }
        }
        .navigationDestination(item: $assignKey) { key in
            // Dedicated sub-screen: same UI, but only saves the shortcut and pops.
            // Pre-seed the query with the existing place name so "Modifica" starts populated.
            LocationPickerView(
                isOrigin: isOrigin,
                excludedStopId: excludedStopId,
                mode: .assign(key),
                initialQuery: savedPlacesStore.savedPlace(key)?.name ?? ""
            )
        }
        .onChange(of: showMapPicker) { _, isShowing in
            guard !isShowing, let picked = mapPickedLocation else { return }
            mapPickedLocation = nil
            commit(name: picked.name, coordinate: picked.coordinate, stopGtfsId: picked.stopId, kind: .place)
        }
    }

    // MARK: - Commit

    /// Finalises the selection based on the current mode:
    /// - `.assign`: saves the place shortcut and pops. Trip is not touched.
    /// - `.select`: passes the location to the planner and pops.
    private func commit(name: String, coordinate: CLLocationCoordinate2D, stopGtfsId: String?, kind: PlannerLocation.Kind) {
        switch mode {
        case .assign(let key):
            savePlace(key, name: name, coordinate: coordinate, isGPS: kind == .userLocation)
        case .select:
            onSelect(PlannerLocation(kind: kind, name: name, coordinate: coordinate, stopId: stopGtfsId))
        }
        dismiss()
    }

    /// Saves the home/work shortcut. When the source is GPS ("Posizione attuale"),
    /// saves a placeholder immediately and then background-resolves the real address
    /// via CLGeocoder — so the row shows a street name, not "Current location".
    private func savePlace(_ key: SavedPlaceKey, name: String, coordinate: CLLocationCoordinate2D, isGPS: Bool) {
        savedPlacesStore.setPlace(key, name: name, coordinate: coordinate)
        guard isGPS else { return }
        // Capture store reference — Task outlives the dismiss.
        let store = savedPlacesStore
        Task { @MainActor in
            let loc = CLLocation(latitude: coordinate.latitude, longitude: coordinate.longitude)
            let placemarks = try? await CLGeocoder().reverseGeocodeLocation(loc)
            if let p = placemarks?.first {
                let resolved = [p.name, p.thoroughfare, p.locality]
                    .compactMap { $0 }
                    .filter { !$0.isEmpty }
                    .first ?? name
                if !resolved.isEmpty {
                    store.setPlace(key, name: resolved, coordinate: coordinate)
                }
            }
        }
    }

    // MARK: - Search field

    private var searchField: some View {
        HStack(spacing: 8) {
            LucideIcon.search.sized(16)
                .foregroundStyle(AppTheme.textTertiary)

            TextField(String(localized: "search_stop_placeholder"), text: $query)
                .focused($fieldFocused)
                .textInputAutocapitalization(.sentences)
                .autocorrectionDisabled()
                .submitLabel(.search)
                .onChange(of: query) { _, newValue in
                    scheduleSearch(for: newValue)
                }

            if !query.isEmpty {
                Button {
                    query = ""
                    addressResults = []
                    isSearching = false
                    searchTask?.cancel()
                } label: {
                    LucideIcon.circleX.sized(16)
                        .foregroundStyle(AppTheme.textTertiary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: - Quick choices (empty query)

    private var quickChoicesSection: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 8) {
                // Home/Work shortcuts: only shown in .select mode.
                // In .assign mode, we're already on the "set shortcut" screen —
                // showing them again would be recursive and confusing.
                if !isAssigning {
                    savedPlaceRow(.home)
                    savedPlaceRow(.work)
                }

                myLocationRow
                mapPickRow

                if !nearbyStops.isEmpty {
                    sectionLabel(String(localized: "nearby_you"))
                        .padding(.top, 8)
                        .padding(.horizontal, 4)
                    stopList(nearbyStops)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 32)
        }
    }

    // MARK: - Saved place row (Home / Work)

    @ViewBuilder
    private func savedPlaceRow(_ key: SavedPlaceKey) -> some View {
        let saved = savedPlacesStore.savedPlace(key)
        let title = key == .home
            ? String(localized: "planner_picker_home")
            : String(localized: "planner_picker_work")
        let icon: LucideIcon = key == .home ? .house : .briefcase
        let subtitle = saved?.name ?? String(localized: "planner_picker_set_address")

        HStack(spacing: 0) {
            // Main tap area: use in trip (if set) or push assign screen (if empty)
            Button {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                if let saved {
                    commit(name: saved.name, coordinate: saved.coordinate, stopGtfsId: nil, kind: .place)
                } else {
                    assignKey = key
                }
            } label: {
                HStack(spacing: 12) {
                    icon.sized(18)
                        .foregroundStyle(AppTheme.textSecondary)
                        .frame(width: 28)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(AppTheme.textPrimary)
                        Text(subtitle)
                            .font(.system(size: 12))
                            .foregroundStyle(AppTheme.textSecondary)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 8)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            // Trailing affordance: menu (edit/remove) when set, chevron when empty
            if saved != nil {
                Menu {
                    Button {
                        assignKey = key
                    } label: {
                        Label(String(localized: "planner_picker_edit"), systemImage: "pencil")
                    }
                    Button(role: .destructive) {
                        savedPlacesStore.removePlace(key)
                    } label: {
                        Label(String(localized: "planner_picker_remove"), systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 16))
                        .foregroundStyle(AppTheme.textTertiary)
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("picker_saved_\(key.rawValue)_menu")
            } else {
                LucideIcon.chevronRight.sized(14)
                    .foregroundStyle(AppTheme.textTertiary)
                    .frame(width: 36, height: 44)
            }
        }
        .padding(.horizontal, 14)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .accessibilityIdentifier("picker_saved_\(key.rawValue)")
    }

    // MARK: - My location row

    private var myLocationRow: some View {
        let hasLocation = locationManager.location != nil
        return Button {
            guard let loc = locationManager.location else { return }
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            commit(
                name: String(localized: "planner_my_location"),
                coordinate: loc.coordinate,
                stopGtfsId: nil,
                kind: .userLocation
            )
        } label: {
            quickChoiceCell(
                icon: LucideIcon.navigation.sized(18).foregroundStyle(AppTheme.accent).eraseToAnyView(),
                title: String(localized: "planner_my_location"),
                subtitle: hasLocation
                    ? String(localized: "planner_location_active")
                    : String(localized: "planner_location_unavailable")
            )
        }
        .buttonStyle(PressableButtonStyle())
        .disabled(!hasLocation)
        .opacity(hasLocation ? 1 : 0.4)
        .accessibilityIdentifier("picker_my_location")
    }

    // MARK: - Map pick row

    private var mapPickRow: some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showMapPicker = true
        } label: {
            quickChoiceCell(
                icon: LucideIcon.mapPin.sized(18).foregroundStyle(AppTheme.textSecondary).eraseToAnyView(),
                title: String(localized: "planner_pick_on_map"),
                subtitle: String(localized: "planner_pick_on_map_subtitle")
            )
        }
        .buttonStyle(PressableButtonStyle())
        .accessibilityIdentifier("picker_map_pick")
    }

    private func quickChoiceCell(icon: AnyView, title: String, subtitle: String) -> some View {
        HStack(spacing: 12) {
            icon.frame(width: 28)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(AppTheme.textPrimary)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(AppTheme.textSecondary)
            }
            Spacer()
            LucideIcon.chevronRight.sized(14)
                .foregroundStyle(AppTheme.textTertiary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: - Search results (partitioned: Stops + Places)

    @ViewBuilder
    private var searchResultsSection: some View {
        if isSearching && addressResults.isEmpty && filteredStops.isEmpty {
            HStack(spacing: 8) {
                ProgressView()
                Text(String(localized: "planner_searching"))
                    .font(.system(size: 13))
                    .foregroundStyle(AppTheme.textSecondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 60)
        } else if filteredStops.isEmpty && addressResults.isEmpty {
            VStack(spacing: 12) {
                LucideIcon.search.sized(28)
                    .foregroundStyle(AppTheme.textTertiary)
                Text(String(localized: "planner_no_results"))
                    .font(.system(size: 15))
                    .foregroundStyle(AppTheme.textSecondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 60)
        } else {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    if !filteredStops.isEmpty {
                        sectionLabel(String(localized: "planner_section_stops"))
                            .padding(.horizontal, 16)
                            .padding(.top, 12)
                            .padding(.bottom, 8)
                        stopList(filteredStops)
                            .padding(.horizontal, 16)
                    }
                    if !addressResults.isEmpty {
                        sectionLabel(String(localized: "planner_section_places"))
                            .padding(.horizontal, 16)
                            .padding(.top, 16)
                            .padding(.bottom, 8)
                        placeList(addressResults)
                            .padding(.horizontal, 16)
                    }
                }
                .padding(.top, 8)
                .padding(.bottom, 32)
            }
        }
    }

    // MARK: - Stop list

    @ViewBuilder
    private func stopList(_ stops: [ResolvedStop]) -> some View {
        VStack(spacing: 0) {
            ForEach(Array(stops.enumerated()), id: \.element.id) { index, stop in
                Button {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    commit(
                        name: stop.name,
                        coordinate: CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng),
                        stopGtfsId: stop.id,
                        kind: .stop
                    )
                } label: {
                    stopRow(stop)
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("picker_stop_\(stop.id)")

                if index < stops.count - 1 {
                    Divider().padding(.leading, 52)
                }
            }
        }
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func stopRow(_ stop: ResolvedStop) -> some View {
        HStack(spacing: 12) {
            LucideIcon.signpost.sized(16)
                .foregroundStyle(AppTheme.accent)
                .frame(width: 28, alignment: .center)

            VStack(alignment: .leading, spacing: 3) {
                Text(stop.name)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(1)
                if !stop.lineNames.isEmpty {
                    Text(stop.lineNames.prefix(5).joined(separator: " · "))
                        .font(.system(size: 12))
                        .foregroundStyle(AppTheme.textTertiary)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
        .contentShape(Rectangle())
    }

    // MARK: - Place list (MKLocalSearch results)

    @ViewBuilder
    private func placeList(_ places: [GeocodeResult]) -> some View {
        VStack(spacing: 0) {
            ForEach(Array(places.enumerated()), id: \.element.id) { index, place in
                Button {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    commit(name: place.name, coordinate: place.coordinate, stopGtfsId: nil, kind: .place)
                } label: {
                    placeRow(place)
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("picker_place_\(place.id)")

                if index < places.count - 1 {
                    Divider().padding(.leading, 52)
                }
            }
        }
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func placeRow(_ place: GeocodeResult) -> some View {
        HStack(spacing: 12) {
            LucideIcon.mapPin.sized(16)
                .foregroundStyle(AppTheme.textSecondary)
                .frame(width: 28, alignment: .center)

            VStack(alignment: .leading, spacing: 3) {
                Text(place.name)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(1)
                if let sub = place.subtitle {
                    Text(sub)
                        .font(.system(size: 12))
                        .foregroundStyle(AppTheme.textTertiary)
                        .lineLimit(1)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
        .contentShape(Rectangle())
    }

    // MARK: - Section label

    private func sectionLabel(_ text: String) -> some View {
        Text(text)
            .font(.footnote.weight(.semibold))
            .foregroundStyle(AppTheme.textTertiary)
            .textCase(.uppercase)
            .kerning(0.5)
    }

    // MARK: - Debounced search

    private func scheduleSearch(for text: String) {
        searchTask?.cancel()
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.count >= 2 else {
            addressResults = []
            isSearching = false
            return
        }
        isSearching = true
        let userCoord = locationManager.location?.coordinate
        let center: CLLocationCoordinate2D? = config.map { cfg in
            CLLocationCoordinate2D(latitude: cfg.map.centerLat, longitude: cfg.map.centerLng)
        }
        searchTask = Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(300))
            guard !Task.isCancelled else { return }
            let places = await Self.geocodingProvider.searchAddresses(
                trimmed,
                near: userCoord,
                operatorCenter: center
            )
            guard !Task.isCancelled else { return }
            self.addressResults = places
            self.isSearching = false
        }
    }
}

// MARK: - AnyView helper

private extension View {
    func eraseToAnyView() -> AnyView { AnyView(self) }
}
