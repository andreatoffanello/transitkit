import SwiftUI

// MARK: - FareInfoView

/// Displays detailed fare information from the operator config.
/// Table-like layout with fare type, price, and optional notes.
struct FareInfoView: View {
    let fares: FareInfo
    let operatorUrl: String

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {

                // MARK: Fare Table
                GlassCard(cornerRadius: 16) {
                    VStack(alignment: .leading, spacing: 0) {
                        // Header row
                        HStack {
                            Text(String(localized: "fare_column_type"))
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(AppTheme.textTertiary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            Text(String(localized: "fare_column_price"))
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(AppTheme.textTertiary)
                                .frame(width: 80, alignment: .trailing)
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 14)
                        .padding(.bottom, 10)

                        Rectangle()
                            .fill(AppTheme.separatorLine)
                            .frame(height: 1)

                        // Fare rows
                        ForEach(Array(fares.types.enumerated()), id: \.element.id) { index, fare in
                            VStack(alignment: .leading, spacing: 4) {
                                HStack(alignment: .top) {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(fare.name)
                                            .font(.subheadline.weight(.medium))
                                            .foregroundStyle(AppTheme.textPrimary)
                                        if let notes = fare.notes {
                                            Text(notes)
                                                .font(.caption)
                                                .foregroundStyle(AppTheme.textSecondary)
                                        }
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)

                                    Text(fare.price)
                                        .font(.subheadline.weight(.bold))
                                        .foregroundStyle(AppTheme.accent)
                                        .frame(width: 80, alignment: .trailing)
                                }
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)

                            if index < fares.types.count - 1 {
                                Rectangle()
                                    .fill(AppTheme.separatorLine)
                                    .frame(height: 0.5)
                                    .padding(.leading, 16)
                            }
                        }
                        .padding(.bottom, 2)
                    }
                }

                // MARK: Notes
                if let notes = fares.notes {
                    GlassCard(cornerRadius: 16) {
                        HStack(alignment: .top, spacing: 12) {
                            LucideIcon.info.image
                                .font(.body)
                                .foregroundStyle(AppTheme.accent)
                            Text(notes)
                                .font(.subheadline)
                                .foregroundStyle(AppTheme.textSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .padding(16)
                    }
                }

                // MARK: Purchase Link
                if let purchaseUrl = fares.purchaseUrl,
                   let url = URL(string: purchaseUrl) {
                    Link(destination: url) {
                        GlassCard(cornerRadius: 16) {
                            HStack(spacing: 12) {
                                LucideIcon.ticket.image
                                    .font(.body.weight(.semibold))
                                    .foregroundStyle(AppTheme.accent)
                                Text(String(localized: "fare_buy_online"))
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(AppTheme.accent)
                                Spacer()
                                LucideIcon.externalLink.image
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(AppTheme.textTertiary)
                            }
                            .padding(16)
                        }
                    }
                } else if let url = URL(string: operatorUrl) {
                    Link(destination: url) {
                        GlassCard(cornerRadius: 16) {
                            HStack(spacing: 12) {
                                LucideIcon.globe.image
                                    .font(.body.weight(.semibold))
                                    .foregroundStyle(AppTheme.accent)
                                Text(String(localized: "fare_visit_website"))
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(AppTheme.accent)
                                Spacer()
                                LucideIcon.externalLink.image
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(AppTheme.textTertiary)
                            }
                            .padding(16)
                        }
                    }
                }

                Spacer(minLength: 32)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle(String(localized: "nav_title_fares"))
        .navigationBarTitleDisplayMode(.large)
    }
}
