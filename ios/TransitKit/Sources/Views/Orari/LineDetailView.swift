import SwiftUI

/// Route detail screen: header with line badge, direction picker, and stop sequence list.
struct LineDetailView: View {
    let route: Route
    @Environment(ScheduleStore.self) private var store
    @State private var selectedDirectionId: Int = 0

    private var lineColor: Color {
        Color(hex: route.color)
    }

    private var headerTextColor: Color {
        Color(hex: contrastingTextColor(for: route.color))
    }

    private var selectedDirection: RouteDirection? {
        route.directions.first { $0.id == selectedDirectionId }
    }

    private var stopsInDirection: [ResolvedStop] {
        store.stopsForRoute(route.id, directionId: selectedDirectionId)
    }

    // MARK: - Body

    var body: some View {
        ZStack(alignment: .top) {
            AppTheme.background.ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    // Header
                    lineHeader

                    // Direction picker
                    if route.directions.count > 1 {
                        directionPicker
                    }

                    // Section title
                    if !stopsInDirection.isEmpty {
                        stopsListHeader
                    }

                    // Stop sequence timeline
                    if !stopsInDirection.isEmpty {
                        stopsTimeline
                    }

                    // Official schedule link (only shown when route_url is present in GTFS)
                    if let routeUrl = route.url, let url = URL(string: routeUrl) {
                        Link(destination: url) {
                            HStack(spacing: 6) {
                                LucideIcon.externalLink.image
                                    .font(.system(size: 14, weight: .semibold))
                                Text(String(localized: "line_official_schedule"))
                                    .font(.system(size: 14, weight: .semibold))
                            }
                            .foregroundStyle(AppTheme.accent)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(AppTheme.accent.opacity(0.08), in: RoundedRectangle(cornerRadius: 10))
                            .padding(.horizontal, 16)
                        }
                        .padding(.top, 16)
                    }

                    Spacer(minLength: 100)
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .navigationDestination(for: ResolvedStop.self) { stop in
            StopDetailView(stop: stop)
        }
        .onAppear {
            if let first = route.directions.first {
                selectedDirectionId = first.id
            }
        }
    }

    // MARK: - Header

    private var lineHeader: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .bottomLeading) {
                // Background gradient
                LinearGradient(
                    colors: [lineColor, lineColor.opacity(0.7)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )

                // Subtle light overlay
                LinearGradient(
                    colors: [.white.opacity(0.15), .clear],
                    startPoint: .top,
                    endPoint: .bottom
                )

                // Content
                VStack(alignment: .leading, spacing: 10) {
                    Spacer(minLength: 16)

                    HStack(spacing: 12) {
                        // Large badge
                        Text(route.name)
                            .font(.system(size: 28, weight: .black, design: .rounded))
                            .foregroundStyle(headerTextColor)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(.white.opacity(0.2), in: RoundedRectangle(cornerRadius: 10))

                        VStack(alignment: .leading, spacing: 2) {
                            Text(route.longName)
                                .font(.system(size: 20, weight: .bold))
                                .foregroundStyle(headerTextColor)
                                .lineLimit(2)

                            Text(route.transitType.displayName)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(headerTextColor.opacity(0.85))
                        }

                        Spacer()
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 18)
            }
            .frame(minHeight: 130)
            .clipShape(
                UnevenRoundedRectangle(
                    bottomLeadingRadius: 20,
                    bottomTrailingRadius: 20
                )
            )
        }
    }

    // MARK: - Direction Picker

    private var directionPicker: some View {
        VStack(spacing: 0) {
            Picker(String(localized: "direction_label"), selection: $selectedDirectionId) {
                ForEach(route.directions) { dir in
                    Text(dir.headsign)
                        .lineLimit(1)
                        .tag(dir.id)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .sensoryFeedback(.selection, trigger: selectedDirectionId)
    }

    // MARK: - Stops List Header

    private var stopsListHeader: some View {
        HStack(spacing: 6) {
            LucideIcon.mapPin.image
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(lineColor)
            Text(String(localized: "stops_served"))
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(AppTheme.textSecondary)
            Spacer()
            Text("\(stopsInDirection.count)")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(lineColor)
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(lineColor.opacity(0.12), in: Capsule())
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 8)
    }

    // MARK: - Stops Timeline

    private var stopsTimeline: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(stopsInDirection.enumerated()), id: \.element.id) { idx, stop in
                let isFirst = idx == 0
                let isLast = idx == stopsInDirection.count - 1
                let isTerminal = isFirst || isLast

                // Other lines at this stop (excluding current)
                let coincidences = stop.lineNames.filter { $0 != route.name }

                NavigationLink(value: stop) {
                    HStack(alignment: .top, spacing: 0) {
                        // Timeline column
                        VStack(spacing: 0) {
                            if isFirst {
                                Color.clear.frame(width: 3, height: 14)
                            } else {
                                Rectangle().fill(lineColor).frame(width: 3, height: 14)
                            }

                            ZStack {
                                Circle()
                                    .fill(lineColor)
                                    .frame(width: isTerminal ? 12 : 8, height: isTerminal ? 12 : 8)
                                if isTerminal {
                                    Circle()
                                        .fill(AppTheme.background)
                                        .frame(width: 5, height: 5)
                                }
                            }

                            if isLast {
                                Color.clear.frame(width: 3).frame(maxHeight: .infinity)
                            } else {
                                Rectangle().fill(lineColor).frame(width: 3).frame(maxHeight: .infinity)
                            }
                        }
                        .frame(width: 32)

                        // Content
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 6) {
                                Text(stop.name)
                                    .font(.system(size: 15, weight: isTerminal ? .semibold : .regular))
                                    .foregroundStyle(AppTheme.textPrimary)
                                    .lineLimit(1)

                                Spacer(minLength: 4)

                                LucideIcon.chevronRight.image
                                    .font(.system(size: 10, weight: .semibold))
                                    .foregroundStyle(AppTheme.textTertiary)
                            }

                            // Transfer indicator
                            if stop.lineNames.count > 1 {
                                HStack(spacing: 3) {
                                    LucideIcon.refreshCw.image
                                        .resizable()
                                        .frame(width: 10, height: 10)
                                        .foregroundStyle(AppTheme.accent)
                                    Text(String(localized: "transfer_here"))
                                        .font(.system(size: 10, weight: .medium))
                                        .foregroundStyle(AppTheme.accent)
                                }
                            }

                            // Coincidence badges
                            if !coincidences.isEmpty {
                                coincidenceBadges(lines: coincidences)
                            }
                        }
                        .padding(.vertical, isTerminal ? 14 : 10)
                        .padding(.trailing, 16)
                    }
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("line_stop_\(stop.id)")
            }
        }
        .padding(.horizontal, 16)
    }

    // MARK: - Coincidence Badges

    @ViewBuilder
    private func coincidenceBadges(lines: [String]) -> some View {
        let maxVisible = 4
        let visible = Array(lines.prefix(maxVisible))
        let overflow = lines.count - visible.count

        HStack(spacing: 4) {
            ForEach(visible, id: \.self) { name in
                let r = store.routes.first { $0.name == name }
                LineBadge(
                    lineName: name,
                    color: r?.color ?? "#666666",
                    textColor: r?.textColor ?? "#FFFFFF",
                    transitType: r?.transitType ?? .bus,
                    size: .tiny
                )
            }
            if overflow > 0 {
                Text("+\(overflow)")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(AppTheme.textTertiary)
                    .padding(.horizontal, 4)
                    .padding(.vertical, 2)
                    .background(AppTheme.textTertiary.opacity(0.12), in: RoundedRectangle(cornerRadius: 3))
            }
        }
    }

}
