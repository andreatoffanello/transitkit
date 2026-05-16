import SwiftUI

/// Single live vehicle card shown in the horizontal scroll of LineDetailView.
/// Top row: bus icon + "Vehicle <label>". Bottom: "NEXT STOP" caption + stop name.
/// Tap opens the Mappa tab centered on this vehicle (handled by parent via onTap).
struct VehicleLiveCard: View {
    let vehicle: GtfsRtVehicle
    let nextStopName: String?
    let lineColor: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 8) {
                    LucideIcon.bus.sized(14)
                        .foregroundStyle(lineColor)
                    Text(vehicleTitle)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)
                    Spacer(minLength: 0)
                    LivePulseDot(color: AppTheme.realtimeGreen)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(nextStopCaption)
                        .font(.system(size: 10, weight: .semibold))
                        .kerning(0.6)
                        .foregroundStyle(AppTheme.textTertiary)
                    if let next = nextStopName {
                        Text(next)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(AppTheme.textPrimary)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    } else {
                        // No upcoming stop known — we still want to communicate
                        // the vehicle is being tracked, not that we lost it.
                        Text(String(localized: "vehicle_next_stop_unknown"))
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(AppTheme.textSecondary)
                            .lineLimit(1)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
            .padding(14)
            .frame(width: 220, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(AppTheme.bgSecondary)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(AppTheme.glassBorder, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("vehicle_card_\(vehicle.id)")
    }

    private var vehicleTitle: String {
        let label = vehicle.label.isEmpty ? vehicle.id : vehicle.label
        return String(format: String(localized: "vehicle_label_format"), label)
    }

    /// The caption above the next stop name flips to "LIVE TRACKING" when no
    /// next stop is known, mirroring Movete's pattern — the card never reads
    /// as broken when the feed lacks `current_stop_id`.
    private var nextStopCaption: String {
        nextStopName != nil
            ? String(localized: "vehicle_next_stop")
            : String(localized: "vehicle_live_tracking")
    }
}

/// Small pulsing live indicator dot. 2s breathing cycle.
struct LivePulseDot: View {
    let color: Color
    @State private var pulse = false

    var body: some View {
        Circle()
            .fill(color)
            .frame(width: 8, height: 8)
            .overlay(
                Circle()
                    .stroke(color.opacity(0.4), lineWidth: 4)
                    .scaleEffect(pulse ? 2.0 : 1.0)
                    .opacity(pulse ? 0.0 : 0.8)
            )
            .onAppear {
                withAnimation(.easeOut(duration: 1.6).repeatForever(autoreverses: false)) {
                    pulse = true
                }
            }
    }
}
