import SwiftUI

// MARK: - OperatorInfoView

/// Detailed operator information: name, region, website, contact, and data attribution.
struct OperatorInfoView: View {
    let config: OperatorConfig

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {

                // MARK: Operator Identity
                GlassCard(cornerRadius: 16) {
                    VStack(spacing: 16) {
                        // Icon + name
                        RoundedRectangle(cornerRadius: 14)
                            .fill(AppTheme.primary.opacity(0.12))
                            .frame(width: 64, height: 64)
                            .overlay(
                                LucideIcon.bus.image
                                    .font(.title2)
                                    .foregroundStyle(AppTheme.primary)
                            )

                        VStack(spacing: 4) {
                            Text(config.name)
                                .font(.title3.weight(.bold))
                                .foregroundStyle(AppTheme.textPrimary)
                            Text(config.fullName)
                                .font(.subheadline)
                                .foregroundStyle(AppTheme.textSecondary)
                                .multilineTextAlignment(.center)
                        }

                        // Region / Country pill
                        HStack(spacing: 6) {
                            LucideIcon.mapPin.image
                                .font(.caption2)
                            Text("\(config.region), \(config.country)")
                                .font(.caption.weight(.medium))
                        }
                        .foregroundStyle(AppTheme.textTertiary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(
                            Capsule()
                                .fill(AppTheme.bgSecondary)
                                .overlay(
                                    Capsule()
                                        .strokeBorder(AppTheme.glassBorder, lineWidth: 1)
                                )
                        )
                    }
                    .frame(maxWidth: .infinity)
                    .padding(20)
                }

                // MARK: Links
                GlassCard(cornerRadius: 16) {
                    VStack(spacing: 0) {
                        // Website
                        if let url = URL(string: config.url) {
                            Link(destination: url) {
                                linkRow(
                                    icon: .globe,
                                    title: String(localized: "operator_website"),
                                    subtitle: config.url
                                        .replacingOccurrences(of: "https://", with: "")
                                        .replacingOccurrences(of: "http://", with: "")
                                )
                            }
                        }

                        // Phone
                        if let phone = config.contact?.phone {
                            Rectangle()
                                .fill(AppTheme.separatorLine)
                                .frame(height: 0.5)
                                .padding(.leading, 48)
                            if let phoneURL = URL(string: "tel:\(phone)") {
                                Link(destination: phoneURL) {
                                    linkRow(
                                        icon: .phone,
                                        title: String(localized: "operator_phone"),
                                        subtitle: phone
                                    )
                                }
                            }
                        }

                        // Email
                        if let email = config.contact?.email {
                            Rectangle()
                                .fill(AppTheme.separatorLine)
                                .frame(height: 0.5)
                                .padding(.leading, 48)
                            if let mailURL = URL(string: "mailto:\(email)") {
                                Link(destination: mailURL) {
                                    linkRow(
                                        icon: .mail,
                                        title: String(localized: "operator_email"),
                                        subtitle: email
                                    )
                                }
                            }
                        }

                        // Privacy policy
                        if let privacyUrl = config.privacyUrl,
                           let url = URL(string: privacyUrl) {
                            Rectangle()
                                .fill(AppTheme.separatorLine)
                                .frame(height: 0.5)
                                .padding(.leading, 48)
                            Link(destination: url) {
                                linkRow(
                                    icon: .shield,
                                    title: String(localized: "operator_privacy_policy"),
                                    subtitle: nil
                                )
                            }
                        }
                    }
                }

                // MARK: Data Attribution
                GlassCard(cornerRadius: 16) {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(spacing: 8) {
                            LucideIcon.table.image
                                .font(.subheadline)
                                .foregroundStyle(AppTheme.accent)
                            Text(String(localized: "operator_data_attribution"))
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(AppTheme.textPrimary)
                        }

                        Text(String(localized: "operator_gtfs_description"))
                            .font(.caption)
                            .foregroundStyle(AppTheme.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                            .lineSpacing(3)
                    }
                    .padding(16)
                }

                Spacer(minLength: 32)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle(String(localized: "nav_title_operator"))
        .navigationBarTitleDisplayMode(.large)
    }

    // MARK: - Link Row

    private func linkRow(icon: LucideIcon, title: String, subtitle: String?) -> some View {
        HStack(spacing: 12) {
            icon.image
                .font(.body)
                .foregroundStyle(AppTheme.accent)
                .frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AppTheme.textPrimary)
                if let subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(AppTheme.textSecondary)
                        .lineLimit(1)
                }
            }
            Spacer()
            LucideIcon.externalLink.image
                .font(.caption2.weight(.semibold))
                .foregroundStyle(AppTheme.textTertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }
}
