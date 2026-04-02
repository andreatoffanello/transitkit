import SwiftUI
import MapKit

/// Full stop detail screen: inline 3D map header + hero expand overlay showing departures.
/// THE MOST IMPORTANT VIEW — shows next departures, full schedule by day, line filtering, dock indicators.
struct StopDetailView: View {
    let stop: ResolvedStop
    @Environment(ScheduleStore.self) private var store
    @State private var mapPosition: MapCameraPosition = .automatic
    @State private var showFullSchedule = false
    @State private var showMoreDepartures = false
    @State private var filterLine: String?
    @State private var linesExpanded = false
    @State private var showNotificationsSheet = false
    @Environment(FavoritesManager.self) private var favoritesManager

    @State private var mapExpanded: Bool = false
    @State private var expandedMapPosition: MapCameraPosition = .automatic
    @State private var showMapAppPicker = false

    private let initialVisibleCount = 5

    private var stopCoordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: stop.lat, longitude: stop.lng)
    }

    // MARK: - Departures

    private var upcomingDepartures: [Departure] {
        let deps = store.upcomingDepartures(forStopId: stop.id, limit: 15)
        guard let line = filterLine else { return deps }
        return deps.filter { $0.lineName == line }
    }

    private var allDayGroups: [DayGroup: [Departure]] {
        store.departures(forStopId: stop.id)
    }

    /// Available line names at this stop (for filtering).
    private var availableLines: [String] {
        let deps = store.todayDepartures(forStopId: stop.id)
        var seen = Set<String>()
        return deps.map(\.lineName).filter { seen.insert($0).inserted }
    }

    /// Line badges with route data for the header.
    private var lineBadges: [(name: String, route: Route?)] {
        stop.lineNames.map { name in
            let route = store.routes.first { $0.name == name }
            return (name, route)
        }
    }

    // MARK: - Body

    var body: some View {
        ZStack {
            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    mapHeader
                    stopInlineContent
                }
            }
            .background(AppTheme.background.ignoresSafeArea())

            if mapExpanded {
                expandedMapOverlay
                    .transition(
                        .asymmetric(
                            insertion: .scale(scale: 0.92, anchor: UnitPoint(x: 0.88, y: 0.22))
                                .combined(with: .opacity),
                            removal: .scale(scale: 0.92, anchor: UnitPoint(x: 0.88, y: 0.22))
                                .combined(with: .opacity)
                        )
                    )
                    .zIndex(10)
            }
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .tabBar)
        .toolbar {
            ToolbarItem(placement: .principal) {
                VStack(spacing: 1) {
                    Text(stop.name)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                HStack(spacing: 4) {
                    Button {
                        showNotificationsSheet = true
                    } label: {
                        Image(systemName: "bell")
                            .foregroundStyle(AppTheme.accent)
                    }
                    .accessibilityLabel(String(localized: "stop_notifications"))
                    .accessibilityIdentifier("btn_notifications")

                    Button {
                        favoritesManager.toggle(stop.id)
                    } label: {
                        Image(systemName: favoritesManager.isFavorite(stop.id) ? "star.fill" : "star")
                            .foregroundStyle(AppTheme.accent)
                    }
                    .accessibilityLabel(favoritesManager.isFavorite(stop.id)
                        ? String(localized: "remove_from_favorites")
                        : String(localized: "add_to_favorites"))
                    .accessibilityIdentifier("btn_favorite")
                }
            }
        }
        .sheet(isPresented: $showNotificationsSheet) {
            NavigationStack {
                VStack(spacing: 16) {
                    Text(String(localized: "stop_notifications"))
                        .font(.headline)
                    Text(String(localized: "stop_notifications_placeholder"))
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.textSecondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(AppTheme.background.ignoresSafeArea())
                .navigationTitle(stop.name)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button { showNotificationsSheet = false } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            .presentationDetents([.medium])
        }
        .fullScreenCover(isPresented: $showFullSchedule) {
            FullScheduleSheet(stop: stop)
        }
        .onAppear {
            centerOnStop()
        }
        .onDisappear { }
    }

    // MARK: - Map Header

    @ViewBuilder
    private var mapHeader: some View {
        Map(position: $mapPosition) {
            if stop.docks.isEmpty {
                Annotation(stop.name, coordinate: stopCoordinate) {
                    ZStack {
                        Circle()
                            .fill(AppTheme.accent)
                            .frame(width: 28, height: 28)
                        (stop.transitTypes.first ?? .bus).icon.image
                            .font(.system(size: 13, weight: .bold))
                            .foregroundStyle(.white)
                    }
                    .shadow(color: .black.opacity(0.2), radius: 3, y: 1)
                }
            } else {
                ForEach(stop.docks, id: \.letter) { dock in
                    let coord = CLLocationCoordinate2D(latitude: dock.lat, longitude: dock.lng)
                    Annotation(String(localized: "dock_label \(dock.letter)"), coordinate: coord) {
                        DockPin(letter: dock.letter)
                    }
                }
            }
        }
        .mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
        .frame(height: 190)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .disabled(true)
        .overlay(alignment: .bottomTrailing) {
            Button {
                withAnimation(.spring(response: 0.42, dampingFraction: 0.82)) {
                    mapExpanded = true
                }
            } label: {
                LucideIcon.maximize2.image
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.primary)
                    .frame(width: 32, height: 32)
                    .background(.regularMaterial, in: Capsule())
            }
            .buttonStyle(.plain)
            .padding(10)
            .accessibilityLabel("Espandi mappa")
            .accessibilityIdentifier("btn_expand_map")
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 8)
    }

    // MARK: - Expanded Map Overlay

    @ViewBuilder
    private var expandedMapOverlay: some View {
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
                                (stop.transitTypes.first ?? .bus).icon.image
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundStyle(.white)
                            }
                            .shadow(color: .black.opacity(0.3), radius: 4, y: 2)
                        }
                    } else {
                        ForEach(stop.docks, id: \.letter) { dock in
                            let coord = CLLocationCoordinate2D(latitude: dock.lat, longitude: dock.lng)
                            Annotation(String(localized: "dock_label \(dock.letter)"), coordinate: coord) {
                                DockPin(letter: dock.letter)
                            }
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
                        LucideIcon.x.image
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(.primary)
                            .frame(width: 44, height: 44)
                            .background(.regularMaterial, in: Capsule())
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Chiudi mappa")
                    .accessibilityIdentifier("btn_close_map")
                }
                .padding(.top, geo.safeAreaInsets.top + 16)
                .padding(.trailing, 16)

                // Bottom controls row
                HStack(alignment: .bottom) {
                    // "Apri in mappe" button — bottom leading
                    Button("Apri in mappe") {
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
        .confirmationDialog("Apri in...", isPresented: $showMapAppPicker) {
            Button("Apple Maps") { openInAppleMaps() }
            if UIApplication.shared.canOpenURL(URL(string: "comgooglemaps://")!) {
                Button("Google Maps") { openInGoogleMaps() }
            }
            if UIApplication.shared.canOpenURL(URL(string: "waze://")!) {
                Button("Waze") { openInWaze() }
            }
            Button("Annulla", role: .cancel) { }
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

    // MARK: - Inline Content (below map header)

    private var stopInlineContent: some View {
        let allNext = upcomingDepartures
        let visible = showMoreDepartures ? allNext : Array(allNext.prefix(initialVisibleCount))
        let extraCount = allNext.count - initialVisibleCount

        return VStack(spacing: 0) {
            // Stop name header
            inlineStopHeader

            // Lines section
            linesSection

            // Next departures
            if !visible.isEmpty {
                nextDeparturesSection(visible)

                if !showMoreDepartures && extraCount > 0 {
                    Button {
                        withAnimation(.spring(duration: 0.3)) {
                            showMoreDepartures = true
                        }
                    } label: {
                        HStack(spacing: 4) {
                            LucideIcon.chevronDown.image
                                .font(.system(size: 11, weight: .semibold))
                            Text(String(localized: "show_more \(extraCount)"))
                                .font(.system(size: 13, weight: .medium))
                        }
                        .foregroundStyle(AppTheme.accent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 4)
                }
            } else if filterLine != nil && upcomingDepartures.isEmpty {
                VStack(spacing: 12) {
                    Text(String(localized: "no_departures_for_line \(filterLine ?? "")"))
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.textSecondary)
                        .multilineTextAlignment(.center)
                    Button(String(localized: "show_all_departures")) {
                        withAnimation { filterLine = nil }
                    }
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(AppTheme.accent)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 24)
                .padding(.horizontal, 20)
            } else if !store.isLoading {
                VStack(spacing: 8) {
                    LucideIcon.clock.image
                        .font(.system(size: 28))
                        .foregroundStyle(AppTheme.textTertiary)
                    Text(String(localized: "no_departures"))
                        .font(.system(size: 14))
                        .foregroundStyle(AppTheme.textSecondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 32)
            }

            // Full schedule button
            if !allDayGroups.isEmpty {
                Button {
                    showFullSchedule = true
                } label: {
                    HStack(spacing: 6) {
                        LucideIcon.clock.image
                            .font(.system(size: 14, weight: .semibold))
                        Text(String(localized: "full_schedule"))
                            .font(.system(size: 14, weight: .semibold))
                    }
                    .foregroundStyle(AppTheme.accent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(AppTheme.accent.opacity(0.08))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .accessibilityIdentifier("btn_full_schedule")
                .padding(.horizontal, 20)
                .padding(.top, 4)
                .padding(.bottom, 100)
            }
        }
    }

    // MARK: - Inline Stop Header

    private var inlineStopHeader: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(stop.name)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(AppTheme.textPrimary)

                HStack(spacing: 6) {
                    ForEach(Array(stop.transitTypes).sorted(by: { $0.rawValue < $1.rawValue }), id: \.self) { type in
                        HStack(spacing: 4) {
                            type.icon.image
                                .font(.system(size: 11, weight: .medium))
                            Text(type.displayName)
                                .font(.system(size: 12, weight: .medium))
                        }
                        .foregroundStyle(AppTheme.textSecondary)
                    }
                }
            }

            Spacer()

            Button { openInMaps() } label: {
                LucideIcon.navigation.image
                    .font(.system(size: 18))
                    .foregroundStyle(AppTheme.accent)
                    .frame(width: 44, height: 44)
                    .background(AppTheme.accent.opacity(0.1))
                    .clipShape(Circle())
            }
            .accessibilityLabel(String(localized: "a11y_navigate_to_stop"))
            .accessibilityIdentifier("btn_navigate_sheet")
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 12)
    }

    // MARK: - Lines Section

    private var linesSection: some View {
        let allBadges = lineBadges
        let compactLimit = 8

        return VStack(alignment: .leading, spacing: 0) {
            Button {
                withAnimation(.easeInOut(duration: 0.25)) {
                    linesExpanded.toggle()
                }
            } label: {
                FlowLayout(spacing: 6) {
                    let visible = linesExpanded ? allBadges : Array(allBadges.prefix(compactLimit))
                    ForEach(visible, id: \.name) { badge in
                        LineBadge(
                            lineName: badge.name,
                            color: badge.route?.color ?? "#666666",
                            textColor: badge.route?.textColor ?? "#FFFFFF",
                            transitType: badge.route?.transitType ?? .bus,
                            size: .small
                        )
                    }
                    if allBadges.count > compactLimit {
                        HStack(spacing: 3) {
                            if !linesExpanded {
                                Text("+\(allBadges.count - compactLimit)")
                                    .font(.system(size: 11, weight: .semibold, design: .rounded))
                                    .foregroundStyle(AppTheme.textSecondary)
                            }
                            (linesExpanded ? LucideIcon.x : LucideIcon.chevronDown).image
                                .font(.system(size: 9, weight: .semibold))
                                .foregroundStyle(AppTheme.textTertiary)
                        }
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(AppTheme.textTertiary.opacity(0.12), in: RoundedRectangle(cornerRadius: 4))
                    }
                }
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("btn_expand_lines")
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 16)
    }

    // MARK: - Next Departures

    private func nextDeparturesSection(_ departures: [Departure]) -> some View {
        VStack(spacing: 0) {
            // Section header with line filter
            HStack {
                Text(String(localized: "next_departures"))
                    .font(.headline)
                    .foregroundStyle(AppTheme.textPrimary)
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 8)

            // Line filter chips (if multiple lines)
            if availableLines.count > 1 {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        FilterChip(
                            label: String(localized: "filter_all"),
                            isSelected: filterLine == nil,
                            action: {
                                withAnimation(.smooth(duration: 0.2)) { filterLine = nil }
                            }
                        )
                        .accessibilityIdentifier("btn_filter_all_lines")

                        ForEach(availableLines, id: \.self) { line in
                            let route = store.routes.first { $0.name == line }
                            let hasTodayDepartures = store.todayDepartures(forStopId: stop.id).contains { $0.lineName == line }
                            LineFilterChip(
                                lineName: line,
                                routeColor: route?.color ?? "#666666",
                                isSelected: filterLine == line
                            ) {
                                withAnimation(.smooth(duration: 0.2)) { filterLine = line }
                            }
                            .disabled(!hasTodayDepartures)
                            .opacity(hasTodayDepartures ? 1.0 : 0.4)
                            .accessibilityIdentifier("btn_filter_line_\(line)")
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 10)
                }
            }

            // Departure rows
            VStack(spacing: 0) {
                ForEach(Array(departures.enumerated()), id: \.element.id) { index, dep in
                    let isFirst = index == 0

                    DepartureRowContent(departure: dep, isFirst: isFirst, hasDocks: !stop.docks.isEmpty)
                        .padding(.horizontal, 20)
                        .padding(.vertical, isFirst ? 14 : 10)
                        .contentShape(Rectangle())

                    if index < departures.count - 1 {
                        Divider()
                            .padding(.leading, 20)
                    }
                }
            }
            .background(AppTheme.bgSecondary.opacity(0.4))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
        }
    }

    // MARK: - Helpers

    private func centerOnStop() {
        if stop.docks.count > 1 {
            let lats = stop.docks.map(\.lat)
            let lngs = stop.docks.map(\.lng)
            let center = CLLocationCoordinate2D(
                latitude: (lats.min()! + lats.max()!) / 2,
                longitude: (lngs.min()! + lngs.max()!) / 2
            )
            let spanDeg = max((lats.max()! - lats.min()!) * 5.0, 0.005)
            let distance = spanDeg * 111_000 * 1.3
            mapPosition = .camera(MapCamera(
                centerCoordinate: center,
                distance: max(distance, 500),
                heading: 0,
                pitch: 50
            ))
        } else {
            mapPosition = .camera(MapCamera(
                centerCoordinate: stopCoordinate,
                distance: 400,
                heading: 0,
                pitch: 50
            ))
        }
    }

    private func openInMaps() {
        showMapAppPicker = true
    }

    private func openInAppleMaps() {
        let mapItem = MKMapItem(placemark: MKPlacemark(coordinate: stopCoordinate))
        mapItem.name = stop.name
        mapItem.openInMaps(launchOptions: [
            MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeWalking
        ])
    }

    private func openInGoogleMaps() {
        let url = URL(string: "comgooglemaps://?q=\(stopCoordinate.latitude),\(stopCoordinate.longitude)&zoom=17")!
        UIApplication.shared.open(url)
    }

    private func openInWaze() {
        let url = URL(string: "waze://?ll=\(stopCoordinate.latitude),\(stopCoordinate.longitude)&navigate=false")!
        UIApplication.shared.open(url)
    }
}

// MARK: - Departure Row Content

private struct DepartureRowContent: View {
    let departure: Departure
    let isFirst: Bool
    let hasDocks: Bool

    var body: some View {
        HStack(spacing: 10) {
            // Line badge
            LineBadge(
                lineName: departure.lineName,
                color: departure.color,
                textColor: departure.textColor,
                transitType: departure.transitType,
                size: .small
            )

            // Headsign
            Text(departure.headsign)
                .font(.system(size: 14))
                .foregroundStyle(AppTheme.textSecondary)
                .lineLimit(1)

            Spacer(minLength: 4)

            // Dock indicator
            if hasDocks && !departure.dock.isEmpty {
                DockBadgeView(letter: departure.dock)
            }

            // Time display with urgency coloring and live countdown
            TimelineView(.periodic(from: .now, by: 30)) { _ in
                TimeDisplay(departure: departure)
            }
        }
    }
}

// MARK: - Dock Pin (map)

private struct DockPin: View {
    let letter: String

    var body: some View {
        ZStack {
            Circle()
                .fill(.white)
                .frame(width: 26, height: 26)
            Circle()
                .fill(Color(red: 1.0, green: 0.82, blue: 0.0))
                .frame(width: 22, height: 22)
            Text(letter)
                .font(.system(size: 13, weight: .heavy, design: .rounded))
                .foregroundStyle(.black)
        }
        .shadow(color: .black.opacity(0.2), radius: 3, y: 1)
    }
}

// MARK: - Dock Badge (inline)

struct DockBadgeView: View {
    let letter: String

    var body: some View {
        Text(letter)
            .font(.system(size: 10, weight: .heavy, design: .rounded))
            .foregroundStyle(.black)
            .frame(width: 18, height: 18)
            .background(Color(red: 1.0, green: 0.82, blue: 0.0), in: Circle())
            .accessibilityLabel(String(localized: "dock_label \(letter)"))
    }
}

// MARK: - Full Schedule Sheet

private struct FullScheduleSheet: View {
    let stop: ResolvedStop
    @Environment(ScheduleStore.self) private var store
    @Environment(\.dismiss) private var dismiss
    @State private var selectedDayGroup: DayGroup?
    @State private var filterLine: String?
    @State private var isReady = false

    private var allGroups: [DayGroup: [Departure]] {
        store.departures(forStopId: stop.id)
    }

    private var sortedDayGroups: [DayGroup] {
        allGroups.keys.sorted { $0.id < $1.id }
    }

    private var currentDepartures: [Departure] {
        guard let group = selectedDayGroup else { return [] }
        return allGroups[group] ?? []
    }

    private var filteredDepartures: [Departure] {
        let deps = currentDepartures
        guard let line = filterLine else { return deps }
        return deps.filter { $0.lineName == line }
    }

    private var availableLines: [String] {
        var seen = Set<String>()
        return currentDepartures.map(\.lineName).filter { seen.insert($0).inserted }
    }

    init(stop: ResolvedStop) {
        self.stop = stop
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    daySelector
                    lineFilter
                    if isReady {
                        departuresBoard
                            .transition(.opacity)
                    } else {
                        ProgressView()
                            .tint(AppTheme.accent)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 40)
                    }
                }
            }
            .navigationTitle(stop.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        LucideIcon.circleX.image
                            .font(.system(size: 24))
                            .foregroundStyle(.secondary)
                    }
                    .accessibilityIdentifier("btn_close_schedule")
                }
            }
            .onAppear {
                // Select today's day group or first available
                if selectedDayGroup == nil {
                    selectedDayGroup = sortedDayGroups.first
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                    withAnimation(.easeIn(duration: 0.2)) { isReady = true }
                }
            }
            .onChange(of: selectedDayGroup) {
                isReady = false
                filterLine = nil
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    withAnimation(.easeIn(duration: 0.15)) { isReady = true }
                }
            }
        }
    }

    // MARK: - Day Selector

    @ViewBuilder
    private var daySelector: some View {
        if sortedDayGroups.count > 1 {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(sortedDayGroups) { group in
                        let isSelected = selectedDayGroup == group
                        Button {
                            withAnimation(.smooth(duration: 0.2)) {
                                selectedDayGroup = group
                            }
                        } label: {
                            Text(group.displayLabel)
                                .font(.system(size: 15, weight: isSelected ? .semibold : .regular))
                                .foregroundStyle(isSelected ? .white : AppTheme.textPrimary)
                                .padding(.horizontal, 16)
                                .frame(height: 44)
                                .background(isSelected ? AppTheme.accent : AppTheme.glassFill)
                                .clipShape(Capsule())
                        }
                        .accessibilityIdentifier("btn_day_\(group.id)")
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
            }
        }
    }

    // MARK: - Line Filter

    @ViewBuilder
    private var lineFilter: some View {
        if availableLines.count > 1 {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    FilterChip(
                        label: String(localized: "filter_all_lines"),
                        isSelected: filterLine == nil,
                        action: {
                            withAnimation(.smooth(duration: 0.2)) { filterLine = nil }
                        }
                    )
                    .accessibilityIdentifier("btn_filter_all_lines_schedule")

                    ForEach(availableLines, id: \.self) { line in
                        let route = store.routes.first { $0.name == line }
                        LineFilterChip(
                            lineName: line,
                            routeColor: route?.color ?? "#666666",
                            isSelected: filterLine == line
                        ) {
                            withAnimation(.smooth(duration: 0.2)) { filterLine = line }
                        }
                        .accessibilityIdentifier("btn_filter_line_\(line)")
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 10)
            }
        }
    }

    // MARK: - Departures Board

    private var departuresBoard: some View {
        let departures = filteredDepartures
        let hasDocks = !stop.docks.isEmpty

        return VStack(spacing: 0) {
            if departures.isEmpty {
                VStack(spacing: 8) {
                    LucideIcon.clock.image
                        .font(.system(size: 28))
                        .foregroundStyle(AppTheme.textTertiary)
                    Text(String(localized: "no_departures"))
                        .font(.system(size: 14))
                        .foregroundStyle(AppTheme.textSecondary)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
            } else {
                // Group by hour
                let grouped = Dictionary(grouping: departures) { dep in
                    String(dep.time.prefix(2))
                }
                let hours = grouped.keys.sorted()

                ForEach(hours, id: \.self) { hour in
                    if let hourDeps = grouped[hour] {
                        // Hour separator
                        HStack(spacing: 6) {
                            Text(hour)
                                .font(.system(size: 13, weight: .semibold, design: .monospaced))
                                .foregroundStyle(AppTheme.textTertiary)
                                .frame(width: 24, alignment: .trailing)
                            Rectangle()
                                .fill(AppTheme.separatorLine)
                                .frame(height: 0.5)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 10)
                        .padding(.bottom, 2)

                        // Departure rows
                        ForEach(hourDeps) { dep in
                            HStack(spacing: 10) {
                                Text(dep.time)
                                    .font(.system(size: 15, weight: .medium, design: .monospaced))
                                    .foregroundStyle(AppTheme.textPrimary)
                                    .frame(width: 52, alignment: .leading)

                                LineBadge(departure: dep, size: .small)

                                Text(dep.headsign)
                                    .font(.system(size: 14))
                                    .foregroundStyle(AppTheme.textSecondary)
                                    .lineLimit(1)

                                Spacer()

                                if hasDocks && !dep.dock.isEmpty {
                                    DockBadgeView(letter: dep.dock)
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 6)
                            .accessibilityIdentifier("schedule_dep_\(dep.lineName)_\(dep.time)")
                        }
                    }
                }

                Color.clear.frame(height: 40)
            }
        }
    }
}

// MARK: - Flow Layout

/// Horizontal wrapping layout, re-used for line badges.
struct FlowLayout: Layout {
    var spacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > width && x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }

        return CGSize(width: width, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX && x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: .unspecified)
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
