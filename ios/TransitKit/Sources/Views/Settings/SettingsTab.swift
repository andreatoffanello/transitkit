import SwiftUI

// MARK: - SettingsTab

/// Main settings tab with favorites, notifications, language, and about sections.
/// Content visibility is driven by `OperatorConfig.features`.
struct SettingsTab: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(FavoritesManager.self) private var favoritesManager

    @AppStorage("notificationsEnabled") private var notificationsEnabled = false

    private var config: OperatorConfig? {
        try? ConfigLoader.load()
    }

    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(version) (\(build))"
    }

    var body: some View {
        NavigationStack {
            List {
                if let config {
                    // MARK: Favorites
                    if config.features.enableFavorites {
                        Section {
                            NavigationLink {
                                FavoritesListView()
                            } label: {
                                settingsRow(
                                    icon: .star,
                                    iconColor: .yellow,
                                    title: String(localized: "settings_favorites"),
                                    detail: favoritesManager.favoriteStopIds.isEmpty
                                        ? String(localized: "settings_favorites_none")
                                        : String(format: NSLocalizedString("settings_favorites_count", comment: ""), favoritesManager.favoriteStopIds.count)
                                )
                            }
                        } header: {
                            Text(String(localized: "settings_section_favorites"))
                        }
                    }

                    // MARK: Notifications
                    if config.features.enableNotifications {
                        Section {
                            Toggle(isOn: $notificationsEnabled) {
                                settingsRow(
                                    icon: .bell,
                                    iconColor: AppTheme.accent,
                                    title: String(localized: "settings_notifications"),
                                    detail: nil
                                )
                            }
                            .tint(AppTheme.accent)
                        } header: {
                            Text(String(localized: "settings_section_notifications"))
                        } footer: {
                            Text(String(localized: "settings_notifications_footer"))
                        }
                    }

                    // MARK: Language
                    Section {
                        settingsRow(
                            icon: .globe,
                            iconColor: AppTheme.accent,
                            title: String(localized: "settings_language"),
                            detail: currentLanguageLabel
                        )
                    } header: {
                        Text(String(localized: "settings_section_language"))
                    } footer: {
                        Text(String(localized: "settings_language_footer"))
                    }

                    // MARK: About
                    Section {
                        NavigationLink {
                            AboutView(config: config)
                        } label: {
                            settingsRow(
                                icon: .info,
                                iconColor: AppTheme.textSecondary,
                                title: String(localized: "settings_about"),
                                detail: nil
                            )
                        }

                        HStack {
                            settingsRow(
                                icon: .shield,
                                iconColor: AppTheme.textTertiary,
                                title: String(localized: "settings_version"),
                                detail: nil
                            )
                            Spacer()
                            Text(appVersion)
                                .font(.subheadline)
                                .foregroundStyle(AppTheme.textTertiary)
                        }
                    } header: {
                        Text(String(localized: "settings_section_about"))
                    }
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle(String(localized: "tab_settings"))
            .navigationBarTitleDisplayMode(.large)
        }
    }

    // MARK: - Settings Row

    private func settingsRow(
        icon: LucideIcon,
        iconColor: Color,
        title: String,
        detail: String?
    ) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 8)
                .fill(iconColor.opacity(0.12))
                .frame(width: 32, height: 32)
                .overlay(
                    icon.sized(14)
                        .foregroundStyle(iconColor)
                )
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AppTheme.textPrimary)
                if let detail {
                    Text(detail)
                        .font(.caption)
                        .foregroundStyle(AppTheme.textSecondary)
                }
            }
        }
    }

    // MARK: - Language Label

    private var currentLanguageLabel: String {
        let preferred = Locale.preferredLanguages.first ?? "en"
        let locale = Locale(identifier: preferred)
        return locale.localizedString(forLanguageCode: preferred)?.capitalized ?? preferred
    }
}
