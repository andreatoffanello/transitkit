import SwiftUI

// MARK: - JourneyCard
// Compact summary card for a single journey result.

struct JourneyCard: View {
    let journey: Journey
    var maxJourneyDurationSeconds: Int = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Row 1: Times + Duration
            HStack(alignment: .firstTextBaseline) {
                Text(timeLabel(journey.departureTime))
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.primary)
                Image(systemName: "arrow.right")
                    .imageScale(.small)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(.tertiaryLabel))
                Text(timeLabel(journey.arrivalTime))
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.primary)
                Spacer()
                Text("\(journey.durationMinutes) min")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.secondary)
            }

            // Row 2: Proportional strip
            JourneyStrip(
                legs: journey.legs,
                journeyDurationSeconds: Int(journey.arrivalTime.timeIntervalSince(journey.departureTime)),
                maxJourneyDurationSeconds: max(
                    maxJourneyDurationSeconds,
                    Int(journey.arrivalTime.timeIntervalSince(journey.departureTime))
                )
            )

            // Row 3: Line badges + countdown + transfer count
            HStack(spacing: 8) {
                ForEach(journey.transitLegs) { leg in
                    LineBadge(
                        name: leg.lineName,
                        color: "#\(leg.routeColor)",
                        textColor: leg.routeTextColor.isEmpty ? nil : "#\(leg.routeTextColor)",
                        size: .small
                    )
                }

                Spacer()

                let mins = journey.minutesUntilDeparture
                if mins <= 60 {
                    let depLabel = String(format: NSLocalizedString("planner_departs_in_min", comment: ""), mins)
                    if mins <= 2 {
                        Text(depLabel)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(AppTheme.realtimeRed)
                            .modifier(PulseBadge())
                    } else {
                        Text(depLabel)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(.secondary)
                    }
                }

                if journey.transfers > 0 {
                    let changeLabel = journey.transfers == 1
                        ? String(localized: "planner_change_one")
                        : String(format: NSLocalizedString("planner_change_count", comment: ""), journey.transfers)
                    Text(changeLabel)
                        .font(.system(size: 13))
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(16)
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func timeLabel(_ date: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "HH:mm"; return f.string(from: date)
    }
}

// MARK: - JourneyStrip

private struct JourneyStrip: View {
    let legs: [Leg]
    let journeyDurationSeconds: Int
    let maxJourneyDurationSeconds: Int

    private var totalLegSec: Int { legs.reduce(0) { $0 + $1.durationSeconds } }

    var body: some View {
        GeometryReader { geo in
            let maxW = geo.size.width
            let fraction: CGFloat = maxJourneyDurationSeconds > 0
                ? CGFloat(journeyDurationSeconds) / CGFloat(maxJourneyDurationSeconds)
                : 1.0
            let usedW = max(8, maxW * fraction)

            HStack(spacing: 0) {
                HStack(spacing: 3) {
                    ForEach(legs) { leg in
                        let legFrac = totalLegSec > 0
                            ? CGFloat(leg.durationSeconds) / CGFloat(totalLegSec)
                            : 1.0 / CGFloat(legs.count)
                        let w = max(4, (usedW - CGFloat(legs.count - 1) * 3) * legFrac)

                        switch leg {
                        case .transit(let t):
                            let c = t.routeColor.uppercased() == "FFFFFF"
                                ? Color(.secondaryLabel)
                                : Color(hex: "#\(t.routeColor)")
                            RoundedRectangle(cornerRadius: 3, style: .continuous)
                                .fill(c)
                                .frame(width: w, height: 8)
                        case .walking:
                            RoundedRectangle(cornerRadius: 3, style: .continuous)
                                .fill(Color(.tertiaryLabel))
                                .frame(width: w, height: 8)
                        }
                    }
                }
                .frame(width: usedW, alignment: .leading)
                Spacer(minLength: 0)
            }
        }
        .frame(height: 8)
    }
}

// MARK: - PulseBadge

private struct PulseBadge: ViewModifier {
    @State private var pulsing = false
    func body(content: Content) -> some View {
        content
            .opacity(pulsing ? 0.5 : 1.0)
            .animation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true), value: pulsing)
            .onAppear { pulsing = true }
    }
}
