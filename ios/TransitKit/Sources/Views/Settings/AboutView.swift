import SwiftUI

// MARK: - AboutView

/// App about screen showing operator branding, version, credits,
/// and links to privacy policy and website.
struct AboutView: View {
    let config: OperatorConfig

    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(version) (\(build))"
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {

                // MARK: App Identity
                GlassCard(cornerRadius: 20) {
                    VStack(spacing: 16) {
                        // App icon placeholder — uses operator primary color
                        RoundedRectangle(cornerRadius: 18)
                            .fill(AppTheme.primary.opacity(0.15))
                            .frame(width: 80, height: 80)
                            .overlay(
                                LucideIcon.bus.sized(32)
                                    .foregroundStyle(AppTheme.primary)
                            )

                        VStack(spacing: 4) {
                            Text(config.store.title)
                                .font(.title2.weight(.bold))
                                .foregroundStyle(AppTheme.textPrimary)
                            Text(config.store.subtitle)
                                .font(.subheadline)
                                .foregroundStyle(AppTheme.textSecondary)
                                .multilineTextAlignment(.center)
                        }

                        Text(String(format: NSLocalizedString("about_version", comment: ""), appVersion))
                            .font(.caption.weight(.medium))
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
                    .padding(24)
                }

                // MARK: Powered by TransitKit
                GlassCard(cornerRadius: 16) {
                    HStack(spacing: 12) {
                        LucideIcon.settings.image
                            .font(.body)
                            .foregroundStyle(AppTheme.accent)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(String(localized: "about_powered_by"))
                                .font(.caption)
                                .foregroundStyle(AppTheme.textTertiary)
                            Text("TransitKit")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(AppTheme.textPrimary)
                        }
                        Spacer()
                    }
                    .padding(16)
                }

                // MARK: Links
                GlassCard(cornerRadius: 16) {
                    VStack(spacing: 0) {
                        // Operator website
                        if let url = URL(string: config.url) {
                            Link(destination: url) {
                                aboutLinkRow(
                                    icon: .globe,
                                    title: String(localized: "about_operator_website"),
                                    subtitle: config.name
                                )
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
                                aboutLinkRow(
                                    icon: .shield,
                                    title: String(localized: "about_privacy_policy"),
                                    subtitle: nil
                                )
                            }
                        }
                    }
                }

                // MARK: Open Source
                GlassCard(cornerRadius: 16) {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(spacing: 8) {
                            LucideIcon.list.image
                                .font(.subheadline)
                                .foregroundStyle(AppTheme.accent)
                            Text(String(localized: "about_licenses"))
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(AppTheme.textPrimary)
                        }
                        Text(String(localized: "about_licenses_description"))
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
        .navigationTitle(String(localized: "nav_title_about"))
        .navigationBarTitleDisplayMode(.large)
        .accessibilityIdentifier("about_view")
    }

    // MARK: - Link Row

    private func aboutLinkRow(icon: LucideIcon, title: String, subtitle: String?) -> some View {
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
