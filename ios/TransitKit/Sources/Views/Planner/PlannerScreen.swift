import SwiftUI
import CoreLocation

// MARK: - WhenSelection

enum WhenSelection: Equatable, Hashable {
    case now
    case departAt(Date)
    case arriveBy(Date)
}

// MARK: - PlannerScreen
// Input header (origin / destination / when) + journey results list.
// Origin auto-populates with the nearest stop on appear unless initialOrigin is provided.

struct PlannerScreen: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(ConnectionsStore.self) private var connectionsStore
    @Environment(LocationManager.self) private var locationManager

    @State private var origin: PlannerLocation?
    @State private var destination: PlannerLocation?
    @State private var journeys: [Journey] = []
    @State private var isSearching = false
    @State private var searchError: String? = nil
    @State private var hasSearched = false
    @State private var searchTask: Task<Void, Never>? = nil
    init(initialOrigin: PlannerLocation? = nil, initialDestination: PlannerLocation? = nil, initialWhen: WhenSelection = .now) {
        _origin = State(initialValue: initialOrigin)
        _destination = State(initialValue: initialDestination)
        _whenSelection = State(initialValue: initialWhen)
    }

    @State private var swapRotation: Double = 0
    @State private var whenSelection: WhenSelection
    // Difensivo: role + flag separati per evitare iOS 26 navigationDestination glitch
    @State private var pickerIsOrigin: Bool = true
    @State private var showPicker = false
    @State private var selectedJourney: Journey? = nil

    var body: some View {
        VStack(spacing: 0) {
            inputHeader
                .padding(16)
                .background(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 8, y: 4)

            resultsList
        }
        .navigationTitle(String(localized: "planner_title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbarRole(.editor)
        .navigationDestination(item: $selectedJourney) { journey in
            JourneyDetailView(
                journey: journey,
                originName: origin?.name,
                destinationName: destination?.name
            )
        }
        .navigationDestination(isPresented: $showPicker) {
            LocationPickerView(
                isOrigin: pickerIsOrigin,
                excludedStopId: pickerIsOrigin ? destination?.stopId : origin?.stopId
            ) { location in
                if pickerIsOrigin { origin = location } else { destination = location }
            }
        }
        .onChange(of: origin) { _, _ in triggerSearch() }
        .onChange(of: destination) { _, _ in triggerSearch() }
        .onChange(of: whenSelection) { _, _ in triggerSearch() }
        .onChange(of: connectionsStore.isReady) { _, ready in if ready { triggerSearch() } }
        .onAppear {
            locationManager.requestPermissionAndStart()
            if origin == nil, let loc = locationManager.location {
                origin = .userLocation(
                    name: String(localized: "planner_my_location"),
                    coordinate: loc.coordinate
                )
            }
            // Se PlannerScreen è stato presentato già con origin+destination
            // (es. via PlannerHomeBox), .onChange(of: origin/destination) non
            // firerà perché i valori sono settati all'init. Triggera qui.
            triggerSearch()
        }
        .onChange(of: locationManager.location) { _, loc in
            if origin == nil, let loc {
                origin = .userLocation(
                    name: String(localized: "planner_my_location"),
                    coordinate: loc.coordinate
                )
            }
        }
    }

    // MARK: - Input Header

    private var inputHeader: some View {
        VStack(spacing: 12) {
            HStack(alignment: .center, spacing: 12) {
                // Origin/destination icon column
                VStack(spacing: 0) {
                    Circle()
                        .fill(AppTheme.accent)
                        .frame(width: 10, height: 10)
                    Rectangle()
                        .fill(Color(.tertiaryLabel))
                        .frame(width: 1.5, height: 28)
                    Circle()
                        .strokeBorder(AppTheme.accent, lineWidth: 2)
                        .frame(width: 10, height: 10)
                }

                // Input fields
                VStack(spacing: 0) {
                    stopField(
                        label: origin?.name ?? String(localized: "planner_from_placeholder"),
                        isFilled: origin != nil,
                        clearAction: { origin = nil },
                        tapAction: {
                            pickerIsOrigin = true
                            showPicker = true
                        }
                    )
                    .accessibilityIdentifier("planner_origin_field")

                    Divider()

                    stopField(
                        label: destination?.name ?? String(localized: "planner_to_placeholder"),
                        isFilled: destination != nil,
                        clearAction: { destination = nil },
                        tapAction: {
                            pickerIsOrigin = false
                            showPicker = true
                        }
                    )
                    .accessibilityIdentifier("planner_dest_field")
                }

                // Swap button
                Button { swapStops() } label: {
                    LucideIcon.arrowUpDown.sized(16)
                        .foregroundStyle(AppTheme.accent)
                        .rotationEffect(.degrees(swapRotation))
                        .frame(width: 36, height: 36)
                        .background(Color(.tertiarySystemFill))
                        .clipShape(Circle())
                }
                .accessibilityIdentifier("planner_swap_btn")
                .sensoryFeedback(.impact(weight: .light), trigger: swapRotation)
            }
            .padding(14)
            .background(Color(.secondarySystemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

            // When chips: Adesso/Parti alle/Arriva entro + (time, date) inline.
            HStack {
                Spacer()
                WhenChipsRow(selection: $whenSelection)
            }
        }
    }

    @ViewBuilder
    private func stopField(
        label: String, isFilled: Bool,
        clearAction: @escaping () -> Void,
        tapAction: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 8) {
            Button(action: tapAction) {
                HStack {
                    Text(label)
                        .font(.system(size: 15, weight: isFilled ? .semibold : .regular))
                        .foregroundStyle(isFilled ? .primary : Color(.placeholderText))
                        .lineLimit(1)
                    Spacer(minLength: 0)
                }
                .padding(.vertical, 10)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if isFilled {
                Button(action: clearAction) {
                    LucideIcon.circleX.sized(16)
                        .foregroundStyle(.tertiary)
                        .frame(width: 28, height: 28)
                        .contentShape(Circle())
                }
                .buttonStyle(.plain)
            }
        }
    }

    // MARK: - Results

    private var resultsList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                switch connectionsStore.state {
                case .unavailable(let msg):
                    plannerUnavailableView(message: msg, showSpinner: false)
                default:
                    if isSearching {
                        ProgressView()
                            .padding(.top, 40)
                            .frame(maxWidth: .infinity)
                    } else if let error = searchError {
                        errorView(message: error)
                    } else if hasSearched && journeys.isEmpty {
                        emptyView
                    } else if !journeys.isEmpty {
                        let maxDur = journeys
                            .map { Int($0.arrivalTime.timeIntervalSince($0.departureTime)) }
                            .max() ?? 1

                        ForEach(Array(journeys.enumerated()), id: \.element.id) { idx, journey in
                            Button { selectedJourney = journey } label: {
                                JourneyCard(journey: journey, maxJourneyDurationSeconds: maxDur)
                            }
                            .buttonStyle(PlannerCardPressStyle())
                            .accessibilityIdentifier("journey_card_\(idx)")
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
        }
    }

    private var emptyView: some View {
        EmptyStateView(icon: .route, title: String(localized: "planner_no_trips"))
    }

    private func errorView(message: String) -> some View {
        EmptyStateView(icon: .alertTriangle, title: message, tint: .orange)
    }

    @ViewBuilder
    private func plannerUnavailableView(message: String, showSpinner: Bool) -> some View {
        if showSpinner {
            VStack(spacing: 12) {
                ProgressView().padding(.top, 40)
                Text(message)
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            .frame(maxWidth: .infinity)
        } else {
            EmptyStateView(icon: .map, title: message)
        }
    }

    // MARK: - Actions

    private func swapStops() {
        withAnimation(.spring(response: 0.42, dampingFraction: 0.72)) {
            let tmp = origin; origin = destination; destination = tmp
            swapRotation += 180
        }
    }

    /// Risolve una `PlannerLocation` in una `PlannerStop` per il routing engine.
    /// - `.stop` → usa direttamente i campi del stop
    /// - `.place` / `.userLocation` → snap alla fermata GTFS più vicina al coordinate
    /// Ritorna `nil` se non c'è nessuna fermata caricata.
    private func resolveStop(_ location: PlannerLocation) -> PlannerStop? {
        if location.kind == .stop,
           let stopId = location.stopId,
           let s = store.stops.first(where: { $0.id == stopId }) {
            return PlannerStop(id: s.id, name: location.name, lat: s.lat, lng: s.lng)
        }
        let lat = location.coordinate.latitude
        let lng = location.coordinate.longitude
        guard let nearest = store.stops.min(by: {
            hypot($0.lat - lat, $0.lng - lng) < hypot($1.lat - lat, $1.lng - lng)
        }) else { return nil }
        return PlannerStop(id: nearest.id, name: location.name, lat: nearest.lat, lng: nearest.lng)
    }

    @MainActor
    private func triggerSearch() {
        guard let o = origin, let d = destination else {
            searchTask?.cancel(); searchTask = nil
            journeys = []; searchError = nil; hasSearched = false
            return
        }
        guard o.id != d.id else {
            searchTask?.cancel(); searchTask = nil
            journeys = []
            searchError = String(localized: "planner_same_stop")
            hasSearched = true
            return
        }
        guard connectionsStore.isReady else { return }
        guard let op = resolveStop(o), let dp = resolveStop(d) else { return }
        guard op.id != dp.id else {
            // Coordinate diverse ma stessa fermata GTFS più vicina: caso degenere.
            searchTask?.cancel(); searchTask = nil
            journeys = []
            searchError = String(localized: "planner_same_stop")
            hasSearched = true
            return
        }

        searchError = nil; isSearching = true; hasSearched = true
        let when = whenSelection
        searchTask?.cancel()
        searchTask = Task { @MainActor in
            let results: [Journey]
            switch when {
            case .now:
                results = await connectionsStore.query(origin: op, destination: dp, after: Date())
            case .departAt(let date):
                results = await connectionsStore.query(origin: op, destination: dp, after: date)
            case .arriveBy(let date):
                results = await connectionsStore.queryArriveBy(origin: op, destination: dp, before: date)
            }
            guard !Task.isCancelled else { return }
            journeys = results; isSearching = false
        }
    }
}

// MARK: - Button Style

private struct PlannerCardPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(.spring(response: 0.2, dampingFraction: 0.8), value: configuration.isPressed)
    }
}
