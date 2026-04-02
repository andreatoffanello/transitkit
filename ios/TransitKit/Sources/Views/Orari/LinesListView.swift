import SwiftUI

/// List of all routes/lines, grouped by transit type with search and filter.
/// Each row shows a LineBadge with GTFS color, long name, transit type icon, and direction count.
struct LinesListView: View {
    let searchQuery: String
    let transitTypeFilter: TransitType?
    @Environment(ScheduleStore.self) private var store
    @State private var collapsedTypes: Set<TransitType> = []

    // MARK: - Filtering

    private var filteredRoutes: [Route] {
        var result = store.routes

        // Filter by transit type
        if let type = transitTypeFilter {
            result = result.filter { $0.transitType == type }
        }

        // Filter by search query
        if !searchQuery.isEmpty {
            let query = searchQuery.lowercased()
            result = result.filter { route in
                route.name.lowercased().contains(query)
                || route.longName.lowercased().contains(query)
            }
        }

        return result.sorted {
            $0.name.localizedStandardCompare($1.name) == .orderedAscending
        }
    }

    /// Group routes by transit type for sectioned display.
    private var groupedRoutes: [(type: TransitType, routes: [Route])] {
        var groups: [(TransitType, [Route])] = []
        for type in TransitType.allCases {
            let matching = filteredRoutes.filter { $0.transitType == type }
            if !matching.isEmpty {
                groups.append((type, matching))
            }
        }
        return groups
    }

    private var hasMultipleTransitTypes: Bool { groupedRoutes.count > 1 }

    // MARK: - Body

    var body: some View {
        if store.isLoading && store.routes.isEmpty {
            loadingState
        } else if filteredRoutes.isEmpty {
            emptyState
        } else {
            routesScrollView
        }
    }

    // MARK: - Loading

    private var loadingState: some View {
        VStack(spacing: 14) {
            Spacer()
            ProgressView()
                .tint(AppTheme.accent)
            Text(String(localized: "lines_loading"))
                .font(.subheadline)
                .foregroundStyle(AppTheme.textTertiary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Empty

    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer()
            LucideIcon.train.image
                .font(.system(size: 28))
                .foregroundStyle(AppTheme.textTertiary)
            Text(String(localized: "lines_no_result"))
                .font(.headline)
                .foregroundStyle(AppTheme.textPrimary)
            Text(String(localized: "lines_no_result_hint"))
                .font(.subheadline)
                .foregroundStyle(AppTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Routes List

    private var routesScrollView: some View {
        ScrollView(showsIndicators: false) {
            LazyVStack(spacing: 16) {
                // Result count when searching
                if !searchQuery.isEmpty {
                    Text(String(localized: "lines_result_count \(filteredRoutes.count)"))
                        .font(.caption)
                        .foregroundStyle(AppTheme.textTertiary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 4)
                }

                ForEach(groupedRoutes, id: \.type) { group in
                    VStack(alignment: .leading, spacing: 10) {
                        // Category header — only shown when multiple transit types are present
                        if hasMultipleTransitTypes {
                            Button {
                                withAnimation(.easeInOut(duration: 0.25)) {
                                    if collapsedTypes.contains(group.type) {
                                        collapsedTypes.remove(group.type)
                                    } else {
                                        collapsedTypes.insert(group.type)
                                    }
                                }
                            } label: {
                                HStack(spacing: 8) {
                                    group.type.icon.image
                                        .font(.footnote.weight(.semibold))
                                        .foregroundStyle(AppTheme.textSecondary)
                                    Text(group.type.displayName)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundStyle(AppTheme.textSecondary)
                                    Text("\(group.routes.count)")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundStyle(AppTheme.textTertiary)
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(AppTheme.textTertiary.opacity(0.1), in: Capsule())
                                    Spacer()
                                    (collapsedTypes.contains(group.type) ? LucideIcon.chevronRight : LucideIcon.chevronDown).image
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(AppTheme.textTertiary)
                                }
                                .padding(.horizontal, 4)
                                .frame(minHeight: 44)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                        }

                        // Route rows — always visible when only one type; collapsible otherwise
                        if !hasMultipleTransitTypes || !collapsedTypes.contains(group.type) {
                            VStack(spacing: 8) {
                                ForEach(group.routes) { route in
                                    NavigationLink(value: route) {
                                        LineRowContent(route: route, hasMultipleTypes: hasMultipleTransitTypes)
                                    }
                                    .buttonStyle(.plain)
                                    .accessibilityIdentifier("line_row_\(route.id)")
                                }
                            }
                            .transition(.opacity.combined(with: .move(edge: .top)))
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 4)
            .padding(.bottom, 80)
        }
        .scrollDismissesKeyboard(.interactively)
    }
}

// MARK: - Line Row Content (glass card)

private struct LineRowContent: View {
    let route: Route
    let hasMultipleTypes: Bool

    private var lineColor: Color {
        // Use luminance to detect very light colors that would be invisible against the card background
        let textOnColor = contrastingTextColor(for: route.color)
        // If white text is needed on this color, the color is dark enough to use as-is
        // If black text is needed, the color is very light — fall back to accent
        return textOnColor == "#FFFFFF" ? Color(hex: route.color) : AppTheme.accent
    }

    var body: some View {
        HStack(spacing: 12) {
            // Line badge
            LineBadge(
                lineName: route.name,
                color: route.color,
                textColor: route.textColor,
                transitType: route.transitType,
                size: .medium
            )

            VStack(alignment: .leading, spacing: 2) {
                // Route long name
                Text(route.longName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)

                // Subtitle: transit type (only when multiple types) + direction count (only when >1)
                let showTransitType = hasMultipleTypes
                let showDirections = route.directions.count > 1
                if showTransitType || showDirections {
                    HStack(spacing: 6) {
                        if showTransitType {
                            route.transitType.icon.image
                                .font(.system(size: 10, weight: .medium))
                                .foregroundStyle(AppTheme.textTertiary)
                            Text(route.transitType.displayName)
                                .font(.caption2.weight(.medium))
                                .foregroundStyle(AppTheme.textTertiary)
                        }

                        if showDirections {
                            if showTransitType {
                                Text("·")
                                    .font(.caption2)
                                    .foregroundStyle(AppTheme.textTertiary)
                            }
                            Text("↔ \(route.directions.count)")
                                .font(.caption2.weight(.medium))
                                .foregroundStyle(AppTheme.textTertiary)
                        }
                    }
                }
            }

            Spacer(minLength: 8)

            LucideIcon.chevronRight.image
                .font(.caption.weight(.semibold))
                .foregroundStyle(AppTheme.textTertiary)
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 12)
        .background(AppTheme.glassFill, in: RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(AppTheme.glassBorder, lineWidth: 1)
        )
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(String(localized: "line_badge_a11y \(route.name)"))
        .accessibilityHint(String(localized: "a11y_hint_show_line_stops"))
        .accessibilityAddTraits(.isButton)
    }
}
