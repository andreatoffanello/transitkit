import SwiftUI

// MARK: - Vehicle Card

/// Inline card shown at the bottom of the map when tapping a live vehicle.
/// Not a sheet — rendered directly in the MappaTab ZStack.
struct VehicleDetailSheet: View {
    let vehicle: GtfsRtVehicle
    let route: APIRoute?
    let onDismiss: () -> Void

    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(ScheduleStore.self) private var scheduleStore

    private var transitType: TransitType {
        route.map { TransitType(gtfsRouteType: $0.transitType) } ?? .bus
    }

    private var directionLabel: String? {
        guard let route else { return nil }
        let headsigns = route.directions
            .sorted { $0.directionId < $1.directionId }
            .compactMap(\.headsign)
            .filter { !$0.isEmpty }
        guard !headsigns.isEmpty else { return nil }
        return headsigns.count >= 2
            ? "\(headsigns[0]) → \(headsigns[1])"
            : headsigns[0]
    }

    private var delay: Int32? {
        vehicleStore.delay(forTripId: vehicle.tripId)
    }

    private var currentStopName: String? {
        guard !vehicle.currentStopId.isEmpty else { return nil }
        return scheduleStore.stop(forId: vehicle.currentStopId)?.name
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Row 1 — Badge + name + Live + X
            HStack(spacing: 10) {
                if let route {
                    LineBadge(route: route, size: .medium)
                    Text(route.longName ?? route.name)
                        .font(.headline)
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)
                } else {
                    Circle()
                        .fill(AppTheme.accent)
                        .frame(width: 28, height: 28)
                        .overlay(transitType.icon.sized(12).foregroundStyle(.white))
                    Text(transitType.displayName)
                        .font(.headline)
                        .foregroundStyle(AppTheme.textPrimary)
                }

                Spacer()

                HStack(spacing: 4) {
                    Circle()
                        .fill(AppTheme.realtimeGreen)
                        .frame(width: 6, height: 6)
                    Text("Live")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(AppTheme.realtimeGreen)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(AppTheme.realtimeGreen.opacity(0.15))
                .clipShape(Capsule())

                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(AppTheme.textSecondary)
                        .frame(width: 26, height: 26)
                        .background(AppTheme.textPrimary.opacity(0.08))
                        .clipShape(Circle())
                }
            }

            // Row 2 — Direction
            if let dir = directionLabel {
                HStack(spacing: 6) {
                    Image(systemName: "arrow.right")
                        .font(.system(size: 11))
                        .foregroundStyle(AppTheme.textTertiary)
                    Text(dir)
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.textSecondary)
                        .lineLimit(1)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(AppTheme.textTertiary)
                }
            }

            // Row 3 — Delay + current stop
            if delay != nil || currentStopName != nil {
                HStack(spacing: 8) {
                    if let d = delay {
                        delayBadge(seconds: d)
                    }
                    if let name = currentStopName {
                        HStack(spacing: 4) {
                            Image(systemName: "mappin.circle.fill")
                                .font(.system(size: 12))
                                .foregroundStyle(AppTheme.textTertiary)
                            Text(name)
                                .font(.subheadline)
                                .foregroundStyle(AppTheme.textSecondary)
                                .lineLimit(1)
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: .black.opacity(0.2), radius: 12, y: 4)
    }

    @ViewBuilder
    private func delayBadge(seconds: Int32) -> some View {
        let absMins = Int(abs(seconds)) / 60
        let (text, color): (String, Color) = {
            if seconds > 60  { return ("In ritardo di \(absMins) min", .orange) }
            if seconds < -60 { return ("In anticipo di \(absMins) min", AppTheme.realtimeGreen) }
            return ("In orario", AppTheme.realtimeGreen)
        }()
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundStyle(color)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(color.opacity(0.15))
            .clipShape(Capsule())
    }
}
