import SwiftUI

/// Non-blocking update banner shown in HomeTab when a newer App Store version
/// is available. Mirrors `SoftUpdateBanner` from DoVe, adapted to use
/// TransitKit's `AppTheme` accent color and `LucideIcon`.
///
/// Two actions:
/// - "Update" → opens App Store via `AppUpdateChecker.shared.openStore(_:)`
/// - "Later"  → calls `dismissSoftUpdate()` — persists per-version, no re-nag
struct SoftUpdateBanner: View {
    let softUpdate: AppUpdateChecker.SoftUpdate

    private var accentColor: Color { AppTheme.accent }

    private var tintedBackground: Color {
        Color(UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(AppTheme.accent).withAlphaComponent(0.18)
                : UIColor(AppTheme.accent).withAlphaComponent(0.10)
        })
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 10) {
                LucideIcon.refreshCw.sized(18)
                    .foregroundStyle(accentColor)

                Text(String(localized: "update_available_title"))
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.primary)

                Spacer()
            }

            Text(softUpdate.message)
                .font(.system(size: 14))
                .foregroundStyle(.secondary)
                .lineLimit(4)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 12) {
                Button {
                    AppUpdateChecker.shared.openStore(softUpdate.storeUrl)
                } label: {
                    Text(String(localized: "update_now_label"))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 8)
                        .background(accentColor, in: Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("btn_soft_update")

                Button {
                    AppUpdateChecker.shared.dismissSoftUpdate()
                } label: {
                    Text(String(localized: "update_later_label"))
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(.secondary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("btn_soft_update_later")

                Spacer()
            }
        }
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: 16)
                .fill(.ultraThinMaterial)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(tintedBackground)
                )
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("soft_update_banner")
    }
}
