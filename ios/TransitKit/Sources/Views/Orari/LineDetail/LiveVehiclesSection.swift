import SwiftUI

/// Live vehicles section for LineDetailView: counter + horizontal scroll of cards,
/// filtered by the currently-selected direction.
/// Hidden when no vehicles are live on the entire route in any direction.
struct LiveVehiclesSection: View {
    let vehicles: [GtfsRtVehicle]
    let oppositeDirectionCount: Int
    let lineColor: Color
    let onVehicleTap: (GtfsRtVehicle) -> Void
    let onSwitchDirection: () -> Void
    let resolveNextStop: (GtfsRtVehicle) -> String?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            header
            if vehicles.isEmpty {
                emptyState
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: 10) {
                        ForEach(vehicles, id: \.id) { vehicle in
                            VehicleLiveCard(
                                vehicle: vehicle,
                                nextStopName: resolveNextStop(vehicle),
                                lineColor: lineColor,
                                onTap: { onVehicleTap(vehicle) }
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                }
                .scrollClipDisabled()
            }
        }
        .padding(.vertical, 12)
    }

    private var header: some View {
        HStack(spacing: 8) {
            LivePulseDot(color: AppTheme.realtimeGreen)
            Text(String(format: String(localized: "line_detail_in_service"), vehicles.count))
                .font(.system(size: 11, weight: .bold))
                .kerning(0.8)
                .foregroundStyle(AppTheme.textSecondary)
            Spacer()
        }
        .padding(.horizontal, 20)
    }

    private var emptyState: some View {
        Button(action: onSwitchDirection) {
            HStack(spacing: 12) {
                LucideIcon.busFront.sized(18)
                    .foregroundStyle(AppTheme.textTertiary)
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "line_detail_no_buses_this_direction"))
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(AppTheme.textSecondary)
                        .multilineTextAlignment(.leading)
                    if oppositeDirectionCount > 0 {
                        Text(String(format: String(localized: "line_detail_n_in_opposite_direction"), oppositeDirectionCount))
                            .font(.caption)
                            .foregroundStyle(AppTheme.textTertiary)
                    }
                }
                Spacer(minLength: 0)
                if oppositeDirectionCount > 0 {
                    LucideIcon.arrowRight.sized(14)
                        .foregroundStyle(AppTheme.textTertiary)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(AppTheme.bgSecondary.opacity(0.6))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(AppTheme.glassBorder, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .disabled(oppositeDirectionCount == 0)
        .padding(.horizontal, 16)
    }
}
