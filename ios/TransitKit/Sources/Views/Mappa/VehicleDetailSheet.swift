import SwiftUI

// MARK: - Vehicle Card

/// Inline card shown at the bottom of the map when tapping a live vehicle.
/// Not a sheet — rendered directly in the MappaTab ZStack.
struct VehicleDetailSheet: View {
    let vehicle: GtfsRtVehicle
    let route: APIRoute?
    let isFollowing: Bool
    let onToggleFollow: () -> Void
    let onOpenLine: (() -> Void)?
    let onOpenTrip: () -> Void
    let onDismiss: () -> Void

    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(ScheduleStore.self) private var scheduleStore

    /// Ticks every 15s — coupled with vehicle feed refresh cadence.
    @State private var now: Date = Date()

    private var transitType: TransitType {
        route.map { TransitType(gtfsRouteType: $0.transitType) } ?? .bus
    }

    private var directionLabel: String? {
        guard let route else { return nil }
        let routeNames = [route.name, route.longName]
            .compactMap { $0?.lowercased() }
        let headsigns = route.directions
            .sorted { $0.directionId < $1.directionId }
            .compactMap(\.headsign)
            .filter { headsign in
                let trimmed = headsign.trimmingCharacters(in: .whitespaces)
                guard !trimmed.isEmpty else { return false }
                return !routeNames.contains(trimmed.lowercased())
            }
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

    /// Predicted arrival at the current stop, combining the best available
    /// signals: (1) per-stop `arrival.time` from TripUpdates if the feed
    /// publishes it, otherwise (2) scheduled arrival from GTFS + `delay`.
    private var predictedArrival: Date? {
        guard !vehicle.currentStopId.isEmpty else { return nil }
        // (1) Direct arrival.time from TripUpdate if available
        if let direct = vehicleStore.arrival(forTripId: vehicle.tripId, stopId: vehicle.currentStopId) {
            return direct
        }
        // (2) Scheduled + delay fallback
        guard let scheduled = scheduledArrivalAtCurrentStop() else { return nil }
        let delaySec = TimeInterval(delay ?? 0)
        return scheduled.addingTimeInterval(delaySec)
    }

    /// Looks up the scheduled departure time for this trip at the current stop
    /// in today's operator calendar.
    private func scheduledArrivalAtCurrentStop() -> Date? {
        let rtStopId = vehicle.currentStopId
        guard !rtStopId.isEmpty,
              let station = scheduleStore.stop(forId: rtStopId),
              let apiStop = scheduleStore.scheduleResponse?.stops.first(where: { $0.id == station.id }),
              let dep = apiStop.departures.first(where: { $0.tripId == vehicle.tripId })
        else { return nil }
        let parts = dep.departureTime.split(separator: ":").compactMap { Int($0) }
        guard parts.count >= 2 else { return nil }
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = scheduleStore.operatorTimezone
        let today = cal.startOfDay(for: now)
        let seconds = parts[0] * 3600 + parts[1] * 60 + (parts.count > 2 ? parts[2] : 0)
        return cal.date(byAdding: .second, value: seconds, to: today)
    }

    /// "HH:mm" in operator tz for the predicted arrival.
    private var etaClockText: String? {
        guard let arrival = predictedArrival else { return nil }
        let fmt = DateFormatter()
        fmt.dateFormat = "HH:mm"
        fmt.timeZone = scheduleStore.operatorTimezone
        return fmt.string(from: arrival)
    }

    /// Maps the predicted arrival to the same four-state model used everywhere
    /// else in the app (DepartureRow / TimeDisplay): `minutes`/`departing` for
    /// the countdown, `absolute` for distant arrivals, `passed` once missed.
    /// Nil when we have no ETA signal at all (graceful hide).
    private var etaState: DepartureTimeState? {
        guard let arrival = predictedArrival, let clock = etaClockText else { return nil }
        let diff = arrival.timeIntervalSince(now)
        let minutes = Int((diff + 30) / 60)
        if diff < -60 { return .passed(clock) }
        if minutes <= 0 { return .departing }
        if minutes <= 60 { return .minutes(minutes) }
        return .absolute(clock)
    }

    /// Status-aware label for the stop row: "Prossima fermata" / "Fermo a" / "In arrivo a".
    private var stopRowLabel: String {
        switch vehicle.currentStatus {
        case .inTransitTo: String(localized: "vehicle_next_stop")
        case .stoppedAt:   String(localized: "vehicle_stopped_at")
        case .incomingAt:  String(localized: "vehicle_incoming_at")
        }
    }

    /// Freshness string derived from vehicle.timestamp (epoch seconds).
    private var freshnessText: String? {
        guard vehicle.timestamp > 0 else { return nil }
        let age = Int(now.timeIntervalSince1970) - Int(vehicle.timestamp)
        guard age >= 0 else { return nil }
        if age < 60 {
            return String(format: String(localized: "vehicle_updated_sec"), age)
        }
        let mins = age / 60
        return String(format: String(localized: "vehicle_updated_min"), mins)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            headerRow
            if directionLabel != nil || vehicle.occupancyStatus != nil {
                metaRow
            }
            if currentStopName != nil { stopRow }
            liveRow
            actionsRow
        }
        .padding(16)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: .black.opacity(0.2), radius: 12, y: 4)
        .task {
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 15_000_000_000)
                if Task.isCancelled { break }
                now = Date()
            }
        }
    }

    // MARK: - Row 1 — Badge + name + vehicle id + Live + close

    @ViewBuilder
    private var headerRow: some View {
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

            Spacer(minLength: 4)

            if !vehicle.label.isEmpty {
                Text("#\(vehicle.label)")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textSecondary)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 3)
                    .background(AppTheme.textPrimary.opacity(0.06))
                    .clipShape(Capsule())
            }

            if vehicle.wheelchairAccessible == .accessible {
                LucideIcon.accessibility.sized(24)
                    .foregroundStyle(AppTheme.accent)
                    .frame(width: 22, height: 22)
                    .background(AppTheme.accent.opacity(0.12))
                    .clipShape(Circle())
                    .accessibilityLabel(String(localized: "vehicle_accessible"))
            }

            Button(action: onDismiss) {
                LucideIcon.x.sized(16)
                    .foregroundStyle(AppTheme.textSecondary)
                    .frame(width: 26, height: 26)
                    .background(AppTheme.textPrimary.opacity(0.08))
                    .clipShape(Circle())
                    .frame(width: 44, height: 44)
                    .contentShape(Circle())
            }
        }
    }

    // MARK: - Row 2 — Direction + occupancy (delay is implicit in ETA now)

    @ViewBuilder
    private var metaRow: some View {
        HStack(spacing: 8) {
            if let dir = directionLabel {
                LucideIcon.chevronRight.sized(14)
                    .foregroundStyle(AppTheme.textTertiary)
                Text(dir)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
            if let occ = vehicle.occupancyStatus, let l = occupancyLabel(occ) {
                occupancyBadge(label: l, icon: occupancyIcon(occ))
            }
        }
    }

    // MARK: - Row 3 — Stop row (label + name/ETA clock + minutes countdown)

    @ViewBuilder
    private var stopRow: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                LucideIcon.mapPin.sized(14)
                    .foregroundStyle(AppTheme.textTertiary)
                Text(stopRowLabel)
                    .font(.caption)
                    .foregroundStyle(AppTheme.textTertiary)
            }
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(currentStopName ?? "")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(1)
                    .truncationMode(.tail)
                Spacer(minLength: 8)
                // Same stacked layout as DepartureRow (minutes "5'" above,
                // clock below), but no ≤5 min green accent — on the map card
                // every selected vehicle is imminent, the green would be
                // visual noise.
                if let state = etaState {
                    VStack(alignment: .trailing, spacing: 1) {
                        etaPrimary(state: state)
                        if case .minutes = state, let clock = etaClockText {
                            etaSecondary(clock)
                        } else if case .departing = state, let clock = etaClockText {
                            etaSecondary(clock)
                        }
                    }
                    .fixedSize(horizontal: true, vertical: false)
                }
            }
        }
    }

    // MARK: - ETA renderers (local variants of TimeDisplay without the ≤5 min green accent)

    @ViewBuilder
    private func etaPrimary(state: DepartureTimeState) -> some View {
        switch state {
        case .departing:
            Text("\u{2192}")
                .font(.system(.headline, weight: .bold))
                .foregroundStyle(AppTheme.textPrimary)
        case .minutes(let mins):
            HStack(alignment: .lastTextBaseline, spacing: 1) {
                Text("\(mins)")
                    .font(.system(.title3, weight: .bold).monospacedDigit())
                    .foregroundStyle(AppTheme.textPrimary)
                    .contentTransition(.numericText())
                Text("m")
                    .font(.system(.subheadline, weight: .bold))
                    .foregroundStyle(AppTheme.textSecondary)
            }
        case .hoursMinutes(let h, let m):
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text("\(h)h")
                    .font(.system(.subheadline, weight: .bold).monospacedDigit())
                    .foregroundStyle(AppTheme.textPrimary)
                if m > 0 {
                    Text("\(m)m")
                        .font(.system(.footnote, weight: .semibold).monospacedDigit())
                        .foregroundStyle(AppTheme.textSecondary)
                }
            }
        case .absolute(let time):
            Text(time)
                .font(.system(.headline, weight: .semibold).monospacedDigit())
                .foregroundStyle(AppTheme.textPrimary)
                .contentTransition(.numericText())
        case .passed(let time):
            Text(time)
                .font(.system(size: 13, weight: .medium, design: .monospaced))
                .foregroundStyle(AppTheme.textTertiary)
        }
    }

    @ViewBuilder
    private func etaSecondary(_ clock: String) -> some View {
        Text(clock)
            .font(.system(size: 11, weight: .medium, design: .monospaced))
            .foregroundStyle(AppTheme.textSecondary)
    }

    // MARK: - Live + freshness row

    @ViewBuilder
    private var liveRow: some View {
        HStack(spacing: 6) {
            LiveBadge()
            if let fresh = freshnessText {
                Text("·")
                    .font(.caption)
                    .foregroundStyle(AppTheme.textTertiary)
                Text(fresh)
                    .font(.caption)
                    .foregroundStyle(AppTheme.textTertiary)
                    .monospacedDigit()
            }
            Spacer(minLength: 0)
        }
    }

    // MARK: - Row N — Full-width action buttons at the bottom
    //
    // Layout: Follow = compact icon-only (fixed 44pt square) to avoid truncation of
    // the long localized label ("Follow vehicle" / "Segui il mezzo"). Linea = ghost/
    // outlined labeled. Corsa = filled/primary labeled. The two labeled buttons share
    // the remaining width so they always have room without squeezing.

    @ViewBuilder
    private var actionsRow: some View {
        HStack(spacing: 10) {
            // Follow — icon-only compact square. No text label at all, so the
            // EN/IT localization length never causes truncation. A11y label is
            // mandatory because icon-only buttons must be discoverable by VoiceOver.
            Button(action: onToggleFollow) {
                LucideIcon.navigation.sized(18)
                    .foregroundStyle(isFollowing ? Color.white : AppTheme.accent)
                    .frame(width: 44, height: 44)
                    .background(
                        isFollowing ? AppTheme.accent : AppTheme.accent.opacity(0.12),
                        in: RoundedRectangle(cornerRadius: 12, style: .continuous)
                    )
            }
            .buttonStyle(.plain)
            .accessibilityLabel(String(localized: isFollowing ? "vehicle_unfollow" : "vehicle_follow"))
            .accessibilityIdentifier("vehicle_card_follow")

            if let onOpenLine {
                actionButton(
                    icon: .map,
                    title: String(localized: "vehicle_open_line"),
                    filled: false,
                    action: onOpenLine
                )
                .accessibilityIdentifier("vehicle_card_open_line")
            }
            // Corsa = filled/primary — visually dominant CTA
            actionButton(
                icon: .list,
                title: String(localized: "vehicle_open_trip"),
                filled: true,
                action: onOpenTrip
            )
            .accessibilityIdentifier("vehicle_card_open_trip")
        }
    }

    @ViewBuilder
    private func actionButton(icon: LucideIcon, title: String, filled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                icon.sized(13)
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
                    .allowsTightening(true)
            }
            .frame(maxWidth: .infinity, minHeight: 44)
            .foregroundStyle(filled ? Color.white : AppTheme.textPrimary)
            .background(filled ? AppTheme.accent : AppTheme.textPrimary.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Occupancy

    @ViewBuilder
    private func occupancyBadge(label: String, icon: LucideIcon) -> some View {
        HStack(spacing: 4) {
            icon.sized(11)
            Text(label)
                .font(.caption.weight(.semibold))
        }
        .foregroundStyle(AppTheme.textSecondary)
        .padding(.horizontal, 8)
        .padding(.vertical, 3)
        .background(AppTheme.textPrimary.opacity(0.08))
        .clipShape(Capsule())
    }

    private func occupancyLabel(_ status: OccupancyStatus) -> String? {
        switch status {
        case .empty, .manySeatsAvailable: return String(localized: "occupancy_seats_available")
        case .fewSeatsAvailable:          return String(localized: "occupancy_few_seats")
        case .standingRoomOnly:           return String(localized: "occupancy_standing_only")
        case .crushedStandingRoomOnly,
             .full:                       return String(localized: "occupancy_full")
        case .notAcceptingPassengers,
             .notBoardable:               return String(localized: "occupancy_not_boarding")
        case .noDataAvailable:            return nil
        }
    }

    private func occupancyIcon(_ status: OccupancyStatus) -> LucideIcon {
        switch status {
        case .empty, .manySeatsAvailable,
             .fewSeatsAvailable,
             .standingRoomOnly,
             .crushedStandingRoomOnly,
             .full:                       return .users
        case .notAcceptingPassengers,
             .notBoardable:               return .x
        case .noDataAvailable:            return .info
        }
    }
}
