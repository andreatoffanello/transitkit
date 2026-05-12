import SwiftUI

/// Trip stop sequence view: shows all stops in a trip with timeline visualization.
/// Current stop is highlighted, times displayed per stop, past stops dimmed.
struct TripDetailView: View {
    let departure: Departure
    let fromStop: ResolvedStop
    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore

    /// One row in the trip timeline: the resolved stop + its scheduled
    /// departure time at this trip (e.g. "16:45"). Stops are ordered by
    /// time, which mirrors physical stop_sequence for any non-loop GTFS trip.
    struct TripStopRow: Identifiable {
        let stop: ResolvedStop
        let timeHHmm: String
        var id: String { stop.id + "_" + timeHHmm }
    }

    /// Reconstructs the trip's stop sequence + per-stop scheduled times by
    /// scanning the CDN schedule JSON for every stop that publishes a
    /// departure with this `tripId`. No runtime API call — fully offline
    /// from the once-loaded schedule cache. Cheap: ~N stops × a few
    /// departures each.
    private var tripRows: [TripStopRow]? {
        guard let tripId = departure.tripId, !tripId.isEmpty,
              let schedule = store.scheduleResponse else { return nil }
        let entries = schedule.stops.compactMap { apiStop -> (stationId: String, rawTime: String)? in
            guard let dep = apiStop.departures.first(where: { $0.tripId == tripId })
            else { return nil }
            return (apiStop.id, dep.departureTime)
        }
        guard !entries.isEmpty else { return nil }
        let sorted = entries.sorted { $0.rawTime < $1.rawTime }
        return sorted.compactMap { entry in
            guard let stop = store.stop(forId: entry.stationId) else { return nil }
            // "HH:MM:SS" → "HH:MM"
            let hhmm = String(entry.rawTime.prefix(5))
            return TripStopRow(stop: stop, timeHHmm: hhmm)
        }
    }

    private var tripStops: [ResolvedStop]? {
        tripRows?.map(\.stop)
    }

    private var lineColor: Color {
        let c = Color(hex: departure.color)
        return isVeryLight(c) ? .blue : c
    }

    /// Live vehicle serving this trip (if still in feed). Drives the "Ora"
    /// highlight so the timeline tracks the bus as it progresses — not frozen
    /// to whatever stop the sheet was opened from.
    private var liveVehicle: GtfsRtVehicle? {
        guard let tripId = departure.tripId, !tripId.isEmpty else { return nil }
        return vehicleStore.vehicle(forTripId: tripId)
    }

    private var originIndex: Int {
        guard let rows = tripRows else { return 0 }
        // 1) Prefer the live vehicle's current stop — moves as the bus does.
        //    Match against both the synthetic station id AND the native GTFS
        //    stop_ids aggregated under it (RT feeds always use the latter).
        if let vehicle = liveVehicle, !vehicle.currentStopId.isEmpty {
            if let idx = rows.firstIndex(where: { row in
                row.stop.id == vehicle.currentStopId ||
                row.stop.gtfsStopIds.contains(vehicle.currentStopId)
            }) {
                return idx
            }
        }
        // 2) Fallback: the stop the sheet was opened from.
        return rows.firstIndex(where: { $0.stop.id == fromStop.id }) ?? 0
    }

    // MARK: - Body

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                tripHeader

                if let rows = tripRows, !rows.isEmpty {
                    stopsTimeline(rows: rows)
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
    }

    // MARK: - Trip Header

    private var tripHeader: some View {
        HStack(spacing: 12) {
            LineBadge(departure: departure, size: .large)

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

    private func stopsTimeline(rows: [TripStopRow]) -> some View {
        let origin = originIndex

        return ScrollViewReader { proxy in
            VStack(alignment: .leading, spacing: 0) {
                Divider()
                    .padding(.horizontal, 20)
                    .padding(.bottom, 4)

                ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
                    let isTerminal = index == 0 || index == rows.count - 1
                    let isOrigin = index == origin
                    let isPast = index < origin
                    let dotSize: CGFloat = isOrigin ? 14 : (isTerminal ? 12 : 8)

                    timelineRow(
                        stop: row.stop,
                        timeHHmm: row.timeHHmm,
                        index: index,
                        totalStops: rows.count,
                        origin: origin,
                        isTerminal: isTerminal,
                        isOrigin: isOrigin,
                        isPast: isPast,
                        dotSize: dotSize
                    )
                    .id(row.id)
                }
            }
            .onAppear {
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 150_000_000)
                    withAnimation(.easeInOut(duration: 0.3)) {
                        proxy.scrollTo(rows[min(origin, rows.count - 1)].id, anchor: .center)
                    }
                }
            }
        }
    }

    // MARK: - Timeline Row

    @ViewBuilder
    private func timelineRow(
        stop: ResolvedStop,
        timeHHmm: String,
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
                    .overlay {
                        if isOrigin && !isTerminal {
                            Circle()
                                .stroke(lineColor, lineWidth: 2)
                                .frame(width: dotSize + 6, height: dotSize + 6)
                        }
                    }
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
                        Text(String(localized: "time_now"))
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
                                name: name,
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

            // Scheduled time at this stop (HH:mm). Dimmed for past stops,
            // accent-colored on the origin to echo the "Ora" pill.
            Text(timeHHmm)
                .font(.system(size: 13, weight: isOrigin ? .semibold : .medium, design: .monospaced))
                .foregroundStyle(
                    isPast ? AppTheme.textTertiary
                        : isOrigin ? lineColor
                        : AppTheme.textSecondary
                )

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
        .background {
            if isOrigin {
                lineColor.opacity(0.12)
                    .overlay(alignment: .leading) {
                        lineColor
                            .frame(width: 3)
                            .clipShape(Capsule())
                    }
            }
        }
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
