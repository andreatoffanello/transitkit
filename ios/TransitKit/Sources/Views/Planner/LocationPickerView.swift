import SwiftUI
import CoreLocation

/// Vista piena per selezione posizione nel planner (origine o destinazione).
/// Niente sheet: viene pushata via NavigationDestination.
///
/// Tre modi di scegliere — tutti producono un `PlannerLocation`:
///  - chip "La mia posizione" → `.userLocation` (coordinata GPS attuale)
///  - chip "Scegli sulla mappa" → `.place` (coordinata libera + reverse geocoding)
///  - ricerca testo sulle fermate dell'operatore → `.stop` (stop ID GTFS)
struct LocationPickerView: View {
    let isOrigin: Bool
    let excludedStopId: String?
    let onSelect: (PlannerLocation) -> Void

    @Environment(ScheduleStore.self) private var store
    @Environment(LocationManager.self) private var locationManager
    @Environment(\.dismiss) private var dismiss

    @State private var query = ""
    @State private var showMapPicker = false
    @State private var mapPickedLocation: PlannerLocation? = nil
    @FocusState private var fieldFocused: Bool

    private var title: String {
        isOrigin
            ? String(localized: "planner_search_from_title")
            : String(localized: "planner_search_to_title")
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
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(isPresented: $showMapPicker) {
            LocationPickerMap { picked in
                // Salviamo qui; il dismiss cascade avviene nel onChange sotto,
                // dopo che LocationPickerMap completa la sua pop animation.
                mapPickedLocation = picked
            }
        }
        .onChange(of: showMapPicker) { _, isShowing in
            // showMapPicker passa a false quando LocationPickerMap si è auto-dismissato.
            // A quel punto, se l'utente aveva confermato un punto, propaghiamo al parent
            // e ci dismiss anche noi, in modo che si torni direttamente al PlannerScreen.
            guard !isShowing, let picked = mapPickedLocation else { return }
            mapPickedLocation = nil
            onSelect(picked)
            // Piccolo delay per evitare conflitti con l'animazione di pop in corso
            // (senza il delay, dismiss viene ignorato quando la nav stack è ancora
            // in transizione, su iOS 18 NavigationStack).
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(280))
                dismiss()
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

            if !query.isEmpty {
                Button {
                    query = ""
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

    // MARK: - Quick choices + suggerimenti vicini

    private var quickChoicesSection: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 8) {
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

    // MARK: - My location chip

    private var myLocationRow: some View {
        let hasLocation = locationManager.location != nil
        return Button {
            guard let loc = locationManager.location else { return }
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            onSelect(.userLocation(
                name: String(localized: "planner_my_location"),
                coordinate: loc.coordinate
            ))
            dismiss()
        } label: {
            quickChoiceCell(
                icon: LucideIcon.navigation.sized(18).foregroundStyle(AppTheme.accent).eraseToAnyView(),
                title: String(localized: "planner_my_location"),
                subtitle: hasLocation
                    ? String(localized: "planner_location_active")
                    : String(localized: "planner_location_unavailable"),
                showChevron: hasLocation
            )
        }
        .buttonStyle(PressableButtonStyle())
        .disabled(!hasLocation)
        .opacity(hasLocation ? 1 : 0.4)
        .accessibilityIdentifier("picker_my_location")
    }

    // MARK: - Map pick chip

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

    private func quickChoiceCell(icon: AnyView, title: String, subtitle: String, showChevron: Bool = true) -> some View {
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
            if showChevron {
                LucideIcon.chevronRight.sized(14)
                    .foregroundStyle(AppTheme.textTertiary)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: - Search results

    private var searchResultsSection: some View {
        Group {
            if filteredStops.isEmpty {
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
                        stopList(filteredStops)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 32)
                }
            }
        }
    }

    // MARK: - Stop list (shared)

    @ViewBuilder
    private func stopList(_ stops: [ResolvedStop]) -> some View {
        VStack(spacing: 0) {
            ForEach(Array(stops.enumerated()), id: \.element.id) { index, stop in
                Button {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    onSelect(.stop(stop))
                    dismiss()
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

    // MARK: - Helpers

    private func sectionLabel(_ text: String) -> some View {
        Text(text)
            .font(.footnote.weight(.semibold))
            .foregroundStyle(AppTheme.textTertiary)
            .textCase(.uppercase)
            .kerning(0.5)
    }
}

// Utility per passare Image/View tipizzata come AnyView nelle celle
private extension View {
    func eraseToAnyView() -> AnyView { AnyView(self) }
}
