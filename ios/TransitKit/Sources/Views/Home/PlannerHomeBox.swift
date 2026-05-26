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

            // Chip mode + (orario, data) fuori card, allineate a sinistra.
            HStack {
                WhenChipsRow(selection: $whenSelection)
                Spacer()
            }
            .padding(.horizontal, 4)
        }
        .accessibilityIdentifier("home_planner_block")
        .navigationDestination(item: $boxNav) { nav in
            switch nav {
            case .picker(let isOrigin, let excludedId):
                LocationPickerView(isOrigin: isOrigin, excludedStopId: excludedId) { location in
                    if isOrigin {
                        origin = location
                        if destination == nil {
                            // Let origin picker dismiss first, then push destination
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.55) {
                                boxNav = .picker(isOrigin: false, excludedId: location.stopId)
                            }
                        }
                    } else {
                        destination = location
                    }
                    tryLaunch()
                }
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
            // Dopo swap, rilancia la search se entrambi sono settati.
            tryLaunch()
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

    // MARK: - Auto-fill origin

    private func autoFillOrigin() {
        guard origin == nil, let loc = locationManager.location else { return }
        origin = .userLocation(
            name: String(localized: "planner_my_location"),
            coordinate: loc.coordinate
        )
    }

    // MARK: - Launch

    private func tryLaunch() {
        guard let o = origin, let d = destination else { return }
        // Delay sufficiente per lasciare completare le pop animation di
        // LocationPickerView / LocationPickerMap prima del push di PlannerScreen.
        // Senza delay, NavigationStack iOS 18+ scarta il push concorrente con
        // i pop e l'utente resta sulla Home con i field popolati ma senza
        // results. 500ms = lunghezza standard pop animation in SwiftUI.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.55) {
            onLaunch(o, d, whenSelection)
        }
    }
}

private enum LocationRole { case origin, destination }
