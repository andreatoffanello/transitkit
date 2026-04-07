import SwiftUI

// MARK: - InfoTab

/// Main tab for ticket/fare information, points of sale, and operator details.
/// Content is driven entirely by `OperatorConfig` — no hardcoded operator names.
struct InfoTab: View {
    @Environment(ScheduleStore.self) private var store

    private var config: OperatorConfig? {
        try? ConfigLoader.load()
    }

    var body: some View {
        NavigationStack {
            if let config {
                scrollContent(config)
            } else {
                Text(String(localized: "error_loading"))
                    .foregroundStyle(AppTheme.textSecondary)
            }
        }
    }

    private func scrollContent(_ config: OperatorConfig) -> some View {
        ScrollView {
            VStack(spacing: 20) {

                // MARK: Tickets & Fares
                if let fares = config.fares, !fares.types.isEmpty {
                    sectionHeader(
                        title: String(localized: "info_section_fares"),
                        lucideIcon: .ticket
                    )
                    NavigationLink {
                        FareInfoView(fares: fares, operatorUrl: config.url)
                    } label: {
                        faresSummaryCard(fares)
                    }
                    .buttonStyle(.plain)
                }

                // MARK: Points of Sale
                if let pos = config.pointsOfSale, !pos.isEmpty {
                    sectionHeader(
                        title: String(localized: "info_section_points_of_sale"),
                        lucideIcon: .mapPin
                    )
                    pointsOfSaleCard(pos)
                }

                // MARK: Operator Info
                sectionHeader(
                    title: String(localized: "info_section_operator"),
                    lucideIcon: .info
                )
                NavigationLink {
                    OperatorInfoView(config: config)
                } label: {
                    operatorCard(config)
                }
                .buttonStyle(.plain)

                // MARK: Data Info
                sectionHeader(
                    title: String(localized: "info_section_data"),
                    lucideIcon: .list
                )
                dataInfoCard

                Spacer(minLength: 32)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle(String(localized: "tab_info"))
        .navigationBarTitleDisplayMode(.large)
    }

    // MARK: - Section Header

    private func sectionHeader(title: String, lucideIcon: LucideIcon? = nil) -> some View {
        HStack(spacing: 8) {
            if let lucideIcon {
                lucideIcon.image
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.accent)
            }
            Text(title)
                .font(.headline)
                .foregroundStyle(AppTheme.textPrimary)
            Spacer()
        }
        .padding(.top, 8)
    }

    // MARK: - Fares Summary Card

    private func faresSummaryCard(_ fares: FareInfo) -> some View {
        GlassCard(cornerRadius: 16) {
            VStack(alignment: .leading, spacing: 12) {
                // Show first 3 fare types as preview
                ForEach(Array(fares.types.prefix(3).enumerated()), id: \.element.id) { index, fare in
                    HStack {
                        Text(fare.name)
                            .font(.subheadline)
                            .foregroundStyle(AppTheme.textPrimary)
                        Spacer()
                        Text(fare.price)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(AppTheme.accent)
                    }
                    if index < min(fares.types.count, 3) - 1 {
                        Rectangle()
                            .fill(AppTheme.separatorLine)
                            .frame(height: 0.5)
                    }
                }

                if fares.types.count > 3 {
                    HStack {
                        Spacer()
                        Text(String(localized: "info_see_all_fares"))
                            .font(.caption.weight(.medium))
                            .foregroundStyle(AppTheme.accent)
                        LucideIcon.chevronRight.image
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(AppTheme.accent)
                    }
                }
            }
            .padding(16)
        }
    }

    // MARK: - Points of Sale Card

    private func pointsOfSaleCard(_ locations: [PointOfSale]) -> some View {
        GlassCard(cornerRadius: 16) {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(locations.enumerated()), id: \.element.id) { index, location in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 10) {
                            LucideIcon.mapPin.image
                                .font(.body)
                                .foregroundStyle(AppTheme.accent)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(location.name)
                                    .font(.subheadline.weight(.medium))
                                    .foregroundStyle(AppTheme.textPrimary)
                                if let address = location.address {
                                    Text(address)
                                        .font(.caption)
                                        .foregroundStyle(AppTheme.textSecondary)
                                }
                                if let hours = location.hours {
                                    Text(hours)
                                        .font(.caption)
                                        .foregroundStyle(AppTheme.textTertiary)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)

                    if index < locations.count - 1 {
                        Rectangle()
                            .fill(AppTheme.separatorLine)
                            .frame(height: 0.5)
                            .padding(.leading, 42)
                    }
                }
            }
        }
    }

    // MARK: - Operator Card

    private func operatorCard(_ config: OperatorConfig) -> some View {
        GlassCard(cornerRadius: 16) {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 10)
                    .fill(AppTheme.primary.opacity(0.12))
                    .frame(width: 44, height: 44)
                    .overlay(
                        LucideIcon.bus.image
                            .font(.body)
                            .foregroundStyle(AppTheme.primary)
                    )
                VStack(alignment: .leading, spacing: 2) {
                    Text(config.name)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                    Text(config.fullName)
                        .font(.caption)
                        .foregroundStyle(AppTheme.textSecondary)
                        .lineLimit(2)
                }
                Spacer()
                LucideIcon.chevronRight.image
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(16)
        }
    }

    // MARK: - Date Formatting Helpers

    /// Formats ISO8601 datetime string (e.g. "2026-04-01T20:42:52Z") to locale date.
    private func formatISO8601Date(_ raw: String) -> String {
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime]
        if let date = iso.date(from: raw) {
            return date.formatted(date: .abbreviated, time: .omitted)
        }
        // Fallback: strip the time portion for display
        return String(raw.prefix(10))
    }

    /// Formats GTFS compact date (YYYYMMDD) to locale date.
    private func formatGTFSDate(_ raw: String) -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyyMMdd"
        if let date = fmt.date(from: raw) {
            return date.formatted(date: .abbreviated, time: .omitted)
        }
        return raw
    }

    // MARK: - Data Info Card

    private var dataInfoCard: some View {
        GlassCard(cornerRadius: 16) {
            VStack(alignment: .leading, spacing: 12) {
                if let lastUpdated = store.lastUpdated {
                    dataRow(
                        label: String(localized: "info_data_last_updated"),
                        value: formatISO8601Date(lastUpdated),
                        icon: .clock
                    )
                }
                // validUntil not available in new API response
                Rectangle()
                    .fill(AppTheme.separatorLine)
                    .frame(height: 0.5)
                dataRow(
                    label: String(localized: "info_data_source"),
                    value: "GTFS",
                    icon: .table
                )
            }
            .padding(16)
        }
    }

    private func dataRow(label: String, value: String, icon: LucideIcon) -> some View {
        HStack(spacing: 10) {
            icon.image
                .font(.caption)
                .foregroundStyle(AppTheme.textTertiary)
                .frame(width: 20)
            Text(label)
                .font(.caption)
                .foregroundStyle(AppTheme.textSecondary)
            Spacer()
            Text(value)
                .font(.caption.weight(.medium))
                .foregroundStyle(AppTheme.textPrimary)
        }
    }
}
