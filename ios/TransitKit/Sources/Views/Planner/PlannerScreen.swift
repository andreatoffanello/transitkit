import SwiftUI
import CoreLocation

// MARK: - WhenSelection

enum WhenSelection: Equatable {
    case now
    case departAt(Date)
    case arriveBy(Date)
}

// MARK: - PlannerScreen
// Input header (origin / destination / when) + journey results list.
// Origin auto-populates with the nearest stop on appear.

struct PlannerScreen: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(ConnectionsStore.self) private var connectionsStore
    @Environment(LocationManager.self) private var locationManager

    @State private var origin: ResolvedStop? = nil
    @State private var destination: ResolvedStop? = nil
    @State private var journeys: [Journey] = []
    @State private var isSearching = false
    @State private var searchError: String? = nil
    @State private var hasSearched = false
    @State private var searchTask: Task<Void, Never>? = nil
    @State private var swapRotation: Double = 0
    @State private var whenSelection: WhenSelection = .now
    @State private var showWhenSheet = false
    @State private var showOriginSearch = false
    @State private var showDestSearch = false
    @State private var selectedJourney: Journey? = nil

    var body: some View {
        VStack(spacing: 0) {
            inputHeader
                .padding(16)
                .background(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 8, y: 4)

            resultsList
        }
        .navigationTitle("Plan a Trip")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarRole(.editor)
        .navigationDestination(item: $selectedJourney) { journey in
            JourneyDetailView(journey: journey)
        }
        .sheet(isPresented: $showOriginSearch) {
            StopSearchSheet(role: .origin, excludedStopId: destination?.id, selected: $origin)
        }
        .sheet(isPresented: $showDestSearch) {
            StopSearchSheet(role: .destination, excludedStopId: origin?.id, selected: $destination)
        }
        .sheet(isPresented: $showWhenSheet) {
            WhenSheet(initial: whenSelection) { sel in
                whenSelection = sel
            }
        }
        .onChange(of: origin) { _, _ in triggerSearch() }
        .onChange(of: destination) { _, _ in triggerSearch() }
        .onChange(of: whenSelection) { _, _ in triggerSearch() }
        .onChange(of: connectionsStore.isReady) { _, ready in if ready { triggerSearch() } }
        .onAppear {
            locationManager.requestPermissionAndStart()
            if origin == nil, let loc = locationManager.location {
                origin = nearestStop(to: loc)
            }
        }
        .onChange(of: locationManager.location) { _, loc in
            if origin == nil, let loc { origin = nearestStop(to: loc) }
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
                        label: origin?.name ?? "From...",
                        isFilled: origin != nil,
                        clearAction: { origin = nil },
                        tapAction: { showOriginSearch = true }
                    )
                    .accessibilityIdentifier("planner_origin_field")

                    Divider()

                    stopField(
                        label: destination?.name ?? "To...",
                        isFilled: destination != nil,
                        clearAction: { destination = nil },
                        tapAction: { showDestSearch = true }
                    )
                    .accessibilityIdentifier("planner_dest_field")
                }

                // Swap button
                Button { swapStops() } label: {
                    Image(systemName: "arrow.up.arrow.down")
                        .font(.system(size: 14, weight: .semibold))
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

            // When chip
            HStack {
                Spacer()
                WhenChip(selection: whenSelection) { showWhenSheet = true }
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
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 14))
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
                case .downloading:
                    plannerUnavailableView(message: "Downloading trip data…", showSpinner: true)
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
        VStack(spacing: 12) {
            Image(systemName: "tram.fill.tunnel")
                .font(.system(size: 32))
                .foregroundStyle(Color(.tertiaryLabel))
                .padding(.top, 40)
            Text("No trips found")
                .font(.system(size: 15))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity)
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 32))
                .foregroundStyle(Color(.tertiaryLabel))
                .padding(.top, 40)
            Text(message)
                .font(.system(size: 15))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity)
    }

    private func plannerUnavailableView(message: String, showSpinner: Bool) -> some View {
        VStack(spacing: 12) {
            if showSpinner {
                ProgressView().padding(.top, 40)
            } else {
                Image(systemName: "map")
                    .font(.system(size: 32))
                    .foregroundStyle(Color(.tertiaryLabel))
                    .padding(.top, 40)
            }
            Text(message)
                .font(.system(size: 14))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Actions

    private func swapStops() {
        withAnimation(.spring(response: 0.42, dampingFraction: 0.72)) {
            let tmp = origin; origin = destination; destination = tmp
            swapRotation += 180
        }
    }

    private func nearestStop(to loc: CLLocation) -> ResolvedStop? {
        store.stops
            .min(by: {
                let da = hypot($0.lat - loc.coordinate.latitude, $0.lng - loc.coordinate.longitude)
                let db = hypot($1.lat - loc.coordinate.latitude, $1.lng - loc.coordinate.longitude)
                return da < db
            })
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
            searchError = "Origin and destination are the same"
            hasSearched = true
            return
        }
        guard connectionsStore.isReady else { return }

        searchError = nil; isSearching = true; hasSearched = true
        let when = whenSelection
        searchTask?.cancel()
        searchTask = Task { @MainActor in
            let results: [Journey]
            switch when {
            case .now:
                results = await connectionsStore.query(originId: o.id, destId: d.id, after: Date())
            case .departAt(let date):
                results = await connectionsStore.query(originId: o.id, destId: d.id, after: date)
            case .arriveBy(let date):
                results = await connectionsStore.queryArriveBy(originId: o.id, destId: d.id, before: date)
            }
            guard !Task.isCancelled else { return }
            journeys = results; isSearching = false
        }
    }
}

// MARK: - WhenChip

private struct WhenChip: View {
    let selection: WhenSelection
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                Image(systemName: "clock")
                    .font(.system(size: 12))
                Text(label)
                    .font(.system(size: 13, weight: .medium))
                Image(systemName: "chevron.down")
                    .font(.system(size: 10, weight: .semibold))
            }
            .foregroundStyle(.secondary)
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(Color(.secondarySystemFill))
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private var label: String {
        switch selection {
        case .now: "Now"
        case .departAt(let d): "Depart \(timeStr(d))"
        case .arriveBy(let d): "Arrive by \(timeStr(d))"
        }
    }

    private func timeStr(_ d: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "HH:mm"; return f.string(from: d)
    }
}

// MARK: - WhenSheet

struct WhenSheet: View {
    let initial: WhenSelection
    let onSelect: (WhenSelection) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var mode: Int = 0   // 0=now, 1=depart, 2=arrive
    @State private var pickedDate: Date = Date()

    var body: some View {
        NavigationStack {
            Form {
                Picker("When", selection: $mode) {
                    Text("Now").tag(0)
                    Text("Depart at").tag(1)
                    Text("Arrive by").tag(2)
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())

                if mode != 0 {
                    DatePicker(
                        mode == 1 ? "Departure time" : "Arrival time",
                        selection: $pickedDate,
                        in: Date()...,
                        displayedComponents: [.date, .hourAndMinute]
                    )
                }
            }
            .navigationTitle("When")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        let sel: WhenSelection
                        switch mode {
                        case 1: sel = .departAt(pickedDate)
                        case 2: sel = .arriveBy(pickedDate)
                        default: sel = .now
                        }
                        onSelect(sel)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
        .onAppear {
            switch initial {
            case .now: mode = 0; pickedDate = Date()
            case .departAt(let d): mode = 1; pickedDate = d
            case .arriveBy(let d): mode = 2; pickedDate = d
            }
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
