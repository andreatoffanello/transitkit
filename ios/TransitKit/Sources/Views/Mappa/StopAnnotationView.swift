import SwiftUI

// MARK: - Stop Annotation View

/// Custom map annotation for a transit stop.
/// Appearance depends on `tier`:
/// - **.city**: hidden (clusters take over — handled by MappaTab).
/// - **.neighborhood**: compact +-square marker (anchor .center). Route color
///   if a route is selected, else stop dominant transit-type color. Thin white
///   outer stroke for legibility. A central white cross (`+`) identifies it as
///   a transit marker (vs. a POI).
/// - **.street**: rounded-square pin with transit-type icon + downward triangle
///   connector (anchor .bottom). Triangle tip aligns with stop coordinate.
///   Name label appears when `isSelected == true` (on tap).
struct StopAnnotationView: View {
    let stop: ResolvedStop
    let tier: MapZoomTier
    let zoomLevel: MapZoomLevel
    let isSelected: Bool
    /// When set (route selected on map), overrides the default transit-type color.
    var routeColor: String? = nil

    /// The dominant transit type determines icon and color.
    private var dominantType: TransitType {
        let priority: [TransitType] = [.ferry, .rail, .tram, .metro, .gondola, .funicular, .cable_tram, .trolleybus, .bus, .monorail]
        for type in priority {
            if stop.transitTypes.contains(type) { return type }
        }
        return .bus
    }

    /// GTFS-preserving background color: route color when overlayed, else transit-type color.
    private var pinColorHex: String {
        if let hex = routeColor {
            return hex.hasPrefix("#") ? hex : "#\(hex)"
        }
        return colorForTransitType(dominantType)
    }

    private var pinColor: Color { Color(hex: pinColorHex) }

    /// WCAG-contrasting foreground color for the `+` glyph on the +-square marker.
    private var crossColor: Color {
        Color(hex: contrastingTextColor(for: pinColorHex))
    }

    var body: some View {
        switch tier {
        case .city:
            // Ultra-compact square (movete nasconde a city; noi mostriamo compatto).
            RoundedRectangle(cornerRadius: 1.5, style: .continuous)
                .fill(pinColor)
                .frame(width: 7, height: 7)
                .overlay(
                    RoundedRectangle(cornerRadius: 1.5, style: .continuous)
                        .stroke(Color.white, lineWidth: 1)
                )
                .frame(width: 20, height: 20)
                .contentShape(Rectangle())
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(stop.name), \(dominantType.displayName)")
                .accessibilityIdentifier("map_stop_\(stop.id)")

        case .neighborhood:
            // Copia movete: 11×11, stroke 1.5pt bianca, hit-target 24×24.
            RoundedRectangle(cornerRadius: 2, style: .continuous)
                .fill(pinColor)
                .frame(width: 11, height: 11)
                .overlay(
                    RoundedRectangle(cornerRadius: 2, style: .continuous)
                        .stroke(Color.white, lineWidth: 1.5)
                )
                .frame(width: 24, height: 24)
                .contentShape(Rectangle())
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(stop.name), \(dominantType.displayName)")
                .accessibilityIdentifier("map_stop_\(stop.id)")

        case .street:
            // Copia movete: box 28×28 (36×36 selezionata), radius 7/10, icon 12/16,
            // stroke bianca 2pt, triangle 8×5 bianco. No name-label (movete parity).
            let boxSize: CGFloat = isSelected ? 36 : 28
            let cornerRadius: CGFloat = isSelected ? 10 : 7
            let iconSize: CGFloat = isSelected ? 16 : 12

            VStack(spacing: 0) {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(pinColor)
                    .frame(width: boxSize, height: boxSize)
                    .overlay(
                        Group {
                            if dominantType == .metro {
                                Text("M")
                                    .font(.system(size: iconSize + 2, weight: .bold))
                                    .foregroundStyle(Color.white)
                            } else {
                                stopPinIcon(transitTypes: stop.transitTypes).sized(iconSize)
                                    .foregroundStyle(Color.white)
                            }
                        }
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .stroke(Color.white, lineWidth: 2)
                    )
                DownwardTriangle()
                    .fill(Color.white)
                    .frame(width: 8, height: 5)
            }
            .animation(.spring(duration: 0.25), value: isSelected)
            // Ancoraggio al bottom dell'host 60×60: il triangle tip coincide col
            // bordo inferiore del frame. TransitMapView usa centerOffset = -30
            // per mappare quel bordo al coordinate esatto della fermata.
            .frame(width: 60, height: 60, alignment: .bottom)
            // Nome fermata sotto al pin (movete parity, come l'Android
            // SymbolLayer): overlay che sborda sotto il frame — l'host non
            // clippa (clipsToBounds = false) e l'alignmentGuide aggancia il
            // top della label al bottom del frame, senza spostare il tip.
            .overlay(alignment: .bottom) {
                StopMapLabel(name: stop.name)
                    .alignmentGuide(.bottom) { d in d[.top] }
            }
            .contentShape(Rectangle())
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(stop.name), \(dominantType.displayName)")
            .accessibilityIdentifier("map_stop_\(stop.id)")
        }
    }
}

// MARK: - Downward Triangle

/// A triangle pointing downward — used as the connector between the stop pin body
/// and the exact coordinate on the map.
private struct DownwardTriangle: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: rect.midX, y: rect.maxY))
        p.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        p.closeSubpath()
        return p
    }
}

// MARK: - Stop Preview Card

/// Bottom overlay card shown when a stop annotation is tapped on the map.
/// Shows stop name, transit types, next 3 departures, and an "Apri fermata" CTA.
/// Modelled after VehicleDetailSheet for visual consistency.
struct StopPreviewCard: View {
    let stop: ResolvedStop
    let onDismiss: () -> Void
    let onOpenStop: () -> Void
    @Environment(ScheduleStore.self) private var store
    /// Incremented every 15s to keep countdowns live — mirrors StopDetailView.
    @State private var refreshTick: Int = 0

    private var upcoming: [Departure] {
        _ = refreshTick
        return store.upcomingDepartures(forStopId: stop.id, limit: 3)
    }

    private func timeState(for dep: Departure) -> DepartureTimeState {
        let nowMinutes = store.currentMinutesFromMidnight()
        let diff = dep.minutesFromMidnight - nowMinutes
        if diff < 0 { return .passed(dep.time) }
        if diff == 0 { return .departing }
        if diff <= 60 { return .minutes(diff) }
        return .absolute(dep.time)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header row
            HStack(alignment: .center, spacing: 10) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(stop.name)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.primary)
                        .lineLimit(2)
                    HStack(spacing: 5) {
                        ForEach(
                            Array(stop.transitTypes).sorted(by: { $0.rawValue < $1.rawValue }),
                            id: \.self
                        ) { type in
                            HStack(spacing: 3) {
                                type.icon.sized(11)
                                Text(type.displayName)
                                    .font(.system(size: 11))
                            }
                            .foregroundStyle(.secondary)
                        }
                    }
                }
                Spacer()
                Button(action: onDismiss) {
                    // Pattern VehicleDetailSheet: visual compatto ma hit-area
                    // 44pt (il vecchio 28×28 era sotto il minimo HIG).
                    LucideIcon.x.sized(14)
                        .foregroundStyle(.secondary)
                        .frame(width: 28, height: 28)
                        .background(AppTheme.textPrimary.opacity(0.08))
                        .clipShape(Circle())
                        .frame(width: 44, height: 44)
                        .contentShape(Circle())
                }
                .accessibilityLabel(Text(String(localized: "a11y_close_stop_preview")))
                .accessibilityIdentifier("btn_stop_preview_dismiss")
            }
            .padding(.horizontal, 14)
            .padding(.top, 14)
            .padding(.bottom, upcoming.isEmpty ? 0 : 10)

            if !upcoming.isEmpty {
                Divider()
                    .padding(.horizontal, 14)

                VStack(spacing: 0) {
                    ForEach(upcoming) { dep in
                        HStack(spacing: 8) {
                            LineBadge(
                                name: dep.lineName,
                                color: dep.color,
                                textColor: dep.textColor,
                                transitType: dep.transitType,
                                size: .medium
                            )
                            Text(dep.headsign)
                                .font(.system(size: 13))
                                .foregroundStyle(.primary)
                                .lineLimit(1)
                            Spacer()
                            TimeDisplay(state: timeState(for: dep))
                                .accessibilityIdentifier("preview_dep_time_\(dep.id)")
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 9)
                        .accessibilityIdentifier("preview_dep_row_\(dep.lineName)")

                        if dep.id != upcoming.last?.id {
                            Divider().padding(.horizontal, 14)
                        }
                    }
                }

                Divider()
                    .padding(.horizontal, 14)
            }

            // CTA
            Button(action: onOpenStop) {
                Text(String(localized: "map_open_stop"))
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(AppTheme.accent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .accessibilityIdentifier("btn_stop_preview_open")
        }
        .background(.regularMaterial)
        .task {
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(15))
                refreshTick &+= 1
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.primary.opacity(0.07), lineWidth: 0.5))
        .shadow(color: .black.opacity(0.18), radius: 12, y: 4)
        .accessibilityIdentifier("stop_preview_card")
    }
}

// MARK: - Zoom Level / Tier
//
// `MapZoomTier` e `MapZoomLevel` sono definiti in `MapZoomLevels.swift` —
// single source of truth condivisa con Android (`MapZoomLevels.kt`).

/// Stop pin icon: signpost (tab icon) for bus stops and mixed stops.
/// Only exclusively non-bus stops (tram-only, rail-only, ferry-only, etc.)
/// show the transit-type icon so the marker communicates the mode at a glance.
func stopPinIcon(transitTypes: Set<TransitType>) -> LucideIcon {
    let busTypes: Set<TransitType> = [.bus, .trolleybus]
    let hasBus = !transitTypes.isDisjoint(with: busTypes) || transitTypes.isEmpty
    if hasBus { return .signpost }
    let priority: [TransitType] = [.ferry, .tram, .metro, .rail, .gondola, .funicular, .cable_tram, .monorail]
    for type in priority {
        if transitTypes.contains(type) { return type.icon }
    }
    return .signpost
}

// MARK: - Transit Type Color

/// Default color per transit type (used when no route color is available).
func colorForTransitType(_ type: TransitType) -> String {
    switch type {
    case .bus, .trolleybus: "#4CAF50"
    case .tram:             "#FF9800"
    case .metro:            "#E91E63"
    case .rail, .monorail:  "#607D8B"
    case .ferry:            "#2196F3"
    case .cable_tram, .gondola, .funicular: "#9C27B0"
    }
}
