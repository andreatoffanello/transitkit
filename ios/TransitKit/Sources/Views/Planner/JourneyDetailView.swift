import SwiftUI

@MainActor
private func formatJourneyTime(_ date: Date, tz: TimeZone) -> String {
    ClockTime.clock(date, timeZone: tz)
}

// MARK: - JourneyDetailView
//
// Timeline 3 colonne (orario | nodo | contenuto). Layout portato da Movete:
// - boardRow / middleSection collassabile / alightRow per ogni TransitLeg
// - TransferConnectorRow tra due transit legs alla stessa fermata
// - WalkingLegView con DashedVerticalLine + footprints + minuti + metri
// - intermediate stops espanse di default per journey diretti (1 transit leg)
// - EndpointRow bookend (PARTENZA/ARRIVO) quando primo/ultimo leg è camminata

struct JourneyDetailView: View {
    let journey: Journey
    /// Bookend "Da:..." sopra timeline quando primo leg è walking.
    var originName: String? = nil
    /// Bookend "A:..." sotto timeline quando ultimo leg è walking.
    var destinationName: String? = nil

    @Environment(\.operatorTimeZone) private var operatorTimeZone
    @State private var expandedLegs: Set<UUID> = []
    @State private var showFullscreenMap = false

    private var defaultExpandedLegs: Set<UUID> {
        journey.transitLegs.count == 1 ? [journey.transitLegs[0].id] : []
    }

    private var headerSubtitle: String {
        let dur = "\(journey.durationMinutes) \(String(localized: "min_abbrev"))"
        let changes: String = {
            switch journey.transfers {
            case 0:  return String(localized: "planner_direct")
            case 1:  return String(localized: "planner_change_one")
            default: return String(format: NSLocalizedString("planner_change_count", comment: ""), journey.transfers)
            }
        }()
        let transit: String = journey.totalTransitSeconds >= 60
            ? " · " + String(format: NSLocalizedString("planner_transit_total", comment: ""), journey.totalTransitSeconds / 60)
            : ""
        let walk: String = journey.totalWalkSeconds > 90
            ? " · " + String(format: NSLocalizedString("planner_walking_total", comment: ""), journey.totalWalkSeconds / 60)
            : ""
        return "\(dur) · \(changes)\(transit)\(walk)"
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                heroHeader

                // Mappa overview compatta — tap per fullscreen
                mapPreview
                    .padding(.bottom, 16)

                // Bookend partenza
                if case .walking = journey.legs.first, let name = originName {
                    EndpointRow(time: journey.departureTime, name: name, role: .origin)
                }

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
                            isDirectTransfer: prevTransit?.alightStop.id == t.boardStop.id && prevTransit != nil,
                            isBeforeTransfer: nextTransit?.boardStop.id == t.alightStop.id && nextTransit != nil,
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

                // Bookend arrivo
                if case .walking = journey.legs.last, let name = destinationName {
                    EndpointRow(time: journey.arrivalTime, name: name, role: .destination)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 32)
        }
        .navigationTitle(String(localized: "planner_journey"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbarRole(.editor)
        .toolbar(.hidden, for: .tabBar)
        .onAppear {
            if expandedLegs.isEmpty { expandedLegs = defaultExpandedLegs }
        }
        .navigationDestination(isPresented: $showFullscreenMap) {
            JourneyMapView(journey: journey)
                .ignoresSafeArea(edges: .bottom)
                .navigationTitle(String(localized: "planner_route_map"))
                .navigationBarTitleDisplayMode(.inline)
        }
    }

    // MARK: - Map preview

    private var mapPreview: some View {
        ZStack(alignment: .topTrailing) {
            JourneyMapView(journey: journey, fixedTier: .city)
                .allowsHitTesting(false)
                .frame(height: 200)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color(.separator), lineWidth: 0.5)
                )

            // Decorativo (l'intera preview è tappabile) — stesso bottone
            // canonico del chrome mappa per coerenza visiva.
            MapCircleButton(icon: .maximize2)
                .padding(8)
        }
        .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .onTapGesture {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            showFullscreenMap = true
        }
        .accessibilityIdentifier("journey_map_preview")
    }

    private var heroHeader: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .firstTextBaseline, spacing: 10) {
                ClockTime.styledText(formatJourneyTime(journey.departureTime, tz: operatorTimeZone),
                                     size: 28, weight: .bold, design: .default, color: .primary)
                    .monospacedDigit()
                LucideIcon.arrowRight.sized(16)
                    .foregroundStyle(.secondary)
                ClockTime.styledText(formatJourneyTime(journey.arrivalTime, tz: operatorTimeZone),
                                     size: 28, weight: .bold, design: .default, color: .primary)
                    .monospacedDigit()
            }
            Text(headerSubtitle)
                .font(.system(size: 15))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
        .padding(.top, 16)
        .padding(.bottom, 20)
    }
}

// MARK: - Layout constants

private let kTimeW:  CGFloat = 56
private let kNodeW:  CGFloat = 14
private let kLineW:  CGFloat = 2.5
private let kDotD:   CGFloat = 12
private let kColGap: CGFloat = 12

// MARK: - EndpointRow
//
// Bookend partenza/arrivo per i journey che iniziano o finiscono con una
// camminata: mostra il nome del posto selezionato dall'utente con l'orario
// canonical del journey, allineato alle 3 colonne della timeline.

private struct EndpointRow: View {
    enum Role { case origin, destination }
    let time: Date
    let name: String
    let role: Role

    @Environment(\.operatorTimeZone) private var operatorTimeZone

    // Allineamento verticale: il dot deve sedere sulla stessa baseline del
    // nome (Text body 15pt). La mini-label PARTENZA/ARRIVO è 11pt sopra il
    // nome — alta circa 18pt incluso il padding spacing(2) + cap-height SF.
    // Time + dot pushati giù di 18pt per allinearsi al nome (non al label).
    private let labelOffset: CGFloat = 18

    var body: some View {
        HStack(alignment: .top, spacing: kColGap) {
            ClockTime.styledText(formatJourneyTime(time, tz: operatorTimeZone),
                                 size: 13, weight: .semibold, design: .default, color: .primary)
                .monospacedDigit()
                .lineLimit(1)
                .frame(width: kTimeW, alignment: .trailing)
                .padding(.top, labelOffset)

            VStack(spacing: 0) {
                if role == .destination {
                    Rectangle()
                        .fill(Color(.tertiaryLabel))
                        .frame(width: kLineW)
                        .frame(maxHeight: .infinity)
                }
                Group {
                    if role == .origin {
                        Circle().fill(AppTheme.accent)
                    } else {
                        Circle()
                            .strokeBorder(Color(.label), lineWidth: 2)
                            .background(Circle().fill(Color(.systemBackground)))
                    }
                }
                .frame(width: kDotD, height: kDotD)
                .padding(.top, role == .origin ? labelOffset : 0)
                .padding(.bottom, role == .destination ? labelOffset : 0)
                if role == .origin {
                    Rectangle()
                        .fill(Color(.tertiaryLabel))
                        .frame(width: kLineW)
                        .frame(maxHeight: .infinity)
                }
            }
            .frame(width: kNodeW, alignment: .center)

            VStack(alignment: .leading, spacing: 2) {
                Text(role == .origin
                     ? String(localized: "planner_endpoint_departure")
                     : String(localized: "planner_endpoint_arrival"))
                    .font(.system(size: 11, weight: .semibold))
                    .kerning(0.6)
                    .foregroundStyle(.tertiary)
                Text(name)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 6)
    }
}

// MARK: - TransitLegView

private struct TransitLegView: View {
    let leg: TransitLeg
    let isFirst: Bool
    let isLast: Bool
    let isDirectTransfer: Bool
    let isBeforeTransfer: Bool
    let isExpanded: Bool
    let onToggleExpand: () -> Void

    @Environment(\.operatorTimeZone) private var operatorTimeZone

    private var tColor: Color {
        leg.routeColor.uppercased() == "FFFFFF"
            ? Color(.label)
            : Color(hex: "#\(leg.routeColor)")
    }

    private func tf(_ d: Date) -> String { formatJourneyTime(d, tz: operatorTimeZone) }

    var body: some View {
        VStack(spacing: 0) {
            boardRow
            middleSection
            alightRow
            if isBeforeTransfer { TransferConnectorRow() }
        }
    }

    // Padding-top comune che allinea verticalmente time + dot + stop name.
    // 10pt = lascia spazio per il gap dalla riga precedente (walking/transfer).
    // 0 per il primo leg o transfer diretti (dove non c'è gap sopra).
    private var topOffset: CGFloat {
        (isFirst || isDirectTransfer) ? 0 : 10
    }

    private var boardRow: some View {
        HStack(alignment: .top, spacing: kColGap) {
            if isDirectTransfer {
                Color.clear.frame(width: kTimeW, height: 1)
            } else {
                ClockTime.styledText(tf(leg.boardTime),
                                     size: 13, weight: .semibold, design: .default, color: .primary)
                    .monospacedDigit()
                    .lineLimit(1)
                    .frame(width: kTimeW, alignment: .trailing)
                    .padding(.top, topOffset + 2)
            }

            // ZStack: linea continua come sfondo, dot/rect in foreground.
            ZStack(alignment: .top) {
                // Background: rect colorato che parte sotto il dot e va in fondo
                // alla row → garantisce continuità con middleSection.
                VStack(spacing: 0) {
                    Color.clear.frame(width: kLineW, height: topOffset + kDotD)
                    Rectangle().fill(tColor).frame(width: kLineW)
                        .frame(maxHeight: .infinity)
                }
                // Foreground: dot pieno al topOffset (allineato col stop name).
                Circle().fill(tColor)
                    .frame(width: kDotD, height: kDotD)
                    .padding(.top, topOffset)
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
                    if !leg.headsign.isEmpty {
                        HStack(spacing: 4) {
                            LucideIcon.arrowRight.sized(11)
                                .foregroundStyle(.tertiary)
                            Text(leg.headsign)
                                .font(.system(size: 13))
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                                .truncationMode(.tail)
                        }
                    }
                    Spacer(minLength: 6)
                    Text(String(format: NSLocalizedString("planner_leg_duration", comment: ""),
                                max(1, Int(leg.alightTime.timeIntervalSince(leg.boardTime)) / 60)))
                        .font(.system(size: 13).monospacedDigit())
                        .foregroundStyle(.tertiary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, (isFirst || isDirectTransfer) ? 0 : 2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, topOffset)
        }
    }

    private var middleSection: some View {
        HStack(alignment: .top, spacing: kColGap) {
            Color.clear.frame(width: kTimeW, height: 1)
            Rectangle()
                .fill(tColor)
                .frame(width: kLineW)
                .frame(width: kNodeW, alignment: .center)
                .frame(maxHeight: .infinity)

            VStack(alignment: .leading, spacing: 0) {
                if !leg.intermediateStops.isEmpty {
                    Button(action: onToggleExpand) {
                        HStack(spacing: 4) {
                            LucideIcon.chevronDown.sized(11)
                                .rotationEffect(isExpanded ? .degrees(0) : .degrees(-90))
                                .animation(.spring(response: 0.25, dampingFraction: 0.8), value: isExpanded)
                                .foregroundStyle(.secondary)
                            Text(intermediateStopsLabel)
                                .font(.system(size: 13))
                                .foregroundStyle(.secondary)
                        }
                    }
                    .buttonStyle(.plain)
                    .padding(.vertical, 10)

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
                                    Text(tf(stop.arrivalTime))
                                        .font(.system(size: 12).monospacedDigit())
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

    private var intermediateStopsLabel: String {
        let n = leg.intermediateStops.count
        return n == 1
            ? String(localized: "planner_intermediate_one")
            : String(format: NSLocalizedString("planner_intermediate_count", comment: ""), n)
    }

    private var alightRow: some View {
        HStack(alignment: .top, spacing: kColGap) {
            ClockTime.styledText(tf(leg.alightTime),
                                 size: 13, weight: .semibold, design: .default, color: .primary)
                .monospacedDigit()
                .lineLimit(1)
                .frame(width: kTimeW, alignment: .trailing)
                .padding(.top, 12)

            // ZStack: linea continua come sfondo, circle in foreground.
            ZStack(alignment: .top) {
                // Background: linea colorata che parte dal top della row e
                // termina visivamente sotto il centro del circle (allineato
                // al nome stop alight). Sotto il circle è trasparente (così
                // la prossima leg/gap inizia pulito).
                VStack(spacing: 0) {
                    Rectangle().fill(tColor).frame(width: kLineW)
                        .frame(height: 10 + kDotD / 2)
                    Color.clear.frame(width: kLineW).frame(maxHeight: .infinity)
                }
                // Foreground: circle al level del nome stop (padding 10).
                Circle()
                    .strokeBorder(tColor, lineWidth: 2)
                    .background(Circle().fill(Color(.systemBackground)))
                    .frame(width: kDotD, height: kDotD)
                    .padding(.top, 10)
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

            DashedVerticalLine()
                .frame(width: kNodeW, alignment: .center)

            HStack(spacing: 5) {
                LucideIcon.repeat2.sized(13)
                    .foregroundStyle(.secondary)
                Text(String(localized: "planner_transfer_line"))
                    .font(.system(size: 13))
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

            DashedVerticalLine()
                .frame(width: kNodeW, alignment: .center)

            HStack(spacing: 6) {
                LucideIcon.footprints.sized(14)
                    .foregroundStyle(.secondary)
                Text(String(format: NSLocalizedString("planner_walking_minutes", comment: ""),
                            max(1, leg.walkSeconds / 60)))
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(.secondary)
                if leg.distanceMeters >= 50 {
                    Text("·")
                        .font(.system(size: 12))
                        .foregroundStyle(.tertiary)
                    Text("\(leg.distanceMeters) m")
                        .font(.system(size: 12))
                        .foregroundStyle(.tertiary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 10)
        }
    }
}

// MARK: - DashedVerticalLine
//
// Linea verticale tratteggiata via Path stroke dash — più elegante dello stack
// di Rectangle. Identica a `movete/.../DashedVerticalLine`.

private struct DashedVerticalLine: View {
    var color: Color = Color(.tertiaryLabel).opacity(0.85)
    var lineWidth: CGFloat = 1.5
    var dash: [CGFloat] = [3, 3]
    var minHeight: CGFloat = 22

    var body: some View {
        GeometryReader { geo in
            Path { path in
                let x = geo.size.width / 2
                path.move(to: CGPoint(x: x, y: 0))
                path.addLine(to: CGPoint(x: x, y: geo.size.height))
            }
            .stroke(color, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round, dash: dash))
        }
        .frame(minHeight: minHeight)
    }
}
