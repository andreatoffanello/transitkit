import SwiftUI

// MARK: - Stop Annotation View

/// Custom map annotation for a transit stop.
/// Appearance:
/// - **far**: compact colored dot (anchor .center)
/// - **medium/close**: rounded-square pin + downward triangle connector (anchor .bottom)
///   The triangle tip aligns exactly with the stop coordinate.
///   Label only appears when `isSelected == true` (on tap).
struct StopAnnotationView: View {
    let stop: ResolvedStop
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

    private var pinColor: Color {
        if let hex = routeColor {
            return Color(hex: hex.hasPrefix("#") ? hex : "#\(hex)")
        }
        return Color(hex: colorForTransitType(dominantType))
    }

    private var iconSize: CGFloat {
        switch zoomLevel {
        case .far: 10
        case .medium: 27
        case .close: 32
        }
    }

    private var symbolSize: CGFloat {
        switch zoomLevel {
        case .far: 0
        case .medium: 11
        case .close: 13
        }
    }

    var body: some View {
        if zoomLevel == .far {
            // Compact dot — anchor .center in MappaTab
            ZStack {
                Color.clear
                Circle()
                    .fill(pinColor)
                    .frame(width: 10, height: 10)
                    .overlay(Circle().stroke(.white, lineWidth: 1))
            }
            .frame(width: 44, height: 44)
            .contentShape(Rectangle())
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(stop.name), \(dominantType.displayName)")
            .accessibilityIdentifier("map_stop_\(stop.id)")
        } else {
            // Pin: rounded-square body (visually distinct from circular VehicleAnnotationView)
            // + downward triangle connector whose tip aligns with anchor .bottom (= coordinate).
            VStack(spacing: 0) {
                // Name label — only when selected, animates in
                if isSelected {
                    Text(stop.name)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color(.label))
                        .lineLimit(1)
                        .fixedSize()
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(Color(.systemBackground).opacity(0.92))
                        .clipShape(RoundedRectangle(cornerRadius: 5))
                        .shadow(color: .black.opacity(0.1), radius: 2, y: 1)
                        .transition(.scale(scale: 0.85, anchor: .bottom).combined(with: .opacity))
                        .padding(.bottom, 4)
                }

                // Pin body + triangle — scaled and shadowed as a single unit.
                // scaleEffect(anchor: .bottom) keeps the triangle tip (= coordinate) fixed.
                // Shadow applied after composition to avoid rasterisation clipping.
                VStack(spacing: 0) {
                    ZStack {
                        RoundedRectangle(cornerRadius: iconSize * 0.3)
                            .fill(.white)
                            .frame(width: iconSize, height: iconSize)
                        RoundedRectangle(cornerRadius: (iconSize - 3) * 0.28)
                            .fill(pinColor)
                            .frame(width: iconSize - 3, height: iconSize - 3)
                        dominantType.icon.sized(symbolSize)
                            .foregroundStyle(.white)
                    }

                    // Triangle connector — tip is at anchor .bottom = exact coordinate
                    DownwardTriangle()
                        .fill(.white)
                        .frame(width: 8, height: 5)
                }
                .shadow(color: .black.opacity(0.25), radius: 3, y: 2)
                .scaleEffect(isSelected ? 1.15 : 1.0, anchor: .bottom)
                .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isSelected)
            }
            .frame(minWidth: 44)
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
                    Image(systemName: "xmark")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(.secondary)
                        .frame(width: 28, height: 28)
                        .background(Color(.secondarySystemFill), in: Circle())
                }
                .accessibilityLabel("Chiudi preview fermata")
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
                                lineName: dep.lineName,
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
                Text("Apri fermata")
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

// MARK: - Zoom Level

/// Zoom level buckets derived from the map camera's latitude delta.
enum MapZoomLevel: Hashable {
    case far    // overview — dots only
    case medium // neighborhood — pin + no label
    case close  // street — pin + no label (label appears on tap via isSelected)

    init(latitudeDelta: Double) {
        switch latitudeDelta {
        case ..<0.008: self = .close
        case ..<0.02:  self = .medium
        default:       self = .far
        }
    }
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
