import SwiftUI
import CoreLocation

/// Card "Pianifica viaggio" in home.
/// Layout transit-classico: dot origin + connecting line + dot destination,
/// chip when capsule pill in basso a destra.
/// Tutte le interazioni navigano in vista piena (niente sheet).
struct PlannerHomeBox: View {
    let onLaunch: (PlannerLocation, PlannerLocation, WhenSelection) -> Void

    @Environment(ScheduleStore.self) private var store
    @Environment(LocationManager.self) private var locationManager

    @State private var origin: PlannerLocation? = nil
    @State private var destination: PlannerLocation? = nil
    @State private var whenSelection: WhenSelection = .now
    @State private var boxNav: BoxNav? = nil

    // Auto-advance origine -> destinazione: dopo aver scelto l'origine (se la
    // destinazione è ancora vuota) apriamo automaticamente il picker della
    // destinazione. Il push avviene quando boxNav torna nil (pop completato),
    // con un hop al runloop successivo perché HomeTab ha più
    // navigationDestination(item:) sullo stesso stack e modificarne due nello
    // stesso tick fa coalescere le transizioni (NavigationStack iOS 18+).
    @State private var pendingNextPicker: BoxNav? = nil

    private var canSearch: Bool { origin != nil && destination != nil }

    /// Singolo navigationDestination per evitare conflitti SwiftUI con
    /// più navigationDestination(isPresented:) sulla stessa view.
    private enum BoxNav: Identifiable, Hashable {
        case picker(isOrigin: Bool, excludedId: String?)

        var id: String {
            switch self {
            case .picker(let isOrigin, _): return "picker-\(isOrigin)"
            }
        }
    }

    // Layout constants
    private static let rowHeight: CGFloat = 50
    private static let dotColumnWidth: CGFloat = 44
    private static let dotSize: CGFloat = 18

    var body: some View {
        VStack(spacing: 8) {
            locationFields
                .background(alignment: .topLeading) {
                    // Connecting line verticale tra i due dot.
                    Rectangle()
                        .fill(AppTheme.accent.opacity(0.32))
                        .frame(width: 1.5)
                        .frame(height: Self.rowHeight + 0.5)
                        .offset(
                            x: Self.dotColumnWidth / 2 - 0.75,
                            y: Self.rowHeight / 2
                        )
                }
                .adaptiveGlass(in: RoundedRectangle(cornerRadius: 14), withShadow: true)
                .overlay(alignment: .trailing) {
                    // Swap button verticalmente centrato tra le 2 righe origin/destination
                    // (overlay sulla card, non dentro la dest row).
                    if origin != nil && destination != nil {
                        swapButton
                            .padding(.trailing, 10)
                    }
                }

            // Chip "quando" a sinistra (scrollabili: in modalità parti/arriva
            // diventano 3), pulsante Cerca pinnato a destra. L'utente compila
            // Da/A, sceglie il quando, poi lancia esplicitamente — niente
            // auto-launch, niente race col pop delle picker.
            HStack(spacing: 8) {
                ScrollView(.horizontal, showsIndicators: false) {
                    WhenChipsRow(selection: $whenSelection)
                        .padding(.vertical, 1)
                }
                searchButton
                    .fixedSize()
            }
            .padding(.horizontal, 4)
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("home_planner_block")
        .navigationDestination(item: $boxNav) { nav in
            switch nav {
            case .picker(let isOrigin, let excludedId):
                LocationPickerView(isOrigin: isOrigin, excludedStopId: excludedId) { location in
                    if isOrigin {
                        origin = location
                        if destination == nil {
                            // Auto-advance: apri il picker destinazione dopo il
                            // pop di questo. Il push avviene nell'.onChange sotto.
                            pendingNextPicker = .picker(isOrigin: false, excludedId: location.stopId)
                        }
                    } else {
                        destination = location
                    }
                    // Niente auto-launch: l'utente lancia con il pulsante Cerca.
                }
            }
        }
        .onChange(of: boxNav) { _, newValue in
            // Quando boxNav torna nil il pop è completato. Se c'era un picker in
            // coda (auto-advance origine -> destinazione) lo apriamo ora, con un
            // hop al runloop successivo: HomeTab ha più navigationDestination(item:)
            // sullo stesso stack e modificarne due nello stesso tick fa coalescere
            // le transizioni (NavigationStack iOS 18+ scarta il push concorrente).
            guard newValue == nil, let next = pendingNextPicker else { return }
            pendingNextPicker = nil
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(320))
                boxNav = next
            }
        }
        .onAppear { autoFillOrigin() }
        .onChange(of: locationManager.location) { _, _ in autoFillOrigin() }
    }

    // MARK: - Location fields

    private var locationFields: some View {
        VStack(spacing: 0) {
            row(role: .origin, value: origin, placeholder: String(localized: "planner_from_placeholder"))
            Rectangle()
                .fill(AppTheme.separatorLine)
                .frame(height: 0.5)
                .padding(.leading, Self.dotColumnWidth)
            row(role: .destination, value: destination, placeholder: String(localized: "planner_to_placeholder"))
        }
    }

    private func row(role: LocationRole, value: PlannerLocation?, placeholder: String) -> some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            boxNav = .picker(
                isOrigin: role == .origin,
                excludedId: role == .origin ? destination?.stopId : origin?.stopId
            )
        } label: {
            HStack(spacing: 0) {
                // Dot (sopra la connecting line, la copre)
                roleDot(role)
                    .frame(width: Self.dotColumnWidth)

                if let value {
                    HStack(spacing: 6) {
                        locationKindIcon(value.kind)
                        Text(value.name)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(AppTheme.textPrimary)
                            .lineLimit(1)
                    }
                } else {
                    Text(placeholder)
                        .font(.system(size: 15))
                        .foregroundStyle(AppTheme.textTertiary)
                }

                Spacer(minLength: 4)

                // Lo swap button è ora in overlay sul card (vedi body),
                // verticalmente centrato tra le 2 righe. Riserviamo spazio
                // a destra del dest per non far passare il testo sotto il button.
                if role == .destination && origin != nil && destination != nil {
                    Color.clear.frame(width: 36)
                }
            }
            .frame(height: Self.rowHeight)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("home_planner_row_\(role == .origin ? "origin" : "destination")")
    }

    @ViewBuilder
    private func locationKindIcon(_ kind: PlannerLocation.Kind) -> some View {
        switch kind {
        case .userLocation:
            LucideIcon.navigation.sized(12).foregroundStyle(AppTheme.accent)
        case .place:
            LucideIcon.mapPin.sized(12).foregroundStyle(AppTheme.textSecondary)
        case .stop:
            EmptyView()
        }
    }

    private func roleDot(_ role: LocationRole) -> some View {
        ZStack {
            // Halo opaco del card per mascherare la connecting line dietro
            // l'icona — l'SVG stroke-based ha trasparenza interna.
            Circle()
                .fill(AppTheme.background)
                .frame(width: Self.dotSize + 6, height: Self.dotSize + 6)

            Group {
                if role == .origin {
                    LucideIcon.locateFixed.sized(Self.dotSize)
                } else {
                    LucideIcon.mapPin.sized(Self.dotSize)
                }
            }
            .foregroundStyle(AppTheme.accent)
        }
    }

    private var swapButton: some View {
        Button {
            UISelectionFeedbackGenerator().selectionChanged()
            let tmp = origin
            origin = destination
            destination = tmp
            // Solo swap: il lancio resta esplicito col pulsante Cerca.
        } label: {
            LucideIcon.arrowUpDown.sized(15)
                .foregroundStyle(AppTheme.textSecondary)
                .frame(width: 32, height: 32)
                .background(Color(.tertiarySystemFill), in: Circle())
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(String(localized: "planner_swap_label"))
    }

    // MARK: - Search button

    private var searchButton: some View {
        Button {
            guard let o = origin, let d = destination else { return }
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            onLaunch(o, d, whenSelection)
        } label: {
            HStack(spacing: 6) {
                LucideIcon.search.sized(14)
                Text(String(localized: "planner_search_button"))
                    .font(.system(size: 14, weight: .semibold))
            }
            .foregroundStyle(canSearch ? .white : AppTheme.textTertiary)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(
                canSearch ? AppTheme.accent : Color(.tertiarySystemFill),
                in: Capsule()
            )
        }
        .buttonStyle(PressableButtonStyle())
        .disabled(!canSearch)
        .animation(.easeOut(duration: 0.2), value: canSearch)
        .accessibilityIdentifier("home_planner_search")
    }

    // MARK: - Auto-fill origin

    private func autoFillOrigin() {
        guard origin == nil, let loc = locationManager.location else { return }
        origin = .userLocation(
            name: String(localized: "planner_my_location"),
            coordinate: loc.coordinate
        )
    }
}

private enum LocationRole { case origin, destination }
