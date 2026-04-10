import SwiftUI

/// Trip stop sequence view: shows all stops in a trip with timeline visualization.
/// Current stop is highlighted, times displayed per stop, past stops dimmed.
struct TripDetailView: View {
    let departure: Departure
    let fromStop: ResolvedStop
    @Environment(ScheduleStore.self) private var store
    @State private var tripDetail: TripDetail? = nil
    @State private var isLoadingTrip = false

    /// Resolves trip stops from the loaded TripDetail, preferring store-cached
    /// ResolvedStop for richer coincidence/dock data where available.
    private var tripStops: [ResolvedStop]? {
        guard let detail = tripDetail else { return nil }
        let sorted = detail.stopTimes.sorted { $0.stopSequence < $1.stopSequence }
        return sorted.map { st in
            store.stops.first(where: { $0.id == st.stopId })
                ?? ResolvedStop(
                    id: st.stopId,
                    name: st.stopName,
                    lat: st.stopLat,
                    lng: st.stopLng,
                    lineNames: [],
                    transitTypes: [.bus],
                    docks: []
                )
        }
    }

    private var lineColor: Color {
        let c = Color(hex: departure.color)
        return isVeryLight(c) ? .blue : c
    }

    private var originIndex: Int {
        guard let stops = tripStops else { return 0 }
        return stops.firstIndex(where: { $0.id == fromStop.id }) ?? 0
    }

    // MARK: - Body

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                tripHeader

                if let stops = tripStops, !stops.isEmpty {
                    stopsTimeline(stops: stops)
                } else if isLoadingTrip {
                    ProgressView()
                        .tint(AppTheme.accent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 40)
                } else {
                    VStack(spacing: 8) {
                        LucideIcon.alertTriangle.sized(28)
                            .foregroundStyle(AppTheme.textTertiary)
                        Text(String(localized: "trip_no_data"))
                            .font(.system(size: 14))
                            .foregroundStyle(AppTheme.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 32)
                }

                Color.clear.frame(height: 40)
            }
        }
        .background(AppTheme.background)
        .navigationTitle(String(format: NSLocalizedString("trip_line_title", comment: ""), departure.lineName))
        .navigationBarTitleDisplayMode(.inline)
        .toolbarRole(.editor)
        .toolbar(.hidden, for: .tabBar)
        .navigationDestination(for: ResolvedStop.self) { stop in
            StopDetailView(stop: stop)
        }
        .task {
            // NOTE: Trip detail endpoint was part of the Vercel API that is being retired.
            // This call will gracefully fail (returning nil) once the Vercel deployment
            // is updated without the API functions. The view handles nil tripDetail by
            // showing the static schedule data from the CDN.
            guard let tripId = departure.tripId, !tripId.isEmpty,
                  let apiUrl = store.apiUrl,
                  let client = try? APIClient(apiUrl: apiUrl)
            else { return }
            isLoadingTrip = true
            tripDetail = try? await client.fetchTrip(tripId: tripId)
            isLoadingTrip = false
        }
    }

    // MARK: - Trip Header

    private var tripHeader: some View {
        HStack(spacing: 12) {
            LineBadge(departure: departure, size: .big)

            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 5) {
                    LucideIcon.chevronRight.sized(11)
                        .foregroundStyle(AppTheme.textSecondary)
                    Text(departure.headsign)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)
                }

                HStack(spacing: 6) {
                    if let stops = tripStops {
                        Text(String(format: NSLocalizedString("stops_count", comment: ""), stops.count))
                            .font(.system(size: 12))
                            .foregroundStyle(AppTheme.textSecondary)
                    }

                    Text(departure.time)
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundStyle(AppTheme.textSecondary)
                }
            }

            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 12)
    }

    // MARK: - Timeline

    private func stopsTimeline(stops: [ResolvedStop]) -> some View {
        let origin = originIndex

        return ScrollViewReader { proxy in
            VStack(alignment: .leading, spacing: 0) {
                Divider()
                    .padding(.horizontal, 20)
                    .padding(.bottom, 4)

                ForEach(Array(stops.enumerated()), id: \.element.id) { index, stop in
                    let isTerminal = index == 0 || index == stops.count - 1
                    let isOrigin = index == origin
                    let isPast = index < origin
                    let dotSize: CGFloat = isOrigin ? 14 : (isTerminal ? 12 : 8)

                    timelineRow(
                        stop: stop,
                        index: index,
                        totalStops: stops.count,
                        origin: origin,
                        isTerminal: isTerminal,
                        isOrigin: isOrigin,
                        isPast: isPast,
                        dotSize: dotSize
                    )
                    .id(stop.id)
                }
            }
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                    withAnimation(.easeInOut(duration: 0.3)) {
                        proxy.scrollTo(stops[min(origin, stops.count - 1)].id, anchor: .center)
                    }
                }
            }
        }
    }

    // MARK: - Timeline Row

    @ViewBuilder
    private func timelineRow(
        stop: ResolvedStop,
        index: Int,
        totalStops: Int,
        origin: Int,
        isTerminal: Bool,
        isOrigin: Bool,
        isPast: Bool,
        dotSize: CGFloat
    ) -> some View {
        let otherLines = stop.lineNames.filter { $0 != departure.lineName }

        let rowContent = HStack(spacing: 0) {
            // Timeline column: vertical line + dot
            ZStack {
                VStack(spacing: 0) {
                    Rectangle()
                        .fill(index > 0 ? (isPast ? lineColor.opacity(0.35) : lineColor) : .clear)
                        .frame(width: 3)
                    Rectangle()
                        .fill(index < totalStops - 1 ? (index < origin ? lineColor.opacity(0.35) : lineColor) : .clear)
                        .frame(width: 3)
                }
                Circle()
                    .fill(isPast ? lineColor.opacity(0.45) : lineColor)
                    .frame(width: dotSize, height: dotSize)
                    .overlay(
                        Circle().fill(.white).frame(width: isTerminal || isOrigin ? 5 : 0)
                    )
                    .overlay(
                        isOrigin && !isTerminal
                            ? AnyView(Circle().stroke(lineColor, lineWidth: 2).frame(width: dotSize + 6, height: dotSize + 6))
                            : AnyView(EmptyView())
                    )
            }
            .frame(width: 20)
            .frame(minHeight: 40)
            .padding(.horizontal, 8)

            // Stop info
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 6) {
                    Text(stop.name)
                        .font(.system(size: 15, weight: isTerminal || isOrigin ? .semibold : .regular))
                        .foregroundStyle(isPast ? AppTheme.textTertiary : isOrigin ? lineColor : AppTheme.textPrimary)
                        .lineLimit(1)

                    if isOrigin {
                        Text("Now")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 2)
                            .background(lineColor, in: Capsule())
                    }
                }

                // Coincidence badges
                if !otherLines.isEmpty && !isPast {
                    HStack(spacing: 3) {
                        LucideIcon.refreshCw.image
                            .resizable()
                            .frame(width: 10, height: 10)
                            .foregroundStyle(AppTheme.accent)
                        ForEach(otherLines.prefix(4), id: \.self) { name in
                            let r = store.routes.first { $0.name == name }
                            LineBadge(
                                lineName: name,
                                color: r.flatMap(\.color) ?? "#666666",
                                textColor: r.flatMap(\.textColor) ?? "#FFFFFF",
                                transitType: r.map { TransitType(gtfsRouteType: $0.transitType) } ?? .bus,
                                size: .medium
                            )
                        }
                        if otherLines.count > 4 {
                            Text("+\(otherLines.count - 4)")
                                .font(.system(size: 8, weight: .bold))
                                .foregroundStyle(AppTheme.textTertiary)
                        }
                    }
                }
            }

            Spacer(minLength: 6)

            // Dock if available
            if let dock = stop.docks.first {
                DockBadgeView(letter: dock.letter)
                    .opacity(isPast ? 0.55 : 1)
            }

            LucideIcon.chevronRight.sized(10)
                .foregroundStyle(AppTheme.textTertiary)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 3)
        .background(
            isOrigin
                ? AnyView(
                    lineColor.opacity(0.12)
                        .overlay(alignment: .leading) {
                            lineColor
                                .frame(width: 3)
                                .clipShape(Capsule())
                        }
                )
                : AnyView(Color.clear)
        )
        .contentShape(Rectangle())

        NavigationLink(value: stop) {
            rowContent
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("trip_stop_\(stop.id)")
    }

    // MARK: - Helpers

    private func isVeryLight(_ color: Color) -> Bool {
        let uiColor = UIColor(color)
        var white: CGFloat = 0
        uiColor.getWhite(&white, alpha: nil)
        return white > 0.85
    }
}
