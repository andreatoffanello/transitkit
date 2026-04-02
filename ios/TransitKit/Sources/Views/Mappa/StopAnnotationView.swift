import SwiftUI

// MARK: - Stop Annotation View

/// Custom map annotation for a transit stop.
/// Adapts its appearance based on zoom level:
/// - **far**: compact colored dot
/// - **medium**: circle with transit type icon + stop name label
/// - **close**: icon + name + line badges
struct StopAnnotationView: View {
    let stop: ResolvedStop
    let zoomLevel: MapZoomLevel
    let isSelected: Bool

    /// The dominant transit type determines icon and color.
    private var dominantType: TransitType {
        // Prefer ferry > rail > tram > metro > bus as visual priority
        let priority: [TransitType] = [.ferry, .rail, .tram, .metro, .gondola, .funicular, .cable_tram, .trolleybus, .bus, .monorail]
        for type in priority {
            if stop.transitTypes.contains(type) { return type }
        }
        return .bus
    }

    private var pinColor: Color {
        Color(hex: colorForTransitType(dominantType))
    }

    private var iconSize: CGFloat {
        switch zoomLevel {
        case .far: 8
        case .medium: 24
        case .close: 28
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
        VStack(spacing: 3) {
            if zoomLevel == .far {
                // Compact dot — no shadow (invisible at this size, saves GPU compositing)
                Circle()
                    .fill(pinColor)
                    .frame(width: 8, height: 8)
                    .overlay(Circle().stroke(.white, lineWidth: 1))
            } else {
                // Circle with transit icon
                ZStack {
                    Circle()
                        .fill(.white)
                        .frame(width: iconSize, height: iconSize)
                    Circle()
                        .fill(pinColor)
                        .frame(width: iconSize - 3, height: iconSize - 3)
                    dominantType.icon.sized(symbolSize)
                        .foregroundStyle(.white)
                }
                .shadow(color: .black.opacity(0.2), radius: 2.5, y: 1)
                .scaleEffect(isSelected ? 1.2 : 1.0)
                .animation(.spring(response: 0.3), value: isSelected)
            }

            // Stop name label — flat background instead of .regularMaterial
            // (avoids per-view blur compositor layer, ~2× faster with 50+ pins)
            if zoomLevel != .far || isSelected {
                Text(stop.name)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(Color(.label))
                    .lineLimit(1)
                    .padding(.horizontal, 5)
                    .padding(.vertical, 2)
                    .background(Color(.systemBackground).opacity(0.88))
                    .clipShape(RoundedRectangle(cornerRadius: 4))
            }
        }
        .drawingGroup() // flatten entire annotation to a single GPU pass
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(stop.name), \(dominantType.displayName)")
        .accessibilityIdentifier("map_stop_\(stop.id)")
    }
}

// MARK: - Zoom Level

/// Zoom level buckets derived from the map camera's latitude delta.
enum MapZoomLevel: Hashable {
    case far    // overview — dots only
    case medium // neighborhood — icon + name
    case close  // street — icon + name + lines

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
