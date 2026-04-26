import SwiftUI

// MARK: - JourneyDetailView
// Full timeline of a journey. 3-column layout: time | node | content.

struct JourneyDetailView: View {
    let journey: Journey
    @State private var expandedLegs: Set<UUID> = []

    private var defaultExpandedLegs: Set<UUID> {
        journey.transitLegs.count == 1 ? [journey.transitLegs[0].id] : []
    }

    private var subtitle: String {
        let dur = "\(journey.durationMinutes) min"
        guard journey.transfers > 0 else { return dur }
        let ch = journey.transfers == 1 ? "1 change" : "\(journey.transfers) changes"
        return "\(dur) · \(ch)"
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                heroHeader

                ForEach(Array(journey.legs.enumerated()), id: \.element.id) { idx, leg in
                    switch leg {
                    case .transit(let t):
                        let prevTransit: TransitLeg? = {
                            guard idx > 0, case .transit(let p) = journey.legs[idx - 1] else { return nil }
                            return p
                        }()
                        let nextTransit: TransitLeg? = {
                            guard idx + 1 < journey.legs.count,
                                  case .transit(let n) = journey.legs[idx + 1] else { return nil }
                            return n
                        }()
                        TransitLegView(
                            leg: t,
                            isFirst: idx == 0,
                            isLast: idx == journey.legs.count - 1,
                            isDirectTransfer: prevTransit?.alightStop.id == t.boardStop.id,
                            isBeforeTransfer: nextTransit?.boardStop.id == t.alightStop.id,
                            isExpanded: expandedLegs.contains(t.id),
                            onToggleExpand: {
                                withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                    if expandedLegs.contains(t.id) { expandedLegs.remove(t.id) }
                                    else { expandedLegs.insert(t.id) }
                                }
                            }
                        )
                    case .walking(let w):
                        WalkingLegView(leg: w)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 32)
        }
        .navigationTitle("Journey")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarRole(.editor)
        .onAppear {
            if expandedLegs.isEmpty { expandedLegs = defaultExpandedLegs }
        }
    }

    private var heroHeader: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .firstTextBaseline, spacing: 10) {
                Text(journey.departureTime, style: .time)
                    .font(.system(size: 28, weight: .bold))
                Image(systemName: "arrow.right")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(.secondary)
                Text(journey.arrivalTime, style: .time)
                    .font(.system(size: 28, weight: .bold))
            }
            Text(subtitle)
                .font(.system(size: 15))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 20)
    }
}

// MARK: - Layout constants

private let kTimeW: CGFloat = 46
private let kNodeW: CGFloat = 14
private let kLineW: CGFloat = 2.5
private let kDotD: CGFloat = 12
private let kColGap: CGFloat = 12

// MARK: - TransitLegView

private struct TransitLegView: View {
    let leg: TransitLeg
    let isFirst: Bool
    let isLast: Bool
    let isDirectTransfer: Bool
    let isBeforeTransfer: Bool
    let isExpanded: Bool
    let onToggleExpand: () -> Void

    private var tColor: Color {
        leg.routeColor.uppercased() == "FFFFFF"
            ? Color(.label)
            : Color(hex: "#\(leg.routeColor)")
    }

    private func tf(_ d: Date) -> String {
        let f = DateFormatter(); f.dateFormat = "HH:mm"; return f.string(from: d)
    }

    var body: some View {
        VStack(spacing: 0) {
            boardRow
            middleSection
            alightRow
            if isBeforeTransfer {
                TransferConnectorRow()
            }
        }
    }

    private var boardRow: some View {
        HStack(alignment: .top, spacing: kColGap) {
            if isDirectTransfer {
                Color.clear.frame(width: kTimeW, height: 1)
            } else {
                Text(tf(leg.boardTime))
                    .font(.system(size: 13, weight: .semibold))
                    .frame(width: kTimeW, alignment: .trailing)
                    .padding(.top, 1)
            }

            VStack(spacing: 0) {
                if !isFirst && !isDirectTransfer {
                    Rectangle()
                        .fill(Color(.tertiaryLabel))
                        .frame(width: kLineW, height: 10)
                }
                Circle().fill(tColor).frame(width: kDotD, height: kDotD)
                Rectangle().fill(tColor).frame(width: kLineW).frame(minHeight: 14)
            }
            .frame(width: kNodeW, alignment: .center)

            VStack(alignment: .leading, spacing: 4) {
                if !isDirectTransfer {
                    Text(leg.boardStop.name)
                        .font(.system(size: 16, weight: .semibold))
                }
                HStack(spacing: 6) {
                    LineBadge(
                        name: leg.lineName,
                        color: "#\(leg.routeColor)",
                        textColor: leg.routeTextColor.isEmpty ? nil : "#\(leg.routeTextColor)",
                        size: .small
                    )
                    Text("→ \(leg.headsign)")
                        .font(.system(size: 13))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                .padding(.top, (isFirst || isDirectTransfer) ? 0 : 2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, (isFirst || isDirectTransfer) ? 0 : 10)
        }
    }

    private var middleSection: some View {
        HStack(alignment: .top, spacing: kColGap) {
            Color.clear.frame(width: kTimeW, height: 1)

            Rectangle()
                .fill(tColor)
                .frame(width: kLineW)
                .frame(width: kNodeW, alignment: .center)

            VStack(alignment: .leading, spacing: 0) {
                if !leg.intermediateStops.isEmpty {
                    Button(action: onToggleExpand) {
                        HStack(spacing: 4) {
                            Image(systemName: "chevron.down")
                                .font(.system(size: 11, weight: .bold))
                                .rotationEffect(isExpanded ? .degrees(0) : .degrees(-90))
                                .animation(.spring(response: 0.25, dampingFraction: 0.8), value: isExpanded)
                            Text("\(leg.intermediateStops.count) stops")
                                .font(.system(size: 13))
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 10)
                    .buttonStyle(.plain)

                    if isExpanded {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(leg.intermediateStops) { stop in
                                HStack(spacing: 8) {
                                    Circle()
                                        .strokeBorder(tColor, lineWidth: 1.5)
                                        .frame(width: 7, height: 7)
                                    Text(stop.name)
                                        .font(.system(size: 13))
                                        .foregroundStyle(.secondary)
                                    Spacer()
                                    Text(stop.time)
                                        .font(.system(size: 12))
                                        .foregroundStyle(.tertiary)
                                }
                            }
                        }
                        .padding(.bottom, 10)
                        .transition(.opacity.combined(with: .move(edge: .top)))
                    }
                } else {
                    Spacer().frame(height: 20)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var alightRow: some View {
        HStack(alignment: .top, spacing: kColGap) {
            Text(tf(leg.alightTime))
                .font(.system(size: 13, weight: .semibold))
                .frame(width: kTimeW, alignment: .trailing)
                .padding(.top, 1)

            VStack(spacing: 0) {
                Rectangle().fill(tColor).frame(width: kLineW).frame(minHeight: 14)
                Circle()
                    .strokeBorder(tColor, lineWidth: 2)
                    .frame(width: kDotD, height: kDotD)
                if !isLast && !isBeforeTransfer {
                    Rectangle()
                        .fill(Color(.tertiaryLabel))
                        .frame(width: kLineW, height: 10)
                }
            }
            .frame(width: kNodeW, alignment: .center)

            Text(leg.alightStop.name)
                .font(.system(size: 16, weight: .semibold))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 10)
        }
    }
}

// MARK: - TransferConnectorRow

private struct TransferConnectorRow: View {
    var body: some View {
        HStack(alignment: .center, spacing: kColGap) {
            Color.clear.frame(width: kTimeW, height: 1)

            VStack(spacing: 3) {
                ForEach(0..<4, id: \.self) { _ in
                    Rectangle()
                        .fill(Color(.tertiaryLabel))
                        .frame(width: 1.5, height: 4)
                }
            }
            .frame(width: kNodeW, alignment: .center)

            HStack(spacing: 5) {
                Image(systemName: "arrow.triangle.2.circlepath")
                    .font(.system(size: 11))
                    .foregroundStyle(Color(.secondaryLabel))
                Text("Change")
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 8)
        }
    }
}

// MARK: - WalkingLegView

private struct WalkingLegView: View {
    let leg: WalkingLeg

    var body: some View {
        HStack(alignment: .center, spacing: kColGap) {
            Color.clear.frame(width: kTimeW, height: 1)

            VStack(spacing: 3) {
                ForEach(0..<4, id: \.self) { _ in
                    Rectangle()
                        .fill(Color(.tertiaryLabel))
                        .frame(width: 1.5, height: 4)
                }
            }
            .frame(width: kNodeW, alignment: .center)

            HStack(spacing: 6) {
                Image(systemName: "figure.walk")
                    .font(.system(size: 16))
                    .foregroundStyle(Color(.secondaryLabel))
                let mins = max(1, leg.walkSeconds / 60)
                Text("\(mins) min walk")
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 10)
        }
    }
}
