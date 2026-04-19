import SwiftUI
import UIKit

// MARK: - AccessibilityInfoView

/// Operator-wide accessibility statement: title, description, feature bullets,
/// and an optional "Learn more" link.
struct AccessibilityInfoView: View {
    let info: AccessibilityInfo

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                hero

                GlassCard(cornerRadius: 16) {
                    Text(info.description.resolved())
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineSpacing(4)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(16)
                }

                GlassCard(cornerRadius: 16) {
                    VStack(alignment: .leading, spacing: 10) {
                        ForEach(Array(info.bullets.enumerated()), id: \.offset) { _, bullet in
                            HStack(alignment: .top, spacing: 10) {
                                Circle()
                                    .fill(AppTheme.accent)
                                    .frame(width: 6, height: 6)
                                    .padding(.top, 7)
                                Text(bullet.resolved())
                                    .font(.subheadline)
                                    .foregroundStyle(AppTheme.textPrimary)
                                    .lineSpacing(3)
                                    .fixedSize(horizontal: false, vertical: true)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                    }
                    .padding(16)
                }

                if let moreUrl = info.moreUrl, let url = URL(string: moreUrl) {
                    Button {
                        UIApplication.shared.open(url)
                    } label: {
                        GlassCard(cornerRadius: 16) {
                            HStack(spacing: 12) {
                                LucideIcon.externalLink.image
                                    .font(.body.weight(.semibold))
                                    .foregroundStyle(AppTheme.accent)
                                Text(String(localized: "services_learn_more"))
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(AppTheme.accent)
                                Spacer()
                                LucideIcon.chevronRight.sized(18)
                                    .foregroundStyle(AppTheme.textTertiary)
                            }
                            .padding(16)
                        }
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("accessibility_more_link")
                }

                Spacer(minLength: 32)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) { EmptyView() }
        }
        .toolbar(.hidden, for: .tabBar)
    }

    // MARK: Hero

    private var hero: some View {
        VStack(alignment: .leading, spacing: 0) {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(AppTheme.accent.opacity(0.14))
                .frame(width: 72, height: 72)
                .overlay(
                    LucideIcon.accessibility.sized(34)
                        .foregroundStyle(AppTheme.accent)
                )
                .padding(.bottom, 16)
            Text(info.title.resolved())
                .font(.system(size: 34, weight: .bold))
                .foregroundStyle(AppTheme.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
        .padding(.top, 4)
    }
}
